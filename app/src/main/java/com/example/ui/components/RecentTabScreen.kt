package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FolderInfo
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import com.example.model.RecentCollectionSession
import com.example.model.songMatchesQuery
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

private data class RecentFolderDisplay(
    val folder: FolderInfo,
    val session: RecentCollectionSession?,
    val resolvedSessionSongs: List<LocalSong>,
    val sortTimestamp: Long
)

private data class RecentPlaylistDisplay(
    val playlist: Playlist,
    val session: RecentCollectionSession?,
    val resolvedSessionSongs: List<LocalSong>,
    val sortTimestamp: Long
)

/**
 * 1st Menu Option: Recent
 * Contains:
 * - Recent playing folders (with saved sub-range C..F & last config on Play, open folder on body tap)
 * - Recent playlists (with saved sub-range C..F & last config on Play, open playlist on body tap)
 * - Recent songs
 */
@Composable
fun RecentTabScreen(
    uiState: PlayerUiState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onOpenFolder: (String) -> Unit,
    onPlayFolder: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onPickAudioFiles: () -> Unit = {},
    onPickAudioFolder: () -> Unit = {}
) {
    val normalizedQuery = uiState.searchQuery.trim().lowercase()
    val songMap = remember(uiState.songs) { uiState.songs.associateBy { it.id } }

    val recentFolders = remember(uiState.songs, uiState.recentSessions, songMap, normalizedQuery) {
        val folderSessionsByName = uiState.recentSessions
            .filter { !it.isPlaylist && !it.folderName.isNullOrBlank() }
            .associateBy { it.folderName!! }
        val allFolderSongs = uiState.songs.groupBy { it.folderName }
        val merged = uiState.folderGroups.map { folder ->
            val session = folderSessionsByName[folder.name]
            val resolved = session?.songIds?.mapNotNull { songMap[it] } ?: emptyList()
            val ts = maxOf(session?.lastPlayedAt ?: 0L, folder.lastPlayedAt)
            RecentFolderDisplay(
                folder = folder,
                session = session,
                resolvedSessionSongs = resolved,
                sortTimestamp = ts
            )
        }.sortedByDescending { it.sortTimestamp }

        val filtered = if (normalizedQuery.isBlank()) {
            merged
        } else {
            merged.filter { entry ->
                entry.folder.name.lowercase().contains(normalizedQuery) ||
                    entry.resolvedSessionSongs.any { songMatchesQuery(it, normalizedQuery) } ||
                    (allFolderSongs[entry.folder.name]?.any { songMatchesQuery(it, normalizedQuery) } == true)
            }
        }
        filtered.take(6)
    }

    val recentPlaylists = remember(uiState.playlists, uiState.recentSessions, songMap, normalizedQuery) {
        val playlistSessionsById = uiState.recentSessions
            .filter { it.isPlaylist && !it.playlistId.isNullOrBlank() }
            .associateBy { it.playlistId!! }
        val merged = uiState.playlists.map { playlist ->
            val session = playlistSessionsById[playlist.id]
            val resolved = session?.songIds?.mapNotNull { songMap[it] } ?: emptyList()
            val ts = maxOf(session?.lastPlayedAt ?: 0L, playlist.lastPlayedAt)
            RecentPlaylistDisplay(
                playlist = playlist,
                session = session,
                resolvedSessionSongs = resolved,
                sortTimestamp = ts
            )
        }.sortedByDescending { it.sortTimestamp }

        val filtered = if (normalizedQuery.isBlank()) {
            merged
        } else {
            merged.filter { entry ->
                entry.playlist.name.lowercase().contains(normalizedQuery) ||
                    entry.playlist.description.lowercase().contains(normalizedQuery) ||
                    entry.resolvedSessionSongs.any { songMatchesQuery(it, normalizedQuery) } ||
                    entry.playlist.songIds.mapNotNull { songMap[it] }.any { songMatchesQuery(it, normalizedQuery) }
            }
        }
        filtered.take(6)
    }

    val recentSongs = remember(uiState.songs, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            uiState.recentSongs
        } else {
            uiState.songs
                .filter { songMatchesQuery(it, normalizedQuery) }
                .sortedByDescending { it.lastPlayedAt }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recent_tab_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (normalizedQuery.isNotBlank() && recentFolders.isEmpty() && recentPlaylists.isEmpty() && recentSongs.isEmpty()) {
            item(key = "recent_empty_search_card") {
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
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = "No search results",
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Matching Recent Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try another keyword or clear your search query.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Section 1: Recent Playing Folders
            item(key = "recent_folders_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Recent Folders",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Recent Playing Folders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (recentFolders.isEmpty()) {
                        Text(
                            text = if (normalizedQuery.isNotBlank()) {
                                "No recent folders match \"${uiState.searchQuery.trim()}\"."
                            } else {
                                "No recent folders found. Play a song to record folders."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in recentFolders) {
                                RecentFolderCard(
                                    display = item,
                                    isCurrent = item.folder.name == uiState.currentFolderName,
                                    onOpenFolder = { onOpenFolder(item.folder.name) },
                                    onPlayFolder = { onPlayFolder(item.folder.name) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Recent Playlists
            item(key = "recent_playlists_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Recent Playlists",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Recent Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (recentPlaylists.isEmpty()) {
                        Text(
                            text = if (normalizedQuery.isNotBlank()) {
                                "No recent playlists match \"${uiState.searchQuery.trim()}\"."
                            } else {
                                "No recent playlists yet. Create a playlist to start looping collections."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in recentPlaylists) {
                                RecentPlaylistCard(
                                    display = item,
                                    isCurrent = item.playlist.name == uiState.currentPlaylistName,
                                    onOpenPlaylist = { onOpenPlaylist(item.playlist.id) },
                                    onPlayPlaylist = { onPlayPlaylist(item.playlist) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Recent Songs Header
            item(key = "recent_songs_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Recent Songs",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Recent Songs (${recentSongs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            items(recentSongs, key = { it.id }) { song ->
                val isCurrent = song.id == uiState.currentSong?.id
                val isPlaying = isCurrent && (
                    uiState.phase == PlaybackPhase.PLAYING ||
                        uiState.phase == PlaybackPhase.WAITING_DELAY
                    )
                RecentSongItemCard(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    onSelect = { onSelectSong(song, uiState.currentPlaylistName, song.folderName) },
                    onAddToPlaylist = { onAddToPlaylistClick(song) },
                    onOpenStudioPlayer = onOpenStudioPlayer
                )
            }
        }

        item(key = "recent_bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecentFolderCard(
    display: RecentFolderDisplay,
    isCurrent: Boolean,
    onOpenFolder: () -> Unit,
    onPlayFolder: () -> Unit
) {
    val folder = display.folder
    val session = display.session
    val resolvedSongs = display.resolvedSessionSongs
    val hasSubRange = session?.isSubSelection == true && resolvedSongs.isNotEmpty()

    Card(
        onClick = onOpenFolder,
        modifier = Modifier
            .width(204.dp)
            .testTag("recent_folder_${folder.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = onPlayFolder,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("recent_play_folder_${folder.name}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play recent folder selection",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleText = if (hasSubRange) {
                "${resolvedSongs.size} selected • ${resolvedSongs.first().title} – ${resolvedSongs.last().title}"
            } else {
                "${folder.songCount} track(s)"
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (session != null) {
                Text(
                    text = session.loopConfig.formatSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentPlaylistCard(
    display: RecentPlaylistDisplay,
    isCurrent: Boolean,
    onOpenPlaylist: () -> Unit,
    onPlayPlaylist: () -> Unit
) {
    val playlist = display.playlist
    val session = display.session
    val resolvedSongs = display.resolvedSessionSongs
    val hasSubRange = session?.isSubSelection == true && resolvedSongs.isNotEmpty()

    Card(
        onClick = onOpenPlaylist,
        modifier = Modifier
            .width(210.dp)
            .testTag("recent_playlist_${playlist.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Playlist",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = onPlayPlaylist,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.tertiary
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("recent_play_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play recent playlist selection",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitleText = when {
                hasSubRange ->
                    "${resolvedSongs.size} selected • ${resolvedSongs.first().title} – ${resolvedSongs.last().title}"
                playlist.description.isNotBlank() ->
                    playlist.description
                else ->
                    "${playlist.songIds.size} songs in playlist"
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (session != null) {
                Text(
                    text = session.loopConfig.formatSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentSongItemCard(
    song: LocalSong,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenStudioPlayer: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_song_card_${song.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isPlaying -> Icons.Filled.GraphicEq
                            isCurrent -> Icons.Filled.PlayArrow
                            else -> Icons.Filled.AudioFile
                        },
                        contentDescription = "Song status",
                        tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val metaText = buildList {
                    if (song.artist.isNotBlank()) add(song.artist)
                    if (song.album.isNotBlank()) add(song.album)
                    add(formatDurationMillis(song.durationMs))
                }.joinToString(" • ")

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Song options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now") },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onSelect()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Studio Looper") },
                        leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onSelect()
                            onOpenStudioPlayer()
                        }
                    )
                }
            }
        }
    }
}
