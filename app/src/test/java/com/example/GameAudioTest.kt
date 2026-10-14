package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundEvent
import com.example.audio.SoundEventRouter
import com.example.audio.SoundGate
import com.example.audio.SoundPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameAudioTest {
    @Test fun operationConfirmationOnlyAfterSuccess() {
        val played = mutableListOf<SoundEvent>()
        SoundEventRouter.confirmed(false, SoundEvent.CONFIRM, played::add)
        assertTrue(played.isEmpty())
        SoundEventRouter.confirmed(true, SoundEvent.CONFIRM, played::add)
        assertEquals(listOf(SoundEvent.CONFIRM), played)
    }

    @Test fun repeatedEventsAreBoundedAndBackgroundOrMuteBlocks() {
        val gate = SoundGate()
        assertFalse(gate.permits(SoundEvent.CONFIRM, 1000, false, true))
        assertFalse(gate.permits(SoundEvent.CONFIRM, 1000, true, false))
        assertTrue(gate.permits(SoundEvent.CONFIRM, 1000, true, true))
        assertFalse(gate.permits(SoundEvent.CONFIRM, 1000, true, true))
        assertFalse(gate.permits(SoundEvent.CONFIRM, 1119, true, true))
        assertTrue(gate.permits(SoundEvent.CONFIRM, 1120, true, true))
        assertTrue(gate.permits(SoundEvent.REVEAL_COMMON, 1200, true, true))
        assertFalse(gate.permits(SoundEvent.REVEAL_EPIC, 1300, true, true))
        assertTrue(gate.permits(SoundEvent.REVEAL_EPIC, 1450, true, true))
    }

    @Test fun soundSettingsSurviveRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = SoundPreferences(context)
        first.setEnabled(false)
        first.setVolume(63)
        val restored = SoundPreferences(context)
        assertFalse(restored.enabled())
        assertEquals(63, restored.volume())
        restored.setVolume(150)
        assertEquals(100, SoundPreferences(context).volume())
    }
}
