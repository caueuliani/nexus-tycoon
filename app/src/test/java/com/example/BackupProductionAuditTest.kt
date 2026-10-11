package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Production acceptance tests: restoring on a fresh installation must preserve progression. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupProductionAuditTest {
    @Test(timeout = 20000)
    fun restoreCannotBeOverwrittenByProductionAlreadyInFlight() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val saved = repo.exportBackup()
            val restoredResources = saved.resources.map { it.copy(quantity = 12345.0) }
            val tickRead = kotlinx.coroutines.CompletableDeferred<Unit>()
            val finishTick = kotlinx.coroutines.CompletableDeferred<Unit>()
            val tick = launch {
                repo.runProductionTransaction {
                    val stale = repo.getResourcesDirect()
                    tickRead.complete(Unit)
                    finishTick.await()
                    repo.updateResources(stale.map { it.copy(quantity = it.quantity + 1) })
                }
            }
            tickRead.await()
            val restore = launch {
                repo.restoreBackup(saved.state, restoredResources, saved.buildings,
                    saved.combatUnits, saved.researchers, saved.missions)
            }
            // Let a non-isolated restore finish first to reproduce the stale-write race.
            kotlinx.coroutines.withTimeoutOrNull(250) { restore.join() }
            finishTick.complete(Unit)
            tick.join()
            restore.join()
            assertEquals(restoredResources, repo.getResourcesDirect())
        } finally { db.close() }
    }

    @Test
    fun backupRestoresDevelopedFactoriesAndInventoryOnFreshInstall() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val source = GameRepository(sourceDb)
            val target = GameRepository(targetDb)
            source.checkAndInitialize()
            target.checkAndInitialize()
            val saved = source.exportBackup()
            val factories = saved.buildings.map { it.copy(level = it.id * 3, isAutomated = true) }
            val inventory = saved.resources.mapIndexed { index, r -> r.copy(quantity = (index + 1) * 1234.0) }
            source.restoreBackup(saved.state.copy(companyName = "Restore test"), inventory,
                factories, saved.combatUnits, saved.researchers, saved.missions)
            val cloud = CloudSaveManager(context)
            cloud.setSandboxMode(true)
            cloud.handleGoogleSignInSuccess("factories@example.invalid")
            cloud.uploadBackup(source, {}, { fail(it) })
            cloud.downloadBackup(target, {}, { fail(it) })
            assertEquals(factories, target.getBuildingsDirect())
            assertEquals(inventory, target.getResourcesDirect())
            assertEquals("Restore test", target.getGameStateDirect()!!.companyName)
        } finally { sourceDb.close(); targetDb.close() }
    }

    @Test
    fun typedEmailCannotAuthenticateWithoutExplicitTestSandbox() = runBlocking {
        val cloud = CloudSaveManager(ApplicationProvider.getApplicationContext())
        assertFalse(cloud.isSandboxMode.value)
        try {
            cloud.handleGoogleSignInSuccess("attacker@example.invalid")
            fail("An email must not authenticate a production session")
        } catch (_: IllegalStateException) {
            assertNull(cloud.userEmail.value)
        }
    }

    @Test
    fun backupPreservesScienceUpgradesOnFreshInstallation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val source = GameRepository(sourceDb)
            val target = GameRepository(targetDb)
            source.checkAndInitialize()
            target.checkAndInitialize()
            source.saveGameState(source.getGameStateDirect()!!.copy(
                scienceProdLevel = 3, scienceCreditsLevel = 2, scienceComradeLevel = 4, scienceCombatLevel = 5
            ))
            val cloud = CloudSaveManager(context)
            cloud.setSandboxMode(true)
            cloud.handleGoogleSignInSuccess("audit@example.invalid")
            var uploaded = false
            cloud.uploadBackup(source, { uploaded = true }, { fail(it) })
            assertTrue("Backup must finish", uploaded)
            cloud.downloadBackup(target, {}, { fail(it) })
            assertEquals("Paid research must survive restore", 3, target.getGameStateDirect()!!.scienceProdLevel)
            assertEquals(2, target.getGameStateDirect()!!.scienceCreditsLevel)
            assertEquals(4, target.getGameStateDirect()!!.scienceComradeLevel)
            assertEquals(5, target.getGameStateDirect()!!.scienceCombatLevel)
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }

    @Test
    fun backupPreservesScientistCardsOnFreshInstallation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val source = GameRepository(sourceDb)
            val target = GameRepository(targetDb)
            source.checkAndInitialize()
            target.checkAndInitialize()
            val scientist = source.getResearchersDirect().first().copy(level = 4, cardsCollected = 23)
            source.updateResearcher(scientist)
            val mission = source.getActiveMissionsDirect().first().copy(isCompleted = true, isClaimed = true)
            source.updateActiveMission(mission)
            val cloud = CloudSaveManager(context)
            cloud.setSandboxMode(true)
            cloud.handleGoogleSignInSuccess("audit@example.invalid")
            cloud.uploadBackup(source, {}, { fail(it) })
            cloud.downloadBackup(target, {}, { fail(it) })
            assertEquals("Collected cards must survive restore", scientist, target.getResearchersDirect().first())
            assertEquals("Claimed missions must not become claimable again", mission, target.getActiveMissionsDirect().first())
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }

    @Test
    fun incompleteLegacyBackupDoesNotOverwriteLocalProgress() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val before = repo.exportBackup()
            try {
                CloudSaveManager(context).restoreFromPayload(repo, mapOf("cash" to 0.0))
                fail("Incomplete legacy backup must be rejected")
            } catch (_: IllegalArgumentException) {
                assertEquals(before, repo.exportBackup())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun malformedSnapshotDoesNotEraseCurrentGame() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val before = repo.exportBackup()
            try {
                repo.restoreBackup(before.state.copy(cash = 0.0), before.resources,
                    before.buildings.dropLast(1), before.combatUnits, before.researchers, before.missions)
                fail("Incomplete snapshot must be rejected")
            } catch (_: IllegalArgumentException) {
                assertEquals(before, repo.exportBackup())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun sandboxAccountsHaveSeparateBackups() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val cloud = CloudSaveManager(context)
            cloud.setSandboxMode(true)
            cloud.handleGoogleSignInSuccess("account-a@example.invalid")
            repo.saveGameState(repo.getGameStateDirect()!!.copy(cash = 999.0))
            cloud.uploadBackup(repo, {}, { fail(it) })
            repo.saveGameState(repo.getGameStateDirect()!!.copy(cash = 123.0))
            cloud.handleGoogleSignInSuccess("account-b@example.invalid")
            var missing = false
            cloud.downloadBackup(repo, { fail("Another account's backup must not be restored") }, { missing = true })
            assertTrue(missing)
            assertEquals(123.0, repo.getGameStateDirect()!!.cash, 0.0)
            cloud.handleGoogleSignInSuccess("account-a@example.invalid")
            cloud.downloadBackup(repo, {}, { fail(it) })
            assertEquals(999.0, repo.getGameStateDirect()!!.cash, 0.0)
        } finally {
            db.close()
        }
    }
}
