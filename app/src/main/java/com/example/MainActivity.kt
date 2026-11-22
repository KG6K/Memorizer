package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.LocalSong
import com.example.model.MainMenuTab
import com.example.player.LoopAudioPlayerViewModel
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.LastPlayingBottomBar
import com.example.ui.components.LibraryTabsPager
import com.example.ui.components.MainTopHeaderBar
import com.example.ui.components.SettingsScreen
import com.example.ui.components.StudioPlayerScreen
import com.example.ui.theme.AppThemeSetting
import com.example.ui.theme.MyApplicationTheme

import android.media.AudioManager
import android.view.KeyEvent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_STUDIO_PLAYER = "com.example.EXTRA_OPEN_STUDIO_PLAYER"
    }

    private val playerViewModel: LoopAudioPlayerViewModel by viewModels()
    private var openStudioPlayerTrigger by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            var themeSetting by rememberSaveable { mutableStateOf(AppThemeSetting.SYSTEM) }
            MyApplicationTheme(themeSetting = themeSetting) {
                LoopWavePlayerApp(
                    themeSetting = themeSetting,
                    onSelectThemeSetting = { themeSetting = it },
                    viewModel = playerViewModel,
                    openStudioPlayerTrigger = openStudioPlayerTrigger,
                    onConsumeOpenStudioPlayer = { openStudioPlayerTrigger = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_STUDIO_PLAYER, false) == true) {
            openStudioPlayerTrigger = true
            intent.removeExtra(EXTRA_OPEN_STUDIO_PLAYER)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (playerViewModel.shouldInterceptVolumeKeyForBoost(isVolumeUp = true)) {
                    playerViewModel.handleHardwareVolumeKey(isVolumeUp = true)
                    true
                } else {
                    val handled = super.onKeyDown(keyCode, event)
                    playerViewModel.syncFromSystemVolumeAfterHardwareKey()
                    handled
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (playerViewModel.shouldInterceptVolumeKeyForBoost(isVolumeUp = false)) {
                    playerViewModel.handleHardwareVolumeKey(isVolumeUp = false)
                    true
                } else {
                    val handled = super.onKeyDown(keyCode, event)
                    playerViewModel.syncFromSystemVolumeAfterHardwareKey()
                    handled
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopWavePlayerApp(
    themeSetting: AppThemeSetting = AppThemeSetting.SYSTEM,
    onSelectThemeSetting: (AppThemeSetting) -> Unit = {},
    viewModel: LoopAudioPlayerViewModel = viewModel(),
    openStudioPlayerTrigger: Boolean = false,
    onConsumeOpenStudioPlayer: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<LocalSong?>(null) }
    var isStudioPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openStudioPlayerTrigger) {
        if (openStudioPlayerTrigger) {
            isStudioPlayerOpen = true
            onConsumeOpenStudioPlayer()
        }
    }

    // Automatic folder & storage audio scan on startup with graceful permission check
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val requestNotificationPermissionIfNeeded = remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasRequestedNotificationPermission &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                hasRequestedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceAudio()
        }
        requestNotificationPermissionIfNeeded()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.scanDeviceAudio()
            requestNotificationPermissionIfNeeded()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    // BackHandler: If Studio Player, Settings, or subview is open, close it first
    BackHandler(
        enabled = isStudioPlayerOpen ||
            isSettingsOpen ||
            uiState.expandedFolder != null ||
            uiState.expandedPlaylistId != null
    ) {
        if (isStudioPlayerOpen) {
            isStudioPlayerOpen = false
        } else if (isSettingsOpen) {
            isSettingsOpen = false
        } else if (uiState.expandedFolder != null) {
            viewModel.setExpandedFolder(null)
        } else if (uiState.expandedPlaylistId != null) {
            viewModel.setExpandedPlaylist(null)
        }
    }

    // SAF Launchers for custom audio files and Scoped Scan Folders
    val audioFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importSongsFromUris(uris)
        }
    }

    val scanFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addScanFolder(uri)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                // Top Bar: App Logo at top left, Search bar in middle, Sort & Settings buttons at top right
                MainTopHeaderBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    sortOption = uiState.sortOption,
                    sortDirection = uiState.sortDirection,
                    onSelectSortOption = viewModel::setSortOption,
                    onSelectSortDirection = viewModel::setSortDirection,
                    onOpenSettings = { isSettingsOpen = true },
                    onOpenStudioPlayer = { isStudioPlayerOpen = true }
                )
            },
            bottomBar = {
                // Bottom Bar: Last playing song, playlist, and folder
                if (uiState.currentSong != null) {
                    LastPlayingBottomBar(
                        uiState = uiState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onNextSong = viewModel::playNextSong,
                        onOpenStudioPlayer = { isStudioPlayerOpen = true }
                    )
                }
            }
        ) { innerPadding ->
            LibraryTabsPager(
                uiState = uiState,
                onSelectTab = viewModel::setSelectedTab,
                onSelectSong = { song, playlistName, folderName ->
                    viewModel.selectSong(
                        song,
                        playlistName = playlistName,
                        folderName = folderName,
                        autoPlay = true
                    )
                },
                onOpenFolderFromRecent = viewModel::openFolderFromRecent,
                onPlayRecentFolderSession = viewModel::playRecentFolderSession,
                onOpenPlaylistFromRecent = viewModel::openPlaylistFromRecent,
                onPlayRecentPlaylistSession = { pl -> viewModel.playRecentPlaylistSession(pl.id) },
                onPlayFolder = viewModel::playFolder,
                onToggleExpandFolder = viewModel::setExpandedFolder,
                onPlaySelectedFolderSongs = { selectedSongs, sourceFolderName ->
                    viewModel.playSelectedSongs(
                        songs = selectedSongs,
                        sourceFolderName = sourceFolderName
                    )
                },
                onPlaySelectedAlbumSongs = { selectedSongs ->
                    viewModel.playSelectedSongs(songs = selectedSongs)
                },
                onPlayPlaylist = viewModel::playPlaylist,
                onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                onAddSongsToPlaylist = viewModel::addSongsToPlaylist,
                onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                onDeletePlaylist = viewModel::deletePlaylist,
                onToggleExpandPlaylist = viewModel::setExpandedPlaylist,
                onPlaySelectedPlaylistSongs = { selectedSongs, sourcePlaylistId, sourcePlaylistName ->
                    viewModel.playSelectedSongs(
                        songs = selectedSongs,
                        sourcePlaylistId = sourcePlaylistId,
                        sourcePlaylistName = sourcePlaylistName
                    )
                },
                onAddToPlaylistClick = { song -> songToAddToPlaylist = song },
                onOpenStudioPlayer = { isStudioPlayerOpen = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(
                        if (uiState.currentSong == null) {
                            Modifier.navigationBarsPadding()
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // Full Studio Player Sheet (Advanced Memorization Looper: repeat count, delay countdown, echo mode, A-B markers, speed)
        AnimatedVisibility(
            visible = isStudioPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    StudioPlayerScreen(
                        uiState = uiState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSeekTo = viewModel::seekTo,
                        onSkipBackward = { viewModel.skipBackward(5000L) },
                        onSkipForward = { viewModel.skipForward(5000L) },
                        onPreviousSong = viewModel::playPreviousSong,
                        onNextSong = viewModel::playNextSong,
                        onResetLoopProgress = viewModel::resetLoopProgress,
                        onSkipDelayNow = viewModel::skipDelayAndPlayNextRepetitionNow,
                        onAddExtraDelay = { viewModel.addExtraSecondsToActiveDelay(5) },
                        onSetRepeatCount = viewModel::setRepeatCount,
                        onIncrementRepeatCount = viewModel::incrementRepeatCount,
                        onSetDelaySeconds = viewModel::setDelaySeconds,
                        onIncrementDelaySeconds = viewModel::incrementDelaySeconds,
                        onSetMatchTrackDuration = viewModel::setMatchTrackDurationDelay,
                        onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                        onSetAfterLoopAction = viewModel::setAfterCompletionAction,
                        onMarkSectionStart = viewModel::markCurrentPositionAsSectionStart,
                        onMarkSectionEnd = viewModel::markCurrentPositionAsSectionEnd,
                        onClearAbSection = viewModel::clearAbSection,
                        onSaveConfigToSong = viewModel::saveLoopConfigToCurrentSong,
                        onPickAudioFiles = {
                            audioFilesPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        onOpenLibraryTab = {
                            isStudioPlayerOpen = false
                            viewModel.setSelectedTab(MainMenuTab.FOLDERS)
                        },
                        onSetVolumePercent = viewModel::setVolumePercent,
                        onToggleMute = viewModel::toggleMute,
                        onSetEqualizerEnabled = viewModel::setEqualizerEnabled,
                        onSetEqualizerBandLevel = viewModel::setEqualizerBandLevel,
                        onSetEqualizerPreset = viewModel::setEqualizerPreset,
                        onSetBassBoostStrength = viewModel::setBassBoostStrength,
                        onSetVirtualizerStrength = viewModel::setVirtualizerStrength,
                        onSetShowEqualizer = viewModel::setShowEqualizer,
                        onSetSleepTimer = viewModel::setSleepTimer,
                        onCancelSleepTimer = viewModel::cancelSleepTimer,
                        onClosePlayer = { isStudioPlayerOpen = false }
                    )
                }
            }
        }
        // Full-Screen Settings Window
        AnimatedVisibility(
            visible = isSettingsOpen && !isStudioPlayerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            SettingsScreen(
                uiState = uiState,
                themeSetting = themeSetting,
                onSelectThemeSetting = onSelectThemeSetting,
                onAddScanFolder = { scanFolderPickerLauncher.launch(null) },
                onRemoveScanFolder = viewModel::removeScanFolder,
                onRescanFolders = viewModel::rescanLibrary,
                onResetAllDeviceScan = viewModel::clearScanFoldersResetToAllDevice,
                onOpenAbout = { showAboutDialog = true },
                onBack = { isSettingsOpen = false }
            )
        }
    }

    // About App Dialog (Shows app relations, GitHub source link & Donate button)
    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            availableSongs = uiState.songs,
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, desc, selectedSongIds ->
                viewModel.createPlaylist(name, desc, selectedSongIds)
                showCreatePlaylistDialog = false
            }
        )
    }

    // Add To Playlist Dialog
    songToAddToPlaylist?.let { targetSong ->
        AddToPlaylistDialog(
            song = targetSong,
            playlists = uiState.playlists,
            onAddToPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, targetSong.id)
                songToAddToPlaylist = null
            },
            onCreateNewPlaylist = {
                songToAddToPlaylist = null
                showCreatePlaylistDialog = true
            },
            onDismiss = { songToAddToPlaylist = null }
        )
    }
}
