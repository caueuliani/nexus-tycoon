package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.buyAutoSellLicense
import com.example.data.buyAutoSellLicenseWithGems
import com.example.data.buySubscription
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PublicBetaConfigTest {
    @Test fun publicVariantHasNoCommercialConfigurationOrPermissions() {
        assertFalse(BuildConfig.DEBUG)
        assertTrue(BuildConfig.ITCH_BETA)
        assertEquals("com.aistudio.idletycoon.nxtyc", BuildConfig.APPLICATION_ID)
        assertTrue(BuildConfig.VERSION_NAME.endsWith("-itch-beta.1"))
        assertEquals("", BuildConfig.ADMOB_APP_ID)
        assertEquals("", BuildConfig.REWARDED_AD_UNIT_ID)
        assertEquals("", BuildConfig.PLAY_VIP_SUBSCRIPTION_ID)
        assertEquals("", BuildConfig.PLAY_AUTOSELL_PRODUCT_ID)
        assertEquals("", BuildConfig.PLAY_PURCHASE_VERIFICATION_URL)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissions = context.packageManager.getPackageInfo(context.packageName,
            PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty().toSet()
        assertFalse("Advertising ID", "com.google.android.gms.permission.AD_ID" in permissions)
        assertFalse("Billing", "com.android.vending.BILLING" in permissions)
        assertFalse("Phone state", "android.permission.READ_BASIC_PHONE_STATE" in permissions)
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assertEquals(0, info.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
        assertFalse(info.metaData?.containsKey("com.google.android.gms.ads.APPLICATION_ID") == true)
        assertFalse(info.metaData?.containsKey("com.google.android.play.billingclient.version") == true)
        for (name in listOf("com.android.billingclient.api.ProxyBillingActivity",
            "com.android.billingclient.api.ProxyBillingActivityV2",
            "com.google.android.libraries.ads.mobile.sdk.common.AdActivity")) {
            assertThrows(PackageManager.NameNotFoundException::class.java) {
                context.packageManager.getActivityInfo(android.content.ComponentName(context.packageName, name), 0)
            }
        }
    }

    @Test fun shopExchangesGameCurrencyWithoutRealPurchases() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(cash = 15000.0, starGems = 0))
            assertTrue(repo.exchangeCashForGems(3000.0, 100))
            assertTrue(repo.exchangeCashForGems(12000.0, 500))
            assertFalse(repo.exchangeCashForGems(3000.0, 100))
            assertEquals(0.0, repo.getGameStateDirect()!!.cash, 0.0)
            assertEquals(600L, repo.getGameStateDirect()!!.starGems)
            repo.buyAutoSellLicenseWithGems({}, { fail(it) })
            assertEquals(300L, repo.getGameStateDirect()!!.starGems)
            assertEquals("GEMS", repo.getGameStateDirect()!!.autoSellLicenseSource)
            repo.buyAutoSellLicenseWithGems({ fail("Duplicate purchase") }, {})
            assertEquals(300L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close() }
    }

    @Test fun commercialControllersCannotBeCreatedInBeta() {
        val activity = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).get()
        assertThrows(IllegalStateException::class.java) { activity.playBilling }
        assertThrows(IllegalStateException::class.java) { activity.rewardedAds }
    }

    @Test fun demoGrantsAreDisabledButGemLicenseRemainsAvailable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.buySubscription()
            repo.buyAutoSellLicense()
            assertFalse(repo.getGameStateDirect()!!.isSubscribed)
            assertFalse(repo.getGameStateDirect()!!.hasAutoSellLicense)
            repo.saveGameState(repo.getGameStateDirect()!!.copy(starGems = 300))
            var purchased = false
            repo.buyAutoSellLicenseWithGems({ purchased = true }, { fail(it) })
            assertTrue(purchased)
            assertTrue(repo.getGameStateDirect()!!.hasAutoSellLicense)
            assertEquals(0L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close() }
    }
}
