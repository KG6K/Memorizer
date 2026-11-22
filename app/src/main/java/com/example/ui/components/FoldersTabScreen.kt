package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderInfo
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.model.songMatchesQuery
import com.example.model.sortFolderGroups
import com.example.model.sortSongs
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

/**
 * 2nd Menu Option: Folders
 * Displays folders which contain songs, with lazy-loaded expandable track lists
 * so even folders with 400+ or thousands of songs open smoothly without freezes or crashes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersTabScreen(
    uiState: PlayerUiState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onPlayFolder: (String) -> Unit,
    onToggleExpandFolder: (String) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onPlaySelectedSongs: (List<LocalSong>, sourceFolderName: String?) -> Unit = { _, _ -> },
    onPickAudioFolder: () -> Unit = {},
    onPickAudioFiles: () -> Unit = {}
) {
    val normalizedQuery = uiState.searchQuery.trim().lowercase()
    val allSongsByFolder = remember(uiState.songs) {
        uiState.songs.groupBy { it.folderName }
    }
    val songsByFolder = remember(
        allSongsByFolder,
        normalizedQuery,
        uiState.sortOption,
        uiState.sortDirection
    ) {
        allSongsByFolder.mapValues { (folderName, songs) ->
            val matching = if (normalizedQuery.isBlank()) {
                songs
            } else {
                val filtered = songs.filter { songMatchesQuery(it, normalizedQuery) }
                if (filtered.isEmpty() && folderName.lowercase().contains(normalizedQuery)) {
                    songs
                } else {
                    filtered
                }
            }
            sortSongs(matching, uiState.sortOption, uiState.sortDirection)
        }.filterValues { it.isNotEmpty() }
    }
    val folderGroups = remember(songsByFolder, uiState.sortOption, uiState.sortDirection) {
        val raw = songsByFolder.map { (name, list) ->
            FolderInfo(
                name = name,
                songCount = list.size,
                totalDurationMs = list.sumOf { it.durationMs },
                lastPlayedAt = list.maxOfOrNull { it.lastPlayedAt } ?: 0L
            )
        }
        sortFolderGroups(raw, songsByFolder, uiState.sortOption, uiState.sortDirection)
    }
    val selectionState = rememberSongSelection()
    val currentSongId = uiState.currentSong?.id
    val isPlaybackActive = uiState.phase == PlaybackPhase.PLAYING ||
        uiState.phase == PlaybackPhase.WAITING_DELAY

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_tab_screen")
    ) {
        SongMultiSelectionBar(
            selectedCount = selectionState.selectedIds.size,
            onClearSelection = selectionState.onClear,
            onSelectAll = {
                val allSongs = folderGroups.flatMap { fg -> songsByFolder[fg.name] ?: emptyList() }
                selectionState.onSelectAll(allSongs)
            },
            onPlaySelected = {
                val allSongs = folderGroups.flatMap { fg -> songsByFolder[fg.name] ?: emptyList() }
                val selected = allSongs.filter { it.id in selectionState.selectedIds }
                if (selected.isNotEmpty()) {
                    val targetFolder = uiState.expandedFolder
                        ?.takeIf { exp -> selected.all { it.folderName == exp } }
                        ?: selected.map { it.folderName }.distinct().singleOrNull()
                    onPlaySelectedSongs(selected, targetFolder)
                    selectionState.onClear()
                }
            }
        )

        val listState = rememberLazyListState()
        val fastScrollLabels = remember(folderGroups, songsByFolder, uiState.expandedFolder, normalizedQuery) {
            if (folderGroups.isEmpty()) {
                listOf(FastScrollLabel("Folders"))
            } else {
                buildList {
                    folderGroups.forEachIndexed { folderIdx, folder ->
                        add(FastScrollLabel(folder.name, "${folderIdx + 1}/${folderGroups.size}"))
                        val songsInFolder = songsByFolder[folder.name] ?: emptyList()
                        val isExpanded = if (normalizedQuery.isNotBlank()) {
                            true
                        } else {
                            uiState.expandedFolder == folder.name
                        }
                        if (isExpanded) {
                            songsInFolder.forEachIndexed { songIdx, song ->
                                add(FastScrollLabel(song.title, "${songIdx + 1}/${songsInFolder.size}"))
                            }
                        }
                    }
                    add(lastOrNull() ?: FastScrollLabel("Folders"))
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            if (folderGroups.isEmpty()) {
                item(key = "empty_folders_card") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (normalizedQuery.isNotBlank()) Icons.Filled.SearchOff else Icons.Filled.FolderOpen,
                                contentDescription = if (normalizedQuery.isNotBlank()) "No search results" else "No folders",
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) "No Matching Folders or Songs" else "No Audio Folders Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) {
                                    "Try another keyword or clear your search query."
                                } else {
                                    "Grant audio permission to automatically scan music folders from your device storage."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                folderGroups.forEach { folder ->
                    val songsInFolder = songsByFolder[folder.name] ?: emptyList()
                    val isExpanded = if (normalizedQuery.isNotBlank()) {
                        true
                    } else {
                        uiState.expandedFolder == folder.name
                    }
                    val isCurrentFolder = uiState.currentFolderName == folder.name

                    item(key = "folder_header_${folder.name}", contentType = "folder_header") {
                        FolderGroupHeaderCard(
                            folder = folder,
                            isExpanded = isExpanded,
                            isCurrentFolder = isCurrentFolder,
                            onToggleExpand = { onToggleExpandFolder(folder.name) },
                            onPlayFolder = { onPlayFolder(folder.name) }
                        )
                    }

                    if (isExpanded) {
                        items(
                            items = songsInFolder,
                            key = { song -> "folder_${folder.name}_song_${song.id}" },
                            contentType = { "folder_song_row" }
                        ) { song ->
                            FolderSongRowItem(
                                song = song,
                                songsInFolder = songsInFolder,
                                isCurrent = song.id == currentSongId,
                                isPlaying = song.id == currentSongId && isPlaybackActive,
                                isSelected = song.id in selectionState.selectedIds,
                                isSelectionMode = selectionState.isSelectionMode,
                                selectionState = selectionState,
                                onSelectSong = {
                                    onSelectSong(song, "Folder: ${folder.name}", folder.name)
                                },
                                onAddToPlaylistClick = { onAddToPlaylistClick(song) },
                                onOpenStudioPlayer = onOpenStudioPlayer
                            )
                        }
                    }
                }
            }

            item(key = "folders_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            }

            FastScrollBarOverlay(
                listState = listState,
                labels = fastScrollLabels,
                testTagPrefix = "folders_fast_scroll"
            )
        }
    }
}

@Composable
private fun FolderGroupHeaderCard(
    folder: FolderInfo,
    isExpanded: Boolean,
    isCurrentFolder: Boolean,
    onToggleExpand: () -> Unit,
    onPlayFolder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("folder_group_card_${folder.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentFolder) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isCurrentFolder) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Folder",
                        tint = if (isCurrentFolder) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentFolder) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.songCount} song(s) • ${formatDurationMillis(folder.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentFolder) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(
                    onClick = onPlayFolder,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_folder_${folder.name}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play entire folder",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse folder songs" else "Expand folder songs"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderSongRowItem(
    song: LocalSong,
    songsInFolder: List<LocalSong>,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionState: SongSelectionState,
    onSelectSong: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onOpenStudioPlayer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showSongMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f))
                } else {
                    Modifier
                }
            )
            .testTag("folder_song_${song.id}")
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        selectionState.onToggle(song)
                    } else {
                        onSelectSong()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) {
                        selectionState.onRangeSelect(song, songsInFolder)
                    } else {
                        selectionState.onToggle(song)
                    }
                }
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            if (isSelectionMode) {
                SelectionCheckBadge(isSelected = isSelected)
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.AudioFile,
                    contentDescription = "Audio track",
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${formatDurationMillis(song.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (song.savedLoopConfig != null) {
                val loopCfg = song.savedLoopConfig
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (loopCfg.isInfinite) "∞" else "${loopCfg.repeatCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showSongMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSongMenu,
                    onDismissRequest = { showSongMenu = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now") },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {
                            showSongMenu = false
                            onSelectSong()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            showSongMenu = false
                            onAddToPlaylistClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Studio Looper") },
                        leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                        onClick = {
                            showSongMenu = false
                            onSelectSong()
                            onOpenStudioPlayer()
                        }
                    )
                }
            }
    }
}
