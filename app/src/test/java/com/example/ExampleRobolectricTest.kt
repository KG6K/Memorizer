package com.example

import com.example.model.LoopConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `verify LoopConfig repetition and delay calculations`() {
        val fixedConfig = LoopConfig(
            repeatCount = 5,
            delaySeconds = 7,
            matchTrackDurationDelay = false
        )
        assertEquals(7000L, fixedConfig.calculateDelayMs(trackDurationMs = 15000L))

        val echoConfig = LoopConfig(
            repeatCount = 3,
            delaySeconds = 5,
            matchTrackDurationDelay = true,
            sectionStartMs = 2000L,
            sectionEndMs = 10000L,
            playbackSpeed = 1.0f
        )
        assertEquals(8000L, echoConfig.calculateDelayMs(trackDurationMs = 20000L))
    }
}
