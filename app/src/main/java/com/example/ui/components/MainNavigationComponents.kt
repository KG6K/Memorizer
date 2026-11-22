package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LocalSong
import com.example.model.MainMenuTab
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.player.PlayerUiState
import com.example.player.formatDelayPreciseMillis
import com.example.ui.theme.JetBrainsMonoFamily

/**
 * Top App Bar containing:
 * - App Logo at top left corner
 * - Compact Search Bar right after the logo
 * - Global Sort button (Name, Date Added, Duration, Song Count + Asc/Desc)
 * - Settings button
 */
@Composable
fun MainTopHeaderBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOption: SortOption,
    sortDirection: SortDirection,
    onSelectSortOption: (SortOption) -> Unit,
    onSelectSortDirection: (SortDirection) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. App logo at top left corner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenStudioPlayer() }
                    .testTag("app_logo_button")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_icon_1794082290110),
                    contentDescription = "Memorizer Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Clean 42.dp Search Pill (borderless, shaded by theme)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search audio...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("top_search_bar")
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search query",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Global Sort button & DropdownMenu
            Box {
                Surface(
                    onClick = { showSortMenu = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("top_sort_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort library",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(230.dp)
                ) {
                    Text(
                        text = "Sort Order",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SortDirection.entries.forEach { dir ->
                            val isSelectedDir = sortDirection == dir
                            Surface(
                                onClick = {
                                    onSelectSortDirection(dir)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelectedDir) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("sort_direction_${dir.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (dir == SortDirection.ASCENDING) {
                                            Icons.Filled.ArrowUpward
                                        } else {
                                            Icons.Filled.ArrowDownward
                                        },
                                        contentDescription = dir.label,
                                        tint = if (isSelectedDir) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (dir == SortDirection.ASCENDING) "Asc" else "Desc",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelectedDir) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelectedDir) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    SortOption.entries.forEach { option ->
                        val isSelectedOpt = sortOption == option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (isSelectedOpt) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelectedOpt) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            trailingIcon = {
                                if (isSelectedOpt) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectSortOption(option)
                                showSortMenu = false
                            },
                            modifier = Modifier.testTag("sort_option_${option.name.lowercase()}")
                        )
                    }
                }
            }

            // 4. Settings button
            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("top_settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Balanced 4-Column Single-Line Pill Tab Bar below the top bar:
 * 1st: Recent | 2nd: Folders | 3rd: Albums | 4th: Playlists
 */
@Composable
fun MainMenuTabsRow(
    selectedTab: MainMenuTab,
    onTabSelected: (MainMenuTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainMenuTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val icon = when (tab) {
                        MainMenuTab.RECENT -> Icons.Filled.AccessTime
                        MainMenuTab.FOLDERS -> Icons.Filled.Folder
                        MainMenuTab.ALBUMS_SINGERS -> Icons.Filled.Album
                        MainMenuTab.PLAYLISTS -> Icons.AutoMirrored.Filled.QueueMusic
                    }
                    Surface(
                        onClick = { onTabSelected(tab) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .semantics { selected = isSelected }
                            .testTag("main_menu_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        }
    }
}

/**
 * Bottom Mini-Player Bar showing:
 * - Active song title & artist
 * - Loop repetition / pause status badge
 * - Play/Pause and Next controls
 * - Tapping opens Studio Player
 */
@Composable
fun LastPlayingBottomBar(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onNextSong: () -> Unit,
    onOpenStudioPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = uiState.currentSong ?: return
    val cfg = uiState.loopConfig

    val progress = if (uiState.phase == PlaybackPhase.WAITING_DELAY && uiState.delayTotalMs > 0L) {
        (uiState.delayRemainingMs.toFloat() / uiState.delayTotalMs.toFloat()).coerceIn(0f, 1f)
    } else if (uiState.trackDurationMs > 0L) {
        (uiState.currentPositionMs.toFloat() / uiState.trackDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isPlayingActive = uiState.phase == PlaybackPhase.PLAYING ||
        (uiState.phase == PlaybackPhase.WAITING_DELAY && !uiState.isDelayPaused)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shape = RectangleShape,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenStudioPlayer() }
            .testTag("last_playing_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Timeline progress indicator on top edge
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (uiState.phase == PlaybackPhase.WAITING_DELAY) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Song avatar/icon
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlayingActive) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                            contentDescription = "Active audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Title & Loop Status + Artist (Fixed loop badge position so it never shifts or collides)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalLoops = if (cfg.isInfinite) "∞" else cfg.repeatCount.toString()
                    val loopText = if (uiState.phase == PlaybackPhase.WAITING_DELAY) {
                        "Pause ${formatDelayPreciseMillis(uiState.delayRemainingMs)}"
                    } else {
                        "Rep ${uiState.currentIteration}/$totalLoops"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = loopText,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (song.artist.isNotBlank()) {
                            Text(
                                text = "• ${song.artist}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Transport Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("bottom_bar_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlayingActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlayingActive) "Pause" else "Play",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextSong,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("bottom_bar_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next song",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to create a new custom playlist with optional song selection during creation
 */
@Composable
fun CreatePlaylistDialog(
    availableSongs: List<LocalSong> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, selectedSongIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = "Create New Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("e.g., Quran Memorization") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g., Daily Hifz interval repetitions") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_playlist_desc_input")
                )

                if (availableSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Songs (${selectedIds.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                selectedIds = if (selectedIds.size == availableSongs.size) {
                                    emptySet()
                                } else {
                                    availableSongs.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (selectedIds.size == availableSongs.size) "Clear All" else "Select All",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableSongs, key = { it.id }) { song ->
                            val isSelected = song.id in selectedIds
                            Surface(
                                onClick = {
                                    selectedIds = if (isSelected) selectedIds - song.id else selectedIds + song.id
                                },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
                                        text = if (isSelected) "✓ Added" else "+ Add",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim(), selectedIds.toList())
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("confirm_create_playlist_button"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Create", style = MaterialTheme.typography.labelMedium)
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

/**
 * Dialog to pick which playlist to add a song to
 */
@Composable
fun AddToPlaylistDialog(
    song: LocalSong,
    playlists: List<Playlist>,
    onAddToPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        title = {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a playlist for \"${song.title}\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists available yet. Tap below to create your first playlist.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists, key = { it.id }) { pl ->
                            val alreadyIn = song.id in pl.songIds
                            Surface(
                                onClick = {
                                    if (!alreadyIn) {
                                        onAddToPlaylist(pl.id)
                                        onDismiss()
                                    }
                                },
                                color = if (alreadyIn) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (alreadyIn) "Added" else "+ Add",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alreadyIn) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onCreateNewPlaylist()
                },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("+ New Playlist", style = MaterialTheme.typography.labelMedium)
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
