package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "Nova Core Inc",
    val cash: Double = 5000.0,
    val starGems: Long = 250,
    val nebulaCores: Long = 15,
    val guildTokens: Long = 50,
    val guildName: String = "No Guild Joined",
    val selectedSkinId: Int = 0, // 0: Industrial Blue, 1: Solar Flare Gold, 2: Cyber Purple Neon
    val selectedAvatarId: Int = 0, // 0: Engineer, 1: Commander, 2: Cyborg
    val lastSavedTime: Long = System.currentTimeMillis(),
    val isSubscribed: Boolean = false,
    val highestCombatStage: Int = 1,
    val hasAutoSellLicense: Boolean = false
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
    val isAutoSelling: Boolean = false
)

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

@Dao
interface GameDao {
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

    // Combat Units
    @Query("SELECT * FROM combat_units ORDER BY id ASC")
    fun getCombatUnitsFlow(): Flow<List<CombatUnit>>

    @Query("SELECT * FROM combat_units ORDER BY id ASC")
    suspend fun getCombatUnitsDirect(): List<CombatUnit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCombatUnits(units: List<CombatUnit>)

    @Update
    suspend fun updateCombatUnit(unit: CombatUnit)
}

@Database(
    entities = [
        GameState::class,
        ResourceInventory::class,
        BusinessBuilding::class,
        CombatUnit::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract val gameDao: GameDao
}
