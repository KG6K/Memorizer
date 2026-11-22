package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Label shown in the floating preview bubble while fast-scrolling a library list.
 */
@Immutable
data class FastScrollLabel(
    val title: String,
    val positionBadge: String? = null
)

/**
 * Reusable Material 3 right-edge fast-scroll navigation slider for [LazyListState].
 *
 * - Hidden completely when the list fits within a single viewport.
 * - Auto-shows whenever the user scrolls or drags, then smoothly fades out after ~1.4 seconds idle.
 * - Supports instant right-edge vertical drag scrubbing without blocking taps on row overflow buttons.
 * - Displays a floating preview bubble aligned beside the thumb while dragging.
 */
@Composable
fun FastScrollBarOverlay(
    listState: LazyListState,
    labels: List<FastScrollLabel>,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "fast_scroll"
) {
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    if (!canScroll) return

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var activeDragIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(false) }

    val latestLabels by rememberUpdatedState(labels)

    // Auto-show on scroll or drag; fade out after ~1.4s of idle time.
    LaunchedEffect(listState.isScrollInProgress, isDragging) {
        if (listState.isScrollInProgress || isDragging) {
            isVisible = true
        } else if (isVisible) {
            delay(1400L)
            isVisible = false
        }
    }

    val scrollFraction by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            if (totalItems <= 1 || visibleItems.isEmpty()) {
                0f
            } else {
                val firstVisible = visibleItems.first()
                val firstItemFraction = if (firstVisible.size > 0) {
                    (-firstVisible.offset.toFloat() / firstVisible.size.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                // Count how many items fit completely inside the viewport to let the thumb reach 1.0 cleanly
                val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                val avgItemHeight = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)
                val estimatedVisibleCount = (viewportHeight / avgItemHeight).coerceAtLeast(1f)
                val maxScrollIndex = (totalItems - estimatedVisibleCount).coerceAtLeast(1f)
                ((firstVisible.index + firstItemFraction) / maxScrollIndex).coerceIn(0f, 1f)
            }
        }
    }

    val displayedFraction = if (isDragging) dragFraction else scrollFraction

    val handleAlpha by animateFloatAsState(
        targetValue = if (isVisible || isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible || isDragging) 160 else 280),
        label = "fast_scroll_alpha"
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.dp,
        animationSpec = tween(durationMillis = 150),
        label = "fast_scroll_thumb_width"
    )

    val thumbHeightDp = 52.dp
    val trackVerticalPaddingDp = 10.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val verticalPadPx = with(density) { trackVerticalPaddingDp.toPx() }
        val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
        val usableTrackPx = (totalHeightPx - verticalPadPx * 2f - thumbHeightPx).coerceAtLeast(1f)
        val thumbTopPx = verticalPadPx + displayedFraction * usableTrackPx

        fun scrubToOffsetY(offsetY: Float) {
            val totalItems = listState.layoutInfo.totalItemsCount.coerceAtLeast(latestLabels.size)
            if (totalItems <= 1) return
            val clampedY = (offsetY - verticalPadPx - thumbHeightPx / 2f).coerceIn(0f, usableTrackPx)
            val fraction = (clampedY / usableTrackPx).coerceIn(0f, 1f)
            dragFraction = fraction

            val targetIndex = (fraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
            activeDragIndex = targetIndex
            coroutineScope.launch {
                listState.scrollToItem(targetIndex)
            }
        }

        // Floating preview bubble aligned beside the thumb while dragging
        val currentLabel = remember(activeDragIndex, latestLabels) {
            if (latestLabels.isEmpty()) {
                null
            } else {
                val safeIdx = activeDragIndex.coerceIn(0, latestLabels.lastIndex)
                latestLabels[safeIdx]
            }
        }
        val bubbleHeightPx = with(density) { 40.dp.toPx() }
        val bubbleTopPx = (thumbTopPx + (thumbHeightPx - bubbleHeightPx) / 2f)
            .coerceIn(verticalPadPx, (totalHeightPx - verticalPadPx - bubbleHeightPx).coerceAtLeast(verticalPadPx))

        AnimatedVisibility(
            visible = isDragging && currentLabel != null,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.92f, animationSpec = tween(120)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f, animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(x = 0, y = bubbleTopPx.roundToInt()) }
                .padding(end = 36.dp)
        ) {
            if (currentLabel != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.testTag("${testTagPrefix}_bubble")
                ) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentLabel.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 170.dp)
                        )
                        if (!currentLabel.positionBadge.isNullOrBlank()) {
                            Text(
                                text = currentLabel.positionBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Right-edge vertical drag track (vertical drags scrub; single taps pass through to underlying rows)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
                .testTag("${testTagPrefix}_track")
                .pointerInput(listState, totalHeightPx) {
                    var currentPointerY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { startOffset ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isDragging = true
                            isVisible = true
                            currentPointerY = startOffset.y
                            scrubToOffsetY(currentPointerY)
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            currentPointerY = (currentPointerY + dragAmount).coerceIn(0f, totalHeightPx)
                            scrubToOffsetY(currentPointerY)
                        },
                        onDragEnd = {
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            // Subtle track line shown while dragging
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp, top = trackVerticalPaddingDp, bottom = trackVerticalPaddingDp)
                        .fillMaxHeight()
                        .width(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f))
                )
            }

            // Draggable pill thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(x = 0, y = thumbTopPx.roundToInt()) }
                    .padding(end = 4.dp)
                    .width(thumbWidth)
                    .height(thumbHeightDp)
                    .alpha(handleAlpha)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDragging) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
                        }
                    )
                    .testTag("${testTagPrefix}_thumb")
            )
        }
    }
}
