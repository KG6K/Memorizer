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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import com.example.model.songMatchesQuery
import com.example.model.sortPlaylists
import com.example.model.sortSongs
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

/**
 * 4th Menu Option: Playlists
 * Create, manage, view, and loop playback of playlists with lazy-loaded track lists.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistsTabScreen(
    uiState: PlayerUiState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onAddSongsToPlaylist: (playlistId: String, songIds: List<String>) -> Unit = { _, _ -> },
    onRemoveSongFromPlaylist: (playlistId: String, songId: String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onToggleExpandPlaylist: (String) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onPlaySelectedSongs: (List<LocalSong>, sourcePlaylistId: String?, sourcePlaylistName: String?) -> Unit = { _, _, _ -> }
) {
    val normalizedQuery = uiState.searchQuery.trim().lowercase()
    val songMap = remember(uiState.songs) { uiState.songs.associateBy { it.id } }
    val playlists = remember(uiState.playlists, songMap, normalizedQuery, uiState.sortOption, uiState.sortDirection) {
        val filtered = if (normalizedQuery.isBlank()) {
            uiState.playlists
        } else {
            uiState.playlists.filter { pl ->
                val plSongs = pl.songIds.mapNotNull { songMap[it] }
                pl.name.lowercase().contains(normalizedQuery) ||
                    pl.description.lowercase().contains(normalizedQuery) ||
                    plSongs.any { songMatchesQuery(it, normalizedQuery) }
            }
        }
        sortPlaylists(filtered, songMap, uiState.sortOption, uiState.sortDirection)
    }
    val playlistSongsById = remember(playlists, songMap, normalizedQuery, uiState.sortOption, uiState.sortDirection) {
        playlists.associate { pl ->
            val rawSongs = pl.songIds.mapNotNull { songMap[it] }
            val matching = if (normalizedQuery.isBlank()) {
                rawSongs
            } else {
                val filteredSongs = rawSongs.filter { songMatchesQuery(it, normalizedQuery) }
                if (filteredSongs.isEmpty() && (pl.name.lowercase().contains(normalizedQuery) || pl.description.lowercase().contains(normalizedQuery))) {
                    rawSongs
                } else {
                    filteredSongs
                }
            }
            pl.id to sortSongs(matching, uiState.sortOption, uiState.sortDirection)
        }
    }
    var playlistForAddingSongs by remember { mutableStateOf<Playlist?>(null) }
    val selectionState = rememberSongSelection()
    val currentSongId = uiState.currentSong?.id
    val isPlaybackActive = uiState.phase == PlaybackPhase.PLAYING ||
        uiState.phase == PlaybackPhase.WAITING_DELAY

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlists_tab_screen")
    ) {
        SongMultiSelectionBar(
            selectedCount = selectionState.selectedIds.size,
            onClearSelection = selectionState.onClear,
            onSelectAll = {
                val allSongs = playlistSongsById.values.flatten().distinctBy { it.id }
                selectionState.onSelectAll(allSongs)
            },
            onPlaySelected = {
                val allSongs = playlistSongsById.values.flatten().distinctBy { it.id }
                val selected = allSongs.filter { it.id in selectionState.selectedIds }
                if (selected.isNotEmpty()) {
                    val expandedPl = uiState.expandedPlaylistId?.let { id ->
                        playlists.firstOrNull { it.id == id }
                    }
                    val sourcePl = if (expandedPl != null && selected.all { it.id in expandedPl.songIds }) {
                        expandedPl
                    } else {
                        playlists.firstOrNull { pl -> selected.all { it.id in pl.songIds } }
                    }
                    onPlaySelectedSongs(selected, sourcePl?.id, sourcePl?.name)
                    selectionState.onClear()
                }
            }
        )

        val listState = rememberLazyListState()
        val fastScrollLabels = remember(playlists, playlistSongsById, uiState.expandedPlaylistId, normalizedQuery) {
            buildList {
                add(FastScrollLabel("Playlists"))
                add(FastScrollLabel("Your Playlists", "${playlists.size}"))
                if (playlists.isEmpty()) {
                    add(FastScrollLabel("No Playlists"))
                } else {
                    playlists.forEachIndexed { plIdx, playlist ->
                        add(FastScrollLabel(playlist.name, "${plIdx + 1}/${playlists.size}"))
                        val playlistSongs = playlistSongsById[playlist.id] ?: emptyList()
                        val isExpanded = if (normalizedQuery.isNotBlank()) {
                            true
                        } else {
                            uiState.expandedPlaylistId == playlist.id
                        }
                        if (isExpanded) {
                            add(FastScrollLabel(playlist.name, "${playlistSongs.size}"))
                            playlistSongs.forEachIndexed { songIdx, song ->
                                add(FastScrollLabel(song.title, "${songIdx + 1}/${playlistSongs.size}"))
                            }
                        }
                    }
                }
                add(lastOrNull() ?: FastScrollLabel("Playlists"))
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
            // Create Playlist Action Row
            item(key = "create_playlist_action_row") {
                Button(
                    onClick = onCreatePlaylistClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("create_playlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create playlist",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Playlist", fontWeight = FontWeight.Bold)
                }
            }

            item(key = "playlists_count_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Playlists (${playlists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (playlists.isEmpty()) {
                item(key = "empty_playlists_card") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
                                imageVector = if (normalizedQuery.isNotBlank()) Icons.Filled.SearchOff else Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = if (normalizedQuery.isNotBlank()) "No search results" else "No playlists",
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) "No Matching Playlists" else "No Playlists Created Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (normalizedQuery.isNotBlank()) {
                                    "Try another keyword or clear your search query."
                                } else {
                                    "Tap 'Create New Playlist' above to group songs for memorization drills or repeat sessions."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                playlists.forEach { playlist ->
                    val playlistSongs = playlistSongsById[playlist.id] ?: emptyList()
                    val isExpanded = if (normalizedQuery.isNotBlank()) {
                        true
                    } else {
                        uiState.expandedPlaylistId == playlist.id
                    }
                    val isCurrentPlaylist = uiState.currentPlaylistName == playlist.name

                    item(key = "playlist_header_${playlist.id}", contentType = "playlist_header") {
                        PlaylistHeaderCard(
                            playlist = playlist,
                            songCount = playlistSongs.size,
                            isExpanded = isExpanded,
                            isCurrentPlaylist = isCurrentPlaylist,
                            onToggleExpand = { onToggleExpandPlaylist(playlist.id) },
                            onPlayPlaylist = { onPlayPlaylist(playlist) },
                            onAddSongsClick = { playlistForAddingSongs = playlist },
                            onDeletePlaylist = { onDeletePlaylist(playlist.id) }
                        )
                    }

                    if (isExpanded) {
                        item(key = "playlist_add_bar_${playlist.id}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { playlistForAddingSongs = playlist },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("add_songs_to_playlist_${playlist.id}"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add songs",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Songs to Playlist", style = MaterialTheme.typography.labelMedium)
                                }

                                if (playlistSongs.isEmpty()) {
                                    Text(
                                        text = "No songs in this playlist yet. Tap 'Add Songs to Playlist' above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        if (playlistSongs.isNotEmpty()) {
                            items(
                                items = playlistSongs,
                                key = { song -> "playlist_${playlist.id}_song_${song.id}" },
                                contentType = { "playlist_song_row" }
                            ) { song ->
                                PlaylistSongRowItem(
                                    song = song,
                                    playlistSongs = playlistSongs,
                                    isCurrent = song.id == currentSongId,
                                    isPlaying = song.id == currentSongId && isPlaybackActive,
                                    isSelected = song.id in selectionState.selectedIds,
                                    isSelectionMode = selectionState.isSelectionMode,
                                    selectionState = selectionState,
                                    onSelectSong = {
                                        onSelectSong(song, playlist.name, song.folderName)
                                    },
                                    onRemoveSong = {
                                        onRemoveSongFromPlaylist(playlist.id, song.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "playlists_bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            }

            FastScrollBarOverlay(
                listState = listState,
                labels = fastScrollLabels,
                testTagPrefix = "playlists_fast_scroll"
            )
        }

        playlistForAddingSongs?.let { targetPlaylist ->
            AddSongsToExistingPlaylistDialog(
                playlist = targetPlaylist,
                allSongs = uiState.songs,
                onDismiss = { playlistForAddingSongs = null },
                onAddSongs = { songIds ->
                    onAddSongsToPlaylist(targetPlaylist.id, songIds)
                    playlistForAddingSongs = null
                }
            )
        }
    }
}

@Composable
private fun AddSongsToExistingPlaylistDialog(
    playlist: Playlist,
    allSongs: List<LocalSong>,
    onDismiss: () -> Unit,
    onAddSongs: (List<String>) -> Unit
) {
    val existingIds = remember(playlist.songIds) { playlist.songIds.toSet() }
    var newlySelectedIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = "Add Songs to \"${playlist.name}\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            if (allSongs.isEmpty()) {
                Text(
                    text = "No songs found in your library.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allSongs, key = { it.id }) { song ->
                        val alreadyIn = song.id in existingIds
                        val isSelected = song.id in newlySelectedIds
                        Surface(
                            onClick = {
                                if (!alreadyIn) {
                                    newlySelectedIds = if (isSelected) {
                                        newlySelectedIds - song.id
                                    } else {
                                        newlySelectedIds + song.id
                                    }
                                }
                            },
                            color = when {
                                alreadyIn -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected || alreadyIn) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = when {
                                        alreadyIn -> "In Playlist"
                                        isSelected -> "✓ Selected"
                                        else -> "+ Select"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        alreadyIn -> MaterialTheme.colorScheme.outline
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddSongs(newlySelectedIds.toList()) },
                enabled = newlySelectedIds.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Add (${newlySelectedIds.size})", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun PlaylistHeaderCard(
    playlist: Playlist,
    songCount: Int,
    isExpanded: Boolean,
    isCurrentPlaylist: Boolean,
    onToggleExpand: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onAddSongsClick: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlaylist) {
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = if (isCurrentPlaylist) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Playlist",
                        tint = if (isCurrentPlaylist) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
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
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (playlist.description.isNotBlank()) {
                        "${playlist.description} • $songCount song(s)"
                    } else {
                        "$songCount song(s)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FilledIconButton(
                    onClick = onPlayPlaylist,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("play_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play playlist",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Playlist options",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Songs") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddSongsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play Playlist") },
                            leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onPlayPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Playlist") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDeletePlaylist()
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse playlist" else "Expand playlist",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSongRowItem(
    song: LocalSong,
    playlistSongs: List<LocalSong>,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    selectionState: SongSelectionState,
    onSelectSong: () -> Unit,
    onRemoveSong: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
                        selectionState.onRangeSelect(song, playlistSongs)
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

            IconButton(
                onClick = onRemoveSong,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlaylistRemove,
                    contentDescription = "Remove from playlist",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}
