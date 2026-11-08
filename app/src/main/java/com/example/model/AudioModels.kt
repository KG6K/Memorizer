package com.example.model

enum class AfterLoopAction(val label: String, val subtitle: String) {
    STOP("Stop Playback", "Pause at start after all repetitions finish"),
    NEXT_SONG("Next Song", "Advance to next song and repeat it with same rules"),
    REPEAT_PLAYLIST("Loop Playlist", "Cycle through all songs in library continuously")
}

enum class PlaybackPhase {
    IDLE,
    PLAYING,
    PAUSED,
    WAITING_DELAY,
    COMPLETED_ALL_LOOPS
}

data class LoopConfig(
    val repeatCount: Int = 3, // 0 = Infinite (∞), 1..999 = exact repetition count
    val delaySeconds: Int = 3, // 0..300 seconds pause between loops
    val matchTrackDurationDelay: Boolean = false, // Echo mode: delay = duration of played track/section
    val sectionStartMs: Long = 0L,
    val sectionEndMs: Long? = null, // null = full song duration
    val playbackSpeed: Float = 1.0f,
    val afterCompletionAction: AfterLoopAction = AfterLoopAction.STOP
) {
    val isInfinite: Boolean get() = repeatCount <= 0
    val hasAbSection: Boolean get() = sectionStartMs > 0L || sectionEndMs != null

    fun effectiveSegmentDurationMs(trackDurationMs: Long): Long {
        val end = (sectionEndMs ?: trackDurationMs).coerceAtMost(trackDurationMs)
        val start = sectionStartMs.coerceIn(0L, end)
        return (end - start).coerceAtLeast(500L)
    }

    fun calculateDelayMs(trackDurationMs: Long): Long {
        return if (matchTrackDurationDelay) {
            val segMs = effectiveSegmentDurationMs(trackDurationMs)
            (segMs / playbackSpeed.coerceAtLeast(0.5f)).toLong().coerceAtLeast(1000L)
        } else {
            (delaySeconds.coerceAtLeast(0) * 1000L)
        }
    }

    fun formatSummary(): String {
        val countStr = if (isInfinite) "∞ Loops" else "${repeatCount}x Repeat"
        val delayStr = if (matchTrackDurationDelay) {
            "Echo Delay (1x Length)"
        } else if (delaySeconds == 0) {
            "No Delay"
        } else {
            "${delaySeconds}s Delay"
        }
        return "$countStr • $delayStr"
    }
}

data class LocalSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val isBuiltInSample: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val savedLoopConfig: LoopConfig? = null,
    val folderName: String = "Built-in Studio Samples",
    val lastPlayedAt: Long = 0L
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long = 0L
)

data class PlaybackHistoryItem(
    val id: String,
    val songId: String,
    val songTitle: String,
    val artist: String,
    val folderName: String,
    val playlistName: String,
    val playedAt: Long = System.currentTimeMillis()
)

data class FolderInfo(
    val name: String,
    val songCount: Int,
    val totalDurationMs: Long = 0L,
    val lastPlayedAt: Long = 0L
)

data class LoopPreset(
    val id: String,
    val name: String,
    val description: String,
    val repeatCount: Int,
    val delaySeconds: Int,
    val matchTrackDurationDelay: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val afterCompletionAction: AfterLoopAction = AfterLoopAction.STOP,
    val isBuiltIn: Boolean = false
)

enum class LibraryFilter(val label: String) {
    ALL("All Tracks"),
    DEVICE_ONLY("Device Files"),
    WITH_SAVED_LOOP("Custom Loop Saved"),
    SAMPLES("Studio Samples")
}

enum class MainMenuTab(val title: String) {
    RECENT("Recent"),
    FOLDERS("Folders"),
    ALBUMS_SINGERS("Albums / Singers"),
    PLAYLISTS("Playlists")
}

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelMb: Short = 0, // -1500 to +1500 millibels (-15dB to +15dB)
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500
) {
    val displayFrequency: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}kHz" else "${centerFreqHz}Hz"
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val currentPreset: String = "Flat",
    val presets: List<String> = listOf("Flat", "Bass Boost", "Vocal / Recitation", "Treble Boost", "Acoustic", "Custom"),
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, 0),
        EqualizerBand(1, 230, 0),
        EqualizerBand(2, 910, 0),
        EqualizerBand(3, 3600, 0),
        EqualizerBand(4, 14000, 0)
    ),
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0 // 0..1000
)
