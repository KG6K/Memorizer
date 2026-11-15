package com.example.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal class SystemVolumeController(
    private val appContext: Context,
    val audioManager: AudioManager,
    private val scope: CoroutineScope,
    private val getCurrentVolumePercent: () -> Int,
    private val onVolumeStateSynced: (volumePercent: Int, isMuted: Boolean) -> Unit,
    private val onSetBoostVolumePercent: (Int) -> Unit
) {
    @Volatile
    private var internalSystemVolumeSyncUntilMs = 0L

    private val systemVolumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                onExternalSystemVolumeChanged()
            }
        }
    }

    private val systemVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            onExternalSystemVolumeChanged()
        }
    }

    fun register() {
        try {
            val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(systemVolumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(systemVolumeReceiver, filter)
            }
        } catch (_: Exception) {
        }

        try {
            appContext.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                systemVolumeObserver
            )
        } catch (_: Exception) {
        }
    }

    fun unregister() {
        runCatching { appContext.unregisterReceiver(systemVolumeReceiver) }
        runCatching { appContext.contentResolver.unregisterContentObserver(systemVolumeObserver) }
    }

    fun readSystemVolumePercent(): Int {
        return try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVol)
            ((curVol.toFloat() / maxVol.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } catch (_: Exception) {
            100
        }
    }

    fun syncVolumeToSystem(effectiveVol: Int) {
        try {
            internalSystemVolumeSyncUntilMs = System.currentTimeMillis() + 450L
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val targetSysVol = if (effectiveVol >= 100) {
                maxVol
            } else {
                ((effectiveVol.toFloat() / 100f) * maxVol.toFloat()).roundToInt().coerceIn(0, maxVol)
            }
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != targetSysVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetSysVol, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun onExternalSystemVolumeChanged() {
        if (System.currentTimeMillis() < internalSystemVolumeSyncUntilMs) return
        val sysPercent = readSystemVolumePercent()
        val currentPercent = getCurrentVolumePercent()
        if (sysPercent < 100 || currentPercent <= 100) {
            if (currentPercent != sysPercent) {
                onVolumeStateSynced(sysPercent, sysPercent == 0)
            }
        }
    }

    fun shouldInterceptVolumeKeyForBoost(isVolumeUp: Boolean): Boolean {
        val current = getCurrentVolumePercent()
        val sysPercent = readSystemVolumePercent()
        return if (isVolumeUp) {
            sysPercent >= 100 && current >= 100
        } else {
            current > 100
        }
    }

    fun syncFromSystemVolumeAfterHardwareKey() {
        scope.launch {
            delay(50L)
            val sysPercent = readSystemVolumePercent()
            if (getCurrentVolumePercent() != sysPercent) {
                onVolumeStateSynced(sysPercent, sysPercent == 0)
            }
        }
    }

    fun handleHardwareVolumeKey(isVolumeUp: Boolean): Boolean {
        val current = getCurrentVolumePercent()
        val maxVol = runCatching {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        }.getOrDefault(15)
        val stepPercent = (100f / maxVol.toFloat()).roundToInt().coerceAtLeast(5)

        return if (isVolumeUp) {
            if (current >= 100) {
                val next = (current + stepPercent).coerceAtMost(200)
                onSetBoostVolumePercent(next)
                runCatching {
                    internalSystemVolumeSyncUntilMs = System.currentTimeMillis() + 450L
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_SAME,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                true
            } else {
                try {
                    internalSystemVolumeSyncUntilMs = System.currentTimeMillis() + 450L
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (_: Exception) {
                }
                val newSys = readSystemVolumePercent()
                onVolumeStateSynced(newSys, newSys == 0)
                true
            }
        } else {
            if (current > 100) {
                val next = (current - stepPercent).coerceAtLeast(100)
                onSetBoostVolumePercent(next)
                runCatching {
                    internalSystemVolumeSyncUntilMs = System.currentTimeMillis() + 450L
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_SAME,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                true
            } else {
                try {
                    internalSystemVolumeSyncUntilMs = System.currentTimeMillis() + 450L
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (_: Exception) {
                }
                val newSys = readSystemVolumePercent()
                onVolumeStateSynced(newSys, newSys == 0)
                true
            }
        }
    }
}
