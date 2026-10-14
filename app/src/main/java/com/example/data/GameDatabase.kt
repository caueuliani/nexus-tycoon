package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.pow

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "Nova Core Inc",
    val cash: Double = 5000.0,
    val starGems: Long = 0,
    val nebulaCores: Long = 0,
    @ColumnInfo(defaultValue = "0") val availableCores: Long = 0,
    @ColumnInfo(defaultValue = "0") val industrialEngineeringLevel: Int = 0,
    @ColumnInfo(defaultValue = "0") val tacticalLogisticsLevel: Int = 0,
    @ColumnInfo(defaultValue = "0") val offlineReserveLevel: Int = 0,
    @ColumnInfo(defaultValue = "''") val claimedCoreEvents: String = "",

    val guildTokens: Long = 0,
    val guildName: String = "No Guild Joined",
    val selectedSkinId: Int = 0, // 0: Industrial Blue, 1: Solar Flare Gold, 2: Cyber Purple Neon
    val selectedAvatarId: Int = 0, // 0: Engineer, 1: Commander, 2: Cyborg
    val lastSavedTime: Long = System.currentTimeMillis(),
    val isSubscribed: Boolean = false,
    val highestCombatStage: Int = 1,
    val hasAutoSellLicense: Boolean = false,
    @ColumnInfo(defaultValue = "'NONE'") val autoSellLicenseSource: String = "NONE",
    val stage1CompletedCount: Int = 0,
    val stage2CompletedCount: Int = 0,
    val stage3CompletedCount: Int = 0,
    val stage4CompletedCount: Int = 0,
    val hasCompletedTutorial: Boolean = false,
    val comrades: Double = 0.0,
    val comradesPerSec: Double = 1.0,
    val playerRank: Int = 1,
    val completedRankMissions: Int = 0,
    val science: Long = 0,
    val scienceProdLevel: Int = 0,
    val scienceCreditsLevel: Int = 0,
    val scienceComradeLevel: Int = 0,
    val scienceCombatLevel: Int = 0
)

@Entity(tableName = "researchers")
data class ResearcherCard(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val level: Int = 0, // 0 = Locked, 1+ = Upgraded
    val cardsCollected: Int = 0,
    val cardsNeededToUpgrade: Int = 5,
    val targetBuildingId: Int, // 1 to 8, or 0 for global
    val boostType: String, // "PRODUCTION" (e.g. x2 production rate), "SPEED" (+50% production speed), "COST_REDUCTION" (-20% upgrade price), "COMRADE_GEN" (+100% Comrade production speed)
    val rarity: String // "COMMON", "RARE", "EPIC", "SUPREME"
) {
    fun getBoostValue(): Double {
        if (level <= 0) return 0.0
        return when (boostType) {
            "PRODUCTION" -> 2.0 * level
            "SPEED" -> 0.5 * level
            "COST_REDUCTION" -> 0.15 * level
            "COMRADE_GEN" -> 1.0 * level
            else -> 1.0
        }
    }

    fun getUpgradeScienceCost(): Long {
        return (100 * 2.0.pow(level.coerceAtLeast(1) - 1)).toLong()
    }
}

@Entity(tableName = "active_missions")
data class ActiveMission(
    @PrimaryKey val id: Int,
    val description: String,
    val progress: Double,
    val target: Double,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false,
    val missionType: String, // "OWN_BUILDING", "COLLECT_RESOURCE", "COMBAT_WIN", "EARN_CASH"
    val targetId: Int = 0,
    val targetString: String = "",
    val rewardGems: Long,
    val rewardScience: Long,
    val rewardCapsuleType: String // "COMUM" (Wooden), "EPICA" (Steel), "SUPREMA" (Zeus)
)

@Entity(tableName = "resources")
data class ResourceInventory(
    @PrimaryKey val name: String, // "Iron Ore", "Energy Cell", "Quantum Chip", "Hyperalloy"
    val quantity: Double = 0.0
)

@Entity(tableName = "buildings")
data class BusinessBuilding(
    @PrimaryKey val id: Int,
    val name: String,
    val level: Int = 1,
    val isAutomated: Boolean = false,
    val baseCost: Double,
    val costMultiplier: Double = 1.15,
    val productionRatePerLevel: Double,
    val resourceProduced: String,
    val inputResource: String? = null,
    val inputAmountPerSec: Double = 0.0,
    val isAutoSelling: Boolean = false,
    @ColumnInfo(defaultValue = "0") val cycleStartedAtMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val cycleEndsAtMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val cycleOutput: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val cycleSalePrice: Double = 0.0
) {
    fun getMilestoneMultiplier(): Double {
        if (level <= 0) return 1.0
        var mult = 1.0
        if (level >= 10) mult *= 2.0
        if (level >= 25) mult *= 2.0
        if (level >= 50) mult *= 2.5
        if (level >= 100) mult *= 4.0
        if (level >= 250) mult *= 5.0
        if (level >= 500) mult *= 10.0
        if (level >= 1000) mult *= 20.0
        return mult
    }

    fun getNextMilestone(): Int {
        return when {
            level < 10 -> 10
            level < 25 -> 25
            level < 50 -> 50
            level < 100 -> 100
            level < 250 -> 250
            level < 500 -> 500
            level < 1000 -> 1000
            else -> ((level / 500) + 1) * 500
        }
    }

    fun getNextMilestoneMultiplierBoost(): Double {
        return when {
            level < 10 -> 2.0
            level < 25 -> 2.0
            level < 50 -> 2.5
            level < 100 -> 4.0
            level < 250 -> 5.0
            level < 500 -> 10.0
            level < 1000 -> 20.0
            else -> 2.0
        }
    }

    fun getActualProductionRate(): Double {
        return productionRatePerLevel * level * getMilestoneMultiplier()
    }
}

@Entity(tableName = "combat_units")
data class CombatUnit(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String, // "Vanguard", "Shield", "Striker", "Interceptor"
    val level: Int = 1,
    val health: Int,
    val attack: Int,
    val upgradeCost: Double,
    val costMultiplier: Double = 1.3
)

@Entity(tableName = "ad_reward_receipts")
data class AdRewardReceipt(@PrimaryKey val token: String, val rewardedAt: Long)

@Entity(tableName = "play_purchase_receipts")
data class PlayPurchaseReceipt(
    @PrimaryKey val tokenHash: String,
    val productId: String,
    val grantedAt: Long
)

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayReceipt(receipt: PlayPurchaseReceipt): Long
    @Query("SELECT * FROM play_purchase_receipts")
    suspend fun getPlayReceipts(): List<PlayPurchaseReceipt>
    @Insert suspend fun insertBattle(battle: CombatBattle)
    @Query("SELECT * FROM combat_battles WHERE id = :id")
    suspend fun getBattle(id: String): CombatBattle?
    @Query("UPDATE combat_battles SET status = :status WHERE id = :id")
    suspend fun finishBattle(id: String, status: String)
    @Query("UPDATE combat_battles SET status = 'abandoned' WHERE status = 'pending'")
    suspend fun abandonBattles()

    @Query("SELECT * FROM ad_reward_receipts ORDER BY token")
    suspend fun getAdReceipts(): List<AdRewardReceipt>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAdReward(receipt: AdRewardReceipt): Long

    @Query("UPDATE game_state SET starGems = starGems + 5 WHERE id = 1")
    suspend fun creditAdReward(): Int

    @Query("DELETE FROM resources")
    suspend fun clearResources()

    @Query("DELETE FROM buildings")
    suspend fun clearBuildings()

    @Query("DELETE FROM combat_units")
    suspend fun clearCombatUnits()

    @Query("DELETE FROM researchers")
    suspend fun clearResearchers()

    @Query("DELETE FROM active_missions")
    suspend fun clearMissions()

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameState?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameStateDirect(): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: GameState)

    // Resources
    @Query("SELECT * FROM resources")
    fun getResourcesFlow(): Flow<List<ResourceInventory>>

    @Query("SELECT * FROM resources")
    suspend fun getResourcesDirect(): List<ResourceInventory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResources(resources: List<ResourceInventory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResource(resource: ResourceInventory)

    // Buildings
    @Query("SELECT * FROM buildings ORDER BY id ASC")
    fun getBuildingsFlow(): Flow<List<BusinessBuilding>>

    @Query("SELECT * FROM buildings ORDER BY id ASC")
    suspend fun getBuildingsDirect(): List<BusinessBuilding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBuildings(buildings: List<BusinessBuilding>)

    @Update
    suspend fun updateBuilding(building: BusinessBuilding)

    @Query("UPDATE buildings SET cycleStartedAtMs = 0, cycleEndsAtMs = 0, cycleOutput = 0, cycleSalePrice = 0")
    suspend fun clearProductionCycles()

    // Combat Units
    @Query("SELECT * FROM combat_units ORDER BY id ASC")
    fun getCombatUnitsFlow(): Flow<List<CombatUnit>>

    @Query("SELECT * FROM combat_units ORDER BY id ASC")
    suspend fun getCombatUnitsDirect(): List<CombatUnit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCombatUnits(units: List<CombatUnit>)

    @Update
    suspend fun updateCombatUnit(unit: CombatUnit)

    // Researchers
    @Query("SELECT * FROM researchers ORDER BY id ASC")
    fun getResearchersFlow(): Flow<List<ResearcherCard>>

    @Query("SELECT * FROM researchers ORDER BY id ASC")
    suspend fun getResearchersDirect(): List<ResearcherCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveResearchers(researchers: List<ResearcherCard>)

    @Update
    suspend fun updateResearcher(researcher: ResearcherCard)

    // Active Missions
    @Query("SELECT * FROM active_missions ORDER BY id ASC")
    fun getActiveMissionsFlow(): Flow<List<ActiveMission>>

    @Query("SELECT * FROM active_missions ORDER BY id ASC")
    suspend fun getActiveMissionsDirect(): List<ActiveMission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveMissions(missions: List<ActiveMission>)

    @Update
    suspend fun updateActiveMission(mission: ActiveMission)
}

@Database(
    entities = [
        GameState::class,
        ResourceInventory::class,
        BusinessBuilding::class,
        CombatUnit::class,
        ResearcherCard::class,
        ActiveMission::class,
        CombatBattle::class,
        AdRewardReceipt::class,
        PlayPurchaseReceipt::class
    ],
    version = 14,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE buildings ADD COLUMN cycleStartedAtMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE buildings ADD COLUMN cycleEndsAtMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE buildings ADD COLUMN cycleOutput REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE buildings ADD COLUMN cycleSalePrice REAL NOT NULL DEFAULT 0")
                // Old one-second saves have no cycle history; do not pay elapsed time twice.
                db.execSQL("UPDATE game_state SET lastSavedTime = ${System.currentTimeMillis()}")
            }
        }
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_state ADD COLUMN autoSellLicenseSource TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("UPDATE game_state SET autoSellLicenseSource = 'LEGACY' WHERE hasAutoSellLicense = 1")
            }
        }
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS play_purchase_receipts (tokenHash TEXT NOT NULL, productId TEXT NOT NULL, grantedAt INTEGER NOT NULL, PRIMARY KEY(tokenHash))")
            }
        }
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS combat_battles (id TEXT NOT NULL, sector INTEGER NOT NULL, enemyLevel INTEGER NOT NULL, credits REAL NOT NULL, diamonds INTEGER NOT NULL, guildTokens INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(id))")
            }
        }
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                listOf("availableCores", "industrialEngineeringLevel", "tacticalLogisticsLevel", "offlineReserveLevel").forEach {
                    db.execSQL("ALTER TABLE game_state ADD COLUMN `$it` INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("ALTER TABLE game_state ADD COLUMN claimedCoreEvents TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE game_state SET availableCores = nebulaCores")
            }
        }
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ad_reward_receipts` (`token` TEXT NOT NULL, `rewardedAt` INTEGER NOT NULL, PRIMARY KEY(`token`))")
            }
        }
    }
    abstract val gameDao: GameDao
}
