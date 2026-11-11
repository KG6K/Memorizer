package com.example.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AfterLoopAction
import com.example.model.PlaybackPhase
import com.example.player.PlayerUiState
import com.example.player.formatDelayPreciseMillis
import com.example.player.formatDurationMillis
import com.example.player.formatSleepTimerMillis
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioPlayerScreen(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onResetLoopProgress: () -> Unit,
    onSkipDelayNow: () -> Unit,
    onAddExtraDelay: () -> Unit,
    onSetRepeatCount: (Int) -> Unit,
    onIncrementRepeatCount: (Int) -> Unit,
    onSetDelaySeconds: (Int) -> Unit,
    onIncrementDelaySeconds: (Int) -> Unit,
    onSetMatchTrackDuration: (Boolean) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAfterLoopAction: (AfterLoopAction) -> Unit,
    onMarkSectionStart: () -> Unit,
    onMarkSectionEnd: () -> Unit,
    onClearAbSection: () -> Unit,
    onSaveConfigToSong: () -> Unit,
    onPickAudioFiles: () -> Unit,
    onOpenLibraryTab: () -> Unit,
    onSetVolumePercent: (Int) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onSetEqualizerBandLevel: (Short, Short) -> Unit = { _, _ -> },
    onSetEqualizerPreset: (String) -> Unit = {},
    onSetBassBoostStrength: (Int) -> Unit = {},
    onSetVirtualizerStrength: (Int) -> Unit = {},
    onSetShowEqualizer: (Boolean) -> Unit = {},
    onSetSleepTimer: (hours: Int, minutes: Int) -> Unit = { _, _ -> },
    onCancelSleepTimer: () -> Unit = {},
    onClosePlayer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVolumeGestureActive by remember { mutableStateOf(false) }
    var hideVolumeJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showTopMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val cfg = uiState.loopConfig
    val song = uiState.currentSong

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.isShowingEqualizer) {
                if (!uiState.isShowingEqualizer) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            if (offset.x > size.width * 0.45f) {
                                hideVolumeJob?.cancel()
                                isVolumeGestureActive = true
                            }
                        },
                        onDragEnd = {
                            hideVolumeJob = coroutineScope.launch {
                                delay(800)
                                isVolumeGestureActive = false
                            }
                        },
                        onDragCancel = {
                            hideVolumeJob = coroutineScope.launch {
                                delay(400)
                                isVolumeGestureActive = false
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (change.position.x > size.width * 0.45f) {
                                change.consume()
                                hideVolumeJob?.cancel()
                                isVolumeGestureActive = true
                                val deltaPercent = (-dragAmount / 3.2f).toInt()
                                if (deltaPercent != 0) {
                                    val newVol = (uiState.volumePercent + deltaPercent).coerceIn(0, 200)
                                    if (newVol != uiState.volumePercent) {
                                        onSetVolumePercent(newVol)
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Header Bar (Clean single-inset alignment below status bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClosePlayer,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("close_studio_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close studio player"
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.sleepTimerRemainingMs > 0L) {
                        Surface(
                            onClick = { showSleepTimerDialog = true },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("active_sleep_timer_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bedtime,
                                    contentDescription = "Sleep Timer Active",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formatSleepTimerMillis(uiState.sleepTimerRemainingMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showTopMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("player_top_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More player options"
                            )
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            DropdownMenuItem(
                                text = { Text("Equalizer", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Equalizer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = if (uiState.isShowingEqualizer) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    showTopMenu = false
                                    onSetShowEqualizer(!uiState.isShowingEqualizer)
                                },
                                modifier = Modifier.testTag("menu_item_equalizer")
                            )

                            DropdownMenuItem(
                                text = { Text("Playback Speed", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        text = "${cfg.playbackSpeed}x",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSpeedDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_playback_speed")
                            )

                            DropdownMenuItem(
                                text = { Text("Sleep Timer", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Bedtime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        text = if (uiState.sleepTimerRemainingMs > 0L) {
                                            formatSleepTimerMillis(uiState.sleepTimerRemainingMs)
                                        } else {
                                            "Off"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        color = if (uiState.sleepTimerRemainingMs > 0L) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = if (uiState.sleepTimerRemainingMs > 0L) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSleepTimerDialog = true
                                },
                                modifier = Modifier.testTag("menu_item_sleep_timer")
                            )
                        }
                    }
                }
            }

            // 2. Vertically Centered 1:1 Square Song Picture / Equalizer Zone (Never overlaps header or metadata)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                val squareSize = minOf(maxWidth, maxHeight)
                SquareSongPictureAndEqualizerCard(
                    uiState = uiState,
                    onSetShowEqualizer = onSetShowEqualizer,
                    onSetVolumePercent = onSetVolumePercent,
                    onSetEqualizerEnabled = onSetEqualizerEnabled,
                    onSetEqualizerBandLevel = onSetEqualizerBandLevel,
                    onSetEqualizerPreset = onSetEqualizerPreset,
                    modifier = Modifier.size(squareSize)
                )
            }

            // 3. Bottom-Anchored Metadata & Controls Section (Fixed height so square thumbnail position NEVER changes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Full-Width Track Metadata & Current Repetition Badge Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenLibraryTab() }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song?.title ?: "Select a Song to Begin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("now_playing_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song?.let {
                                listOfNotNull(
                                    it.artist.takeIf { a -> a.isNotBlank() },
                                    it.album.takeIf { al -> al.isNotBlank() }
                                ).joinToString(" • ")
                            } ?: "Tap to browse songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (cfg.isInfinite) Icons.Filled.AllInclusive else Icons.Filled.RepeatOne,
                                contentDescription = "Current loop iteration",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            val loopStatusText = if (cfg.isInfinite) {
                                "Rep ${uiState.currentIteration} / ∞"
                            } else {
                                "Rep ${uiState.currentIteration} / ${cfg.repeatCount}"
                            }
                            Text(
                                text = loopStatusText,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.testTag("repetition_counter_badge")
                            )
                        }
                    }
                }

                // Fixed-height single-line slot for Pause Delay progress bar (prevents any layout shift)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.phase == PlaybackPhase.WAITING_DELAY) {
                        DelayCountdownBanner(
                            uiState = uiState,
                            onSkipDelayNow = onSkipDelayNow,
                            onTogglePauseDelay = onTogglePlayPause,
                            onAddExtraDelay = onAddExtraDelay
                        )
                    }
                }

                // Repeat, Pause Delay, and Loop Mode Pills Row above the bottom controls
                LooperQuickActionsRow(
                    loopConfig = cfg,
                    onOpenRepeatDialog = { showRepeatDialog = true },
                    onOpenDelayDialog = { showDelayDialog = true },
                    onSetAfterLoopAction = onSetAfterLoopAction
                )

                // Timeline LineSlider + Time & Mode Row + Bottom Transport Buttons
                BottomTransportAndTimelineSection(
                    uiState = uiState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipBackward = onSkipBackward,
                    onSkipForward = onSkipForward,
                    onPreviousSong = onPreviousSong,
                    onNextSong = onNextSong,
                    onSetAfterLoopAction = onSetAfterLoopAction
                )
            }
        }

        // Floating Volume HUD Overlay that ONLY appears while swiping right side
        AnimatedVisibility(
            visible = isVolumeGestureActive,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            VlcVolumeHudCard(volumePercent = uiState.volumePercent)
        }
    }

    // Modal Dialogs
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = cfg.playbackSpeed,
            onSelectSpeed = onSetPlaybackSpeed,
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showRepeatDialog) {
        RepeatCountDialog(
            currentRepeatCount = cfg.repeatCount,
            onSetRepeatCount = onSetRepeatCount,
            onDismiss = { showRepeatDialog = false }
        )
    }

    if (showDelayDialog) {
        DelayIntervalDialog(
            delaySeconds = cfg.delaySeconds,
            matchTrackDuration = cfg.matchTrackDurationDelay,
            onSetDelaySeconds = onSetDelaySeconds,
            onSetMatchTrackDuration = onSetMatchTrackDuration,
            onDismiss = { showDelayDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = uiState.sleepTimerRemainingMs,
            onSetSleepTimer = onSetSleepTimer,
            onCancelSleepTimer = onCancelSleepTimer,
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

/**
 * Classic thin horizontal Line Slider with continuous 2dp track line, active line, and compact circular thumb.
 */
@Composable
fun LineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
    thumbColor: Color = activeColor,
    trackThickness: Dp = 2.dp,
    thumbRadius: Dp = 6.dp
) {
    val rangeSpan = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(28.dp)
            .pointerInput(valueRange.start, valueRange.endInclusive) {
                detectTapGestures { offset ->
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    val tapRatio = (offset.x / widthPx).coerceIn(0f, 1f)
                    val newVal = valueRange.start + tapRatio * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newVal)
                }
            }
            .pointerInput(valueRange.start, valueRange.endInclusive) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val dragRatio = (offset.x / widthPx).coerceIn(0f, 1f)
                        val newVal = valueRange.start + dragRatio * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newVal)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val dragRatio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val newVal = valueRange.start + dragRatio * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newVal)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = thumbRadius.toPx()
            val strokePx = trackThickness.toPx()
            val startX = radiusPx
            val endX = (size.width - radiusPx).coerceAtLeast(startX)
            val usableWidth = (endX - startX).coerceAtLeast(0f)
            val centerY = size.height / 2f
            val thumbX = startX + fraction * usableWidth

            // Inactive continuous thin line
            drawLine(
                color = inactiveColor,
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Active thin line
            drawLine(
                color = activeColor,
                start = Offset(startX, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = strokePx * 1.25f,
                cap = StrokeCap.Round
            )

            // Thumb circle
            drawCircle(
                color = thumbColor,
                radius = radiusPx,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

/**
 * 1:1 Square Song Picture & Equalizer Card.
 * Displays embedded artwork from the song (or blank surface if none), and flips to the 5-Band Equalizer.
 */
@Composable
private fun SquareSongPictureAndEqualizerCard(
    uiState: PlayerUiState,
    onSetShowEqualizer: (Boolean) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerBandLevel: (Short, Short) -> Unit,
    onSetEqualizerPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val song = uiState.currentSong
    val eqState = uiState.equalizerState
    val isShowingEq = uiState.isShowingEqualizer

    // Load embedded album art picture from the song file; null if the song has no embedded picture
    val embeddedArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = song?.uriString) {
        val uriStr = song?.uriString
        if (uriStr.isNullOrBlank() || song.isBuiltInSample || uriStr.endsWith(".wav", ignoreCase = true)) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriStr)

            // 1. For MediaStore content:// URIs on Android 10+, use ContentResolver.loadThumbnail (no JNI error if absent)
            if (uri.scheme == "content" && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val thumb = runCatching {
                    context.contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)?.asImageBitmap()
                }.getOrNull()
                if (thumb != null) return@withContext thumb
            }

            // 2. Check stream header for embedded cover art tags (ID3 APIC / MP4 covr / FLAC picture) before calling JNI getEmbeddedPicture
            val hasEmbeddedPictureTag = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArray(65536)
                    val bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) {
                        false
                    } else {
                        val headerText = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
                        headerText.contains("APIC") || headerText.contains("covr") || headerText.contains("image/jpeg") || headerText.contains("image/png")
                    }
                } ?: false
            }.getOrDefault(false)

            if (!hasEmbeddedPictureTag) {
                return@withContext null
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val bytes = retriever.embeddedPicture
                if (bytes != null && bytes.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -25f) {
                        onSetShowEqualizer(true)
                    } else if (dragAmount > 25f) {
                        onSetShowEqualizer(false)
                    }
                }
            }
            .testTag("hero_now_playing_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompactSquare = maxHeight < 250.dp
            if (!isShowingEq) {
                // --- VIEW 1: SQUARE SONG PICTURE FROM TRACK METADATA (Or music logo if song has no picture) ---
                val art = embeddedArtwork
                if (art != null) {
                    Image(
                        bitmap = art,
                        contentDescription = "Song picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = CircleShape,
                            modifier = Modifier.size(if (isCompactSquare) 110.dp else 160.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = "Music logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(if (isCompactSquare) 64.dp else 96.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // --- VIEW 2: 5-BAND VERTICAL LINE EQUALIZER + 200% SYSTEM-SYNCED LINE VOLUME SLIDER ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isCompactSquare) 10.dp else 14.dp,
                            vertical = if (isCompactSquare) 6.dp else 10.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactSquare) 4.dp else 6.dp)
                ) {
                    // Top Row of Equalizer Window (No "Equalizer ON" text — clean switch only)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactSquare) 30.dp else 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onSetShowEqualizer(false) },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(if (isCompactSquare) 30.dp else 34.dp)
                                .testTag("switch_to_artwork_tab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AudioFile,
                                    contentDescription = "Back to song picture",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Switch(
                            checked = eqState.isEnabled,
                            onCheckedChange = onSetEqualizerEnabled
                        )
                    }

                    // Equalizer Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        eqState.presets.forEach { preset ->
                            val isSel = eqState.currentPreset == preset
                            FilterChip(
                                selected = isSel,
                                onClick = { onSetEqualizerPreset(preset) },
                                label = {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                border = null,
                                modifier = Modifier.height(if (isCompactSquare) 24.dp else 28.dp)
                            )
                        }
                    }

                    // 5 Vertical Line Sliders for Equalizer Bands
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        eqState.bands.forEach { band ->
                            val gainDb = band.levelMb / 100 // -15 to +15 dB
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (gainDb > 0) "+$gainDb" else "$gainDb",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (gainDb != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                VerticalEqBandLineSlider(
                                    levelMb = band.levelMb,
                                    minLevelMb = band.minLevelMb,
                                    maxLevelMb = band.maxLevelMb,
                                    enabled = eqState.isEnabled,
                                    onLevelChange = { newMb ->
                                        onSetEqualizerBandLevel(band.bandIndex, newMb)
                                    },
                                    modifier = Modifier
                                        .width(34.dp)
                                        .weight(1f)
                                        .padding(vertical = 2.dp)
                                )

                                Text(
                                    text = band.displayFrequency,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // 0%–200% System-Synced Volume LineSlider
                    val isBoost = uiState.volumePercent > 100
                    val volColor = if (isBoost) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                uiState.volumePercent == 0 -> Icons.Filled.VolumeMute
                                uiState.volumePercent < 50 -> Icons.Filled.VolumeDown
                                else -> Icons.Filled.VolumeUp
                            },
                            contentDescription = "Volume",
                            tint = volColor,
                            modifier = Modifier.size(18.dp)
                        )
                        LineSlider(
                            value = uiState.volumePercent.toFloat(),
                            onValueChange = { onSetVolumePercent(it.roundToInt()) },
                            valueRange = 0f..200f,
                            activeColor = volColor,
                            thumbColor = volColor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("equalizer_volume_slider")
                        )
                        Text(
                            text = "${uiState.volumePercent}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = volColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vertical thin line slider for Equalizer bands.
 */
@Composable
private fun VerticalEqBandLineSlider(
    levelMb: Short,
    minLevelMb: Short,
    maxLevelMb: Short,
    enabled: Boolean,
    onLevelChange: (Short) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val range = (maxLevelMb - minLevelMb).coerceAtLeast(1)
    val normalized = ((levelMb - minLevelMb).toFloat() / range.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .pointerInput(minLevelMb, maxLevelMb, enabled) {
                detectTapGestures { offset ->
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val ratio = (1f - (offset.y / h)).coerceIn(0f, 1f)
                    val newLevel = (minLevelMb + ratio * (maxLevelMb - minLevelMb)).toInt().toShort()
                    onLevelChange(newLevel)
                }
            }
            .pointerInput(minLevelMb, maxLevelMb, enabled) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val ratio = (1f - (offset.y / h)).coerceIn(0f, 1f)
                        val newLevel = (minLevelMb + ratio * (maxLevelMb - minLevelMb)).toInt().toShort()
                        onLevelChange(newLevel)
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val ratio = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                        val newLevel = (minLevelMb + ratio * (maxLevelMb - minLevelMb)).toInt().toShort()
                        onLevelChange(newLevel)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbRadiusPx = 5.5.dp.toPx()
            val strokePx = 2.dp.toPx()
            val centerX = size.width / 2f
            val topY = thumbRadiusPx
            val bottomY = (size.height - thumbRadiusPx).coerceAtLeast(topY)
            val usableHeight = (bottomY - topY).coerceAtLeast(0f)

            // Full vertical thin background line
            drawLine(
                color = trackColor,
                start = Offset(centerX, topY),
                end = Offset(centerX, bottomY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Center 0 dB tick mark
            val midY = topY + usableHeight / 2f
            drawLine(
                color = primaryColor.copy(alpha = 0.4f),
                start = Offset(centerX - 6.dp.toPx(), midY),
                end = Offset(centerX + 6.dp.toPx(), midY),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Active vertical thin line from bottom to thumb
            val thumbY = bottomY - normalized * usableHeight
            drawLine(
                color = primaryColor,
                start = Offset(centerX, bottomY),
                end = Offset(centerX, thumbY),
                strokeWidth = strokePx * 1.25f,
                cap = StrokeCap.Round
            )

            // Thumb circle
            drawCircle(
                color = primaryColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbY)
            )
        }
    }
}

/**
 * Repeat Count & Pause Delay quick action pills placed directly above the bottom transport controls.
 */
@Composable
private fun LooperQuickActionsRow(
    loopConfig: com.example.model.LoopConfig,
    onOpenRepeatDialog: () -> Unit,
    onOpenDelayDialog: () -> Unit,
    onSetAfterLoopAction: (AfterLoopAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Repeat Count Pill (Icon + Value only)
        Surface(
            onClick = onOpenRepeatDialog,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("looper_repeat_pill")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "Repeat count",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (loopConfig.isInfinite) "∞" else "${loopConfig.repeatCount}x",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Delay Interval Pill (Pause between loops button)
        Surface(
            onClick = onOpenDelayDialog,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("looper_delay_pill")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = "Delay interval",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (loopConfig.matchTrackDurationDelay) "Echo" else "${loopConfig.delaySeconds}s",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Loop Mode Pill (Icon only, matching other buttons without text or special color; white when not active)
        val nextMode = when (loopConfig.afterCompletionAction) {
            AfterLoopAction.NEXT_SONG -> AfterLoopAction.REPEAT_PLAYLIST
            AfterLoopAction.REPEAT_PLAYLIST -> AfterLoopAction.STOP
            AfterLoopAction.STOP -> AfterLoopAction.NEXT_SONG
        }
        val isLoopActive = loopConfig.afterCompletionAction == AfterLoopAction.REPEAT_PLAYLIST
        val modeIcon = if (loopConfig.afterCompletionAction == AfterLoopAction.STOP) {
            Icons.Filled.RepeatOne
        } else {
            Icons.Filled.Repeat
        }
        val iconTint = if (isLoopActive) MaterialTheme.colorScheme.primary else Color.White

        Surface(
            onClick = { onSetAfterLoopAction(nextMode) },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("folder_loop_mode_button")
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = loopConfig.afterCompletionAction.label,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
        }
    }
}

/**
 * Bottom-anchored timeline line slider and primary playback transport buttons.
 */
@Composable
private fun BottomTransportAndTimelineSection(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onSetAfterLoopAction: (AfterLoopAction) -> Unit
) {
    val cfg = uiState.loopConfig
    val totalDuration = uiState.trackDurationMs.coerceAtLeast(1L)
    val currentPos = uiState.currentPositionMs.coerceIn(0L, totalDuration)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transport_timeline_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thin Line Timeline Scrubber
        Column(modifier = Modifier.fillMaxWidth()) {
            LineSlider(
                value = currentPos.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..totalDuration.toFloat(),
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDurationMillis(currentPos),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("current_position_text")
                )

                Text(
                    text = formatDurationMillis(totalDuration),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.testTag("total_duration_text")
                )
            }
        }

        // Primary Transport Buttons anchored at the bottom (SpaceEvenly balances previous, play/pause, and next)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousSong,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("previous_song_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous song",
                    modifier = Modifier.size(28.dp)
                )
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("play_pause_button")
            ) {
                val isPlayingActive = uiState.phase == PlaybackPhase.PLAYING ||
                    (uiState.phase == PlaybackPhase.WAITING_DELAY && !uiState.isDelayPaused)
                Icon(
                    imageVector = if (isPlayingActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlayingActive) "Pause playback" else "Start playback",
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = onNextSong,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("next_song_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next song",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Floating volume overlay HUD (0% to 200%).
 */
@Composable
private fun VlcVolumeHudCard(volumePercent: Int) {
    val isBoost = volumePercent > 100
    val boostOrange = Color(0xFFFF9800)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF0111827),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(82.dp)
            .testTag("vlc_volume_hud_overlay")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when {
                    volumePercent == 0 -> Icons.Filled.VolumeMute
                    volumePercent < 50 -> Icons.Filled.VolumeDown
                    else -> Icons.Filled.VolumeUp
                },
                contentDescription = "Volume",
                tint = if (isBoost) boostOrange else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )

            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF334155))
            ) {
                val fillRatio = (volumePercent / 200f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillRatio)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isBoost) boostOrange else MaterialTheme.colorScheme.primary)
                )
            }

            Text(
                text = "$volumePercent%",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = if (isBoost) boostOrange else Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Modal dialog for adjusting playback speed using LineSlider and a 2x3 preset grid so labels never overlap
 */
@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableStateOf(currentSpeed) }
    val presetRows = listOf(
        listOf(0.5f, 0.75f, 1.0f),
        listOf(1.25f, 1.5f, 2.0f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Playback Speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = String.format(Locale.US, "%.2fx", speed),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                LineSlider(
                    value = speed,
                    onValueChange = { raw ->
                        speed = ((raw * 20f).roundToInt() / 20f).coerceIn(0.5f, 2.0f)
                    },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetRows.forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPresets.forEach { s ->
                                val isSelected = abs(speed - s) < 0.05f
                                Surface(
                                    onClick = { speed = s },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "${s}x",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectSpeed(speed)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Modal dialog for Sleep Timer with Hours and Minutes options
 */
@Composable
private fun SleepTimerDialog(
    remainingMs: Long,
    onSetSleepTimer: (hours: Int, minutes: Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialTotalMinutes = if (remainingMs > 0L) ((remainingMs + 59_999L) / 60_000L).toInt() else 30
    var hours by remember { mutableStateOf((initialTotalMinutes / 60).coerceIn(0, 12)) }
    var minutes by remember { mutableStateOf((initialTotalMinutes % 60).coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (remainingMs > 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sleep Timer Active",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Stops in ${formatSleepTimerMillis(remainingMs)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(
                                onClick = {
                                    onCancelSleepTimer()
                                    onDismiss()
                                }
                            ) {
                                Text("Turn Off", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text(
                    text = "Stop playback automatically after:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Presets Row (Minutes & Hours)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple(0, 15, "15m"),
                        Triple(0, 30, "30m"),
                        Triple(0, 45, "45m"),
                        Triple(1, 0, "1h"),
                        Triple(2, 0, "2h")
                    )
                    presets.forEach { (h, m, label) ->
                        val isSelected = hours == h && minutes == m
                        Surface(
                            onClick = {
                                hours = h
                                minutes = m
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Hours LineSlider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hours", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$hours hr",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LineSlider(
                        value = hours.toFloat(),
                        onValueChange = { hours = it.roundToInt().coerceIn(0, 12) },
                        valueRange = 0f..12f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Minutes LineSlider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Minutes", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$minutes min",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LineSlider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = it.roundToInt().coerceIn(0, 59) },
                        valueRange = 0f..59f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Summary
                val totalMin = hours * 60 + minutes
                Text(
                    text = if (totalMin > 0) {
                        buildString {
                            append("Timer set for ")
                            if (hours > 0) append("$hours hr ")
                            if (minutes > 0) append("$minutes min")
                        }.trim()
                    } else {
                        "Select at least 1 minute to start timer"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (totalMin > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetSleepTimer(hours, minutes)
                    onDismiss()
                },
                enabled = (hours * 60 + minutes) > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("start_sleep_timer_button"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Start Timer", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Modal dialog for setting repetition counts
 */
@Composable
private fun RepeatCountDialog(
    currentRepeatCount: Int,
    onSetRepeatCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf(currentRepeatCount) }
    var customText by remember { mutableStateOf(if (currentRepeatCount > 0) currentRepeatCount.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Song Repetitions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Times this track should loop before stopping:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1x", 3 to "3x", 5 to "5x", 10 to "10x", 0 to "∞").forEach { (num, label) ->
                        val isSelected = count == num
                        Surface(
                            onClick = {
                                count = num
                                customText = if (num > 0) num.toString() else ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = customText,
                    onValueChange = { input ->
                        customText = input.filter { it.isDigit() }.take(3)
                        count = customText.toIntOrNull() ?: 0
                    },
                    label = { Text("Custom count (0 for ∞)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetRepeatCount(count)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Modal dialog for delay pause between repetitions
 */
@Composable
private fun DelayIntervalDialog(
    delaySeconds: Int,
    matchTrackDuration: Boolean,
    onSetDelaySeconds: (Int) -> Unit,
    onSetMatchTrackDuration: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var seconds by remember { mutableStateOf(delaySeconds) }
    var isEcho by remember { mutableStateOf(matchTrackDuration) }
    var customText by remember { mutableStateOf(delaySeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Pause Between Loops",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { isEcho = !isEcho },
                    color = if (isEcho) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Echo Mode (1x Track Length)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pauses for exact track duration",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(checked = isEcho, onCheckedChange = { isEcho = it })
                    }
                }

                if (!isEcho) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "0s", 2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                            val isSelected = seconds == sec
                            Surface(
                                onClick = {
                                    seconds = sec
                                    customText = sec.toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            customText = input.filter { it.isDigit() }.take(3)
                            seconds = customText.toIntOrNull() ?: 0
                        },
                        label = { Text("Pause duration in seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSetMatchTrackDuration(isEcho)
                    if (!isEcho) onSetDelaySeconds(seconds)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun DelayCountdownBanner(
    uiState: PlayerUiState,
    onSkipDelayNow: () -> Unit,
    onTogglePauseDelay: () -> Unit,
    onAddExtraDelay: () -> Unit
) {
    val progress = if (uiState.delayTotalMs > 0L) {
        (uiState.delayRemainingMs.toFloat() / uiState.delayTotalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .testTag("delay_countdown_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = "Memorization delay active",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(14.dp)
                .clickable { onTogglePauseDelay() }
                .testTag("pause_delay_button")
        )

        Text(
            text = "Pause ${formatDelayPreciseMillis(uiState.delayRemainingMs)}",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.testTag("delay_remaining_text")
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
        )

        Surface(
            onClick = onAddExtraDelay,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .height(22.dp)
                .testTag("add_extra_delay_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "+5s",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Surface(
            onClick = onSkipDelayNow,
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .height(22.dp)
                .testTag("skip_delay_now_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
