package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.model.LocalSong
import com.example.model.MainMenuTab
import com.example.model.Playlist
import com.example.player.PlayerUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Synchronized top menu tab bar + live-sliding [HorizontalPager] across all four library menus:
 * Recent (0) ↔ Folders (1) ↔ Albums (2) ↔ Playlists (3).
 *
 * - Swiping left/right smoothly slides between adjacent tabs and highlights the active tab pill live.
 * - Horizontal card rows inside Recent scroll first and hand off cleanly to the pager past the edge.
 * - Tapping a tab pill or jumping programmatically (e.g., opening a Recent folder/playlist) animates
 *   smoothly to the target page without feedback loops.
 */
@Composable
fun LibraryTabsPager(
    uiState: PlayerUiState,
    onSelectTab: (MainMenuTab) -> Unit,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onOpenFolderFromRecent: (String) -> Unit,
    onPlayRecentFolderSession: (String) -> Unit,
    onOpenPlaylistFromRecent: (String) -> Unit,
    onPlayRecentPlaylistSession: (Playlist) -> Unit,
    onPlayFolder: (String) -> Unit,
    onToggleExpandFolder: (String?) -> Unit,
    onPlaySelectedFolderSongs: (List<LocalSong>, String?) -> Unit,
    onPlaySelectedAlbumSongs: (List<LocalSong>) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onAddSongsToPlaylist: (String, List<String>) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onToggleExpandPlaylist: (String?) -> Unit,
    onPlaySelectedPlaylistSongs: (List<LocalSong>, String?, String?) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuTabs = remember { MainMenuTab.entries }
    val coroutineScope = rememberCoroutineScope()
    val initialPage = remember(menuTabs) {
        menuTabs.indexOf(uiState.selectedTab).coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { menuTabs.size }
    )

    val latestSelectedTab by rememberUpdatedState(uiState.selectedTab)
    val latestOnSelectTab by rememberUpdatedState(onSelectTab)

    // Sync user swipes -> ViewModel state only when the pager settles on a page
    LaunchedEffect(pagerState, menuTabs) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                val settledTab = menuTabs.getOrNull(settledPage) ?: return@collect
                if (settledTab != latestSelectedTab) {
                    latestOnSelectTab(settledTab)
                }
            }
    }

    // Sync programmatic tab changes (Recent folder/playlist jump, Studio Player link, tab tap) -> pager
    LaunchedEffect(uiState.selectedTab, menuTabs) {
        val targetIndex = menuTabs.indexOf(uiState.selectedTab)
        if (targetIndex in menuTabs.indices &&
            (pagerState.currentPage != targetIndex || pagerState.targetPage != targetIndex)
        ) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // Reflect live swipe midpoint Crossing in the top tab pills
    val liveSelectedTab = menuTabs.getOrElse(pagerState.currentPage) { uiState.selectedTab }

    Column(modifier = modifier.fillMaxSize()) {
        MainMenuTabsRow(
            selectedTab = liveSelectedTab,
            onTabSelected = { tab ->
                val targetIndex = menuTabs.indexOf(tab)
                if (tab != uiState.selectedTab) {
                    onSelectTab(tab)
                } else if (targetIndex in menuTabs.indices && pagerState.currentPage != targetIndex) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetIndex)
                    }
                }
            }
        )

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("library_horizontal_pager")
        ) { pageIndex ->
            when (menuTabs[pageIndex]) {
                MainMenuTab.RECENT -> {
                    RecentTabScreen(
                        uiState = uiState,
                        onSelectSong = onSelectSong,
                        onOpenFolder = onOpenFolderFromRecent,
                        onPlayFolder = onPlayRecentFolderSession,
                        onOpenPlaylist = onOpenPlaylistFromRecent,
                        onPlayPlaylist = onPlayRecentPlaylistSession,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onOpenStudioPlayer = onOpenStudioPlayer
                    )
                }

                MainMenuTab.FOLDERS -> {
                    FoldersTabScreen(
                        uiState = uiState,
                        onSelectSong = onSelectSong,
                        onPlayFolder = onPlayFolder,
                        onToggleExpandFolder = onToggleExpandFolder,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onOpenStudioPlayer = onOpenStudioPlayer,
                        onPlaySelectedSongs = onPlaySelectedFolderSongs
                    )
                }

                MainMenuTab.ALBUMS_SINGERS -> {
                    AlbumsSingersTabScreen(
                        uiState = uiState,
                        onSelectSong = onSelectSong,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onOpenStudioPlayer = onOpenStudioPlayer,
                        onPlaySelectedSongs = onPlaySelectedAlbumSongs
                    )
                }

                MainMenuTab.PLAYLISTS -> {
                    PlaylistsTabScreen(
                        uiState = uiState,
                        onSelectSong = onSelectSong,
                        onPlayPlaylist = onPlayPlaylist,
                        onCreatePlaylistClick = onCreatePlaylistClick,
                        onAddSongsToPlaylist = onAddSongsToPlaylist,
                        onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                        onDeletePlaylist = onDeletePlaylist,
                        onToggleExpandPlaylist = onToggleExpandPlaylist,
                        onOpenStudioPlayer = onOpenStudioPlayer,
                        onPlaySelectedSongs = onPlaySelectedPlaylistSongs
                    )
                }
            }
        }
    }
}
