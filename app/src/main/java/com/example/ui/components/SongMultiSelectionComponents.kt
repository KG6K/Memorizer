package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LocalSong

class SongSelectionState(
    val selectedIds: Set<String>,
    val anchorSong: LocalSong?,
    val onToggle: (LocalSong) -> Unit,
    val onRangeSelect: (LocalSong, List<LocalSong>) -> Unit,
    val onSelectAll: (List<LocalSong>) -> Unit,
    val onClear: () -> Unit
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
}

@Composable
fun rememberSongSelection(): SongSelectionState {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var anchorSong by remember { mutableStateOf<LocalSong?>(null) }

    return remember(selectedIds, anchorSong) {
        SongSelectionState(
            selectedIds = selectedIds,
            anchorSong = anchorSong,
            onToggle = { song ->
                selectedIds = if (song.id in selectedIds) {
                    val next = selectedIds - song.id
                    if (next.isEmpty()) anchorSong = null
                    next
                } else {
                    anchorSong = song
                    selectedIds + song.id
                }
            },
            onRangeSelect = { target, list ->
                val anchor = anchorSong
                if (anchor == null || !selectedIds.contains(anchor.id)) {
                    anchorSong = target
                    selectedIds = selectedIds + target.id
                } else {
                    val idxA = list.indexOfFirst { it.id == anchor.id }
                    val idxB = list.indexOfFirst { it.id == target.id }
                    if (idxA != -1 && idxB != -1) {
                        val start = minOf(idxA, idxB)
                        val end = maxOf(idxA, idxB)
                        val rangeIds = list.subList(start, end + 1).map { it.id }.toSet()
                        selectedIds = selectedIds + rangeIds
                        anchorSong = target
                    } else {
                        anchorSong = target
                        selectedIds = selectedIds + target.id
                    }
                }
            },
            onSelectAll = { list ->
                selectedIds = list.map { it.id }.toSet()
                anchorSong = list.firstOrNull()
            },
            onClear = {
                selectedIds = emptySet()
                anchorSong = null
            }
        )
    }
}

/**
 * Contextual selection top bar that shows item count, Select All, Clear, and Play button.
 */
@Composable
fun SongMultiSelectionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onPlaySelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("song_multi_selection_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear selection",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelectAll,
                            contentDescription = "Select all",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("All", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onPlaySelected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("play_selected_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play selected songs",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean circular checkmark badge for selection mode.
 */
@Composable
fun SelectionCheckBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(22.dp)
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    } else {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = androidx.compose.ui.graphics.Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = modifier.size(22.dp)
        ) {}
    }
}
