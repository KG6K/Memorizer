package com.example.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.model.AfterLoopAction
import com.example.model.LibraryFilter
import com.example.model.LocalSong
import com.example.model.LoopPreset
import com.example.model.MainMenuTab
import com.example.model.Playlist
import com.example.model.SortDirection
import com.example.model.SortOption
import kotlinx.coroutines.flow.StateFlow

class LoopAudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = AudioPlaybackEngine.getInstance(application)
    val uiState: StateFlow<PlayerUiState> = engine.uiState

    fun shouldInterceptVolumeKeyForBoost(isVolumeUp: Boolean): Boolean =
        engine.shouldInterceptVolumeKeyForBoost(isVolumeUp)

    fun syncFromSystemVolumeAfterHardwareKey() =
        engine.syncFromSystemVolumeAfterHardwareKey()

    fun handleHardwareVolumeKey(isVolumeUp: Boolean): Boolean =
        engine.handleHardwareVolumeKey(isVolumeUp)

    fun scanDeviceAudio() = engine.scanDeviceAudio()

    fun rescanLibrary() = engine.rescanLibrary()

    fun addScanFolder(treeUri: Uri?) = engine.addScanFolder(treeUri)

    fun removeScanFolder(uriString: String) = engine.removeScanFolder(uriString)

    fun clearScanFoldersResetToAllDevice() = engine.clearScanFoldersResetToAllDevice()

    fun setSelectedTab(tab: MainMenuTab) = engine.setSelectedTab(tab)

    fun setExpandedFolder(folderName: String?) = engine.setExpandedFolder(folderName)

    fun setExpandedPlaylist(playlistId: String?) = engine.setExpandedPlaylist(playlistId)

    fun setExpandedArtist(artistName: String?) = engine.setExpandedArtist(artistName)

    fun selectSong(
        song: LocalSong,
        playlistName: String? = null,
        folderName: String? = null,
        autoPlay: Boolean = true
    ) = engine.selectSong(song, playlistName, folderName, autoPlay)

    fun playFolder(folderName: String) = engine.playFolder(folderName)

    fun playPlaylist(playlist: Playlist) = engine.playPlaylist(playlist)

    fun setSearchQuery(query: String) = engine.setSearchQuery(query)

    fun setSortOption(option: SortOption) = engine.setSortOption(option)

    fun setSortDirection(direction: SortDirection) = engine.setSortDirection(direction)

    fun openFolderFromRecent(folderName: String) = engine.openFolderFromRecent(folderName)

    fun openPlaylistFromRecent(playlistId: String) = engine.openPlaylistFromRecent(playlistId)

    fun playRecentFolderSession(folderName: String) = engine.playRecentFolderSession(folderName)

    fun playRecentPlaylistSession(playlistId: String) = engine.playRecentPlaylistSession(playlistId)

    fun playSelectedSongs(
        songs: List<LocalSong>,
        sourceFolderName: String? = null,
        sourcePlaylistId: String? = null,
        sourcePlaylistName: String? = null
    ) = engine.playSelectedSongs(songs, sourceFolderName, sourcePlaylistId, sourcePlaylistName)

    fun createPlaylist(
        name: String,
        description: String = "",
        initialSongIds: List<String> = emptyList()
    ) = engine.createPlaylist(name, description, initialSongIds)

    fun addSongToPlaylist(playlistId: String, songId: String) =
        engine.addSongToPlaylist(playlistId, songId)

    fun addSongsToPlaylist(playlistId: String, songIds: List<String>) =
        engine.addSongsToPlaylist(playlistId, songIds)

    fun removeSongFromPlaylist(playlistId: String, songId: String) =
        engine.removeSongFromPlaylist(playlistId, songId)

    fun deletePlaylist(playlistId: String) = engine.deletePlaylist(playlistId)

    fun togglePlayPause() = engine.togglePlayPause()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun skipForward(deltaMs: Long = 5000L) = engine.skipForward(deltaMs)

    fun skipBackward(deltaMs: Long = 5000L) = engine.skipBackward(deltaMs)

    fun skipDelayAndPlayNextRepetitionNow() = engine.skipDelayAndPlayNextRepetitionNow()

    fun addExtraSecondsToActiveDelay(extraSeconds: Int = 5) =
        engine.addExtraSecondsToActiveDelay(extraSeconds)

    fun resetLoopProgress() = engine.resetLoopProgress()

    fun playNextSong() = engine.playNextSong()

    fun playPreviousSong() = engine.playPreviousSong()

    fun setRepeatCount(count: Int) = engine.setRepeatCount(count)

    fun incrementRepeatCount(delta: Int) = engine.incrementRepeatCount(delta)

    fun setDelaySeconds(seconds: Int) = engine.setDelaySeconds(seconds)

    fun incrementDelaySeconds(delta: Int) = engine.incrementDelaySeconds(delta)

    fun setMatchTrackDurationDelay(enabled: Boolean) = engine.setMatchTrackDurationDelay(enabled)

    fun setPlaybackSpeed(speed: Float) = engine.setPlaybackSpeed(speed)

    fun setAfterCompletionAction(action: AfterLoopAction) = engine.setAfterCompletionAction(action)

    fun markCurrentPositionAsSectionStart() = engine.markCurrentPositionAsSectionStart()

    fun markCurrentPositionAsSectionEnd() = engine.markCurrentPositionAsSectionEnd()

    fun clearAbSection() = engine.clearAbSection()

    fun saveLoopConfigToCurrentSong() = engine.saveLoopConfigToCurrentSong()

    fun applyPreset(preset: LoopPreset) = engine.applyPreset(preset)

    fun createCustomPreset(name: String, description: String) =
        engine.createCustomPreset(name, description)

    fun deleteCustomPreset(presetId: String) = engine.deleteCustomPreset(presetId)

    fun importSongsFromUris(uris: List<Uri>) = engine.importSongsFromUris(uris)

    fun importSongsFromFolder(treeUri: Uri?) = engine.importSongsFromFolder(treeUri)

    fun removeSong(songId: String) = engine.removeSong(songId)

    fun restoreBuiltInSamples() = engine.restoreBuiltInSamples()

    fun setLibraryFilter(filter: LibraryFilter) = engine.setLibraryFilter(filter)

    fun clearBannerMessages() = engine.clearBannerMessages()

    fun setVolumePercent(percent: Int) = engine.setVolumePercent(percent)

    fun toggleMute() = engine.toggleMute()

    fun setShowEqualizer(show: Boolean) = engine.setShowEqualizer(show)

    fun toggleShowEqualizer() = engine.toggleShowEqualizer()

    fun setEqualizerEnabled(enabled: Boolean) = engine.setEqualizerEnabled(enabled)

    fun setEqualizerBandLevel(bandIndex: Short, levelMb: Short) =
        engine.setEqualizerBandLevel(bandIndex, levelMb)

    fun setEqualizerPreset(presetName: String) = engine.setEqualizerPreset(presetName)

    fun setBassBoostStrength(strength: Int) = engine.setBassBoostStrength(strength)

    fun setVirtualizerStrength(strength: Int) = engine.setVirtualizerStrength(strength)

    fun setSleepTimer(hours: Int, minutes: Int) = engine.setSleepTimer(hours, minutes)

    fun cancelSleepTimer() = engine.cancelSleepTimer()
}
