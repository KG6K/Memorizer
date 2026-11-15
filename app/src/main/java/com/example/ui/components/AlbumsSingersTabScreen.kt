package com.example.ui.components

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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.model.songMatchesQuery
import com.example.model.sortAlbumEntries
import com.example.model.sortSongs
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

/**
 * 3rd Menu Option: Albums
 * Allows users to browse and play their songs categorized by Album with lazy-loaded tracks.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsSingersTabScreen(
    uiState: PlayerUiState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onPlaySelectedSongs: (List<LocalSong>) -> Unit = {}
) {
    var expandedItemKey by remember { mutableStateOf<String?>(null) }
    val normalizedQuery = uiState.searchQuery.trim().lowercase()

    val albumsMap = remember(uiState.songs, normalizedQuery, uiState.sortOption, uiState.sortDirection) {
        val grouped = uiState.songs.groupBy { it.album.ifBlank { "Unknown Album" } }
        grouped.mapValues { (albumName, songs) ->
            val matching = if (normalizedQuery.isBlank()) {
                songs
            } else {
                val filtered = songs.filter { songMatchesQuery(it, normalizedQuery) }
                if (filtered.isEmpty() && albumName.lowercase().contains(normalizedQuery)) {
                    songs
                } else {
                    filtered
                }
            }
            sortSongs(matching, uiState.sortOption, uiState.sortDirection)
        }.filterValues { it.isNotEmpty() }
    }
    val sortedAlbumEntries = remember(albumsMap, uiState.sortOption, uiState.sortDirection) {
        sortAlbumEntries(albumsMap.entries.toList(), uiState.sortOption, uiState.sortDirection)
    }
    val selectionState = rememberSongSelection()
    val currentSongId = uiState.currentSong?.id
    val isPlaybackActive = uiState.phase == PlaybackPhase.PLAYING ||
        uiState.phase == PlaybackPhase.WAITING_DELAY

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("albums_singers_tab_screen")
    ) {
        SongMultiSelectionBar(
            selectedCount = selectionState.selectedIds.size,
            onClearSelection = selectionState.onClear,
            onSelectAll = {
                val allSongs = sortedAlbumEntries.flatMap { it.value }
                selectionState.onSelectAll(allSongs)
            },
            onPlaySelected = {
                val allSongs = sortedAlbumEntries.flatMap { it.value }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (sortedAlbumEntries.isEmpty()) {
                item(key = "empty_albums_card") {
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
                                imageVector = if (normalizedQuery.isNotBlank()) Icons.Filled.SearchOff else Icons.Filled.Album,
                                contentDescription = if (normalizedQuery.isNotBlank()) "No search results" else "No albums",
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) "No Matching Albums or Songs" else "No Albums Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) {
                                    "Try another keyword or clear your search query."
                                } else {
                                    "Grant audio permission to automatically scan albums from your device storage."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                sortedAlbumEntries.forEach { (albumName, songs) ->
                    val isExpanded = if (normalizedQuery.isNotBlank()) {
                        true
                    } else {
                        expandedItemKey == "album_$albumName"
                    }
                    val isCurrentAlbum = songs.any { it.id == currentSongId }
                    val mainArtist = songs.firstOrNull()?.artist ?: ""

                    item(key = "album_header_$albumName", contentType = "album_header") {
                        AlbumHeaderCard(
                            albumName = albumName,
                            mainArtist = mainArtist,
                            songCount = songs.size,
                            isExpanded = isExpanded,
                            isCurrentAlbum = isCurrentAlbum,
                            onToggleExpand = {
                                expandedItemKey = if (isExpanded) null else "album_$albumName"
                            },
                            onPlayAlbum = {
                                songs.firstOrNull()?.let { first ->
                                    onSelectSong(first, "Album: $albumName", first.folderName)
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        items(
                            items = songs,
                            key = { song -> "album_${albumName}_song_${song.id}" },
                            contentType = { "album_song_row" }
                        ) { song ->
                            AlbumSongRowItem(
                                song = song,
                                songsInAlbum = songs,
                                playlistTitle = "Album: $albumName",
                                isCurrent = song.id == currentSongId,
                                isPlaying = song.id == currentSongId && isPlaybackActive,
                                isSelected = song.id in selectionState.selectedIds,
                                isSelectionMode = selectionState.isSelectionMode,
                                selectionState = selectionState,
                                onSelectSong = onSelectSong,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onOpenStudioPlayer = onOpenStudioPlayer
                            )
                        }
                    }
                }
            }

            item(key = "albums_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AlbumHeaderCard(
    albumName: String,
    mainArtist: String,
    songCount: Int,
    isExpanded: Boolean,
    isCurrentAlbum: Boolean,
    onToggleExpand: () -> Unit,
    onPlayAlbum: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("album_card_$albumName"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentAlbum) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
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
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Album,
                        contentDescription = "Album",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentAlbum) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$mainArtist • $songCount track(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentAlbum) {
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
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
                    onClick = onPlayAlbum,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play album",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand songs"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumSongRowItem(
    song: LocalSong,
    songsInAlbum: List<LocalSong>,
    playlistTitle: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionState: SongSelectionState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
            .padding(start = 10.dp)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        selectionState.onToggle(song)
                    } else {
                        onSelectSong(song, playlistTitle, song.folderName)
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) {
                        selectionState.onRangeSelect(song, songsInAlbum)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDurationMillis(song.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                            onSelectSong(song, playlistTitle, song.folderName)
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
                            onSelectSong(song, playlistTitle, song.folderName)
                            onOpenStudioPlayer()
                        }
                    )
                }
            }
        }
    }
}
