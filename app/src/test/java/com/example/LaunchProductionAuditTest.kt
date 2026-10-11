package com.example

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LaunchProductionAuditTest {
    @Test(timeout = 60000)
    fun mainActivityCanBeCreatedAndResumedBeforeFirstFrame() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            // Rendering the continuously animated Compose scene requires a controlled frame clock.
            // This smoke test covers lifecycle creation, not visual correctness or navigation.
            controller.create().start().resume()
            assertFalse(controller.get().isFinishing)
        }
    }
}
