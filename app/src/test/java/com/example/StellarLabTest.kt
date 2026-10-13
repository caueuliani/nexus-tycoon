package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StellarLabTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()

    @Test fun purchasesAreAtomicCappedAndDoNotReducePassiveBonus() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(nebulaCores = 1000, availableCores = 1000))
            val stale = repo.getGameStateDirect()!!
            val results = List(10) { async(Dispatchers.Default) { repo.buyStellarUpgrade(StellarUpgrade.INDUSTRIAL, 0) } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(995L, repo.getGameStateDirect()!!.availableCores)
            repo.saveGameState(stale.copy(companyName = "Profile update"))
            assertEquals(995L, repo.getGameStateDirect()!!.availableCores)
            for (level in 1..9) assertTrue(repo.buyStellarUpgrade(StellarUpgrade.INDUSTRIAL, level))
            assertFalse(repo.buyStellarUpgrade(StellarUpgrade.INDUSTRIAL, 10))
            val state = repo.getGameStateDirect()!!
            assertEquals(1000L, state.nebulaCores)
            assertEquals(405L, state.availableCores)
            assertEquals(repo.getPrestigeMinCash(1000), repo.getPrestigeMinCash(state.nebulaCores), 0.0)
            db.gameDao.saveGameState(state.copy(availableCores = 0))
            assertFalse(repo.buyStellarUpgrade(StellarUpgrade.OFFLINE, 0))
            assertEquals(0L, repo.getGameStateDirect()!!.availableCores)
        } finally { db.close() }
    }

    @Test fun grantsIncreaseBothCountersAndPrestigePreservesUpgrades() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(nebulaCores = 10, availableCores = 5, industrialEngineeringLevel = 1, cash = 400000.0))
            assertTrue(repo.performPrestige())
            assertEquals(11L, repo.getGameStateDirect()!!.nebulaCores)
            assertEquals(6L, repo.getGameStateDirect()!!.availableCores)
            assertEquals(1, repo.getGameStateDirect()!!.industrialEngineeringLevel)
            val results = List(10) { async(Dispatchers.Default) { repo.claimCoreEvent("Energy Supplier", true) } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(13L, repo.getGameStateDirect()!!.nebulaCores)
            assertEquals(8L, repo.getGameStateDirect()!!.availableCores)
            assertFalse(repo.claimCoreEvent("unknown", true))
            assertFalse(repo.claimCoreEvent("Celestial Capitalist", false))
        } finally { db.close() }
    }

    @Test fun displayedQuotesMatchSingleAndBulkDebitsAndOtherCurrenciesStayUnchanged() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            for (mode in listOf("x1", "x5", "x10", "MAX")) {
                db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(cash = 100000.0, nebulaCores = StellarUpgrade.INDUSTRIAL.costs.sum() + StellarUpgrade.TACTICAL.costs.sum(), availableCores = 0, industrialEngineeringLevel = 10, tacticalLogisticsLevel = 10))
                val state = repo.getGameStateDirect()!!
                val b = repo.getBuildingsDirect().first()
                val scientists = repo.getResearchersDirect()
                val base = EconomyCosts.buildingBase(b, scientists, state)
                assertEquals(b.baseCost * 0.85 * 0.8, base, 1e-9)
                val quote = calculateMultiUpgrade(base, b.costMultiplier, b.level, state.cash, mode)
                assertTrue(repo.upgradeBuilding(b.id, mode))
                assertEquals(state.cash - quote.second, repo.getGameStateDirect()!!.cash, 1e-7)
                assertEquals(b.level + quote.first, repo.getBuildingsDirect().first().level)
                assertTrue(repo.getGameStateDirect()!!.cash >= 0)
            }
            val before = repo.getGameStateDirect()!!.copy(cash = 100000.0)
            db.gameDao.saveGameState(before)
            val unit = repo.getCombatUnitsDirect().first()
            assertEquals(unit.upgradeCost * 0.8, EconomyCosts.combat(unit, before), 1e-9)
            assertTrue(repo.upgradeCombatUnit(unit.id))
            assertEquals(before.cash - EconomyCosts.combat(unit, before), repo.getGameStateDirect()!!.cash, 1e-9)
            assertEquals(before.starGems, repo.getGameStateDirect()!!.starGems)
            assertEquals(before.science, repo.getGameStateDirect()!!.science)
        } finally { db.close() }
    }

    @Test fun offlineReserveAddsHoursWithoutChangingRateOrPayingTwice() = runBlocking {
        for (vip in listOf(false, true)) {
            val db = memory()
            try {
                val repo = GameRepository(db); repo.checkAndInitialize()
                db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(isSubscribed = vip, nebulaCores = StellarUpgrade.OFFLINE.costs.sum(), availableCores = 0, offlineReserveLevel = 4, lastSavedTime = System.currentTimeMillis() - 24 * 3600000L))
                val report = repo.processOfflineDifference()!!
                val seconds = (if (vip) 16 else 12) * 3600L
                assertEquals(seconds, report.offlineSeconds)
                assertEquals(2.0 * seconds * (if (vip) 1.5 else 1.0) * (1.0 + StellarUpgrade.OFFLINE.costs.sum() * 0.10), report.resourcesEarned["Energy Cell"]!!, 1e-6)
                val inventory = repo.getResourcesDirect()
                assertNull(repo.processOfflineDifference())
                assertEquals(inventory, repo.getResourcesDirect())
            } finally { db.close() }
        }
    }

    @Test fun migrationInitializesBalanceOnceAndReopenPreservesSpending() = runBlocking {
        val name = "stellar-${java.util.UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, GameDatabase::class.java, name).build()
        try {
            var repo = GameRepository(db); repo.checkAndInitialize()
            db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(nebulaCores = 77, science = 123, companyName = "Existing"))
            val old = repo.exportBackup()
            db.close()
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { raw ->
                listOf("availableCores", "industrialEngineeringLevel", "tacticalLogisticsLevel", "offlineReserveLevel", "claimedCoreEvents").forEach { raw.execSQL("ALTER TABLE game_state DROP COLUMN `$it`") }
                raw.execSQL("DROP TABLE combat_battles")
                raw.execSQL("DROP TABLE play_purchase_receipts")
                raw.execSQL("ALTER TABLE game_state DROP COLUMN autoSellLicenseSource")
                raw.version = 9
            }
            fun reopen() = Room.databaseBuilder(context, GameDatabase::class.java, name).addMigrations(
                GameDatabase.MIGRATION_9_10, GameDatabase.MIGRATION_10_11,
                GameDatabase.MIGRATION_11_12, GameDatabase.MIGRATION_12_13).build()
            db = reopen(); repo = GameRepository(db)
            assertEquals(old.copy(state = old.state.copy(availableCores = 77)), repo.exportBackup())
            assertTrue(repo.buyStellarUpgrade(StellarUpgrade.OFFLINE, 0))
            db.close(); db = reopen(); repo = GameRepository(db); repo.checkAndInitialize()
            assertEquals(67L, repo.getGameStateDirect()!!.availableCores)
            assertEquals(1, repo.getGameStateDirect()!!.offlineReserveLevel)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun backupV3PreservesLabAndV2ConvertsWithoutAddingToCurrentBalance() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(nebulaCores = 200, availableCores = 200))
            StellarUpgrade.entries.forEach { assertTrue(repo.buyStellarUpgrade(it, 0)) }
            assertTrue(repo.claimCoreEvent("Energy Supplier", true))
            val snapshot = repo.exportBackup()
            val cloud = CloudSaveManager(context)
            val payload = cloud.createBackupPayload(snapshot)
            assertEquals(5, payload["backupVersion"])
            cloud.restoreFromPayload(repo, payload)
            assertEquals(snapshot.state.copy(lastSavedTime = repo.getGameStateDirect()!!.lastSavedTime), repo.getGameStateDirect())
            assertFalse(repo.claimCoreEvent("Energy Supplier", true))
            val invalid = payload.toMutableMap().apply { remove("availableCores") }
            try { cloud.restoreFromPayload(repo, invalid); fail("Incomplete v3 must fail") } catch (_: IllegalArgumentException) { }
            val legacy = payload.toMutableMap().apply {
                this["backupVersion"] = 2
                listOf("availableCores", "industrialEngineeringLevel", "tacticalLogisticsLevel", "offlineReserveLevel", "claimedCoreEvents").forEach { remove(it) }
            }
            repeat(2) {
                cloud.restoreFromPayload(repo, legacy)
                assertEquals(202L, repo.getGameStateDirect()!!.availableCores)
                assertEquals(0, repo.getGameStateDirect()!!.offlineReserveLevel)
            }
        } finally { db.close() }
    }
}
