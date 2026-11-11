package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderInfo
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

/**
 * 2nd Menu Option: Folders
 * Displays folders which contain songs, with folder-level playback and browsing.
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
    onPlaySelectedSongs: (List<LocalSong>) -> Unit = {},
    onPickAudioFolder: () -> Unit = {},
    onPickAudioFiles: () -> Unit = {}
) {
    val folderGroups = uiState.folderGroups
    val songsByFolder = remember(uiState.songs) {
        uiState.songs.groupBy { it.folderName }
    }
    val selectionState = rememberSongSelection()

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
                    onPlaySelectedSongs(selected)
                    selectionState.onClear()
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        if (folderGroups.isEmpty()) {
            item {
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
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "No folders",
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Audio Folders Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grant audio permission to automatically scan music folders from your device storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(folderGroups, key = { it.name }) { folder ->
                val songsInFolder = songsByFolder[folder.name] ?: emptyList()
                val isExpanded = uiState.expandedFolder == folder.name
                val isCurrentFolder = uiState.currentFolderName == folder.name

                FolderGroupCard(
                    folder = folder,
                    songs = songsInFolder,
                    isExpanded = isExpanded,
                    isCurrentFolder = isCurrentFolder,
                    uiState = uiState,
                    onToggleExpand = { onToggleExpandFolder(folder.name) },
                    onPlayFolder = { onPlayFolder(folder.name) },
                    onSelectSong = { song: LocalSong ->
                        onSelectSong(song, "Folder: ${folder.name}", folder.name)
                    },
                    selectionState = selectionState,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onOpenStudioPlayer = onOpenStudioPlayer
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderGroupCard(
    folder: FolderInfo,
    songs: List<LocalSong>,
    isExpanded: Boolean,
    isCurrentFolder: Boolean,
    uiState: PlayerUiState,
    selectionState: SongSelectionState,
    onToggleExpand: () -> Unit,
    onPlayFolder: () -> Unit,
    onSelectSong: (LocalSong) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit
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
        Column(modifier = Modifier.fillMaxWidth()) {
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

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))

                    val haptic = LocalHapticFeedback.current

                    songs.forEach { song ->
                        val isCurrent = song.id == uiState.currentSong?.id
                        val isSelected = selectionState.selectedIds.contains(song.id)
                        val isPlaying = isCurrent && (
                            uiState.phase == PlaybackPhase.PLAYING ||
                                uiState.phase == PlaybackPhase.WAITING_DELAY
                            )
                        var showSongMenu by remember { mutableStateOf(false) }

                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            } else if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("folder_song_${song.id}")
                                .combinedClickable(
                                    onClick = {
                                        if (selectionState.isSelectionMode) {
                                            selectionState.onToggle(song)
                                        } else {
                                            onSelectSong(song)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (selectionState.isSelectionMode) {
                                            selectionState.onRangeSelect(song, songs)
                                        } else {
                                            selectionState.onToggle(song)
                                        }
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (selectionState.isSelectionMode) {
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${formatDurationMillis(song.durationMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                onSelectSong(song)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add to Playlist") },
                                            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                                            onClick = {
                                                showSongMenu = false
                                                onAddToPlaylistClick(song)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Studio Looper") },
                                            leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                                            onClick = {
                                                showSongMenu = false
                                                onSelectSong(song)
                                                onOpenStudioPlayer()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
