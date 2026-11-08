package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Remove
import com.example.model.EqualizerBand
import com.example.model.EqualizerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AfterLoopAction
import com.example.model.PlaybackPhase
import com.example.player.PlayerUiState
import com.example.player.formatDelayPreciseMillis
import com.example.player.formatDurationMillis
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.sin

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
    modifier: Modifier = Modifier
) {
    var isVolumeGestureActive by remember { mutableStateOf(false) }
    var hideVolumeJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }
    var showAbDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val cfg = uiState.loopConfig

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        // Only activate right-side gesture if touch is on right 50% of the player window (like VLC Player)
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
                            // Dragging UP is negative dragAmount (increases volume), DOWN is positive (decreases volume)
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hero Song Picture + Swipe Left Equalizer in the same window!
                HeroNowPlayingCard(
                    uiState = uiState,
                    onPickAudioFiles = onPickAudioFiles,
                    onOpenLibraryTab = onOpenLibraryTab,
                    onSetShowEqualizer = onSetShowEqualizer,
                    onSetVolumePercent = onSetVolumePercent,
                    onSetEqualizerEnabled = onSetEqualizerEnabled,
                    onSetEqualizerBandLevel = onSetEqualizerBandLevel,
                    onSetEqualizerPreset = onSetEqualizerPreset,
                    onSetBassBoostStrength = onSetBassBoostStrength,
                    onSetVirtualizerStrength = onSetVirtualizerStrength
                )

                // 2. Live Delay Countdown Overlay Card (visible when waiting between loops)
                AnimatedVisibility(
                    visible = uiState.phase == PlaybackPhase.WAITING_DELAY,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    DelayCountdownBanner(
                        uiState = uiState,
                        onSkipDelayNow = onSkipDelayNow,
                        onTogglePauseDelay = onTogglePlayPause,
                        onAddExtraDelay = onAddExtraDelay
                    )
                }

                // 3. Repetition Status + Timeline + Transport Card
                TransportAndTimelineCard(
                    uiState = uiState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSkipBackward = onSkipBackward,
                    onSkipForward = onSkipForward,
                    onPreviousSong = onPreviousSong,
                    onNextSong = onNextSong,
                    onResetLoopProgress = onResetLoopProgress,
                    onMarkSectionStart = onMarkSectionStart,
                    onMarkSectionEnd = onMarkSectionEnd,
                    onClearAbSection = onClearAbSection
                )

                // 4. CLEAN Looper Quick Action Pills (Speed, Repeat, Delay, Presets popups)
                LooperQuickActionsRow(
                    loopConfig = cfg,
                    onOpenSpeedDialog = { showSpeedDialog = true },
                    onOpenRepeatDialog = { showRepeatDialog = true },
                    onOpenDelayDialog = { showDelayDialog = true },
                    onOpenPresetsDialog = { showPresetsDialog = true }
                )

                // 5. Clean Track Options Row (Save for track, reset progress)
                CleanTrackOptionsRow(
                    hasSavedConfig = uiState.currentSong?.savedLoopConfig != null,
                    onSaveConfigToSong = onSaveConfigToSong,
                    onResetLoopProgress = onResetLoopProgress
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Floating VLC Volume HUD Overlay that ONLY appears while swiping right side!
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

    // Modal Dialogs for Clean UI:
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

    if (showPresetsDialog) {
        PresetsDialog(
            presets = uiState.presets,
            onSelectPreset = { preset ->
                onSetRepeatCount(preset.repeatCount)
                onSetDelaySeconds(preset.delaySeconds)
                onSetMatchTrackDuration(preset.matchTrackDurationDelay)
                onSetPlaybackSpeed(preset.playbackSpeed)
                onSetAfterLoopAction(preset.afterCompletionAction)
            },
            onDismiss = { showPresetsDialog = false }
        )
    }
}

/**
 * Clean looper action pills row: Speed, Repeat, Pause Delay, Presets.
 * Locked to 4 equal-width columns (Modifier.weight(1f).height(52.dp)) so changing values NEVER resizes buttons.
 */
@Composable
private fun LooperQuickActionsRow(
    loopConfig: com.example.model.LoopConfig,
    onOpenSpeedDialog: () -> Unit,
    onOpenRepeatDialog: () -> Unit,
    onOpenDelayDialog: () -> Unit,
    onOpenPresetsDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Speed Button (Fixed equal width & height)
            Surface(
                onClick = onOpenSpeedDialog,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("looper_speed_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = "Speed",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Speed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${loopConfig.playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Repeat Button (Fixed equal width & height - never changes size when repeat count changes)
            Surface(
                onClick = onOpenRepeatDialog,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("looper_repeat_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (loopConfig.isInfinite) "∞" else "${loopConfig.repeatCount}x",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Delay Button (Fixed equal width & height)
            Surface(
                onClick = onOpenDelayDialog,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("looper_delay_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Pause Delay",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Pause",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (loopConfig.matchTrackDurationDelay) "Echo" else "${loopConfig.delaySeconds}s",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Presets Button (Fixed equal width & height)
            Surface(
                onClick = onOpenPresetsDialog,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("looper_presets_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = "Presets",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Presets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Hifz",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean track options row for saving song configurations and resetting loop counts.
 */
@Composable
private fun CleanTrackOptionsRow(
    hasSavedConfig: Boolean,
    onSaveConfigToSong: () -> Unit,
    onResetLoopProgress: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onSaveConfigToSong,
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .testTag("save_track_config_button")
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasSavedConfig) "Update Loop Config" else "Save for this Track",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onResetLoopProgress,
            modifier = Modifier
                .width(104.dp)
                .height(42.dp)
                .testTag("reset_loop_progress_button")
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Floating VLC-style volume overlay HUD.
 * ONLY displays while user is swiping on the right side of the screen!
 */
@Composable
private fun VlcVolumeHudCard(volumePercent: Int) {
    val isBoost = volumePercent > 100
    val vlcOrange = Color(0xFFFF9800)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF0111827),
        border = BorderStroke(1.5.dp, if (isBoost) vlcOrange else MaterialTheme.colorScheme.primary),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(86.dp)
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
                tint = if (isBoost) vlcOrange else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )

            // Vertical Level Gauge with 100% boundary marker
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
                        .background(if (isBoost) vlcOrange else MaterialTheme.colorScheme.primary)
                )

                // 100% boundary marker line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.Center)
                        .background(Color.White.copy(alpha = 0.6f))
                )
            }

            Text(
                text = "$volumePercent%",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                fontWeight = FontWeight.Bold,
                color = if (isBoost) vlcOrange else Color.White,
                maxLines = 1,
                softWrap = false
            )

            if (isBoost) {
                Surface(
                    color = vlcOrange.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "VLC 200%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = vlcOrange,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal dialog for adjusting playback speed
 */
@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var speed by remember { mutableStateOf(currentSpeed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 320.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Playback Speed", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed", style = MaterialTheme.typography.bodySmall)
                    Text(
                        String.format(java.util.Locale.US, "%.2fx", speed),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth()
                )
                // Quick preset pills with equal fixed widths
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                        val isSelected = kotlin.math.abs(speed - s) < 0.05f
                        Surface(
                            onClick = { speed = s },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "${s}x",
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectSpeed(speed)
                    onDismiss()
                },
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
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
        modifier = Modifier.widthIn(max = 320.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Song Repetitions", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Times this track should loop before stopping:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1 to "1x", 3 to "3x", 5 to "5x", 10 to "10x", 0 to "∞").forEach { (num, label) ->
                        val isSelected = count == num
                        Surface(
                            onClick = {
                                count = num
                                customText = if (num > 0) num.toString() else ""
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
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
                OutlinedTextField(
                    value = customText,
                    onValueChange = { input ->
                        customText = input.filter { it.isDigit() }.take(3)
                        count = customText.toIntOrNull() ?: 0
                    },
                    label = { Text("Custom count (0 for ∞)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
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
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
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
        modifier = Modifier.widthIn(max = 320.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Pause Between Loops", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { isEcho = !isEcho },
                    color = if (isEcho) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Echo Mode (1x Track Length)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Pauses for exact track duration", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = isEcho, onCheckedChange = { isEcho = it })
                    }
                }

                if (!isEcho) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0 to "0s", 2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                            val isSelected = seconds == sec
                            Surface(
                                onClick = {
                                    seconds = sec
                                    customText = sec.toString()
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
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
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            customText = input.filter { it.isDigit() }.take(3)
                            seconds = customText.toIntOrNull() ?: 0
                        },
                        label = { Text("Pause duration in seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
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
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text("Apply", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Modal dialog for A-B section looping
 */
@Composable
private fun AbSectionDialog(
    currentStartMs: Long,
    currentEndMs: Long?,
    trackDurationMs: Long,
    currentPlaybackMs: Long,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onClearAb: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 320.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("A–B Loop Section", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${formatDurationMillis(currentStartMs)} → ${currentEndMs?.let { formatDurationMillis(it) } ?: "End"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pos: ${formatDurationMillis(currentPlaybackMs)} / ${formatDurationMillis(trackDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onMarkStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Set [A]", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onMarkEnd,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Set [B]", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (currentStartMs > 0 || currentEndMs != null) {
                    OutlinedButton(
                        onClick = onClearAb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear A–B Markers", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Text("Done", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

/**
 * Modal dialog for choosing memorization presets
 */
@Composable
private fun PresetsDialog(
    presets: List<com.example.model.LoopPreset>,
    onSelectPreset: (com.example.model.LoopPreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 320.dp),
        title = { Text("Memorization Presets", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (preset in presets) {
                    Surface(
                        onClick = {
                            onSelectPreset(preset)
                            onDismiss()
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(preset.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text(preset.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(34.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text("Close", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun HeroNowPlayingCard(
    uiState: PlayerUiState,
    onPickAudioFiles: () -> Unit,
    onOpenLibraryTab: () -> Unit,
    onSetShowEqualizer: (Boolean) -> Unit,
    onSetVolumePercent: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerBandLevel: (Short, Short) -> Unit,
    onSetEqualizerPreset: (String) -> Unit,
    onSetBassBoostStrength: (Int) -> Unit,
    onSetVirtualizerStrength: (Int) -> Unit
) {
    val song = uiState.currentSong
    val eqState = uiState.equalizerState
    val isShowingEq = uiState.isShowingEqualizer

    val isVisualizerActive = uiState.phase == PlaybackPhase.PLAYING ||
        (uiState.phase == PlaybackPhase.WAITING_DELAY && !uiState.isDelayPaused)

    val animPhase = if (isVisualizerActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 6.283f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
        animated
    } else {
        0.8f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -25f) {
                        // Swiped LEFT: Open equalizer in the same window!
                        onSetShowEqualizer(true)
                    } else if (dragAmount > 25f) {
                        // Swiped RIGHT: Return to song picture!
                        onSetShowEqualizer(false)
                    }
                }
            }
            .testTag("hero_now_playing_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
        ) {
            if (!isShowingEq) {
                // --- VIEW 1: SONG PICTURE & HERO ACOUSTIC WAVE (Swipe left for Equalizer) ---
                Image(
                    painter = painterResource(id = R.drawable.img_hero_audio_wave_1794082291260),
                    contentDescription = "Studio audio wave background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(210.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x880B1120),
                                    Color(0xD90B1120),
                                    Color(0xF50B1120)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Source Badge + Mode Switcher Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (song?.isBuiltInSample == false) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (song?.isBuiltInSample == false) Icons.Filled.AudioFile else Icons.Filled.LibraryMusic,
                                    contentDescription = "Audio source type",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (song?.isBuiltInSample == false) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                                Text(
                                    text = if (song == null) {
                                        "No Track Selected"
                                    } else if (song.isBuiltInSample) {
                                        "Studio Reference Track"
                                    } else {
                                        "Local Device Audio"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (song?.isBuiltInSample == false) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        // Window Flip Button: Tap or Swipe left to Equalizer
                        Surface(
                            onClick = { onSetShowEqualizer(true) },
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("switch_to_equalizer_tab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Equalizer,
                                    contentDescription = "Equalizer",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Swipe ◀ for Equalizer",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Middle: Track Title & Artist + Reactive Visualizer Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenLibraryTab() }
                        ) {
                            Text(
                                text = song?.title ?: "Select a Local Song to Begin",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("now_playing_title")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = song?.let { "${it.artist} • ${it.album}" }
                                    ?: "Tap Import Songs to pick MP3, WAV, M4A, or OGG files",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Reactive Acoustic Bar Visualizer
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        Canvas(
                            modifier = Modifier
                                .width(88.dp)
                                .height(44.dp)
                        ) {
                            val barCount = 11
                            val spacing = 3.dp.toPx()
                            val barWidth = (size.width - spacing * (barCount - 1)) / barCount
                            for (i in 0 until barCount) {
                                val ratio = when (uiState.phase) {
                                    PlaybackPhase.PLAYING -> {
                                        val wave = sin(animPhase + i * 0.65f)
                                        (0.28f + 0.72f * ((wave + 1f) / 2f))
                                    }
                                    PlaybackPhase.WAITING_DELAY -> {
                                        val pulse = sin(animPhase * 0.5f + i * 0.3f)
                                        (0.18f + 0.25f * ((pulse + 1f) / 2f))
                                    }
                                    else -> 0.22f
                                }
                                val barHeight = (size.height * ratio).coerceAtLeast(6.dp.toPx())
                                val top = size.height - barHeight
                                val barColor = if (uiState.phase == PlaybackPhase.WAITING_DELAY) {
                                    secondaryColor
                                } else {
                                    primaryColor
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(i * (barWidth + spacing), top),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                }
            } else {
                // --- VIEW 2: 5-BAND EQUALIZER & VLC 200% BOOST (Opened by swiping left in same window!) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row of Equalizer Window
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onSetShowEqualizer(false) },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("switch_to_artwork_tab")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AudioFile,
                                    contentDescription = "Artwork",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Swipe ▶ for Picture",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (eqState.isEnabled) "Equalizer ON" else "Equalizer OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = eqState.isEnabled,
                                onCheckedChange = onSetEqualizerEnabled,
                                modifier = Modifier.size(36.dp)
                            )
                        }
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
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // 5 Equalizer Bands
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        eqState.bands.forEach { band ->
                            val gainDb = band.levelMb / 100 // -15 to +15 dB
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (gainDb > 0) "+$gainDb" else "$gainDb",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.Bold,
                                    color = if (gainDb != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Slider(
                                    value = band.levelMb.toFloat(),
                                    onValueChange = { newLevel ->
                                        onSetEqualizerBandLevel(band.bandIndex, newLevel.toInt().toShort())
                                    },
                                    valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                    modifier = Modifier
                                        .height(100.dp)
                                        .width(52.dp)
                                )

                                Text(
                                    text = band.displayFrequency,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // VLC 200% Volume Boost Slider (directly inside the equalizer window)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = if (uiState.volumePercent > 100) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "200% VLC Volume Boost",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (uiState.volumePercent > 100) {
                                        "${uiState.volumePercent}% (+${((uiState.volumePercent - 100) * 0.08).toInt()}dB Boost 🔥)"
                                    } else {
                                        "${uiState.volumePercent}%"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = if (uiState.volumePercent > 100) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Slider(
                                value = uiState.volumePercent.toFloat(),
                                onValueChange = { onSetVolumePercent(it.toInt()) },
                                valueRange = 0f..200f,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (uiState.volumePercent > 100) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                                    activeTrackColor = if (uiState.volumePercent > 100) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Card for 200% VLC Audio Volume & Gain Boost
 */
@Composable
private fun VolumeAndBoostControlCard(
    uiState: PlayerUiState,
    onSetVolumePercent: (Int) -> Unit,
    onToggleMute: () -> Unit
) {
    val isBoosted = uiState.volumePercent > 100
    val boostColor = Color(0xFFFF9800) // VLC Signature Orange

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("volume_and_boost_control_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isBoosted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            if (isBoosted) 1.5.dp else 1.dp,
            if (isBoosted) boostColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (isBoosted) boostColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    uiState.isMuted || uiState.volumePercent == 0 -> Icons.Filled.VolumeOff
                                    uiState.volumePercent <= 70 -> Icons.Filled.VolumeDown
                                    else -> Icons.Filled.VolumeUp
                                },
                                contentDescription = "Volume",
                                tint = if (isBoosted) boostColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Volume & 200% VLC Boost",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBoosted) {
                                "Over-amplification engine active (+8dB gain)"
                            } else {
                                "Standard hardware volume range (0%–100%)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isBoosted) boostColor else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (uiState.isMuted) {
                            "MUTED"
                        } else if (isBoosted) {
                            "🔥 ${uiState.volumePercent}% BOOST"
                        } else {
                            "${uiState.volumePercent}%"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = if (isBoosted) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Slider from 0 to 200%
            Slider(
                value = if (uiState.isMuted) 0f else uiState.volumePercent.toFloat(),
                onValueChange = { onSetVolumePercent(it.toInt()) },
                valueRange = 0f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = if (isBoosted) boostColor else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (isBoosted) boostColor else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("volume_boost_slider")
            )

            // Preset Pills: 50%, 100% (Standard), 150% (Boost), 200% (Max VLC Boost)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 100, 150, 200).forEach { pct ->
                    val isCurrent = uiState.volumePercent == pct && !uiState.isMuted
                    FilledTonalButton(
                        onClick = { onSetVolumePercent(pct) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isCurrent) {
                                if (pct > 100) boostColor else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isCurrent) {
                                if (pct > 100) Color.Black else MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("vol_preset_${pct}")
                    ) {
                        Text(
                            text = if (pct == 200) "200% 🔥" else "$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeMute,
                        contentDescription = "Mute",
                        tint = if (uiState.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
    val nextLoopNum = uiState.currentIteration + 1
    val totalLoopsLabel = if (uiState.loopConfig.isInfinite) "∞" else uiState.loopConfig.repeatCount.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delay_countdown_banner"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HourglassTop,
                        contentDescription = "Memorization delay active",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isDelayPaused) {
                                "Memorization Pause (Timer Paused)"
                            } else {
                                "Memorization Pause Between Loops"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Up Next: Repetition $nextLoopNum of $totalLoopsLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = formatDelayPreciseMillis(uiState.delayRemainingMs),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("delay_remaining_text")
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSkipDelayNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("skip_delay_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play next repetition now",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play Loop $nextLoopNum Now",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onAddExtraDelay,
                    modifier = Modifier.testTag("add_extra_delay_button")
                ) {
                    Text(
                        text = "+5s Pause",
                        maxLines = 1,
                        softWrap = false
                    )
                }

                FilledTonalIconButton(
                    onClick = onTogglePauseDelay,
                    modifier = Modifier.testTag("pause_delay_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isDelayPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (uiState.isDelayPaused) "Resume delay timer" else "Hold delay timer"
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportAndTimelineCard(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onResetLoopProgress: () -> Unit,
    onMarkSectionStart: () -> Unit,
    onMarkSectionEnd: () -> Unit,
    onClearAbSection: () -> Unit
) {
    val cfg = uiState.loopConfig
    val totalDuration = uiState.trackDurationMs.coerceAtLeast(1L)
    val currentPos = uiState.currentPositionMs.coerceIn(0L, totalDuration)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transport_timeline_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Loop Counter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (cfg.isInfinite) Icons.Filled.AllInclusive else Icons.Filled.RepeatOne,
                            contentDescription = "Current loop iteration",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        val loopStatusText = if (cfg.isInfinite) {
                            "Repetition ${uiState.currentIteration} of ∞"
                        } else {
                            "Repetition ${uiState.currentIteration} of ${cfg.repeatCount}"
                        }
                        Text(
                            text = loopStatusText,
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("repetition_counter_badge")
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delay summary badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Configured delay between loops",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp)
                        )
                        val delayText = if (cfg.matchTrackDurationDelay) {
                            "Echo Delay (${formatDurationMillis(cfg.calculateDelayMs(totalDuration))})"
                        } else {
                            "${cfg.delaySeconds}s Loop Delay"
                        }
                        Text(
                            text = delayText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("configured_delay_badge")
                        )
                    }
                }
            }

            // Visual step pills for repetitions (up to 12 dots)
            if (!cfg.isInfinite && cfg.repeatCount in 2..12) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..cfg.repeatCount) {
                        val isDone = step < uiState.currentIteration ||
                            uiState.phase == PlaybackPhase.COMPLETED_ALL_LOOPS
                        val isCurrent = step == uiState.currentIteration &&
                            uiState.phase != PlaybackPhase.COMPLETED_ALL_LOOPS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isCurrent -> MaterialTheme.colorScheme.primary
                                        isDone -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                        )
                    }
                }
            }

            // Timeline Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..totalDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDurationMillis(currentPos),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("current_position_text")
                    )

                    Text(
                        text = "Full Song Loop",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )

                    Text(
                        text = formatDurationMillis(totalDuration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("total_duration_text")
                    )
                }
            }

            // Primary Transport Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                FilledTonalIconButton(
                    onClick = onSkipBackward,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rewind_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Rewind 5 seconds"
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
                        .size(68.dp)
                        .testTag("play_pause_button")
                ) {
                    val isPlayingActive = uiState.phase == PlaybackPhase.PLAYING ||
                        (uiState.phase == PlaybackPhase.WAITING_DELAY && !uiState.isDelayPaused)
                    Icon(
                        imageVector = if (isPlayingActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingActive) "Pause playback" else "Start playback",
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSkipForward,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("forward_5s_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Forward 5 seconds"
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatCountControlCard(
    repeatCount: Int,
    currentIteration: Int,
    onSetRepeatCount: (Int) -> Unit,
    onIncrementRepeatCount: (Int) -> Unit,
    onOpenCustomDialog: () -> Unit
) {
    val isInfinite = repeatCount <= 0
    val quickCounts = listOf(1, 2, 3, 5, 7, 10, 15, 20, 0) // 0 = Infinite

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repeat_count_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = "Song repeat count",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Song Repeat Count",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "How many times a single song plays before stopping or advancing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stepper Row
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = { onIncrementRepeatCount(-1) },
                            enabled = !isInfinite && repeatCount > 1,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("repeat_minus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Remove,
                                contentDescription = "Decrease repeat count"
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onOpenCustomDialog() }
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (isInfinite) "∞ Infinite" else "${repeatCount}x Plays",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("repeat_count_value_text")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Tap to type custom repeat count",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Currently on play #$currentIteration • Tap to type",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = { onIncrementRepeatCount(1) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("repeat_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase repeat count"
                            )
                        }
                    }
                }
            }

            // Quick Preset Chips for Repeat Count
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCounts.forEach { countOption ->
                    val selected = repeatCount == countOption
                    val labelText = if (countOption == 0) "∞ Loop" else "${countOption}x"
                    FilterChip(
                        selected = selected,
                        onClick = { onSetRepeatCount(countOption) },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("repeat_chip_$countOption")
                    )
                }

                AssistChip(
                    onClick = onOpenCustomDialog,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Custom repeat count",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text("Custom") },
                    modifier = Modifier.testTag("repeat_custom_button")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DelayBetweenLoopsControlCard(
    delaySeconds: Int,
    matchTrackDurationDelay: Boolean,
    effectiveCalculatedDelayMs: Long,
    onSetDelaySeconds: (Int) -> Unit,
    onIncrementDelaySeconds: (Int) -> Unit,
    onSetMatchTrackDuration: (Boolean) -> Unit,
    onOpenCustomDelayDialog: () -> Unit
) {
    val quickDelays = listOf(0, 2, 3, 5, 10, 15, 30, 60)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("delay_between_loops_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Delay between loops",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delay Between Loops (Pause Interval)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wait time between each repetition so you can recite or practice",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // QuranCentral-style "Match Track Duration (Echo Mode)" Switch
            Surface(
                color = if (matchTrackDurationDelay) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetMatchTrackDuration(!matchTrackDurationDelay) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Match Track Duration (Echo Recitation Mode)",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (matchTrackDurationDelay) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "Automatically sets pause between loops equal to the song/segment length (${formatDurationMillis(effectiveCalculatedDelayMs)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (matchTrackDurationDelay) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = matchTrackDurationDelay,
                        onCheckedChange = onSetMatchTrackDuration,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("match_duration_switch")
                    )
                }
            }

            // Fixed Seconds Delay Stepper
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { onIncrementDelaySeconds(-1) },
                        enabled = !matchTrackDurationDelay && delaySeconds > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delay_minus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "Decrease delay seconds"
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onOpenCustomDelayDialog() }
                            .padding(horizontal = 12.dp)
                    ) {
                        val mainLabel = if (matchTrackDurationDelay) {
                            "1x Track (${effectiveCalculatedDelayMs / 1000}s)"
                        } else if (delaySeconds == 0) {
                            "0s (Immediate)"
                        } else {
                            "${delaySeconds}s Pause"
                        }
                        Text(
                            text = mainLabel,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag("delay_seconds_value_text")
                        )
                        Text(
                            text = "Tap to enter exact seconds (0s – 600s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onIncrementDelaySeconds(1) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delay_plus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase delay seconds"
                        )
                    }
                }
            }

            // Quick Delay Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickDelays.forEach { sec ->
                    val selected = !matchTrackDurationDelay && delaySeconds == sec
                    FilterChip(
                        selected = selected,
                        onClick = { onSetDelaySeconds(sec) },
                        label = {
                            Text(
                                text = if (sec == 0) "0s (None)" else "${sec}s",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.testTag("delay_chip_${sec}s")
                    )
                }

                AssistChip(
                    onClick = onOpenCustomDelayDialog,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Custom delay seconds",
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text("Custom Sec") },
                    modifier = Modifier.testTag("delay_custom_button")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedAndCompletionCard(
    playbackSpeed: Float,
    afterLoopAction: AfterLoopAction,
    hasSavedSongConfig: Boolean,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAfterLoopAction: (AfterLoopAction) -> Unit,
    onSaveConfigToSong: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("speed_and_completion_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Speed Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Playback speed",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Playback Speed (${playbackSpeed}x)",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speeds.forEach { spd ->
                    val selected = kotlin.math.abs(playbackSpeed - spd) < 0.01f
                    FilterChip(
                        selected = selected,
                        onClick = { onSetPlaybackSpeed(spd) },
                        label = { Text("${spd}x") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("speed_chip_$spd")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // After All Loops Complete Action
            Text(
                text = "When All Repetitions Finish",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AfterLoopAction.entries.forEach { action ->
                    val selected = afterLoopAction == action
                    FilterChip(
                        selected = selected,
                        onClick = { onSetAfterLoopAction(action) },
                        label = { Text(action.label) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("after_loop_action_${action.name}")
                    )
                }
            }

            // Save Loop Config to Current Song Button
            FilledTonalButton(
                onClick = onSaveConfigToSong,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_song_loop_button")
            ) {
                Icon(
                    imageVector = if (hasSavedSongConfig) Icons.Filled.BookmarkAdded else Icons.Filled.Save,
                    contentDescription = "Save loop settings to current song",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasSavedSongConfig) {
                        "Update Saved Loop Config for This Song"
                    } else {
                        "Save Loop & Delay Config to This Song"
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomNumberInputDialog(
    title: String,
    subtitle: String,
    initialValue: String,
    unitLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Value ($unitLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_number_dialog_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toIntOrNull() ?: 1
                    onConfirm(parsed)
                },
                modifier = Modifier.testTag("custom_number_dialog_confirm")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
