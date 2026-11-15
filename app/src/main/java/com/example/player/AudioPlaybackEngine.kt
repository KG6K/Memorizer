package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.example.BuildConfig
import com.example.data.SongRepository
import com.example.model.AfterLoopAction
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AudioPlaybackEngine private constructor(private val appContext: Context) {

    companion object {
        @Volatile
        private var instance: AudioPlaybackEngine? = null

        fun getInstance(context: Context): AudioPlaybackEngine {
            val appCtx = context.applicationContext
            val existing = instance
            if (existing != null && existing.appContext === appCtx) {
                return existing
            }
            return synchronized(this) {
                val current = instance
                if (current != null && current.appContext === appCtx) {
                    current
                } else {
                    current?.disposeForReinit()
                    AudioPlaybackEngine(appCtx).also { instance = it }
                }
            }
        }
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun disposeForReinit() {
        scanLibraryJob?.cancel()
        sleepTimerJob?.cancel()
        positionTickerJob?.cancel()
        delayCountdownJob?.cancel()
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        effectsController.release()
        volumeController.unregister()
        runCatching { (engineScope.coroutineContext[Job])?.cancel() }
    }
    private val repository = SongRepository(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val effectsController = AudioEffectsController()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val volumeController = SystemVolumeController(
        appContext = appContext,
        audioManager = audioManager,
        scope = engineScope,
        getCurrentVolumePercent = { _uiState.value.volumePercent },
        onVolumeStateSynced = { sysVol, muted ->
            _uiState.update { it.copy(volumePercent = sysVol, isMuted = muted) }
            applyVolumeAndBoostInternal(syncToSystem = false)
        },
        onSetBoostVolumePercent = { boostPercent -> setVolumePercent(boostPercent) }
    )

    private var mediaPlayer: MediaPlayer? = null
    private var positionTickerJob: Job? = null
    private var delayCountdownJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var scanLibraryJob: Job? = null
    private var isPlayerPrepared = false

    init {
        volumeController.register()
        val initialSysVol = volumeController.readSystemVolumePercent()

        engineScope.launch {
            val initialConfig = withContext(Dispatchers.IO) { repository.loadActiveLoopConfig() }
            val initialPresets = withContext(Dispatchers.IO) { repository.loadPresets() }
            val initialSongs = withContext(Dispatchers.IO) { repository.loadSongs() }
            val initialPlaylists = withContext(Dispatchers.IO) { repository.loadPlaylists() }
            val initialSortOption = withContext(Dispatchers.IO) { repository.loadSortOption() }
            val initialSortDirection = withContext(Dispatchers.IO) { repository.loadSortDirection() }
            val initialRecentSessions = withContext(Dispatchers.IO) { repository.loadRecentSessions() }
            val initialScanFolders = withContext(Dispatchers.IO) { repository.loadScanFolders() }
            val firstSong = initialSongs.firstOrNull()
            val activeConfig = firstSong?.savedLoopConfig ?: initialConfig

            _uiState.value = PlayerUiState(
                songs = initialSongs,
                playlists = initialPlaylists,
                sortOption = initialSortOption,
                sortDirection = initialSortDirection,
                recentSessions = initialRecentSessions,
                scanFolders = initialScanFolders,
                currentSong = firstSong,
                currentFolderName = firstSong?.folderName ?: "",
                currentPlaylistName = "All Songs",
                loopConfig = activeConfig,
                trackDurationMs = firstSong?.durationMs ?: 0L,
                presets = initialPresets,
                volumePercent = initialSysVol,
                isMuted = initialSysVol == 0
            )

            if (firstSong != null) {
                prepareSongInternal(firstSong, autoPlay = false, resetIteration = true)
            }

            scanDeviceAudio()
        }
    }

    fun getLivePositionMs(): Long {
        val mp = mediaPlayer
        return if (mp != null && isPlayerPrepared) {
            runCatching { mp.currentPosition.toLong() }.getOrDefault(_uiState.value.currentPositionMs)
        } else {
            _uiState.value.currentPositionMs
        }
    }

    fun shouldInterceptVolumeKeyForBoost(isVolumeUp: Boolean): Boolean =
        volumeController.shouldInterceptVolumeKeyForBoost(isVolumeUp)

    fun syncFromSystemVolumeAfterHardwareKey() =
        volumeController.syncFromSystemVolumeAfterHardwareKey()

    fun handleHardwareVolumeKey(isVolumeUp: Boolean): Boolean =
        volumeController.handleHardwareVolumeKey(isVolumeUp)

    fun scanDeviceAudio() {
        val configuredFolders = _uiState.value.scanFolders
        if (configuredFolders.isNotEmpty()) {
            rescanConfiguredFoldersInternal(switchToFirstIfNeeded = false)
        } else {
            scanAllDeviceAudioInternal(replaceExisting = false)
        }
    }

    fun rescanLibrary() {
        scanDeviceAudio()
    }

    fun addScanFolder(treeUri: Uri?) {
        if (treeUri == null) return
        engineScope.launch {
            val uriStr = treeUri.toString()
            val displayName = withContext(Dispatchers.IO) {
                repository.takePersistableReadPermission(treeUri)
                repository.extractFolderDisplayName(treeUri)
            }
            val existing = _uiState.value.scanFolders
            val updatedFolders = if (existing.any { it.uriString == uriStr }) {
                existing
            } else {
                existing + ScanFolderEntry(
                    uriString = uriStr,
                    displayName = displayName,
                    trackCount = 0,
                    addedAt = System.currentTimeMillis()
                )
            }
            _uiState.update { it.copy(scanFolders = updatedFolders) }
            withContext(Dispatchers.IO) { repository.saveScanFolders(updatedFolders) }
            rescanConfiguredFoldersInternal(switchToFirstIfNeeded = true)
        }
    }

    fun removeScanFolder(uriString: String) {
        scanLibraryJob?.cancel()
        val current = _uiState.value.scanFolders
        val updated = current.filterNot { it.uriString == uriString }
        _uiState.update { it.copy(scanFolders = updated) }
        scanLibraryJob = engineScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveScanFolders(updated)
                repository.releasePersistableReadPermission(uriString)
            }
            if (updated.isNotEmpty()) {
                rescanConfiguredFoldersInternal(switchToFirstIfNeeded = true)
            } else {
                scanAllDeviceAudioInternal(replaceExisting = true)
            }
        }
    }

    fun clearScanFoldersResetToAllDevice() {
        scanLibraryJob?.cancel()
        val oldFolders = _uiState.value.scanFolders
        _uiState.update { it.copy(scanFolders = emptyList()) }
        scanLibraryJob = engineScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveScanFolders(emptyList())
                oldFolders.forEach { repository.releasePersistableReadPermission(it.uriString) }
            }
            scanAllDeviceAudioInternal(replaceExisting = true)
        }
    }

    private fun rescanConfiguredFoldersInternal(switchToFirstIfNeeded: Boolean) {
        scanLibraryJob?.cancel()
        val foldersSnapshot = _uiState.value.scanFolders
        if (foldersSnapshot.isEmpty()) return
        val existingSongsSnapshot = _uiState.value.songs
        _uiState.update { it.copy(isScanningLibrary = true) }

        scanLibraryJob = engineScope.launch(Dispatchers.IO) {
            val (scopedSongs, updatedEntries) = repository.scanSelectedFolders(
                scanFolders = foldersSnapshot,
                existingSongs = existingSongsSnapshot
            )
            if (!isActive || _uiState.value.scanFolders.map { it.uriString } != foldersSnapshot.map { it.uriString }) {
                return@launch
            }
            repository.saveSongs(scopedSongs)
            repository.saveScanFolders(updatedEntries)

            withContext(Dispatchers.Main) {
                if (_uiState.value.scanFolders.map { it.uriString } != foldersSnapshot.map { it.uriString }) {
                    return@withContext
                }
                val prevCurrent = _uiState.value.currentSong
                val currentStillValid = prevCurrent != null && scopedSongs.any { it.id == prevCurrent.id }
                val nextCurrent = if (currentStillValid) {
                    scopedSongs.first { it.id == prevCurrent!!.id }
                } else if (switchToFirstIfNeeded || prevCurrent == null) {
                    scopedSongs.firstOrNull()
                } else {
                    prevCurrent
                }

                _uiState.update { state ->
                    state.copy(
                        songs = scopedSongs,
                        scanFolders = updatedEntries,
                        isScanningLibrary = false,
                        currentSong = nextCurrent,
                        currentFolderName = if (currentStillValid) {
                            nextCurrent?.folderName ?: state.currentFolderName
                        } else {
                            nextCurrent?.folderName ?: ""
                        },
                        trackDurationMs = if (currentStillValid) state.trackDurationMs else (nextCurrent?.durationMs ?: 0L),
                        expandedFolder = state.expandedFolder?.takeIf { exp -> scopedSongs.any { it.folderName == exp } },
                        statusBannerMessage = "Loaded ${scopedSongs.size} track(s) from ${updatedEntries.size} selected folder(s)"
                    )
                }

                if (!currentStillValid) {
                    if (nextCurrent != null) {
                        prepareSongInternal(nextCurrent, autoPlay = false, resetIteration = true)
                    } else {
                        cancelDelayCountdown()
                        stopPositionTicker()
                        releaseMediaPlayer()
                        _uiState.update {
                            it.copy(
                                phase = PlaybackPhase.IDLE,
                                currentPositionMs = 0L,
                                trackDurationMs = 0L,
                                notificationSessionVisible = false,
                                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scanAllDeviceAudioInternal(replaceExisting: Boolean) {
        scanLibraryJob?.cancel()
        _uiState.update { it.copy(isScanningLibrary = true) }
        scanLibraryJob = engineScope.launch(Dispatchers.IO) {
            val scanned = repository.scanDeviceAudioMedia()
            val mergedSongs = if (replaceExisting) {
                val existingByUri = _uiState.value.songs.associateBy { it.uriString }
                val restored = scanned.map { item ->
                    val prev = existingByUri[item.uriString]
                    if (prev != null) {
                        item.copy(
                            id = prev.id,
                            savedLoopConfig = prev.savedLoopConfig,
                            lastPlayedAt = prev.lastPlayedAt
                        )
                    } else {
                        item
                    }
                }
                if (restored.isEmpty() && BuildConfig.INCLUDE_TEST_SONGS) {
                    repository.buildBuiltInSampleSongs()
                } else {
                    restored
                }
            } else {
                val currentSongs = _uiState.value.songs
                    .filterNot {
                        !BuildConfig.INCLUDE_TEST_SONGS &&
                            (it.isBuiltInSample || it.id.startsWith("builtin_sample_"))
                    }
                    .toMutableList()
                val existingUris = currentSongs.map { it.uriString }.toSet()
                val newItems = scanned.filter { it.uriString !in existingUris }
                currentSongs.addAll(newItems)
                currentSongs
            }

            repository.saveSongs(mergedSongs)

            withContext(Dispatchers.Main) {
                val prevCurrent = _uiState.value.currentSong
                val currentValid = prevCurrent != null && mergedSongs.any { it.id == prevCurrent.id }
                val first = mergedSongs.firstOrNull()
                val targetSong = if (currentValid) prevCurrent else first
                _uiState.update {
                    it.copy(
                        songs = mergedSongs,
                        isScanningLibrary = false,
                        currentSong = targetSong,
                        currentFolderName = if (currentValid) it.currentFolderName else (targetSong?.folderName ?: ""),
                        trackDurationMs = if (currentValid) it.trackDurationMs else (targetSong?.durationMs ?: 0L),
                        statusBannerMessage = if (replaceExisting) {
                            "Scanned ${mergedSongs.size} track(s) from all device audio"
                        } else {
                            it.statusBannerMessage
                        }
                    )
                }
                if (!currentValid && targetSong != null) {
                    prepareSongInternal(targetSong, autoPlay = false, resetIteration = true)
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

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        engineScope.launch(Dispatchers.IO) { repository.saveSortOption(option) }
    }

    fun setSortDirection(direction: SortDirection) {
        _uiState.update { it.copy(sortDirection = direction) }
        engineScope.launch(Dispatchers.IO) { repository.saveSortDirection(direction) }
    }

    fun openFolderFromRecent(folderName: String) {
        _uiState.update {
            it.copy(
                selectedTab = MainMenuTab.FOLDERS,
                expandedFolder = folderName
            )
        }
    }

    fun openPlaylistFromRecent(playlistId: String) {
        _uiState.update {
            it.copy(
                selectedTab = MainMenuTab.PLAYLISTS,
                expandedPlaylistId = playlistId
            )
        }
    }

    fun selectSong(
        song: LocalSong,
        playlistName: String? = null,
        folderName: String? = null,
        autoPlay: Boolean = true
    ) {
        selectSongInternal(
            song = song,
            playlistName = playlistName,
            folderName = folderName,
            autoPlay = autoPlay,
            preserveQueueAndSession = false,
            overrideLoopConfig = null
        )
    }

    private fun selectSongInternal(
        song: LocalSong,
        playlistName: String?,
        folderName: String?,
        autoPlay: Boolean,
        preserveQueueAndSession: Boolean,
        overrideLoopConfig: LoopConfig?
    ) {
        val state = _uiState.value
        val effectivePlaylist = playlistName ?: state.currentPlaylistName
        val effectiveFolder = folderName ?: song.folderName
        val songConfig = overrideLoopConfig
            ?: song.savedLoopConfig
            ?: state.loopConfig.copy(sectionStartMs = 0L, sectionEndMs = null)
        val now = System.currentTimeMillis()
        val updatedSong = song.copy(lastPlayedAt = now, folderName = effectiveFolder)
        val updatedSongs = state.songs.map { if (it.id == song.id) updatedSong else it }
        val updatedPlaylists = state.playlists.map { pl ->
            if (pl.name.equals(effectivePlaylist, ignoreCase = true)) pl.copy(lastPlayedAt = now) else pl
        }

        val nextSessionKey = if (preserveQueueAndSession) state.activeRecentSessionKey else null
        val updatedSessions = if (preserveQueueAndSession && nextSessionKey != null) {
            state.recentSessions.map { session ->
                if (session.key == nextSessionKey) {
                    session.copy(lastSongId = updatedSong.id, lastPlayedAt = now)
                } else {
                    session
                }
            }.sortedByDescending { it.lastPlayedAt }
        } else {
            state.recentSessions
        }

        _uiState.update {
            it.copy(
                songs = updatedSongs,
                playlists = updatedPlaylists,
                recentSessions = updatedSessions,
                activeRecentSessionKey = nextSessionKey,
                customPlaybackQueue = if (preserveQueueAndSession) it.customPlaybackQueue else emptyList(),
                currentSong = updatedSong,
                currentPlaylistName = effectivePlaylist,
                currentFolderName = effectiveFolder,
                loopConfig = songConfig,
                trackDurationMs = song.durationMs,
                currentIteration = 1,
                errorMessage = null,
                statusBannerMessage = null,
                notificationSessionVisible = it.notificationSessionVisible || autoPlay,
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
            )
        }
        engineScope.launch(Dispatchers.IO) {
            repository.saveSongs(updatedSongs)
            repository.updatePlaylistLastPlayed(effectivePlaylist)
            repository.saveActiveLoopConfig(songConfig)
            if (preserveQueueAndSession && nextSessionKey != null) {
                repository.saveRecentSessions(updatedSessions)
            }
        }
        prepareSongInternal(updatedSong, autoPlay = autoPlay, resetIteration = true)
    }

    private fun upsertRecentSession(session: RecentCollectionSession): List<RecentCollectionSession> {
        val current = _uiState.value.recentSessions.filterNot { it.key == session.key }
        return (listOf(session) + current).sortedByDescending { it.lastPlayedAt }.take(20)
    }

    fun playFolder(folderName: String) {
        val state = _uiState.value
        val folderSongs = com.example.model.sortSongs(
            state.songs.filter { it.folderName == folderName },
            state.sortOption,
            state.sortDirection
        )
        val firstSong = folderSongs.firstOrNull() ?: return
        val sessionConfig = firstSong.savedLoopConfig ?: state.loopConfig.copy(
            sectionStartMs = 0L,
            sectionEndMs = null
        )
        val now = System.currentTimeMillis()
        val session = RecentCollectionSession(
            key = "folder:$folderName",
            isPlaylist = false,
            name = folderName,
            folderName = folderName,
            songIds = folderSongs.map { it.id },
            lastSongId = firstSong.id,
            loopConfig = sessionConfig,
            isSubSelection = false,
            lastPlayedAt = now
        )
        val updatedSessions = upsertRecentSession(session)
        _uiState.update {
            it.copy(
                customPlaybackQueue = emptyList(),
                activeRecentSessionKey = session.key,
                recentSessions = updatedSessions
            )
        }
        engineScope.launch(Dispatchers.IO) { repository.saveRecentSessions(updatedSessions) }
        selectSongInternal(
            song = firstSong,
            playlistName = "Folder: $folderName",
            folderName = folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = sessionConfig
        )
    }

    fun playPlaylist(playlist: Playlist) {
        val state = _uiState.value
        val songMap = state.songs.associateBy { it.id }
        val playlistSongs = com.example.model.sortSongs(
            playlist.songIds.mapNotNull { songMap[it] },
            state.sortOption,
            state.sortDirection
        )
        val firstSong = playlistSongs.firstOrNull() ?: return
        val sessionConfig = firstSong.savedLoopConfig ?: state.loopConfig.copy(
            sectionStartMs = 0L,
            sectionEndMs = null
        )
        val now = System.currentTimeMillis()
        val session = RecentCollectionSession(
            key = "playlist:${playlist.id}",
            isPlaylist = true,
            name = playlist.name,
            playlistId = playlist.id,
            songIds = playlistSongs.map { it.id },
            lastSongId = firstSong.id,
            loopConfig = sessionConfig,
            isSubSelection = false,
            lastPlayedAt = now
        )
        val updatedSessions = upsertRecentSession(session)
        _uiState.update {
            it.copy(
                customPlaybackQueue = emptyList(),
                activeRecentSessionKey = session.key,
                recentSessions = updatedSessions
            )
        }
        engineScope.launch(Dispatchers.IO) { repository.saveRecentSessions(updatedSessions) }
        selectSongInternal(
            song = firstSong,
            playlistName = playlist.name,
            folderName = firstSong.folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = sessionConfig
        )
    }

    fun playSelectedSongs(
        songs: List<LocalSong>,
        sourceFolderName: String? = null,
        sourcePlaylistId: String? = null,
        sourcePlaylistName: String? = null
    ) {
        if (songs.isEmpty()) return
        val firstSong = songs.first()
        val inferredFolder = sourceFolderName
            ?: songs.map { it.folderName }.distinct().singleOrNull()
        val queueTitle = when {
            !sourcePlaylistName.isNullOrBlank() -> sourcePlaylistName
            !inferredFolder.isNullOrBlank() -> "Folder: $inferredFolder"
            else -> "Selection (${songs.size})"
        }
        val songConfig = firstSong.savedLoopConfig ?: _uiState.value.loopConfig.copy(
            sectionStartMs = 0L,
            sectionEndMs = null
        )
        val now = System.currentTimeMillis()
        val updatedSong = firstSong.copy(lastPlayedAt = now)
        val updatedSongs = _uiState.value.songs.map { if (it.id == firstSong.id) updatedSong else it }
        val updatedPlaylists = if (sourcePlaylistId != null) {
            _uiState.value.playlists.map { pl ->
                if (pl.id == sourcePlaylistId) pl.copy(lastPlayedAt = now) else pl
            }
        } else {
            _uiState.value.playlists
        }

        val sessionKey: String?
        val updatedSessions: List<RecentCollectionSession>
        if (sourcePlaylistId != null && !sourcePlaylistName.isNullOrBlank()) {
            val session = RecentCollectionSession(
                key = "playlist:$sourcePlaylistId",
                isPlaylist = true,
                name = sourcePlaylistName,
                playlistId = sourcePlaylistId,
                songIds = songs.map { it.id },
                lastSongId = updatedSong.id,
                loopConfig = songConfig,
                isSubSelection = true,
                lastPlayedAt = now
            )
            sessionKey = session.key
            updatedSessions = upsertRecentSession(session)
        } else if (!inferredFolder.isNullOrBlank()) {
            val session = RecentCollectionSession(
                key = "folder:$inferredFolder",
                isPlaylist = false,
                name = inferredFolder,
                folderName = inferredFolder,
                songIds = songs.map { it.id },
                lastSongId = updatedSong.id,
                loopConfig = songConfig,
                isSubSelection = true,
                lastPlayedAt = now
            )
            sessionKey = session.key
            updatedSessions = upsertRecentSession(session)
        } else {
            sessionKey = null
            updatedSessions = _uiState.value.recentSessions
        }

        _uiState.update {
            it.copy(
                customPlaybackQueue = songs,
                songs = updatedSongs,
                playlists = updatedPlaylists,
                recentSessions = updatedSessions,
                activeRecentSessionKey = sessionKey,
                currentSong = updatedSong,
                currentPlaylistName = queueTitle,
                currentFolderName = inferredFolder ?: firstSong.folderName,
                loopConfig = songConfig,
                trackDurationMs = firstSong.durationMs,
                currentIteration = 1,
                errorMessage = null,
                statusBannerMessage = "Playing ${songs.size} selected songs",
                notificationSessionVisible = true,
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
            )
        }
        engineScope.launch(Dispatchers.IO) {
            repository.saveSongs(updatedSongs)
            if (sourcePlaylistId != null) {
                repository.savePlaylists(updatedPlaylists)
            }
            repository.saveActiveLoopConfig(songConfig)
            if (sessionKey != null) {
                repository.saveRecentSessions(updatedSessions)
            }
        }
        prepareSongInternal(updatedSong, autoPlay = true, resetIteration = true)
    }

    fun playRecentFolderSession(folderName: String) {
        val state = _uiState.value
        val session = state.recentSessions.firstOrNull { !it.isPlaylist && it.key == "folder:$folderName" }
        if (session == null) {
            playFolder(folderName)
            return
        }
        val songMap = state.songs.associateBy { it.id }
        val resolvedSongs = session.songIds.mapNotNull { songMap[it] }
        if (resolvedSongs.isEmpty()) {
            playFolder(folderName)
            return
        }
        val firstSong = resolvedSongs.first()
        val now = System.currentTimeMillis()
        val updatedSession = session.copy(lastSongId = firstSong.id, lastPlayedAt = now)
        val updatedSessions = upsertRecentSession(updatedSession)
        _uiState.update {
            it.copy(
                customPlaybackQueue = if (session.isSubSelection) resolvedSongs else emptyList(),
                activeRecentSessionKey = session.key,
                recentSessions = updatedSessions
            )
        }
        engineScope.launch(Dispatchers.IO) { repository.saveRecentSessions(updatedSessions) }
        selectSongInternal(
            song = firstSong,
            playlistName = "Folder: $folderName",
            folderName = folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = session.loopConfig
        )
    }

    fun playRecentPlaylistSession(playlistId: String) {
        val state = _uiState.value
        val playlist = state.playlists.firstOrNull { it.id == playlistId } ?: return
        val session = state.recentSessions.firstOrNull { it.isPlaylist && it.key == "playlist:$playlistId" }
        if (session == null) {
            playPlaylist(playlist)
            return
        }
        val songMap = state.songs.associateBy { it.id }
        val resolvedSongs = session.songIds.mapNotNull { songMap[it] }
        if (resolvedSongs.isEmpty()) {
            playPlaylist(playlist)
            return
        }
        val firstSong = resolvedSongs.first()
        val now = System.currentTimeMillis()
        val updatedSession = session.copy(lastSongId = firstSong.id, lastPlayedAt = now)
        val updatedSessions = upsertRecentSession(updatedSession)
        _uiState.update {
            it.copy(
                customPlaybackQueue = if (session.isSubSelection) resolvedSongs else emptyList(),
                activeRecentSessionKey = session.key,
                recentSessions = updatedSessions
            )
        }
        engineScope.launch(Dispatchers.IO) { repository.saveRecentSessions(updatedSessions) }
        selectSongInternal(
            song = firstSong,
            playlistName = playlist.name,
            folderName = firstSong.folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = session.loopConfig
        )
    }

    fun createPlaylist(
        name: String,
        description: String = "",
        initialSongIds: List<String> = emptyList()
    ) {
        if (name.isBlank()) return
        val current = _uiState.value.playlists.toMutableList()
        val newPl = Playlist(
            id = "pl_${UUID.randomUUID()}",
            name = name.trim(),
            description = description.trim(),
            songIds = initialSongIds.distinct(),
            createdAt = System.currentTimeMillis(),
            lastPlayedAt = 0L
        )
        current.add(0, newPl)
        repository.savePlaylists(current)
        _uiState.update {
            it.copy(
                playlists = current,
                expandedPlaylistId = newPl.id,
                statusBannerMessage = null
            )
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        addSongsToPlaylist(playlistId, listOf(songId))
    }

    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) {
        if (songIds.isEmpty()) return
        val current = _uiState.value.playlists.toMutableList()
        val idx = current.indexOfFirst { it.id == playlistId }
        if (idx >= 0) {
            val pl = current[idx]
            val merged = (pl.songIds + songIds).distinct()
            current[idx] = pl.copy(songIds = merged)
            repository.savePlaylists(current)
            _uiState.update {
                it.copy(
                    playlists = current,
                    statusBannerMessage = null
                )
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
        val updatedSessions = _uiState.value.recentSessions.filterNot { it.key == "playlist:$playlistId" }
        repository.savePlaylists(current)
        repository.saveRecentSessions(updatedSessions)
        _uiState.update {
            it.copy(
                playlists = current,
                recentSessions = updatedSessions,
                statusBannerMessage = "Deleted playlist"
            )
        }
    }

    private fun prepareSongInternal(song: LocalSong, autoPlay: Boolean, resetIteration: Boolean) {
        cancelDelayCountdown()
        stopPositionTicker()
        releaseMediaPlayer()

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
            player.setDataSource(appContext, uri)
            player.setOnPreparedListener { mp ->
                if (mediaPlayer !== mp) return@setOnPreparedListener
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
                        ),
                        playbackStateSyncNonce = state.playbackStateSyncNonce + 1
                    )
                }

                if (validStart > 0L) {
                    runCatching { mp.seekTo(validStart.toInt()) }
                }

                val sessionId = runCatching { mp.audioSessionId }.getOrDefault(0)
                val updatedBands = effectsController.attach(sessionId, _uiState.value.equalizerState)
                if (updatedBands != null) {
                    _uiState.update {
                        it.copy(equalizerState = it.equalizerState.copy(bands = updatedBands))
                    }
                }
                applyVolumeAndBoostInternal()

                if (autoPlay) {
                    startPlayback(mp)
                } else {
                    _uiState.update { it.copy(phase = PlaybackPhase.PAUSED) }
                }
            }

            player.setOnCompletionListener { mp ->
                if (mediaPlayer !== mp) return@setOnCompletionListener
                onCurrentRepetitionFinished()
            }

            player.setOnErrorListener { mp, what, extra ->
                if (mediaPlayer !== mp) return@setOnErrorListener true
                isPlayerPrepared = false
                if (what == -38) return@setOnErrorListener true
                _uiState.update {
                    it.copy(
                        phase = PlaybackPhase.IDLE,
                        errorMessage = "Audio playback error (code $what/$extra). Verify the file is still on your device.",
                        playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                    )
                }
                true
            }

            player.prepareAsync()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.IDLE,
                    errorMessage = "Unable to open audio file: ${e.localizedMessage ?: "Access denied"}",
                    playbackStateSyncNonce = it.playbackStateSyncNonce + 1
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
                    errorMessage = null,
                    notificationSessionVisible = true,
                    playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                )
            }
            MediaPlaybackService.startForegroundPlayback(appContext)
            startPositionTicker()
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.PAUSED,
                    errorMessage = "Could not start playback: ${e.localizedMessage ?: ""}",
                    playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                )
            }
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        val song = state.currentSong ?: return

        if (state.phase == PlaybackPhase.WAITING_DELAY) {
            val nextDelayPaused = !state.isDelayPaused
            _uiState.update {
                it.copy(
                    isDelayPaused = nextDelayPaused,
                    notificationSessionVisible = true,
                    playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                )
            }
            if (!nextDelayPaused) {
                MediaPlaybackService.startForegroundPlayback(appContext)
            }
            return
        }

        val mp = mediaPlayer
        if (mp == null || !isPlayerPrepared) {
            prepareSongInternal(song, autoPlay = true, resetIteration = true)
            return
        }

        if (state.phase == PlaybackPhase.COMPLETED_ALL_LOOPS) {
            _uiState.update { it.copy(currentIteration = 1) }
            val startMs = state.loopConfig.sectionStartMs.toInt()
            runCatching { mp.seekTo(startMs) }
            startPlayback(mp)
            return
        }

        if (mp.safeIsPlaying()) {
            runCatching { mp.pause() }
            stopPositionTicker()
            _uiState.update {
                it.copy(
                    phase = PlaybackPhase.PAUSED,
                    playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                )
            }
        } else {
            val endLimit = state.loopConfig.sectionEndMs ?: state.trackDurationMs
            if (mp.safeCurrentPosition() >= endLimit - 150) {
                runCatching { mp.seekTo(state.loopConfig.sectionStartMs.toInt()) }
            }
            startPlayback(mp)
        }
    }

    fun playOrResumeFromNotification() {
        val state = _uiState.value
        if (!state.isActivelyPlayingForNotification) {
            togglePlayPause()
        }
    }

    fun pauseFromNotification() {
        val state = _uiState.value
        if (state.isActivelyPlayingForNotification) {
            togglePlayPause()
        }
    }

    fun dismissNotificationAndPause() {
        cancelDelayCountdown()
        stopPositionTicker()
        runCatching {
            if (mediaPlayer?.safeIsPlaying() == true) {
                mediaPlayer?.pause()
            }
        }
        _uiState.update { state ->
            state.copy(
                phase = if (state.currentSong != null) PlaybackPhase.PAUSED else PlaybackPhase.IDLE,
                isDelayPaused = if (state.phase == PlaybackPhase.WAITING_DELAY) true else state.isDelayPaused,
                notificationSessionVisible = false,
                playbackStateSyncNonce = state.playbackStateSyncNonce + 1
            )
        }
    }

    fun stopAndReleaseForTaskRemoved() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        cancelDelayCountdown()
        stopPositionTicker()
        releaseMediaPlayer()
        _uiState.update { state ->
            state.copy(
                phase = if (state.currentSong != null) PlaybackPhase.PAUSED else PlaybackPhase.IDLE,
                delayRemainingMs = 0L,
                delayTotalMs = 0L,
                isDelayPaused = false,
                sleepTimerRemainingMs = 0L,
                sleepTimerTotalMs = 0L,
                notificationSessionVisible = false,
                playbackStateSyncNonce = state.playbackStateSyncNonce + 1
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val state = _uiState.value
        val clamped = positionMs.coerceIn(0L, state.trackDurationMs.coerceAtLeast(1L))
        _uiState.update {
            it.copy(
                currentPositionMs = clamped,
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
            )
        }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared) {
            try {
                mp.seekTo(clamped.toInt())
            } catch (_: Exception) {
            }
        }
    }

    fun skipForward(deltaMs: Long = 5000L) {
        seekTo(_uiState.value.currentPositionMs + deltaMs)
    }

    fun skipBackward(deltaMs: Long = 5000L) {
        seekTo(_uiState.value.currentPositionMs - deltaMs)
    }

    fun skipDelayAndPlayNextRepetitionNow() {
        if (_uiState.value.phase != PlaybackPhase.WAITING_DELAY) return
        cancelDelayCountdown()
        startNextRepetitionImmediately()
    }

    fun addExtraSecondsToActiveDelay(extraSeconds: Int = 5) {
        if (_uiState.value.phase != PlaybackPhase.WAITING_DELAY) return
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
                statusBannerMessage = "Loop counter reset to 1",
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
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
        positionTickerJob = engineScope.launch {
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
            if (mp != null && isPlayerPrepared && mp.safeIsPlaying()) {
                mp.pause()
            }
        } catch (_: Exception) {
        }

        val totalCompleted = state.completedIterationsTotal + 1
        val hasMoreLoops = cfg.isInfinite || state.currentIteration < cfg.repeatCount

        if (hasMoreLoops) {
            val delayMs = cfg.calculateDelayMs(state.trackDurationMs)
            if (delayMs > 0L) {
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
                        isDelayPaused = false,
                        playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                    )
                }
                startDelayCountdown()
            } else {
                _uiState.update { it.copy(completedIterationsTotal = totalCompleted) }
                startNextRepetitionImmediately()
            }
        } else {
            _uiState.update { it.copy(completedIterationsTotal = totalCompleted) }
            handleAllRepetitionsCompleted()
        }
    }

    private fun startDelayCountdown() {
        cancelDelayCountdown()
        delayCountdownJob = engineScope.launch {
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
                isDelayPaused = false,
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
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

    private fun getActiveQueue(state: PlayerUiState): List<LocalSong> {
        if (state.customPlaybackQueue.isNotEmpty()) return state.customPlaybackQueue
        val activeUserPlaylist = state.playlists.firstOrNull {
            it.name.equals(state.currentPlaylistName, ignoreCase = true)
        }
        if (activeUserPlaylist != null) {
            val songMap = state.songs.associateBy { it.id }
            val playlistSongs = activeUserPlaylist.songIds.mapNotNull { songMap[it] }
            if (playlistSongs.isNotEmpty()) return playlistSongs
        }
        if (state.currentFolderName.isNotBlank()) {
            val folderSongs = state.songs.filter { it.folderName == state.currentFolderName }
            if (folderSongs.isNotEmpty()) return folderSongs
        }
        return state.filteredSongs.ifEmpty { state.songs }
    }

    private fun handleAllRepetitionsCompleted() {
        val state = _uiState.value
        val cfg = state.loopConfig
        val queue = getActiveQueue(state)
        val currentIdx = queue.indexOfFirst { it.id == state.currentSong?.id }

        when (cfg.afterCompletionAction) {
            AfterLoopAction.STOP -> {
                val startMs = cfg.sectionStartMs
                runCatching { mediaPlayer?.seekTo(startMs.toInt()) }
                _uiState.update {
                    it.copy(
                        phase = PlaybackPhase.COMPLETED_ALL_LOOPS,
                        currentPositionMs = startMs,
                        statusBannerMessage = null,
                        playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                    )
                }
            }
            AfterLoopAction.NEXT_SONG -> {
                if (currentIdx >= 0 && currentIdx + 1 < queue.size) {
                    val nextSong = queue[currentIdx + 1]
                    selectSongInternal(
                        song = nextSong,
                        playlistName = state.currentPlaylistName,
                        folderName = nextSong.folderName,
                        autoPlay = true,
                        preserveQueueAndSession = true,
                        overrideLoopConfig = state.loopConfig
                    )
                } else {
                    runCatching { mediaPlayer?.seekTo(cfg.sectionStartMs.toInt()) }
                    _uiState.update {
                        it.copy(
                            phase = PlaybackPhase.COMPLETED_ALL_LOOPS,
                            currentPositionMs = cfg.sectionStartMs,
                            statusBannerMessage = null,
                            playbackStateSyncNonce = it.playbackStateSyncNonce + 1
                        )
                    }
                }
            }
            AfterLoopAction.REPEAT_PLAYLIST -> {
                if (queue.isNotEmpty()) {
                    val nextIndex = if (currentIdx >= 0) (currentIdx + 1) % queue.size else 0
                    val nextSong = queue[nextIndex]
                    selectSongInternal(
                        song = nextSong,
                        playlistName = state.currentPlaylistName,
                        folderName = nextSong.folderName,
                        autoPlay = true,
                        preserveQueueAndSession = true,
                        overrideLoopConfig = state.loopConfig
                    )
                }
            }
        }
    }

    fun playNextSong() {
        val state = _uiState.value
        val queue = getActiveQueue(state)
        if (queue.isEmpty()) return
        val idx = queue.indexOfFirst { it.id == state.currentSong?.id }
        val nextIdx = if (idx >= 0) (idx + 1) % queue.size else 0
        val nextSong = queue[nextIdx]
        selectSongInternal(
            song = nextSong,
            playlistName = state.currentPlaylistName,
            folderName = nextSong.folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = if (state.activeRecentSessionKey != null) state.loopConfig else null
        )
    }

    fun playPreviousSong() {
        val state = _uiState.value
        if (getLivePositionMs() > 4000L) {
            seekTo(state.loopConfig.sectionStartMs)
            return
        }
        val queue = getActiveQueue(state)
        if (queue.isEmpty()) return
        val idx = queue.indexOfFirst { it.id == state.currentSong?.id }
        val prevIdx = if (idx > 0) idx - 1 else queue.lastIndex
        val prevSong = queue[prevIdx]
        selectSongInternal(
            song = prevSong,
            playlistName = state.currentPlaylistName,
            folderName = prevSong.folderName,
            autoPlay = true,
            preserveQueueAndSession = true,
            overrideLoopConfig = if (state.activeRecentSessionKey != null) state.loopConfig else null
        )
    }

    fun setRepeatCount(count: Int) = updateLoopConfig { it.copy(repeatCount = count.coerceIn(0, 999)) }

    fun incrementRepeatCount(delta: Int) {
        val current = _uiState.value.loopConfig.repeatCount
        val base = if (current <= 0) 1 else current
        setRepeatCount((base + delta).coerceIn(1, 999))
    }

    fun setDelaySeconds(seconds: Int) {
        val sanitized = seconds.coerceIn(0, 600)
        updateLoopConfig { it.copy(delaySeconds = sanitized, matchTrackDurationDelay = false) }
    }

    fun incrementDelaySeconds(delta: Int) =
        setDelaySeconds((_uiState.value.loopConfig.delaySeconds + delta).coerceIn(0, 600))

    fun setMatchTrackDurationDelay(enabled: Boolean) =
        updateLoopConfig { it.copy(matchTrackDurationDelay = enabled) }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        updateLoopConfig { it.copy(playbackSpeed = clamped) }
        val mp = mediaPlayer
        if (mp != null && isPlayerPrepared && mp.safeIsPlaying() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mp.playbackParams = PlaybackParams().setSpeed(clamped)
            } catch (_: Exception) {
            }
        }
        _uiState.update { it.copy(playbackStateSyncNonce = it.playbackStateSyncNonce + 1) }
    }

    fun setAfterCompletionAction(action: AfterLoopAction) =
        updateLoopConfig { it.copy(afterCompletionAction = action) }

    fun markCurrentPositionAsSectionStart() {
        val state = _uiState.value
        val pos = state.currentPositionMs.coerceIn(0L, (state.trackDurationMs - 500L).coerceAtLeast(0L))
        val currentEnd = state.loopConfig.sectionEndMs
        val validEnd = if (currentEnd != null && currentEnd <= pos + 500L) null else currentEnd
        updateLoopConfig { it.copy(sectionStartMs = pos, sectionEndMs = validEnd) }
        _uiState.update { it.copy(statusBannerMessage = "Set Loop Start [A] at ${formatDurationMillis(pos)}") }
    }

    fun markCurrentPositionAsSectionEnd() {
        val state = _uiState.value
        val safeDuration = state.trackDurationMs.coerceAtLeast(0L)
        val minEnd = (state.loopConfig.sectionStartMs + 500L).coerceAtMost(safeDuration)
        val pos = state.currentPositionMs.coerceIn(minEnd, safeDuration)
        updateLoopConfig { it.copy(sectionEndMs = pos) }
        _uiState.update { it.copy(statusBannerMessage = "Set Loop End [B] at ${formatDurationMillis(pos)}") }
    }

    fun clearAbSection() {
        updateLoopConfig { it.copy(sectionStartMs = 0L, sectionEndMs = null) }
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
            it.copy(currentIteration = 1, statusBannerMessage = "Applied preset: ${preset.name}")
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
            it.copy(presets = updatedPresets, statusBannerMessage = "Saved preset \"${newPreset.name}\"")
        }
    }

    fun deleteCustomPreset(presetId: String) {
        val updated = repository.deleteCustomPreset(presetId)
        _uiState.update { it.copy(presets = updated) }
    }

    fun importSongsFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        engineScope.launch {
            val imported = withContext(Dispatchers.IO) { repository.importAudioUris(uris) }
            if (imported.isNotEmpty()) {
                mergeImportedSongs(imported)
            } else {
                _uiState.update { it.copy(errorMessage = "Could not read selected audio files.") }
            }
        }
    }

    fun importSongsFromFolder(treeUri: Uri?) {
        if (treeUri == null) return
        engineScope.launch {
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
        _uiState.update { it.copy(songs = updated, statusBannerMessage = "Removed track from library") }
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
                        trackDurationMs = 0L,
                        notificationSessionVisible = false,
                        playbackStateSyncNonce = it.playbackStateSyncNonce + 1
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
            it.copy(songs = combined, statusBannerMessage = "Restored built-in studio loop samples")
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
        val state = _uiState.value
        val updated = transform(state.loopConfig)
        val activeKey = state.activeRecentSessionKey
        val updatedSessions = if (activeKey != null) {
            state.recentSessions.map { session ->
                if (session.key == activeKey) {
                    session.copy(loopConfig = updated)
                } else {
                    session
                }
            }
        } else {
            state.recentSessions
        }
        _uiState.update { it.copy(loopConfig = updated, recentSessions = updatedSessions) }
        engineScope.launch(Dispatchers.IO) {
            repository.saveActiveLoopConfig(updated)
            if (activeKey != null) {
                repository.saveRecentSessions(updatedSessions)
            }
        }
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 200)
        _uiState.update { it.copy(volumePercent = clamped, isMuted = clamped == 0) }
        applyVolumeAndBoostInternal(syncToSystem = true)
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = nextMuted) }
        applyVolumeAndBoostInternal(syncToSystem = true)
    }

    fun setShowEqualizer(show: Boolean) {
        _uiState.update { it.copy(isShowingEqualizer = show) }
    }

    fun toggleShowEqualizer() {
        _uiState.update { it.copy(isShowingEqualizer = !it.isShowingEqualizer) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        effectsController.setEqualizerEnabled(enabled)
        _uiState.update { it.copy(equalizerState = it.equalizerState.copy(isEnabled = enabled)) }
    }

    fun setEqualizerBandLevel(bandIndex: Short, levelMb: Short) {
        effectsController.setBandLevel(bandIndex, levelMb)
        _uiState.update { state ->
            val updated = state.equalizerState.bands.map {
                if (it.bandIndex == bandIndex) it.copy(levelMb = levelMb) else it
            }
            state.copy(
                equalizerState = state.equalizerState.copy(bands = updated, currentPreset = "Custom")
            )
        }
    }

    fun setEqualizerPreset(presetName: String) {
        _uiState.update { state ->
            val updated = effectsController.applyPreset(presetName, state.equalizerState.bands)
            state.copy(
                equalizerState = state.equalizerState.copy(currentPreset = presetName, bands = updated)
            )
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        effectsController.setBassBoostStrength(clamped)
        _uiState.update { it.copy(equalizerState = it.equalizerState.copy(bassBoostStrength = clamped)) }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        effectsController.setVirtualizerStrength(clamped)
        _uiState.update { it.copy(equalizerState = it.equalizerState.copy(virtualizerStrength = clamped)) }
    }

    private fun applyVolumeAndBoostInternal(syncToSystem: Boolean = false) {
        val state = _uiState.value
        val effectiveVol = if (state.isMuted) 0 else state.volumePercent
        if (syncToSystem) {
            volumeController.syncVolumeToSystem(effectiveVol)
        }
        effectsController.applyVolumeAndBoost(
            mp = mediaPlayer,
            isPlayerPrepared = isPlayerPrepared,
            effectiveVolumePercent = effectiveVol,
            audioManager = audioManager
        )
    }

    private fun releaseMediaPlayer() {
        val mp = mediaPlayer
        val wasPrepared = isPlayerPrepared
        mediaPlayer = null
        isPlayerPrepared = false
        effectsController.release()
        if (mp != null) {
            try {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
            } catch (_: Exception) {
            }
            if (wasPrepared) {
                runCatching {
                    if (mp.safeIsPlaying()) mp.pause()
                }
            }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
    }

    fun setSleepTimer(hours: Int, minutes: Int) {
        val totalMinutes = (hours.coerceAtLeast(0) * 60L) + minutes.coerceAtLeast(0)
        val totalMs = totalMinutes * 60_000L
        if (totalMs <= 0L) {
            cancelSleepTimer()
            return
        }
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerRemainingMs = totalMs, sleepTimerTotalMs = totalMs) }
        sleepTimerJob = engineScope.launch {
            val stepMs = 1000L
            while (isActive) {
                delay(stepMs)
                val nextRem = (_uiState.value.sleepTimerRemainingMs - stepMs).coerceAtLeast(0L)
                _uiState.update { it.copy(sleepTimerRemainingMs = nextRem) }
                if (nextRem <= 0L) {
                    stopPlaybackForSleepTimer()
                    break
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _uiState.update { it.copy(sleepTimerRemainingMs = 0L, sleepTimerTotalMs = 0L) }
    }

    private fun stopPlaybackForSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        cancelDelayCountdown()
        stopPositionTicker()
        runCatching {
            if (mediaPlayer?.safeIsPlaying() == true) {
                mediaPlayer?.pause()
            }
        }
        _uiState.update {
            it.copy(
                phase = PlaybackPhase.PAUSED,
                delayRemainingMs = 0L,
                delayTotalMs = 0L,
                isDelayPaused = false,
                sleepTimerRemainingMs = 0L,
                sleepTimerTotalMs = 0L,
                playbackStateSyncNonce = it.playbackStateSyncNonce + 1
            )
        }
    }
}
