package com.example.model

fun songMatchesQuery(song: LocalSong, normalizedQuery: String): Boolean {
    if (normalizedQuery.isBlank()) return true
    return song.title.lowercase().contains(normalizedQuery) ||
        song.artist.lowercase().contains(normalizedQuery) ||
        song.album.lowercase().contains(normalizedQuery) ||
        song.folderName.lowercase().contains(normalizedQuery)
}

fun sortSongs(
    songs: List<LocalSong>,
    option: SortOption,
    direction: SortDirection
): List<LocalSong> {
    val comparator = when (option) {
        SortOption.NAME, SortOption.SONG_COUNT -> compareBy<LocalSong> { it.title.lowercase() }
        SortOption.DATE_ADDED -> compareBy<LocalSong> { it.addedAt }
            .thenBy { it.title.lowercase() }
        SortOption.DURATION -> compareBy<LocalSong> { it.durationMs }
            .thenBy { it.title.lowercase() }
    }
    val sorted = songs.sortedWith(comparator)
    return if (direction == SortDirection.ASCENDING) sorted else sorted.reversed()
}

fun sortFolderGroups(
    folders: List<FolderInfo>,
    songsByFolder: Map<String, List<LocalSong>>,
    option: SortOption,
    direction: SortDirection
): List<FolderInfo> {
    val comparator = when (option) {
        SortOption.NAME -> compareBy<FolderInfo> { it.name.lowercase() }
        SortOption.DATE_ADDED -> compareBy<FolderInfo> { folder ->
            songsByFolder[folder.name]?.maxOfOrNull { it.addedAt } ?: 0L
        }.thenBy { it.name.lowercase() }
        SortOption.DURATION -> compareBy<FolderInfo> { it.totalDurationMs }
            .thenBy { it.name.lowercase() }
        SortOption.SONG_COUNT -> compareBy<FolderInfo> { it.songCount }
            .thenBy { it.name.lowercase() }
    }
    val sorted = folders.sortedWith(comparator)
    return if (direction == SortDirection.ASCENDING) sorted else sorted.reversed()
}

fun sortAlbumEntries(
    entries: List<Map.Entry<String, List<LocalSong>>>,
    option: SortOption,
    direction: SortDirection
): List<Map.Entry<String, List<LocalSong>>> {
    val comparator = when (option) {
        SortOption.NAME -> compareBy<Map.Entry<String, List<LocalSong>>> { it.key.lowercase() }
        SortOption.DATE_ADDED -> compareBy<Map.Entry<String, List<LocalSong>>> { entry ->
            entry.value.maxOfOrNull { it.addedAt } ?: 0L
        }.thenBy { it.key.lowercase() }
        SortOption.DURATION -> compareBy<Map.Entry<String, List<LocalSong>>> { entry ->
            entry.value.sumOf { it.durationMs }
        }.thenBy { it.key.lowercase() }
        SortOption.SONG_COUNT -> compareBy<Map.Entry<String, List<LocalSong>>> { it.value.size }
            .thenBy { it.key.lowercase() }
    }
    val sorted = entries.sortedWith(comparator)
    return if (direction == SortDirection.ASCENDING) sorted else sorted.reversed()
}

fun sortPlaylists(
    playlists: List<Playlist>,
    songMap: Map<String, LocalSong>,
    option: SortOption,
    direction: SortDirection
): List<Playlist> {
    val comparator = when (option) {
        SortOption.NAME -> compareBy<Playlist> { it.name.lowercase() }
        SortOption.DATE_ADDED -> compareBy<Playlist> { it.createdAt }
            .thenBy { it.name.lowercase() }
        SortOption.DURATION -> compareBy<Playlist> { pl ->
            pl.songIds.mapNotNull { songMap[it] }.sumOf { it.durationMs }
        }.thenBy { it.name.lowercase() }
        SortOption.SONG_COUNT -> compareBy<Playlist> { pl ->
            pl.songIds.count { it in songMap }
        }.thenBy { it.name.lowercase() }
    }
    val sorted = playlists.sortedWith(comparator)
    return if (direction == SortDirection.ASCENDING) sorted else sorted.reversed()
}
