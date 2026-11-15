package com.example.player

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.model.EqualizerBand
import com.example.model.EqualizerState

internal class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun attach(
        audioSessionId: Int,
        equalizerState: EqualizerState
    ): List<EqualizerBand>? {
        release()
        if (audioSessionId <= 0) return null

        var updatedBands: List<EqualizerBand>? = null
        try {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = equalizerState.isEnabled
            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]

            val currentBands = equalizerState.bands
            val bandsList = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val bandIdx = i.toShort()
                val centerFreq = eq.getCenterFreq(bandIdx) / 1000
                val existing = currentBands.find { it.bandIndex == bandIdx }
                val targetLevel = existing?.levelMb ?: 0.toShort()
                try {
                    eq.setBandLevel(bandIdx, targetLevel)
                } catch (_: Exception) {
                }
                bandsList.add(
                    EqualizerBand(
                        bandIndex = bandIdx,
                        centerFreqHz = centerFreq,
                        levelMb = targetLevel,
                        minLevelMb = minLevel,
                        maxLevelMb = maxLevel
                    )
                )
            }
            equalizer = eq
            updatedBands = bandsList
        } catch (_: Exception) {
            equalizer = null
        }

        try {
            val le = LoudnessEnhancer(audioSessionId)
            le.enabled = true
            loudnessEnhancer = le
        } catch (_: Exception) {
            loudnessEnhancer = null
        }

        try {
            val bb = BassBoost(0, audioSessionId)
            bb.enabled = true
            bb.setStrength(equalizerState.bassBoostStrength.toShort())
            bassBoost = bb
        } catch (_: Exception) {
            bassBoost = null
        }

        try {
            val virt = Virtualizer(0, audioSessionId)
            virt.enabled = true
            virt.setStrength(equalizerState.virtualizerStrength.toShort())
            virtualizer = virt
        } catch (_: Exception) {
            virtualizer = null
        }

        return updatedBands
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        loudnessEnhancer = null

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (_: Exception) {
        }
        bassBoost = null

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (_: Exception) {
        }
        virtualizer = null
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {
        }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMb)
        } catch (_: Exception) {
        }
    }

    fun applyPreset(
        presetName: String,
        currentBands: List<EqualizerBand>
    ): List<EqualizerBand> {
        val gains = when (presetName) {
            "Bass Boost" -> listOf(600, 450, 0, -200, -300)
            "Vocal / Recitation" -> listOf(-300, 0, 500, 600, 300)
            "Treble Boost" -> listOf(-400, -200, 0, 450, 700)
            "Acoustic" -> listOf(350, 200, 0, 300, 400)
            "Speech" -> listOf(-500, 0, 600, 400, -200)
            else -> listOf(0, 0, 0, 0, 0)
        }
        return currentBands.mapIndexed { idx, band ->
            val level = (gains.getOrNull(idx) ?: 0).toShort()
            try {
                equalizer?.setBandLevel(band.bandIndex, level)
            } catch (_: Exception) {
            }
            band.copy(levelMb = level)
        }
    }

    fun setBassBoostStrength(strength: Int) {
        try {
            bassBoost?.setStrength(strength.toShort())
        } catch (_: Exception) {
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        try {
            virtualizer?.setStrength(strength.toShort())
        } catch (_: Exception) {
        }
    }

    fun applyVolumeAndBoost(
        mp: MediaPlayer?,
        isPlayerPrepared: Boolean,
        effectiveVolumePercent: Int,
        audioManager: AudioManager
    ) {
        if (mp == null || !isPlayerPrepared) return

        if (effectiveVolumePercent == 0) {
            try {
                mp.setVolume(0f, 0f)
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {
            }
            return
        }

        if (effectiveVolumePercent <= 100) {
            val maxVol = runCatching {
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            }.getOrDefault(15)
            val oneStepPercent = 100f / maxVol.toFloat()
            val playerScalar = if (effectiveVolumePercent < oneStepPercent) {
                (effectiveVolumePercent / oneStepPercent).coerceIn(0.08f, 1.0f)
            } else {
                1.0f
            }
            try {
                mp.setVolume(playerScalar, playerScalar)
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.setTargetGain(0)
            } catch (_: Exception) {
            }
        } else {
            val boostRatio = (effectiveVolumePercent - 100) / 100f
            val targetGainMb = (boostRatio * 2500f).toInt().coerceIn(0, 2500)
            try {
                mp.setVolume(1.0f, 1.0f)
                loudnessEnhancer?.enabled = true
                loudnessEnhancer?.setTargetGain(targetGainMb)
            } catch (_: Exception) {
            }
        }
    }
}
