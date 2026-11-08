package com.example.player

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SongRepository
import com.example.model.AfterLoopAction
import com.example.model.EqualizerBand
import com.example.model.EqualizerState
import com.example.model.FolderInfo
import com.example.model.LibraryFilter
import com.example.model.LocalSong
import com.example.model.LoopConfig
import com.example.model.LoopPreset
import com.example.model.MainMenuTab
import com.example.model.PlaybackHistoryItem
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PlayerUiState(
    val songs: List<LocalSong> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val history: List<PlaybackHistoryItem> = emptyList(),
    val searchQuery: String = "",
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedTab: MainMenuTab = MainMenuTab.RECENT,
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
    val volumePercent: Int = 100, // 0..200 (100% = normal, >100% = VLC-style LoudnessEnhancer boost)
    val isMuted: Boolean = false,
    val isShowingEqualizer: Boolean = false,
    val equalizerState: EqualizerState = EqualizerState()
) {
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
        get() {
            return folderGroups.take(6)
        }

    val recentPlaylists: List<Playlist>
        get() {
            return playlists.sortedByDescending { it.lastPlayedAt }.take(6)
        }
}

class LoopAudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SongRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var positionTickerJob: Job? = null
    private var delayCountdownJob: Job? = null
    private var isPlayerPrepared = false

    init {
        viewModelScope.launch {
            val initialConfig = withContext(Dispatchers.IO) { repository.loadActiveLoopConfig() }
            val initialPresets = withContext(Dispatchers.IO) { repository.loadPresets() }
            val initialSongs = withContext(Dispatchers.IO) { repository.loadSongs() }
            val initialPlaylists = withContext(Dispatchers.IO) { repository.loadPlaylists() }
            val initialHistory = withContext(Dispatchers.IO) { repository.loadHistory() }
            val firstSong = initialSongs.firstOrNull()
            val activeConfig = firstSong?.savedLoopConfig ?: initialConfig

            _uiState.value = PlayerUiState(
                songs = initialSongs,
                playlists = initialPlaylists,
                history = initialHistory,
                currentSong = firstSong,
                currentFolderName = firstSong?.folderName ?: "",
                currentPlaylistName = "All Songs",
                loopConfig = activeConfig,
                trackDurationMs = firstSong?.durationMs ?: 0L,
                presets = initialPresets
            )

            if (firstSong != null) {
                prepareSongInternal(firstSong, autoPlay = false, resetIteration = true)
            }

            // Automatically scan folders and device storage for audio files at startup
            scanDeviceAudio()
        }
    }

    /**
     * Automatically scans device storage for songs and folders on startup or when permissions are granted.
     */
    fun scanDeviceAudio() {
        viewModelScope.launch(Dispatchers.IO) {
            val scanned = repository.scanDeviceAudioMedia()
            if (scanned.isNotEmpty()) {
                val currentSongs = _uiState.value.songs
                    .filterNot { it.isBuiltInSample || it.id.startsWith("builtin_sample_") }
                    .toMutableList()
                val existingUris = currentSongs.map { it.uriString }.toSet()
                val newItems = scanned.filter { it.uriString !in existingUris }
                if (newItems.isNotEmpty() || currentSongs.size != _uiState.value.songs.size) {
                    currentSongs.addAll(newItems)
                    repository.saveSongs(currentSongs)
                    withContext(Dispatchers.Main) {
                        val shouldSelectFirst = _uiState.value.currentSong == null && currentSongs.isNotEmpty()
                        val first = currentSongs.firstOrNull()
                        _uiState.update {
                            it.copy(
                                songs = currentSongs,
                                currentSong = it.currentSong ?: first,
                                currentFolderName = if (it.currentFolderName.isBlank()) (first?.folderName ?: "") else it.currentFolderName,
                                trackDurationMs = if (it.currentSong == null) (first?.durationMs ?: 0L) else it.trackDurationMs
                            )
                        }
                        if (shouldSelectFirst && first != null) {
                            prepareSongInternal(first, autoPlay = false, resetIteration = true)
                        }
                    }
                }
            }
        }
    }

    fun setSelectedTab(tab: MainMenuTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setExpandedFolder(folderName: String?) {
        _uiState.update { it.copy(expandedFolder = if (it.expandedFolder == folderName) null else folderName) }
    }

    fun setExpandedPlaylist(playlistId: String?) {
        _uiState.update { it.copy(expandedPlaylistId = if (it.expandedPlaylistId == playlistId) null else playlistId) }
    }

    fun setExpandedArtist(artistName: String?) {
        _uiState.update { it.copy(expandedArtist = if (it.expandedArtist == artistName) null else artistName) }
    }

    fun selectSong(
        song: LocalSong,
        playlistName: String? = null,
        folderName: String? = null,
        autoPlay: Boolean = true
    ) {
        val effectivePlaylist = playlistName ?: _uiState.value.currentPlaylistName
        val effectiveFolder = folderName ?: song.folderName
        val songConfig = song.savedLoopConfig ?: _uiState.value.loopConfig.copy(
            sectionStartMs = 0L,
            sectionEndMs = null
        )
        val now = System.currentTimeMillis()
        val updatedSong = song.copy(lastPlayedAt = now, folderName = effectiveFolder)
        val updatedSongs = _uiState.value.songs.map { if (it.id == song.id) updatedSong else it }
        repository.saveSongs(updatedSongs)
        val updatedHistory = repository.recordPlayback(updatedSong, effectiveFolder, effectivePlaylist)
        repository.updatePlaylistLastPlayed(effectivePlaylist)
        val updatedPlaylists = repository.loadPlaylists()

        _uiState.update {
            it.copy(
                songs = updatedSongs,
                playlists = updatedPlaylists,
                history = updatedHistory,
                currentSong = updatedSong,
                currentPlaylistName = effectivePlaylist,
                currentFolderName = effectiveFolder,
                loopConfig = songConfig,
                trackDurationMs = song.durationMs,
                currentIteration = 1,
                errorMessage = null,
                statusBannerMessage = null
            )
        }
        repository.saveActiveLoopConfig(songConfig)
        prepareSongInternal(updatedSong, autoPlay = autoPlay, resetIteration = true)
    }

    fun playFolder(folderName: String) {
        val folderSongs = _uiState.value.songs.filter { it.folderName == folderName }
        val firstSong = folderSongs.firstOrNull() ?: return
        selectSong(firstSong, playlistName = "Folder: $folderName", folderName = folderName, autoPlay = true)
    }

    fun playPlaylist(playlist: Playlist) {
        val songMap = _uiState.value.songs.associateBy { it.id }
        val playlistSongs = playlist.songIds.mapNotNull { songMap[it] }
        val firstSong = playlistSongs.firstOrNull() ?: _uiState.value.songs.firstOrNull() ?: return
        selectSong(firstSong, playlistName = playlist.name, folderName = firstSong.folderName, autoPlay = true)
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        val current = _uiState.value.playlists.toMutableList()
        val newPl = Playlist(
            id = "pl_${UUID.randomUUID()}",
            name = name.trim(),
            description = description.trim(),
            songIds = emptyList(),
            createdAt = System.currentTimeMillis(),
            lastPlayedAt = 0L
        )
        current.add(0, newPl)
        repository.savePlaylists(current)
        _uiState.update {
            it.copy(
                playlists = current,
                statusBannerMessage = "Created playlist \"${newPl.name}\""
            )
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        val current = _uiState.value.playlists.toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            val pl = current[idx]
            if (songId !in pl.songIds) {
                current[idx] = pl.copy(songIds = pl.songIds + songId)
                repository.savePlaylists(current)
                _uiState.update {
                    it.copy(
                        playlists = current,
                        statusBannerMessage = "Added track to \"${pl.name}\""
                    )
                }
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val current = _uiState.value.playlists.toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            val pl = current[idx]
            current[idx] = pl.copy(songIds = pl.songIds.filter { it != songId })
            repository.savePlaylists(current)
            _uiState.update {
                it.copy(
                    playlists = current,
                    statusBannerMessage = "Removed track from \"${pl.name}\""
                )
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        val current = _uiState.value.playlists.filterNot { it.id == playlistId }
        repository.savePlaylists(current)
        _uiState.update {
            it.copy(
                playlists = current,
                statusBannerMessage = "Deleted playlist"
            )
        }
    }

    fun clearPlaybackHistory() {
        repository.clearHistory()
        _uiState.update {
            it.copy(
                history = emptyList(),
                statusBannerMessage = "Playback history cleared"
            )
        }
    }

    private fun prepareSongInternal(song: LocalSong, autoPlay: Boolean, resetIteration: Boolean) {
        cancelDelayCountdown()
        stopPositionTicker()
        releaseMediaPlayer()

        val context = getApplication<Application>().applicationContext
        val uri = Uri.parse(song.uriString)
        val player = MediaPlayer()
        mediaPlayer = player
        isPlayerPrepared = false

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(context, uri)
            player.setOnPreparedListener { mp ->
                isPlayerPrepared = true
                val rawDuration = runCatching { mp.duration.toLong() }.getOrDefault(0L)
                val actualDuration = rawDuration.coerceAtLeast(song.durationMs.coerceAtLeast(1000L))
                val cfg = _uiState.value.loopConfig
                val validStart = cfg.sectionStartMs.coerceIn(0L, (actualDuration - 500L).coerceAtLeast(0L))
                val validEnd = cfg.sectionEndMs?.coerceIn((validStart + 500L).coerceAtMost(actualDuration), actualDuration)

                _uiState.update { state ->
                    state.copy(
                        trackDurationMs = actualDuration,
                        currentPositionMs = validStart,
                        currentIteration = if (resetIteration) 1 else state.currentIteration,
                        loopConfig = state.loopConfig.copy(
                            sectionStartMs = validStart,
                            sectionEndMs = validEnd
                        )
                    )
                }

                if (validStart > 0L) {
                    runCatching { mp.seekTo(validStart.toInt()) }
                }

                val sessionId = runCatching { mp.audioSessionId }.getOrDefault(0)
                attachAudioEffects(sessionId)

                if (autoPlay) {
                    startPlayback(mp)
                } else {
                    _uiState.update { it.copy(phase = PlaybackPhase.PAUSED) }
                }
            }

            player.setOnCompletionListener {
                onCurrentRepetitionFinished()
            }

            player.setOnErrorListener { _, what, extra ->
                _uiState.update {
                    it.copy(
                        phase = PlaybackPhase.IDLE,
                        errorMessage = "Audio playback error (code $what/$extra). Verify the file is still on your device."
                    )
                }
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.IDLE,
                    errorMessage = "Unable to open audio file: ${e.localizedMessage ?: "Access denied"}"
                )
            }
        }
    }

    private fun MediaPlayer.safeIsPlaying(): Boolean =
        runCatching { isPlaying }.getOrDefault(false)

    private fun MediaPlayer.safeCurrentPosition(): Int =
        runCatching { currentPosition }.getOrDefault(0)

    private fun startPlayback(mp: MediaPlayer) {
        try {
            val speed = _uiState.value.loopConfig.playbackSpeed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = PlaybackParams().setSpeed(speed)
                } catch (_: Exception) {
                    runCatching { mp.start() }
                }
            }
            if (!mp.safeIsPlaying()) {
                mp.start()
            }
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.PLAYING,
                    delayRemainingMs = 0L,
                    delayTotalMs = 0L,
                    isDelayPaused = false,
                    errorMessage = null
                )
            }
            startPositionTicker()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.PAUSED,
                    errorMessage = "Could not start playback: ${e.localizedMessage ?: ""}"
                )
            }
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        val song = state.currentSong ?: return

        if (state.phase == PlaybackPhase.WAITING_DELAY) {
            // Toggle pause/resume on the active delay countdown
            _uiState.update { it.copy(isDelayPaused = !it.isDelayPaused) }
            return
        }

        val mp = mediaPlayer
        if (mp == null || !isPlayerPrepared) {
            prepareSongInternal(song, autoPlay = true, resetIteration = true)
            return
        }

        if (state.phase == PlaybackPhase.COMPLETED_ALL_LOOPS) {
            // Restart from loop 1
            _uiState.update { it.copy(currentIteration = 1) }
            val startMs = state.loopConfig.sectionStartMs.toInt()
            runCatching { mp.seekTo(startMs) }
            startPlayback(mp)
            return
        }

        if (mp.safeIsPlaying()) {
            runCatching { mp.pause() }
            stopPositionTicker()
            _uiState.update { it.copy(phase = PlaybackPhase.PAUSED) }
        } else {
            val endLimit = state.loopConfig.sectionEndMs ?: state.trackDurationMs
            if (mp.safeCurrentPosition() >= endLimit - 150) {
                runCatching { mp.seekTo(state.loopConfig.sectionStartMs.toInt()) }
            }
            startPlayback(mp)
        }
    }

    fun seekTo(positionMs: Long) {
        val state = _uiState.value
        val clamped = positionMs.coerceIn(0L, state.trackDurationMs.coerceAtLeast(1L))
        _uiState.update { it.copy(currentPositionMs = clamped) }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                mp.seekTo(clamped.toInt())
            } catch (_: Exception) {
            }
        }
    }

    fun skipForward(deltaMs: Long = 5000L) {
        val state = _uiState.value
        seekTo(state.currentPositionMs + deltaMs)
    }

    fun skipBackward(deltaMs: Long = 5000L) {
        val state = _uiState.value
        seekTo(state.currentPositionMs - deltaMs)
    }

    fun skipDelayAndPlayNextRepetitionNow() {
        val state = _uiState.value
        if (state.phase != PlaybackPhase.WAITING_DELAY) return
        cancelDelayCountdown()
        startNextRepetitionImmediately()
    }

    fun addExtraSecondsToActiveDelay(extraSeconds: Int = 5) {
        val state = _uiState.value
        if (state.phase != PlaybackPhase.WAITING_DELAY) return
        val extraMs = extraSeconds * 1000L
        _uiState.update {
            it.copy(
                delayRemainingMs = (it.delayRemainingMs + extraMs).coerceAtLeast(0L),
                delayTotalMs = (it.delayTotalMs + extraMs).coerceAtLeast(1000L)
            )
        }
    }

    fun resetLoopProgress() {
        cancelDelayCountdown()
        val state = _uiState.value
        val startMs = state.loopConfig.sectionStartMs
        _uiState.update {
            it.copy(
                currentIteration = 1,
                currentPositionMs = startMs,
                delayRemainingMs = 0L,
                delayTotalMs = 0L,
                isDelayPaused = false,
                statusBannerMessage = "Loop counter reset to 1"
            )
        }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            runCatching { mp.seekTo(startMs.toInt()) }
            if (state.phase == PlaybackPhase.WAITING_DELAY || state.phase == PlaybackPhase.PLAYING) {
                startPlayback(mp)
            } else {
                _uiState.update { it.copy(phase = PlaybackPhase.PAUSED) }
            }
        }
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && isPlayerPrepared && mp.safeIsPlaying()) {
                    val pos = mp.safeCurrentPosition().toLong()
                    val state = _uiState.value
                    val sectionEnd = state.loopConfig.sectionEndMs
                    _uiState.update { it.copy(currentPositionMs = pos) }

                    if (sectionEnd != null && pos >= sectionEnd) {
                        onCurrentRepetitionFinished()
                        break
                    }
                }
                delay(50L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun onCurrentRepetitionFinished() {
        stopPositionTicker()
        val state = _uiState.value
        val cfg = state.loopConfig
        val mp = mediaPlayer

        try {
            if (mp != null && isPlayerPrepared && mp.isPlaying) {
                mp.pause()
            }
        } catch (_: Exception) {
        }

        val totalCompleted = state.completedIterationsTotal + 1
        val hasMoreLoops = cfg.isInfinite || state.currentIteration < cfg.repeatCount

        if (hasMoreLoops) {
            val delayMs = cfg.calculateDelayMs(state.trackDurationMs)
            if (delayMs > 0L) {
                // Seek back to start point and wait for the configured delay interval
                try {
                    mp?.seekTo(cfg.sectionStartMs.toInt())
                } catch (_: Exception) {
                }
                _uiState.update {
                    it.copy(
                        phase = PlaybackPhase.WAITING_DELAY,
                        currentPositionMs = cfg.sectionStartMs,
                        completedIterationsTotal = totalCompleted,
                        delayRemainingMs = delayMs,
                        delayTotalMs = delayMs,
                        isDelayPaused = false
                    )
                }
                startDelayCountdown()
            } else {
                _uiState.update { it.copy(completedIterationsTotal = totalCompleted) }
                startNextRepetitionImmediately()
            }
        } else {
            // All repetitions for this song completed!
            _uiState.update { it.copy(completedIterationsTotal = totalCompleted) }
            handleAllRepetitionsCompleted()
        }
    }

    private fun startDelayCountdown() {
        cancelDelayCountdown()
        delayCountdownJob = viewModelScope.launch {
            val tickMs = 50L
            while (isActive) {
                delay(tickMs)
                val current = _uiState.value
                if (current.phase != PlaybackPhase.WAITING_DELAY) break
                if (current.isDelayPaused) continue

                val nextRemaining = (current.delayRemainingMs - tickMs).coerceAtLeast(0L)
                _uiState.update { it.copy(delayRemainingMs = nextRemaining) }
                if (nextRemaining <= 0L) {
                    startNextRepetitionImmediately()
                    break
                }
            }
        }
    }

    private fun cancelDelayCountdown() {
        delayCountdownJob?.cancel()
        delayCountdownJob = null
    }

    private fun startNextRepetitionImmediately() {
        val state = _uiState.value
        val nextIteration = state.currentIteration + 1
        val startMs = state.loopConfig.sectionStartMs
        _uiState.update {
            it.copy(
                currentIteration = nextIteration,
                currentPositionMs = startMs,
                delayRemainingMs = 0L,
                delayTotalMs = 0L,
                isDelayPaused = false
            )
        }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                mp.seekTo(startMs.toInt())
                startPlayback(mp)
            } catch (_: Exception) {
                state.currentSong?.let { prepareSongInternal(it, autoPlay = true, resetIteration = false) }
            }
        } else {
            state.currentSong?.let { prepareSongInternal(it, autoPlay = true, resetIteration = false) }
        }
    }

    private fun handleAllRepetitionsCompleted() {
        val state = _uiState.value
        val cfg = state.loopConfig
        val playlist = state.filteredSongs.ifEmpty { state.songs }
        val currentIdx = playlist.indexOfFirst { it.id == state.currentSong?.id }

        when (cfg.afterCompletionAction) {
            AfterLoopAction.STOP -> {
                val startMs = cfg.sectionStartMs
                try {
                    mediaPlayer?.seekTo(startMs.toInt())
                } catch (_: Exception) {
                }
                _uiState.update {
                    it.copy(
                        phase = PlaybackPhase.COMPLETED_ALL_LOOPS,
                        currentPositionMs = startMs,
                        statusBannerMessage = null
                    )
                }
            }
            AfterLoopAction.NEXT_SONG -> {
                if (currentIdx >= 0 && currentIdx + 1 < playlist.size) {
                    selectSong(playlist[currentIdx + 1], autoPlay = true)
                } else {
                    try {
                        mediaPlayer?.seekTo(cfg.sectionStartMs.toInt())
                    } catch (_: Exception) {
                    }
                    _uiState.update {
                        it.copy(
                            phase = PlaybackPhase.COMPLETED_ALL_LOOPS,
                            currentPositionMs = cfg.sectionStartMs,
                            statusBannerMessage = null
                        )
                    }
                }
            }
            AfterLoopAction.REPEAT_PLAYLIST -> {
                if (playlist.isNotEmpty()) {
                    val nextIndex = if (currentIdx >= 0) (currentIdx + 1) % playlist.size else 0
                    selectSong(playlist[nextIndex], autoPlay = true)
                }
            }
        }
    }

    fun playNextSong() {
        val state = _uiState.value
        val playlist = state.filteredSongs.ifEmpty { state.songs }
        if (playlist.isEmpty()) return
        val idx = playlist.indexOfFirst { it.id == state.currentSong?.id }
        val nextIdx = if (idx >= 0) (idx + 1) % playlist.size else 0
        selectSong(playlist[nextIdx], autoPlay = true)
    }

    fun playPreviousSong() {
        val state = _uiState.value
        if (state.currentPositionMs > 4000L) {
            seekTo(state.loopConfig.sectionStartMs)
            return
        }
        val playlist = state.filteredSongs.ifEmpty { state.songs }
        if (playlist.isEmpty()) return
        val idx = playlist.indexOfFirst { it.id == state.currentSong?.id }
        val prevIdx = if (idx > 0) idx - 1 else playlist.lastIndex
        selectSong(playlist[prevIdx], autoPlay = true)
    }

    // Loop Configuration Updates
    fun setRepeatCount(count: Int) {
        val sanitized = count.coerceIn(0, 999)
        updateLoopConfig { it.copy(repeatCount = sanitized) }
    }

    fun incrementRepeatCount(delta: Int) {
        val current = _uiState.value.loopConfig.repeatCount
        val base = if (current <= 0) 1 else current
        val next = (base + delta).coerceIn(1, 999)
        setRepeatCount(next)
    }

    fun setDelaySeconds(seconds: Int) {
        val sanitized = seconds.coerceIn(0, 600)
        updateLoopConfig {
            it.copy(
                delaySeconds = sanitized,
                matchTrackDurationDelay = false
            )
        }
    }

    fun incrementDelaySeconds(delta: Int) {
        val current = _uiState.value.loopConfig.delaySeconds
        setDelaySeconds((current + delta).coerceIn(0, 600))
    }

    fun setMatchTrackDurationDelay(enabled: Boolean) {
        updateLoopConfig { it.copy(matchTrackDurationDelay = enabled) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        updateLoopConfig { it.copy(playbackSpeed = clamped) }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared && mp.isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mp.playbackParams = PlaybackParams().setSpeed(clamped)
            } catch (_: Exception) {
            }
        }
    }

    fun setAfterCompletionAction(action: AfterLoopAction) {
        updateLoopConfig { it.copy(afterCompletionAction = action) }
    }

    fun markCurrentPositionAsSectionStart() {
        val state = _uiState.value
        val pos = state.currentPositionMs.coerceIn(0L, (state.trackDurationMs - 500L).coerceAtLeast(0L))
        val currentEnd = state.loopConfig.sectionEndMs
        val validEnd = if (currentEnd != null && currentEnd <= pos + 500L) null else currentEnd
        updateLoopConfig {
            it.copy(
                sectionStartMs = pos,
                sectionEndMs = validEnd
            )
        }
        _uiState.update { it.copy(statusBannerMessage = "Set Loop Start [A] at ${formatDurationMillis(pos)}") }
    }

    fun markCurrentPositionAsSectionEnd() {
        val state = _uiState.value
        val minEnd = (state.loopConfig.sectionStartMs + 500L).coerceAtMost(state.trackDurationMs)
        val pos = state.currentPositionMs.coerceIn(minEnd, state.trackDurationMs)
        updateLoopConfig { it.copy(sectionEndMs = pos) }
        _uiState.update { it.copy(statusBannerMessage = "Set Loop End [B] at ${formatDurationMillis(pos)}") }
    }

    fun clearAbSection() {
        updateLoopConfig {
            it.copy(
                sectionStartMs = 0L,
                sectionEndMs = null
            )
        }
        _uiState.update { it.copy(statusBannerMessage = "Cleared A–B markers (Full Song Loop)") }
    }

    fun saveLoopConfigToCurrentSong() {
        val state = _uiState.value
        val song = state.currentSong ?: return
        val updatedSong = song.copy(savedLoopConfig = state.loopConfig)
        val updatedList = state.songs.map { if (it.id == song.id) updatedSong else it }
        repository.saveSongs(updatedList)
        _uiState.update {
            it.copy(
                songs = updatedList,
                currentSong = updatedSong,
                statusBannerMessage = "Saved ${state.loopConfig.formatSummary()} to \"${song.title}\""
            )
        }
    }

    fun applyPreset(preset: LoopPreset) {
        updateLoopConfig { current ->
            current.copy(
                repeatCount = preset.repeatCount,
                delaySeconds = preset.delaySeconds,
                matchTrackDurationDelay = preset.matchTrackDurationDelay,
                playbackSpeed = preset.playbackSpeed,
                afterCompletionAction = preset.afterCompletionAction
            )
        }
        setPlaybackSpeed(preset.playbackSpeed)
        _uiState.update {
            it.copy(
                currentIteration = 1,
                statusBannerMessage = "Applied preset: ${preset.name}"
            )
        }
    }

    fun createCustomPreset(name: String, description: String) {
        val cfg = _uiState.value.loopConfig
        val newPreset = LoopPreset(
            id = "custom_${UUID.randomUUID()}",
            name = name.trim().ifBlank { cfg.formatSummary() },
            description = description.trim().ifBlank {
                "Custom loop preset (${cfg.formatSummary()} • ${cfg.playbackSpeed}x speed)"
            },
            repeatCount = cfg.repeatCount,
            delaySeconds = cfg.delaySeconds,
            matchTrackDurationDelay = cfg.matchTrackDurationDelay,
            playbackSpeed = cfg.playbackSpeed,
            afterCompletionAction = cfg.afterCompletionAction,
            isBuiltIn = false
        )
        val updatedPresets = repository.saveCustomPreset(newPreset)
        _uiState.update {
            it.copy(
                presets = updatedPresets,
                statusBannerMessage = "Saved preset \"${newPreset.name}\""
            )
        }
    }

    fun deleteCustomPreset(presetId: String) {
        val updated = repository.deleteCustomPreset(presetId)
        _uiState.update { it.copy(presets = updated) }
    }

    fun importSongsFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) { repository.importAudioUris(uris) }
            if (imported.isNotEmpty()) {
                mergeImportedSongs(imported)
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Could not read selected audio files.")
                }
            }
        }
    }

    fun importSongsFromFolder(treeUri: Uri?) {
        if (treeUri == null) return
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) { repository.importAudioFolder(treeUri) }
            if (imported.isNotEmpty()) {
                mergeImportedSongs(imported)
            } else {
                _uiState.update {
                    it.copy(statusBannerMessage = "No supported audio files found in selected folder.")
                }
            }
        }
    }

    private fun mergeImportedSongs(imported: List<LocalSong>) {
        val existing = _uiState.value.songs.toMutableList()
        val existingUris = existing.map { it.uriString }.toSet()
        val newOnes = imported.filter { it.uriString !in existingUris }
        val combined = newOnes + existing
        repository.saveSongs(combined)

        val newlySelected = newOnes.firstOrNull() ?: imported.first()
        _uiState.update {
            it.copy(
                songs = combined,
                libraryFilter = LibraryFilter.ALL,
                statusBannerMessage = "Imported ${imported.size} local audio track(s)!"
            )
        }
        selectSong(newlySelected, autoPlay = true)
    }

    fun removeSong(songId: String) {
        val currentList = _uiState.value.songs
        val updated = currentList.filterNot { it.id == songId }
        repository.saveSongs(updated)
        val wasCurrent = _uiState.value.currentSong?.id == songId
        _uiState.update {
            it.copy(
                songs = updated,
                statusBannerMessage = "Removed track from library"
            )
        }
        if (wasCurrent) {
            val nextSong = updated.firstOrNull()
            if (nextSong != null) {
                selectSong(nextSong, autoPlay = false)
            } else {
                cancelDelayCountdown()
                stopPositionTicker()
                releaseMediaPlayer()
                _uiState.update {
                    it.copy(
                        currentSong = null,
                        phase = PlaybackPhase.IDLE,
                        currentPositionMs = 0L,
                        trackDurationMs = 0L
                    )
                }
            }
        }
    }

    fun restoreBuiltInSamples() {
        val samples = repository.buildBuiltInSampleSongs()
        val current = _uiState.value.songs.filterNot { it.isBuiltInSample }
        val combined = current + samples
        repository.saveSongs(combined)
        _uiState.update {
            it.copy(
                songs = combined,
                statusBannerMessage = "Restored built-in studio loop samples"
            )
        }
        if (_uiState.value.currentSong == null && combined.isNotEmpty()) {
            selectSong(combined.first(), autoPlay = false)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(libraryFilter = filter) }
    }

    fun clearBannerMessages() {
        _uiState.update { it.copy(statusBannerMessage = null, errorMessage = null) }
    }

    private fun updateLoopConfig(transform: (LoopConfig) -> LoopConfig) {
        val updated = transform(_uiState.value.loopConfig)
        _uiState.update { it.copy(loopConfig = updated) }
        repository.saveActiveLoopConfig(updated)
    }

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private fun attachAudioEffects(audioSessionId: Int) {
        releaseAudioEffects()
        if (audioSessionId <= 0) return
        try {
            // 1. Equalizer
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = _uiState.value.equalizerState.isEnabled
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]

            val currentBands = _uiState.value.equalizerState.bands
            val updatedBands = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val bandIdx = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIdx) / 1000 // mHz to Hz
                val existing = currentBands.find { it.bandIndex == bandIdx }
                val targetLevel = existing?.levelMb ?: 0.toShort()
                try {
                    eq.setBandLevel(bandIdx, targetLevel)
                } catch (_: Exception) {}
                updatedBands.add(
                    EqualizerBand(
                        bandIndex = bandIdx,
                        centerFreqHz = centerFreq,
                        levelMb = targetLevel,
                        minLevelMb = minLevel,
                        maxLevelMb = maxLevel
                    )
                )
            }
            equalizer = eq
            _uiState.update {
                it.copy(equalizerState = it.equalizerState.copy(bands = updatedBands))
            }
        } catch (_: Exception) {
            equalizer = null
        }

        try {
            // 2. LoudnessEnhancer (VLC 200% volume boost)
            val le = LoudnessEnhancer(audioSessionId)
            le.enabled = true
            loudnessEnhancer = le
        } catch (_: Exception) {
            loudnessEnhancer = null
        }

        try {
            // 3. BassBoost
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = true
            bb.setStrength(_uiState.value.equalizerState.bassBoostStrength.toShort())
            bassBoost = bb
        } catch (_: Exception) {
            bassBoost = null
        }

        try {
            // 4. Virtualizer
            val virt = Virtualizer(0, audioSessionId)
            virt.enabled = true
            virt.setStrength(_uiState.value.equalizerState.virtualizerStrength.toShort())
            virtualizer = virt
        } catch (_: Exception) {
            virtualizer = null
        }

        applyVolumeAndBoostInternal()
    }

    private fun releaseAudioEffects() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        loudnessEnhancer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (_: Exception) {}
        bassBoost = null

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Exception) {}
        virtualizer = null
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 200)
        _uiState.update { it.copy(volumePercent = clamped, isMuted = false) }
        applyVolumeAndBoostInternal()
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        applyVolumeAndBoostInternal()
    }

    fun setShowEqualizer(show: Boolean) {
        _uiState.update { it.copy(isShowingEqualizer = show) }
    }

    fun toggleShowEqualizer() {
        _uiState.update { it.copy(isShowingEqualizer = !it.isShowingEqualizer) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {}
        _uiState.update {
            it.copy(equalizerState = it.equalizerState.copy(isEnabled = enabled))
        }
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMb)
        } catch (_: Exception) {}
        _uiState.update { state ->
            val updated = state.equalizerState.bands.map {
                if (it.bandIndex == bandIndex) it.copy(levelMb = levelMb) else it
            }
            state.copy(
                equalizerState = state.equalizerState.copy(
                    bands = updated,
                    currentPreset = "Custom"
                )
            )
        }
    }

    fun setEqualizerPreset(presetName: String) {
        val gains = when (presetName) {
            "Bass Boost" -> listOf(600, 450, 0, -200, -300)
            "Vocal / Recitation" -> listOf(-300, 0, 500, 600, 300)
            "Treble Boost" -> listOf(-400, -200, 0, 450, 700)
            "Acoustic" -> listOf(350, 200, 0, 300, 400)
            "Speech" -> listOf(-500, 0, 600, 400, -200)
            else -> listOf(0, 0, 0, 0, 0) // Flat
        }

        _uiState.update { state ->
            val updated = state.equalizerState.bands.mapIndexed { idx, band ->
                val level = (gains.getOrNull(idx) ?: 0).toShort()
                try {
                    equalizer?.setBandLevel(band.bandIndex, level)
                } catch (_: Exception) {}
                band.copy(levelMb = level)
            }
            state.copy(
                equalizerState = state.equalizerState.copy(
                    currentPreset = presetName,
                    bands = updated
                )
            )
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(clamped.toShort())
        } catch (_: Exception) {}
        _uiState.update {
            it.copy(equalizerState = it.equalizerState.copy(bassBoostStrength = clamped))
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(clamped.toShort())
        } catch (_: Exception) {}
        _uiState.update {
            it.copy(equalizerState = it.equalizerState.copy(virtualizerStrength = clamped))
        }
    }

    private fun applyVolumeAndBoostInternal() {
        val state = _uiState.value
        val mp = mediaPlayer ?: return
        if (!isPlayerPrepared) return

        if (state.isMuted || state.volumePercent == 0) {
            try {
                mp.setVolume(0f, 0f)
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {}
            return
        }

        val vol = state.volumePercent
        if (vol <= 100) {
            val scalar = vol / 100f
            try {
                mp.setVolume(scalar, scalar)
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {}
        } else {
            // VLC 200% volume boost:
            // 101% to 200% maps to +100 mB to +8000 mB gain (+8dB boost)
            val boostRatio = (vol - 100) / 100f
            val targetGainMb = (boostRatio * 8000f).toInt().coerceIn(0, 8000)
            try {
                mp.setVolume(1.0f, 1.0f)
                loudnessEnhancer?.setTargetGain(targetGainMb)
            } catch (_: Exception) {}
        }
    }

    private fun releaseMediaPlayer() {
        isPlayerPrepared = false
        releaseAudioEffects()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelDelayCountdown()
        stopPositionTicker()
        releaseMediaPlayer()
    }
}

fun formatDurationMillis(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

fun formatDelayPreciseMillis(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val seconds = clamped / 1000L
    val tenths = (clamped % 1000L) / 100L
    return "%02d.%ds".format(seconds, tenths)
}
