package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDatabase
import com.example.data.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayEntitlementTest {
    @Test fun repeatedVerifiedVipTokenGrantsStarterGiftOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val tokenHash = "a".repeat(64)
            val results = List(10) { async(Dispatchers.Default) {
                repo.applyVerifiedPlayPurchase(tokenHash, "configured.vip", true, true)
            } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(1000L, repo.getGameStateDirect()!!.starGems)
            assertTrue(repo.getGameStateDirect()!!.isSubscribed)
            repo.revokeVipWhenAbsent()
            assertFalse(repo.getGameStateDirect()!!.isSubscribed)
            repo.applyVerifiedPlayPurchase(tokenHash, "configured.vip", true, true)
            assertTrue(repo.getGameStateDirect()!!.isSubscribed)
            assertEquals(1000L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close() }
    }

    @Test fun verifiedLicenseIsIdempotentAndDoesNotGrantVip() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val tokenHash = "b".repeat(64)
            assertTrue(repo.applyVerifiedPlayPurchase(tokenHash, "configured.license", false, false))
            assertFalse(repo.applyVerifiedPlayPurchase(tokenHash, "configured.license", false, false))
            assertTrue(repo.getGameStateDirect()!!.hasAutoSellLicense)
            assertEquals("PLAY", repo.getGameStateDirect()!!.autoSellLicenseSource)
            assertFalse(repo.getGameStateDirect()!!.isSubscribed)
            assertEquals(0L, repo.getGameStateDirect()!!.starGems)
            repo.revokePlayLicenseWhenAbsent()
            assertFalse(repo.getGameStateDirect()!!.hasAutoSellLicense)
            assertEquals("NONE", repo.getGameStateDirect()!!.autoSellLicenseSource)
        } finally { db.close() }
    }
}
