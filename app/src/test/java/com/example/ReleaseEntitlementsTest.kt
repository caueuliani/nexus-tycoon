package com.example

import com.example.data.GameState
import com.example.data.withoutUnverifiedReleaseEntitlements
import org.junit.Assert.*
import org.junit.Test

class ReleaseEntitlementsTest {
    @Test fun legacyAndDemoFlagsNeverGrantReleaseBenefits() {
        for (source in listOf("LEGACY", "DEMO", "NONE")) {
            val result = withoutUnverifiedReleaseEntitlements(
                GameState(isSubscribed = true, hasAutoSellLicense = true,
                    autoSellLicenseSource = source))
            assertFalse(result.isSubscribed)
            assertFalse(result.hasAutoSellLicense)
            assertEquals("NONE", result.autoSellLicenseSource)
        }
    }

    @Test fun backedUpPlayRightWaitsForReconciliationButGemRightPersists() {
        val play = GameState(hasAutoSellLicense = true, autoSellLicenseSource = "PLAY")
        assertTrue(withoutUnverifiedReleaseEntitlements(play).hasAutoSellLicense)
        assertFalse(withoutUnverifiedReleaseEntitlements(play, restoredFromBackup = true).hasAutoSellLicense)
        val gems = GameState(hasAutoSellLicense = true, autoSellLicenseSource = "GEMS")
        assertTrue(withoutUnverifiedReleaseEntitlements(gems, restoredFromBackup = true).hasAutoSellLicense)
    }
}
