package com.example.player

import com.example.model.EqualizerState
import com.example.model.FolderInfo
import com.example.model.LibraryFilter
import com.example.model.LocalSong
import com.example.model.LoopConfig
import com.example.model.LoopPreset
import com.example.model.MainMenuTab
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import com.example.model.RecentCollectionSession
import com.example.model.ScanFolderEntry
import com.example.model.SortDirection
import com.example.model.SortOption

data class PlayerUiState(
    val songs: List<LocalSong> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val recentSessions: List<RecentCollectionSession> = emptyList(),
    val activeRecentSessionKey: String? = null,
    val scanFolders: List<ScanFolderEntry> = emptyList(),
    val isScanningLibrary: Boolean = false,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedTab: MainMenuTab = MainMenuTab.RECENT,
    val customPlaybackQueue: List<LocalSong> = emptyList(),
    val currentSong: LocalSong? = null,
    val currentPlaylistName: String = "All Songs",
    val currentFolderName: String = "",
    val loopConfig: LoopConfig = LoopConfig(),
    val phase: PlaybackPhase = PlaybackPhase.IDLE,
    val currentPositionMs: Long = 0L,
    val trackDurationMs: Long = 0L,
    val currentIteration: Int = 1,
    val completedIterationsTotal: Int = 0,
    val delayRemainingMs: Long = 0L,
    val delayTotalMs: Long = 0L,
    val isDelayPaused: Boolean = false,
    val presets: List<LoopPreset> = emptyList(),
    val statusBannerMessage: String? = null,
    val errorMessage: String? = null,
    val expandedFolder: String? = null,
    val expandedPlaylistId: String? = null,
    val expandedArtist: String? = null,
    val volumePercent: Int = 100, // 0..200 (100% = normal, >100% = LoudnessEnhancer boost)
    val isMuted: Boolean = false,
    val isShowingEqualizer: Boolean = false,
    val equalizerState: EqualizerState = EqualizerState(),
    val sleepTimerRemainingMs: Long = 0L,
    val sleepTimerTotalMs: Long = 0L,
    val notificationSessionVisible: Boolean = false,
    val playbackStateSyncNonce: Long = 0L
) {
    val isActivelyPlayingForNotification: Boolean
        get() = phase == PlaybackPhase.PLAYING ||
            (phase == PlaybackPhase.WAITING_DELAY && !isDelayPaused)

    val notificationContextLabel: String
        get() {
            val rawPlaylist = currentPlaylistName.trim()
            val rawFolder = currentFolderName.ifBlank { currentSong?.folderName ?: "" }.trim()
            return when {
                rawPlaylist.startsWith("Folder:", ignoreCase = true) ->
                    rawPlaylist.removePrefix("Folder:").trim().ifBlank { rawFolder }
                rawPlaylist.isNotBlank() && !rawPlaylist.equals("All Songs", ignoreCase = true) ->
                    rawPlaylist
                rawFolder.isNotBlank() ->
                    rawFolder
                else ->
                    "All Songs"
            }
        }

    val filteredSongs: List<LocalSong>
        get() {
            val byFilter = when (libraryFilter) {
                LibraryFilter.ALL -> songs
                LibraryFilter.DEVICE_ONLY -> songs.filter { !it.isBuiltInSample }
                LibraryFilter.WITH_SAVED_LOOP -> songs.filter { it.savedLoopConfig != null }
                LibraryFilter.SAMPLES -> songs.filter { it.isBuiltInSample }
            }
            if (searchQuery.isBlank()) return byFilter
            val q = searchQuery.trim().lowercase()
            return byFilter.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q)
            }
        }

    val folderGroups: List<FolderInfo>
        get() {
            val map = mutableMapOf<String, MutableList<LocalSong>>()
            songs.forEach { song ->
                map.getOrPut(song.folderName) { mutableListOf() }.add(song)
            }
            return map.map { (name, list) ->
                FolderInfo(
                    name = name,
                    songCount = list.size,
                    totalDurationMs = list.sumOf { it.durationMs },
                    lastPlayedAt = list.maxOfOrNull { it.lastPlayedAt } ?: 0L
                )
            }.sortedByDescending { it.lastPlayedAt }
        }

    val recentSongs: List<LocalSong>
        get() {
            val played = songs.filter { it.lastPlayedAt > 0L }.sortedByDescending { it.lastPlayedAt }
            return if (played.isNotEmpty()) played.take(15) else songs.take(5)
        }

    val recentFolders: List<FolderInfo>
        get() = folderGroups.take(6)

    val recentPlaylists: List<Playlist>
        get() = playlists.sortedByDescending { it.lastPlayedAt }.take(6)
}

fun formatDurationMillis(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

fun formatSleepTimerMillis(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatDelayPreciseMillis(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val seconds = clamped / 1000L
    val tenths = (clamped % 1000L) / 100L
    return "%02d.%ds".format(seconds, tenths)
}
