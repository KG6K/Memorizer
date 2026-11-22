package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.example.model.AfterLoopAction
import com.example.model.LocalSong
import com.example.model.LoopConfig
import com.example.model.MainMenuTab
import com.example.model.PlaybackPhase
import com.example.model.Playlist
import com.example.model.RecentCollectionSession
import com.example.player.PlayerUiState
import com.example.ui.components.FoldersTabScreen
import com.example.ui.components.LastPlayingBottomBar
import com.example.ui.components.LibraryTabsPager
import com.example.ui.components.RecentTabScreen
import com.example.ui.components.StudioPlayerScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h915dp")
class UiPolishRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleSong(
        id: String,
        title: String,
        folderName: String = "010_Yunus"
    ) = LocalSong(
        id = id,
        title = title,
        artist = "Reciter",
        album = "Quran",
        durationMs = 15_000L,
        uriString = "content://media/external/audio/media/$id",
        addedAt = 1_000L,
        folderName = folderName
    )

    @Test
    fun `RecentTabScreen hides repeat and delay summary on both Recent Folder and Recent Playlist cards`() {
        val song1 = sampleSong("s1", "010001")
        val song2 = sampleSong("s2", "010002")
        val loopConfig = LoopConfig(repeatCount = 50, delaySeconds = 2)
        val summaryText = loopConfig.formatSummary()

        val playlist = Playlist(
            id = "pl_1",
            name = "Morning Hifz",
            songIds = listOf(song1.id, song2.id),
            createdAt = 500L
        )
        val folderSession = RecentCollectionSession(
            key = "folder:010_Yunus",
            isPlaylist = false,
            name = "010_Yunus",
            folderName = "010_Yunus",
            songIds = listOf(song1.id, song2.id),
            lastSongId = song1.id,
            loopConfig = loopConfig,
            isSubSelection = false,
            lastPlayedAt = 2_000L
        )
        val playlistSession = RecentCollectionSession(
            key = "playlist:pl_1",
            isPlaylist = true,
            name = "Morning Hifz",
            playlistId = "pl_1",
            songIds = listOf(song1.id, song2.id),
            lastSongId = song1.id,
            loopConfig = loopConfig,
            isSubSelection = false,
            lastPlayedAt = 1_500L
        )

        val state = PlayerUiState(
            songs = listOf(song1, song2),
            playlists = listOf(playlist),
            recentSessions = listOf(folderSession, playlistSession),
            currentSong = song1,
            phase = PlaybackPhase.PAUSED
        )

        composeTestRule.setContent {
            MaterialTheme {
                RecentTabScreen(
                    uiState = state,
                    onSelectSong = { _, _, _ -> },
                    onOpenFolder = {},
                    onPlayFolder = {},
                    onOpenPlaylist = {},
                    onPlayPlaylist = {},
                    onAddToPlaylistClick = {},
                    onOpenStudioPlayer = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("recent_folder_010_Yunus").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_playlist_pl_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("recent_song_card_s1").assertIsDisplayed()
        composeTestRule.onNodeWithText("010_Yunus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning Hifz").assertIsDisplayed()
        composeTestRule.onNodeWithText(summaryText, substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("50x Repeat", substring = true).assertDoesNotExist()
    }

    @Test
    fun `StudioPlayerScreen thumbnail vertical drag adjusts volume and aligns HUD inside thumbnail right edge`() {
        val song = sampleSong("s1", "010001")
        var volumePercent by mutableIntStateOf(100)
        var afterLoopAction by mutableStateOf(AfterLoopAction.NEXT_SONG)

        composeTestRule.setContent {
            MaterialTheme {
                StudioPlayerScreen(
                    uiState = PlayerUiState(
                        songs = listOf(song),
                        currentSong = song,
                        trackDurationMs = 15_000L,
                        currentPositionMs = 3_000L,
                        volumePercent = volumePercent,
                        loopConfig = LoopConfig(
                            repeatCount = 5,
                            delaySeconds = 2,
                            afterCompletionAction = afterLoopAction
                        )
                    ),
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSkipBackward = {},
                    onSkipForward = {},
                    onPreviousSong = {},
                    onNextSong = {},
                    onResetLoopProgress = {},
                    onSkipDelayNow = {},
                    onAddExtraDelay = {},
                    onSetRepeatCount = {},
                    onIncrementRepeatCount = {},
                    onSetDelaySeconds = {},
                    onIncrementDelaySeconds = {},
                    onSetMatchTrackDuration = {},
                    onSetPlaybackSpeed = {},
                    onSetAfterLoopAction = { afterLoopAction = it },
                    onMarkSectionStart = {},
                    onMarkSectionEnd = {},
                    onClearAbSection = {},
                    onSaveConfigToSong = {},
                    onPickAudioFiles = {},
                    onOpenLibraryTab = {},
                    onSetVolumePercent = { volumePercent = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify all 3 borderless looper buttons are present and interactive
        composeTestRule.onNodeWithTag("looper_repeat_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("looper_delay_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folder_loop_mode_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folder_loop_mode_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(AfterLoopAction.REPEAT_PLAYLIST, afterLoopAction)

        // Start a vertical swipe up on the thumbnail and hold down to inspect HUD alignment
        composeTestRule.onNodeWithTag("hero_now_playing_card").performTouchInput {
            down(center)
            moveBy(Offset(0f, -140f))
        }
        composeTestRule.waitForIdle()

        val peakVolume = volumePercent
        val thumbBounds = composeTestRule.onNodeWithTag("hero_now_playing_card").getUnclippedBoundsInRoot()
        val hudNode = composeTestRule.onNodeWithTag("vlc_volume_hud_overlay")
        hudNode.assertIsDisplayed()
        val hudBounds = hudNode.getUnclippedBoundsInRoot()

        // Drag down to verify decreasing volume works and release pointer cleanly
        composeTestRule.onNodeWithTag("hero_now_playing_card").performTouchInput {
            moveBy(Offset(0f, 220f))
            up()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()

        assertTrue("Swiping up on thumbnail should increase volume (was $peakVolume)", peakVolume > 100)
        assertTrue("Swiping down on thumbnail should decrease volume (peak=$peakVolume, now=$volumePercent)", volumePercent < peakVolume)
        assertTrue("HUD top (${hudBounds.top}) should be inside thumbnail top (${thumbBounds.top})", hudBounds.top >= thumbBounds.top)
        assertTrue("HUD bottom (${hudBounds.bottom}) should be inside thumbnail bottom (${thumbBounds.bottom})", hudBounds.bottom <= thumbBounds.bottom)
        assertTrue("HUD right (${hudBounds.right}) should be inside thumbnail right (${thumbBounds.right})", hudBounds.right <= thumbBounds.right)
        val hudCenterX = (hudBounds.left + hudBounds.right) / 2
        val thumbCenterX = (thumbBounds.left + thumbBounds.right) / 2
        assertTrue(
            "HUD center ($hudCenterX) should sit on the right half of the thumbnail (thumbCenterX=$thumbCenterX)",
            hudCenterX > thumbCenterX
        )
    }

    @Test
    fun `FoldersTabScreen fast-scroll slider scrubs large song list and shows floating preview bubble`() {
        val manySongs = (1..60).map { idx ->
            sampleSong(
                id = "s_$idx",
                title = "002${idx.toString().padStart(3, '0')}",
                folderName = "Suratulbakara"
            )
        }
        val state = PlayerUiState(
            songs = manySongs,
            expandedFolder = "Suratulbakara",
            currentSong = manySongs.first(),
            phase = PlaybackPhase.PAUSED
        )

        composeTestRule.setContent {
            MaterialTheme {
                FoldersTabScreen(
                    uiState = state,
                    onSelectSong = { _, _, _ -> },
                    onPlayFolder = {},
                    onToggleExpandFolder = {},
                    onAddToPlaylistClick = {},
                    onOpenStudioPlayer = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Fast-scroll track exists for long scrollable list and preview bubble is initially hidden
        composeTestRule.onNodeWithTag("folders_fast_scroll_track").assertExists()
        composeTestRule.onNodeWithTag("folders_fast_scroll_bubble").assertDoesNotExist()

        // Drag down along the right-edge fast-scroll track to scrub near the end of the 60-song folder
        composeTestRule.onNodeWithTag("folders_fast_scroll_track").performTouchInput {
            down(Offset(centerX, top + 20f))
            moveTo(Offset(centerX, bottom - 20f))
        }
        composeTestRule.waitForIdle()

        // Floating preview bubble appears during drag and late songs are scrolled into view
        composeTestRule.onNodeWithTag("folders_fast_scroll_bubble").assertIsDisplayed()
        composeTestRule.onNodeWithTag("folder_song_s_60").assertIsDisplayed()

        // Release drag and let auto-hide settle cleanly
        composeTestRule.onNodeWithTag("folders_fast_scroll_track").performTouchInput {
            up()
        }
        composeTestRule.mainClock.advanceTimeBy(1_600L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("folders_fast_scroll_bubble").assertDoesNotExist()
    }

    @Test
    fun `LastPlayingBottomBar renders cleanly with flat edges and active controls`() {
        val song = sampleSong("s1", "036001")
        var openedPlayer = false

        composeTestRule.setContent {
            MaterialTheme {
                LastPlayingBottomBar(
                    uiState = PlayerUiState(
                        songs = listOf(song),
                        currentSong = song,
                        trackDurationMs = 15_000L,
                        currentPositionMs = 5_000L,
                        phase = PlaybackPhase.PAUSED
                    ),
                    onTogglePlayPause = {},
                    onNextSong = {},
                    onOpenStudioPlayer = { openedPlayer = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("last_playing_bottom_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("036001").assertIsDisplayed()
        composeTestRule.onNodeWithTag("last_playing_bottom_bar").performClick()
        composeTestRule.waitForIdle()
        assertTrue(openedPlayer)
    }

    @Test
    fun `LibraryTabsPager swipes smoothly left and right across all 4 tabs and stays synced with tab pills`() {
        val songs = listOf(
            sampleSong("s1", "001001", folderName = "001_Fatiha"),
            sampleSong("s2", "002001", folderName = "002_Baqara")
        )
        val playlist = Playlist(
            id = "pl_hifz",
            name = "Daily Hifz",
            songIds = listOf("s1", "s2"),
            createdAt = 1_000L
        )
        var selectedTab by mutableStateOf(MainMenuTab.RECENT)

        composeTestRule.setContent {
            MaterialTheme {
                LibraryTabsPager(
                    uiState = PlayerUiState(
                        selectedTab = selectedTab,
                        songs = songs,
                        playlists = listOf(playlist),
                        currentSong = songs.first(),
                        phase = PlaybackPhase.PAUSED
                    ),
                    onSelectTab = { selectedTab = it },
                    onSelectSong = { _, _, _ -> },
                    onOpenFolderFromRecent = { selectedTab = MainMenuTab.FOLDERS },
                    onPlayRecentFolderSession = {},
                    onOpenPlaylistFromRecent = { selectedTab = MainMenuTab.PLAYLISTS },
                    onPlayRecentPlaylistSession = {},
                    onPlayFolder = {},
                    onToggleExpandFolder = {},
                    onPlaySelectedFolderSongs = { _, _ -> },
                    onPlaySelectedAlbumSongs = {},
                    onPlayPlaylist = {},
                    onCreatePlaylistClick = {},
                    onAddSongsToPlaylist = { _, _ -> },
                    onRemoveSongFromPlaylist = { _, _ -> },
                    onDeletePlaylist = {},
                    onToggleExpandPlaylist = {},
                    onPlaySelectedPlaylistSongs = { _, _, _ -> },
                    onAddToPlaylistClick = {},
                    onOpenStudioPlayer = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Starts on Recent
        composeTestRule.onNodeWithTag("main_menu_tab_recent").assertIsSelected()
        composeTestRule.onNodeWithTag("recent_tab_screen").assertIsDisplayed()

        // Swipe left -> Folders
        composeTestRule.onNodeWithTag("library_horizontal_pager").performTouchInput {
            swipeLeft(startX = right - 24f, endX = left + 24f)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainMenuTab.FOLDERS, selectedTab)
        composeTestRule.onNodeWithTag("main_menu_tab_folders").assertIsSelected()
        composeTestRule.onNodeWithTag("folders_tab_screen").assertIsDisplayed()

        // Swipe left -> Albums
        composeTestRule.onNodeWithTag("library_horizontal_pager").performTouchInput {
            swipeLeft(startX = right - 24f, endX = left + 24f)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainMenuTab.ALBUMS_SINGERS, selectedTab)
        composeTestRule.onNodeWithTag("main_menu_tab_albums_singers").assertIsSelected()

        // Swipe left -> Playlists
        composeTestRule.onNodeWithTag("library_horizontal_pager").performTouchInput {
            swipeLeft(startX = right - 24f, endX = left + 24f)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainMenuTab.PLAYLISTS, selectedTab)
        composeTestRule.onNodeWithTag("main_menu_tab_playlists").assertIsSelected()

        // Swipe right -> Albums
        composeTestRule.onNodeWithTag("library_horizontal_pager").performTouchInput {
            swipeRight(startX = left + 24f, endX = right - 24f)
        }
        composeTestRule.waitForIdle()
        assertEquals(MainMenuTab.ALBUMS_SINGERS, selectedTab)
        composeTestRule.onNodeWithTag("main_menu_tab_albums_singers").assertIsSelected()

        // Tap top Recent pill -> animates smoothly back to Recent without intermediate glitch
        composeTestRule.onNodeWithTag("main_menu_tab_recent").performClick()
        composeTestRule.waitForIdle()
        assertEquals(MainMenuTab.RECENT, selectedTab)
        composeTestRule.onNodeWithTag("main_menu_tab_recent").assertIsSelected()
        composeTestRule.onNodeWithTag("recent_tab_screen").assertIsDisplayed()
    }
}
