package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import com.example.player.MediaPlaybackService
import com.example.player.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MediaNotificationRobolectricTest {

    @Test
    fun `notificationContextLabel prioritizes named playlist or folder name`() {
        val song = LocalSong(
            id = "s1",
            title = "Surah Al-Fatiha",
            artist = "Reciter",
            album = "Quran",
            durationMs = 45000L,
            uriString = "content://media/external/audio/media/1",
            folderName = "Recitations"
        )

        val folderPrefixed = PlayerUiState(
            currentSong = song,
            currentPlaylistName = "Folder: Tajweed",
            currentFolderName = "Recitations"
        )
        assertEquals("Tajweed", folderPrefixed.notificationContextLabel)

        val namedPlaylist = PlayerUiState(
            currentSong = song,
            currentPlaylistName = "Morning Review",
            currentFolderName = "Recitations"
        )
        assertEquals("Morning Review", namedPlaylist.notificationContextLabel)

        val allSongsFallbackToFolder = PlayerUiState(
            currentSong = song,
            currentPlaylistName = "All Songs",
            currentFolderName = "Recitations"
        )
        assertEquals("Recitations", allSongsFallbackToFolder.notificationContextLabel)
    }

    @Test
    fun `isActivelyPlayingForNotification handles playing and delay states`() {
        assertTrue(PlayerUiState(phase = PlaybackPhase.PLAYING).isActivelyPlayingForNotification)
        assertTrue(PlayerUiState(phase = PlaybackPhase.WAITING_DELAY, isDelayPaused = false).isActivelyPlayingForNotification)
        assertFalse(PlayerUiState(phase = PlaybackPhase.WAITING_DELAY, isDelayPaused = true).isActivelyPlayingForNotification)
        assertFalse(PlayerUiState(phase = PlaybackPhase.PAUSED).isActivelyPlayingForNotification)
        assertFalse(PlayerUiState(phase = PlaybackPhase.COMPLETED_ALL_LOOPS).isActivelyPlayingForNotification)
    }

    @Test
    fun `MediaPlaybackService creates playback notification channel and handles foreground start`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val controller = Robolectric.buildService(MediaPlaybackService::class.java).create()
        val service = controller.get()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = nm.getNotificationChannel(MediaPlaybackService.CHANNEL_ID)
            assertNotNull(channel)
            assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        }

        controller.startCommand(0, 0)
        assertNotNull(service)
        controller.destroy()
    }

    @Test
    fun `MainActivity captures EXTRA_OPEN_STUDIO_PLAYER intent without crashing`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val intent = android.content.Intent(app, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_STUDIO_PLAYER, true)
        }
        val activity = Robolectric.buildActivity(MainActivity::class.java, intent).create().get()
        assertNotNull(activity)
        assertFalse(activity.intent.hasExtra(MainActivity.EXTRA_OPEN_STUDIO_PLAYER))
        shadowOf(activity).let { }
    }

    @Test
    fun `MediaPlaybackService omits progress seekbar metadata and ACTION_SEEK_TO while keeping 3 transport actions`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val song = LocalSong(
            id = "notif_song_1",
            title = "Surah Ar-Rahman",
            artist = "Qari",
            album = "Recitations",
            durationMs = 120_000L,
            uriString = "content://media/external/audio/media/999",
            folderName = "Memorize"
        )
        com.example.data.SongRepository(app).saveSongs(listOf(song))

        val engine = com.example.player.AudioPlaybackEngine.getInstance(app)
        val deadline = System.currentTimeMillis() + 3000L
        while (engine.uiState.value.songs.isEmpty() && System.currentTimeMillis() < deadline) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20L)
        }
        engine.playSelectedSongs(listOf(song), sourceFolderName = "Memorize")
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val controller = Robolectric.buildService(
            MediaPlaybackService::class.java,
            android.content.Intent(app, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_START_FOREGROUND
            }
        ).create().startCommand(0, 0)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val service = controller.get()
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = shadowOf(service).lastForegroundNotification
            ?: shadowOf(nm).getNotification(MediaPlaybackService.NOTIFICATION_ID)
            ?: shadowOf(nm).allNotifications.firstOrNull()
        assertNotNull(notification)

        val actualNotif = notification!!
        // Verify genuine MediaStyle media notification with no custom inner/outer RemoteViews card, no colorized/wave fallback effects, and 3 transport actions
        assertFalse(actualNotif.extras.getBoolean(android.app.Notification.EXTRA_COLORIZED, false))
        assertEquals(android.app.Notification.CATEGORY_TRANSPORT, actualNotif.category)
        assertEquals(
            "android.app.Notification\$MediaStyle",
            actualNotif.extras.getString(android.app.Notification.EXTRA_TEMPLATE)
        )
        assertEquals(null, actualNotif.contentView)
        assertEquals(null, actualNotif.bigContentView)
        assertEquals(null, actualNotif.extras.getCharSequence(android.app.Notification.EXTRA_SUB_TEXT))
        assertEquals(3, actualNotif.actions.size)
        assertEquals("Surah Ar-Rahman", actualNotif.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString())
        assertEquals("Memorize", actualNotif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString())
        assertEquals(0, actualNotif.extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, 0))
        assertEquals(0, actualNotif.extras.getInt(android.app.Notification.EXTRA_PROGRESS, 0))

        // Verify MediaSession stays active without progress seekbar metadata or ACTION_SEEK_TO
        val mediaController = service.mediaSessionControllerForTest()
        val playbackState = mediaController.playbackState
        assertNotNull(playbackState)
        assertEquals(
            0L,
            playbackState.actions and android.support.v4.media.session.PlaybackStateCompat.ACTION_SEEK_TO
        )
        assertEquals(
            android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            playbackState.position
        )
        assertEquals(0f, playbackState.playbackSpeed, 0.001f)

        val metadata = service.mediaMetadataForTest()
        assertNotNull(metadata)
        assertEquals(
            0L,
            metadata!!.getLong(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION)
        )
        assertEquals("Surah Ar-Rahman", metadata.getString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE))
        assertEquals("Memorize", metadata.getString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST))
        assertEquals(null, metadata.getBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
        assertEquals(null, actualNotif.getLargeIcon())

        controller.destroy()
    }

    @Test
    fun `hasEmbeddedArtworkHeaderTag returns false for headerless audio and true when APIC or covr present`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val controller = Robolectric.buildService(MediaPlaybackService::class.java).create()
        val service = controller.get()

        val plainFile = java.io.File(app.cacheDir, "plain_track.wav").apply {
            writeBytes("RIFF1234WAVEfmt plain audio without cover".toByteArray(Charsets.ISO_8859_1))
        }
        assertFalse(service.hasEmbeddedArtworkHeaderTag(android.net.Uri.fromFile(plainFile)))

        val taggedMp3 = java.io.File(app.cacheDir, "tagged_track.mp3").apply {
            writeBytes("ID3\u0003\u0000\u0000APICimage/jpegdummybytes".toByteArray(Charsets.ISO_8859_1))
        }
        assertTrue(service.hasEmbeddedArtworkHeaderTag(android.net.Uri.fromFile(taggedMp3)))

        controller.destroy()
    }

    @Test
    fun `MediaPlaybackService handles ACTION_START_FOREGROUND safely when currentSong is null`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val controller = Robolectric.buildService(
            MediaPlaybackService::class.java,
            android.content.Intent(app, MediaPlaybackService::class.java).apply {
                action = MediaPlaybackService.ACTION_START_FOREGROUND
            }
        ).create().startCommand(0, 0)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNotNull(controller.get())
        controller.destroy()
    }

    @Test
    fun `effectiveSegmentDurationMs handles negative duration without throwing`() {
        val cfg = com.example.model.LoopConfig(sectionStartMs = 1500L, sectionEndMs = 3000L)
        assertEquals(500L, cfg.effectiveSegmentDurationMs(-100L))
        assertEquals(500L, cfg.effectiveSegmentDurationMs(0L))
    }
}

