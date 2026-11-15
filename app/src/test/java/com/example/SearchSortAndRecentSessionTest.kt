package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.SongRepository
import com.example.model.AfterLoopAction
import com.example.model.FolderInfo
import com.example.model.LocalSong
import com.example.model.LoopConfig
import com.example.model.MainMenuTab
import com.example.model.Playlist
import com.example.model.RecentCollectionSession
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.model.songMatchesQuery
import com.example.model.sortAlbumEntries
import com.example.model.sortFolderGroups
import com.example.model.sortPlaylists
import com.example.model.sortSongs
import com.example.player.AudioPlaybackEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchSortAndRecentSessionTest {

    private fun sampleSong(
        id: String,
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        durationMs: Long = 10_000L,
        addedAt: Long = 1_000L,
        folderName: String = "FolderA"
    ) = LocalSong(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uriString = "content://media/external/audio/media/$id",
        addedAt = addedAt,
        folderName = folderName
    )

    @Test
    fun `songMatchesQuery matches title artist album and folder case-insensitively`() {
        val song = sampleSong(
            id = "1",
            title = "Surah Mulk",
            artist = "Mishary Alafasy",
            album = "Juz Tabarak",
            folderName = "Hifz_Track"
        )
        assertTrue(songMatchesQuery(song, "mulk"))
        assertTrue(songMatchesQuery(song, "afasy"))
        assertTrue(songMatchesQuery(song, "tabarak"))
        assertTrue(songMatchesQuery(song, "hifz"))
        assertFalse(songMatchesQuery(song, "baqarah"))
    }

    @Test
    fun `sortSongs supports Name DateAdded Duration and SongCount in Asc and Desc`() {
        val s1 = sampleSong("1", title = "Bravo", durationMs = 30_000L, addedAt = 200L)
        val s2 = sampleSong("2", title = "Alpha", durationMs = 10_000L, addedAt = 300L)
        val s3 = sampleSong("3", title = "Charlie", durationMs = 20_000L, addedAt = 100L)
        val list = listOf(s1, s2, s3)

        assertEquals(listOf("2", "1", "3"), sortSongs(list, SortOption.NAME, SortDirection.ASCENDING).map { it.id })
        assertEquals(listOf("3", "1", "2"), sortSongs(list, SortOption.NAME, SortDirection.DESCENDING).map { it.id })

        assertEquals(listOf("3", "1", "2"), sortSongs(list, SortOption.DATE_ADDED, SortDirection.ASCENDING).map { it.id })
        assertEquals(listOf("2", "1", "3"), sortSongs(list, SortOption.DATE_ADDED, SortDirection.DESCENDING).map { it.id })

        assertEquals(listOf("2", "3", "1"), sortSongs(list, SortOption.DURATION, SortDirection.ASCENDING).map { it.id })
        assertEquals(listOf("1", "3", "2"), sortSongs(list, SortOption.DURATION, SortDirection.DESCENDING).map { it.id })
    }

    @Test
    fun `sortFolderGroups Albums and Playlists honor all criteria`() {
        val a1 = sampleSong("a1", "A1", durationMs = 5_000L, addedAt = 500L, folderName = "Zeta", album = "BetaAlbum")
        val b1 = sampleSong("b1", "B1", durationMs = 20_000L, addedAt = 100L, folderName = "Alpha", album = "AlphaAlbum")
        val b2 = sampleSong("b2", "B2", durationMs = 20_000L, addedAt = 200L, folderName = "Alpha", album = "AlphaAlbum")

        val songsByFolder = mapOf(
            "Zeta" to listOf(a1),
            "Alpha" to listOf(b1, b2)
        )
        val folders = listOf(
            FolderInfo("Zeta", songCount = 1, totalDurationMs = 5_000L),
            FolderInfo("Alpha", songCount = 2, totalDurationMs = 40_000L)
        )

        assertEquals(
            listOf("Alpha", "Zeta"),
            sortFolderGroups(folders, songsByFolder, SortOption.NAME, SortDirection.ASCENDING).map { it.name }
        )
        assertEquals(
            listOf("Alpha", "Zeta"),
            sortFolderGroups(folders, songsByFolder, SortOption.DATE_ADDED, SortDirection.ASCENDING).map { it.name }
        )
        assertEquals(
            listOf("Zeta", "Alpha"),
            sortFolderGroups(folders, songsByFolder, SortOption.DATE_ADDED, SortDirection.DESCENDING).map { it.name }
        )
        assertEquals(
            listOf("Zeta", "Alpha"),
            sortFolderGroups(folders, songsByFolder, SortOption.SONG_COUNT, SortDirection.ASCENDING).map { it.name }
        )
        assertEquals(
            listOf("Alpha", "Zeta"),
            sortFolderGroups(folders, songsByFolder, SortOption.DURATION, SortDirection.DESCENDING).map { it.name }
        )

        val albumMap = mapOf(
            "BetaAlbum" to listOf(a1),
            "AlphaAlbum" to listOf(b1, b2)
        )
        assertEquals(
            listOf("AlphaAlbum", "BetaAlbum"),
            sortAlbumEntries(albumMap.entries.toList(), SortOption.SONG_COUNT, SortDirection.DESCENDING).map { it.key }
        )

        val songMap = mapOf("a1" to a1, "b1" to b1, "b2" to b2)
        val pl1 = Playlist(id = "p1", name = "Short", songIds = listOf("a1"), createdAt = 300L)
        val pl2 = Playlist(id = "p2", name = "Long", songIds = listOf("b1", "b2"), createdAt = 100L)
        assertEquals(
            listOf("p2", "p1"),
            sortPlaylists(listOf(pl1, pl2), songMap, SortOption.DURATION, SortDirection.DESCENDING).map { it.id }
        )
        assertEquals(
            listOf("p1", "p2"),
            sortPlaylists(listOf(pl1, pl2), songMap, SortOption.DATE_ADDED, SortDirection.DESCENDING).map { it.id }
        )
    }

    @Test
    fun `SongRepository persists sort preferences and recent sub-range sessions`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = SongRepository(app)

        repo.saveSortOption(SortOption.DURATION)
        repo.saveSortDirection(SortDirection.DESCENDING)
        assertEquals(SortOption.DURATION, repo.loadSortOption())
        assertEquals(SortDirection.DESCENDING, repo.loadSortDirection())

        val cfg = LoopConfig(
            repeatCount = 7,
            delaySeconds = 4,
            matchTrackDurationDelay = false,
            sectionStartMs = 1500L,
            sectionEndMs = 6000L,
            playbackSpeed = 1.25f,
            afterCompletionAction = AfterLoopAction.REPEAT_PLAYLIST
        )
        val session = RecentCollectionSession(
            key = "folder:Surahs",
            isPlaylist = false,
            name = "Surahs",
            folderName = "Surahs",
            songIds = listOf("c", "d", "e", "f"),
            lastSongId = "c",
            loopConfig = cfg,
            isSubSelection = true,
            lastPlayedAt = 9999L
        )
        repo.saveRecentSessions(listOf(session))

        val loaded = repo.loadRecentSessions()
        assertEquals(1, loaded.size)
        val restored = loaded.first()
        assertEquals("folder:Surahs", restored.key)
        assertEquals(listOf("c", "d", "e", "f"), restored.songIds)
        assertTrue(restored.isSubSelection)
        assertEquals(7, restored.loopConfig.repeatCount)
        assertEquals(4, restored.loopConfig.delaySeconds)
        assertEquals(1500L, restored.loopConfig.sectionStartMs)
        assertEquals(6000L, restored.loopConfig.sectionEndMs)
        assertEquals(1.25f, restored.loopConfig.playbackSpeed, 0.01f)
        assertEquals(AfterLoopAction.REPEAT_PLAYLIST, restored.loopConfig.afterCompletionAction)
    }

    @Test
    fun `AudioPlaybackEngine tracks C-to-F sub-selection and restores config on Recent play and open`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val songsAtoF = listOf("a", "b", "c", "d", "e", "f").map { label ->
            sampleSong(id = "song_$label", title = "Song $label", folderName = "PracticeFolder")
        }
        val otherSong = sampleSong(id = "song_other", title = "Other Track", folderName = "OtherFolder")
        val allSongs = songsAtoF + otherSong
        val repo = SongRepository(app)
        repo.saveSongs(allSongs)

        val engine = AudioPlaybackEngine.getInstance(app)
        // Allow AudioPlaybackEngine's initial async load on IO -> Main to finish
        val deadline = System.currentTimeMillis() + 3000L
        while (engine.uiState.value.songs.isEmpty() && System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(25L)
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val songsCtoF = songsAtoF.subList(2, 6)
        engine.playSelectedSongs(songs = songsCtoF, sourceFolderName = "PracticeFolder")
        engine.setRepeatCount(9)
        engine.setDelaySeconds(2)

        val folderSession = engine.uiState.value.recentSessions.firstOrNull { it.key == "folder:PracticeFolder" }
        assertNotNull(folderSession)
        assertTrue(folderSession!!.isSubSelection)
        assertEquals(listOf("song_c", "song_d", "song_e", "song_f"), folderSession.songIds)
        assertEquals(9, folderSession.loopConfig.repeatCount)
        assertEquals(2, folderSession.loopConfig.delaySeconds)

        engine.openFolderFromRecent("PracticeFolder")
        assertEquals(MainMenuTab.FOLDERS, engine.uiState.value.selectedTab)
        assertEquals("PracticeFolder", engine.uiState.value.expandedFolder)

        // Switch away to a standalone song and mutate loop config, then resume Recent folder session
        engine.selectSong(otherSong, autoPlay = false)
        engine.setRepeatCount(1)
        engine.setDelaySeconds(0)
        assertEquals(1, engine.uiState.value.loopConfig.repeatCount)

        engine.playRecentFolderSession("PracticeFolder")
        assertEquals(listOf("song_c", "song_d", "song_e", "song_f"), engine.uiState.value.customPlaybackQueue.map { it.id })
        assertEquals(9, engine.uiState.value.loopConfig.repeatCount)
        assertEquals(2, engine.uiState.value.loopConfig.delaySeconds)

        // Verify same behavior for a Playlist C-to-F sub-selection
        engine.createPlaylist("Hifz List", initialSongIds = songsAtoF.map { it.id })
        val createdPl = engine.uiState.value.playlists.first { it.name == "Hifz List" }
        engine.playSelectedSongs(
            songs = songsCtoF,
            sourcePlaylistId = createdPl.id,
            sourcePlaylistName = createdPl.name
        )
        engine.setRepeatCount(5)
        engine.setDelaySeconds(4)

        engine.selectSong(otherSong, autoPlay = false)
        engine.setRepeatCount(2)
        engine.setDelaySeconds(1)

        engine.playRecentPlaylistSession(createdPl.id)
        assertEquals(listOf("song_c", "song_d", "song_e", "song_f"), engine.uiState.value.customPlaybackQueue.map { it.id })
        assertEquals(5, engine.uiState.value.loopConfig.repeatCount)
        assertEquals(4, engine.uiState.value.loopConfig.delaySeconds)

        engine.openPlaylistFromRecent(createdPl.id)
        assertEquals(MainMenuTab.PLAYLISTS, engine.uiState.value.selectedTab)
        assertEquals(createdPl.id, engine.uiState.value.expandedPlaylistId)
    }
}
