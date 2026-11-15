package com.example.player

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.LruCache
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R
import com.example.model.LocalSong
import com.example.model.PlaybackPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "memorizer_media_playback"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_FOREGROUND = "com.example.action.START_FOREGROUND_PLAYBACK"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_DISMISS = "com.example.action.DISMISS"

        fun startForegroundPlayback(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_START_FOREGROUND
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }
    }

    private data class NotificationRenderKey(
        val songId: String?,
        val title: String?,
        val artist: String?,
        val contextLabel: String,
        val uriString: String?,
        val durationMs: Long,
        val isPlaying: Boolean,
        val phase: PlaybackPhase,
        val speed: Float,
        val notificationVisible: Boolean,
        val syncNonce: Long
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: AudioPlaybackEngine
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManagerCompat

    private val artworkCache = LruCache<String, Bitmap>(4)
    private var artworkLoadJob: Job? = null
    private var activeArtworkUri: String? = null
    private var activeArtworkBitmap: Bitmap? = null
    private var lastPublishedMetadata: MediaMetadataCompat? = null
    private var isInForeground = false
    private var noisyReceiverRegistered = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                engine.pauseFromNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = AudioPlaybackEngine.getInstance(applicationContext)
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
        initMediaSession()
        registerNoisyReceiver()
        observePlaybackState()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MemorizerMediaSession").apply {
            setSessionActivity(buildOpenStudioPendingIntent())
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    engine.playOrResumeFromNotification()
                }

                override fun onPause() {
                    engine.pauseFromNotification()
                }

                override fun onSkipToPrevious() {
                    engine.playPreviousSong()
                }

                override fun onSkipToNext() {
                    engine.playNextSong()
                }

                override fun onSeekTo(pos: Long) {
                    engine.seekTo(pos)
                }

                override fun onStop() {
                    engine.dismissNotificationAndPause()
                    stopNotificationAndService()
                }
            })
            isActive = true
        }
    }

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(becomingNoisyReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(becomingNoisyReceiver, filter)
            }
            noisyReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            engine.uiState
                .map { state ->
                    NotificationRenderKey(
                        songId = state.currentSong?.id,
                        title = state.currentSong?.title,
                        artist = state.currentSong?.artist,
                        contextLabel = state.notificationContextLabel,
                        uriString = state.currentSong?.uriString,
                        durationMs = state.trackDurationMs,
                        isPlaying = state.isActivelyPlayingForNotification,
                        phase = state.phase,
                        speed = state.loopConfig.playbackSpeed,
                        notificationVisible = state.notificationSessionVisible,
                        syncNonce = state.playbackStateSyncNonce
                    )
                }
                .distinctUntilChanged()
                .collect {
                    val currentState = engine.uiState.value
                    renderNotificationForState(currentState, forceStartForegroundFirst = false)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                renderNotificationForState(engine.uiState.value, forceStartForegroundFirst = true)
            }
            ACTION_PLAY_PAUSE -> {
                engine.togglePlayPause()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInForeground) {
                    renderNotificationForState(engine.uiState.value, forceStartForegroundFirst = true)
                }
            }
            ACTION_PREVIOUS -> {
                engine.playPreviousSong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInForeground) {
                    renderNotificationForState(engine.uiState.value, forceStartForegroundFirst = true)
                }
            }
            ACTION_NEXT -> {
                engine.playNextSong()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInForeground) {
                    renderNotificationForState(engine.uiState.value, forceStartForegroundFirst = true)
                }
            }
            ACTION_DISMISS -> {
                engine.dismissNotificationAndPause()
                stopNotificationAndService()
            }
            else -> {
                renderNotificationForState(engine.uiState.value, forceStartForegroundFirst = false)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        engine.stopAndReleaseForTaskRemoved()
        stopNotificationAndService()
    }

    override fun onDestroy() {
        artworkLoadJob?.cancel()
        if (noisyReceiverRegistered) {
            runCatching { unregisterReceiver(becomingNoisyReceiver) }
            noisyReceiverRegistered = false
        }
        mediaSession.isActive = false
        mediaSession.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @androidx.annotation.VisibleForTesting
    internal fun mediaSessionControllerForTest(): android.support.v4.media.session.MediaControllerCompat =
        mediaSession.controller

    @androidx.annotation.VisibleForTesting
    internal fun mediaMetadataForTest(): MediaMetadataCompat? = lastPublishedMetadata

    private fun renderNotificationForState(
        state: PlayerUiState,
        forceStartForegroundFirst: Boolean
    ) {
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

        val song = state.currentSong
        if (song == null || (!state.notificationSessionVisible && !forceStartForegroundFirst)) {
            if (forceStartForegroundFirst && !isInForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val placeholder = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_music)
                    .setContentTitle(getString(R.string.app_name))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setSilent(true)
                    .build()
                runCatching {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, placeholder, fgType)
                    isInForeground = true
                }
            }
            if (isInForeground || !state.notificationSessionVisible) {
                stopNotificationAndService()
            }
            return
        }

        ensureArtworkLoaded(song)
        updateMediaSessionStateAndMetadata(state, song)
        val notification = buildMediaNotification(state, song)

        if (state.isActivelyPlayingForNotification) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
            isInForeground = true
        } else if (forceStartForegroundFirst) {
            // Satisfy startForegroundService() contract even if playback paused before onStartCommand ran
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
            isInForeground = true
            if (!state.notificationSessionVisible) {
                stopNotificationAndService()
            } else {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                isInForeground = false
                postNotificationIfPermitted(notification)
            }
        } else {
            if (isInForeground) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                isInForeground = false
            }
            postNotificationIfPermitted(notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotificationIfPermitted(notification: Notification) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
        }
    }

    private fun stopNotificationAndService() {
        if (isInForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            isInForeground = false
        }
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
        stopSelf()
    }

    private fun updateMediaSessionStateAndMetadata(state: PlayerUiState, song: LocalSong) {
        val isPlaying = state.isActivelyPlayingForNotification
        val playbackStateCode = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_STOP

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    playbackStateCode,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    0f
                )
                .build()
        )

        val contextLabel = state.notificationContextLabel
        val embeddedArtwork = currentEmbeddedArtworkFor(song)

        val publishedMetadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, contextLabel)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, contextLabel)
            .apply {
                if (embeddedArtwork != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, embeddedArtwork)
                }
            }
            .build()
        lastPublishedMetadata = publishedMetadata
        mediaSession.setMetadata(publishedMetadata)
    }

    private fun buildMediaNotification(state: PlayerUiState, song: LocalSong): Notification {
        val isPlaying = state.isActivelyPlayingForNotification
        val contextLabel = state.notificationContextLabel
        val embeddedArtwork = currentEmbeddedArtworkFor(song)

        val prevPendingIntent = buildServiceActionPendingIntent(ACTION_PREVIOUS, 101)
        val playPausePendingIntent = buildServiceActionPendingIntent(ACTION_PLAY_PAUSE, 102)
        val nextPendingIntent = buildServiceActionPendingIntent(ACTION_NEXT, 103)
        val dismissPendingIntent = buildServiceActionPendingIntent(ACTION_DISMISS, 104)

        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseTitle = getString(
            if (isPlaying) R.string.notification_action_pause else R.string.notification_action_play
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(song.title)
            .setContentText(contextLabel)
            .setLargeIcon(embeddedArtwork)
            .setContentIntent(buildOpenStudioPendingIntent())
            .setDeleteIntent(dismissPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColorized(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(R.drawable.ic_notif_prev, getString(R.string.notification_action_previous), prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(R.drawable.ic_notif_next, getString(R.string.notification_action_next), nextPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun buildOpenStudioPendingIntent(): PendingIntent {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_STUDIO_PLAYER, true)
        }
        return PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildServiceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action != ACTION_DISMISS) {
            PendingIntent.getForegroundService(this, requestCode, intent, flags)
        } else {
            PendingIntent.getService(this, requestCode, intent, flags)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun currentEmbeddedArtworkFor(song: LocalSong): Bitmap? {
        return artworkCache.get(song.uriString)
            ?: (if (activeArtworkUri == song.uriString) activeArtworkBitmap else null)
    }

    private fun ensureArtworkLoaded(song: LocalSong) {
        val uriKey = song.uriString
        if (artworkCache.get(uriKey) != null || activeArtworkUri == uriKey) return
        activeArtworkUri = uriKey
        activeArtworkBitmap = null
        artworkLoadJob?.cancel()
        if (uriKey.isBlank() || song.isBuiltInSample || uriKey.endsWith(".wav", ignoreCase = true)) {
            return
        }
        artworkLoadJob = serviceScope.launch {
            val loaded = withContext(Dispatchers.IO) { decodeSongArtwork(uriKey) }
            if (loaded != null) {
                artworkCache.put(uriKey, loaded)
                if (activeArtworkUri == uriKey) {
                    activeArtworkBitmap = loaded
                    val state = engine.uiState.value
                    if (state.currentSong?.uriString == uriKey && state.notificationSessionVisible) {
                        renderNotificationForState(state, forceStartForegroundFirst = false)
                    }
                }
            }
        }
    }

    private fun decodeSongArtwork(uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)

        if (uri.scheme == "content" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thumb = runCatching {
                contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
            }.getOrNull()
            if (thumb != null) {
                return cropToCircleBitmap(thumb)
            }
        }

        val hasEmbeddedPictureTag = hasEmbeddedArtworkHeaderTag(uri)
        if (!hasEmbeddedPictureTag) {
            return null
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val decoded = decodeSampledBitmap(bytes, maxDimension = 512) ?: return null
            cropToCircleBitmap(decoded)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    internal fun hasEmbeddedArtworkHeaderTag(uri: Uri): Boolean {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(65536)
                val bytesRead = stream.read(buffer)
                if (bytesRead <= 0) {
                    false
                } else {
                    val headerText = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
                    headerText.contains("APIC") ||
                        headerText.contains("covr") ||
                        headerText.contains("METADATA_BLOCK_PICTURE") ||
                        headerText.contains("image/jpeg") ||
                        headerText.contains("image/png") ||
                        headerText.contains("image/webp")
                }
            } ?: false
        }.getOrDefault(false)
    }

    private fun cropToCircleBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height).coerceAtLeast(1)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f

        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcLeft = ((source.width - size) / 2).coerceAtLeast(0)
        val srcTop = ((source.height - size) / 2).coerceAtLeast(0)
        val srcRect = Rect(srcLeft, srcTop, srcLeft + size, srcTop + size)
        val dstRect = Rect(0, 0, size, size)
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return output
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
