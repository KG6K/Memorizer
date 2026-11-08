package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.player.PlayerUiState
import com.example.player.formatDurationMillis

enum class ArtistAlbumViewMode {
    SINGERS,
    ALBUMS
}

/**
 * 3rd Menu Option: Albums or Singers
 * Allows users to browse and play their songs categorized by Singer (Artist) or Album.
 */
@Composable
fun AlbumsSingersTabScreen(
    uiState: PlayerUiState,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ArtistAlbumViewMode.SINGERS) }
    var expandedItemKey by remember { mutableStateOf<String?>(null) }

    val artistsMap = remember(uiState.songs) {
        uiState.songs.groupBy { it.artist.ifBlank { "Unknown Singer" } }
    }

    val albumsMap = remember(uiState.songs) {
        uiState.songs.groupBy { it.album.ifBlank { "Unknown Album" } }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("albums_singers_tab_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Toggle Filter Chips: Singers / Albums
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = viewMode == ArtistAlbumViewMode.SINGERS,
                    onClick = {
                        viewMode = ArtistAlbumViewMode.SINGERS
                        expandedItemKey = null
                    },
                    border = null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Singers",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Singers / Artists (${artistsMap.size})") },
                    modifier = Modifier.testTag("filter_singers_button")
                )

                FilterChip(
                    selected = viewMode == ArtistAlbumViewMode.ALBUMS,
                    onClick = {
                        viewMode = ArtistAlbumViewMode.ALBUMS
                        expandedItemKey = null
                    },
                    border = null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Album,
                            contentDescription = "Albums",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Albums (${albumsMap.size})") },
                    modifier = Modifier.testTag("filter_albums_button")
                )
            }
        }

        if (viewMode == ArtistAlbumViewMode.SINGERS) {
            items(artistsMap.keys.toList(), key = { "artist_$it" }) { artistName ->
                val songs = artistsMap[artistName] ?: emptyList()
                val isExpanded = expandedItemKey == "artist_$artistName"
                val isCurrentArtist = songs.any { it.id == uiState.currentSong?.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("artist_card_$artistName"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentArtist) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedItemKey = if (isExpanded) null else "artist_$artistName"
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Singer",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${songs.size} song(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    songs.firstOrNull()?.let { first ->
                                        onSelectSong(first, "Artist: $artistName", first.folderName)
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play singer",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    expandedItemKey = if (isExpanded) null else "artist_$artistName"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Expand songs"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            SongListSubSection(
                                songs = songs,
                                uiState = uiState,
                                playlistTitle = "Artist: $artistName",
                                onSelectSong = onSelectSong,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onOpenStudioPlayer = onOpenStudioPlayer
                            )
                        }
                    }
                }
            }
        } else {
            // ALBUMS VIEW
            items(albumsMap.keys.toList(), key = { "album_$it" }) { albumName ->
                val songs = albumsMap[albumName] ?: emptyList()
                val isExpanded = expandedItemKey == "album_$albumName"
                val isCurrentAlbum = songs.any { it.id == uiState.currentSong?.id }
                val mainArtist = songs.firstOrNull()?.artist ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_card_$albumName"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentAlbum) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedItemKey = if (isExpanded) null else "album_$albumName"
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Album,
                                        contentDescription = "Album",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$mainArtist • ${songs.size} track(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledIconButton(
                                onClick = {
                                    songs.firstOrNull()?.let { first ->
                                        onSelectSong(first, "Album: $albumName", first.folderName)
                                    }
                                },
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
                                onClick = {
                                    expandedItemKey = if (isExpanded) null else "album_$albumName"
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Expand songs"
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            SongListSubSection(
                                songs = songs,
                                uiState = uiState,
                                playlistTitle = "Album: $albumName",
                                onSelectSong = onSelectSong,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onOpenStudioPlayer = onOpenStudioPlayer
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SongListSubSection(
    songs: List<LocalSong>,
    uiState: PlayerUiState,
    playlistTitle: String,
    onSelectSong: (LocalSong, playlistName: String?, folderName: String?) -> Unit,
    onAddToPlaylistClick: (LocalSong) -> Unit,
    onOpenStudioPlayer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        songs.forEach { song ->
            val isCurrent = song.id == uiState.currentSong?.id
            val isPlaying = isCurrent && (
                uiState.phase == PlaybackPhase.PLAYING ||
                    uiState.phase == PlaybackPhase.WAITING_DELAY
                )
            var showSongMenu by remember { mutableStateOf(false) }

            Surface(
                onClick = { onSelectSong(song, playlistTitle, song.folderName) },
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.AudioFile,
                        contentDescription = "Audio track",
                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
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
                            onDismissRequest = { showSongMenu = false }
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
        Spacer(modifier = Modifier.height(4.dp))
    }
}
