package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class CombatReward(val credits: Double, val diamonds: Long, val guildTokens: Long)

object CombatRewards {
    fun victories(state: GameState, sector: Int): Int = when (sector) {
        1 -> state.stage1CompletedCount
        2 -> state.stage2CompletedCount
        3 -> state.stage3CompletedCount
        4 -> state.stage4CompletedCount
        else -> error("Setor inválido")
    }
    fun forLevel(sector: Int, level: Int): CombatReward {
        require(sector in 1..4 && level >= 1)
        val previous = level - 1
        return CombatReward(
            listOf(500.0, 1000.0, 1500.0, 2500.0)[sector - 1] * (1.0 + 0.06 * previous),
            1L + 2L * (previous / 10),
            ((sector * 5L) * (1.0 + 0.05 * previous)).toLong()
        )
    }
}

@Entity(tableName = "combat_battles")
data class CombatBattle(
    @PrimaryKey val id: String,
    val sector: Int,
    val enemyLevel: Int,
    val credits: Double,
    val diamonds: Long,
    val guildTokens: Long,
    val status: String = "pending"
) {
    fun reward() = CombatReward(credits, diamonds, guildTokens)
}
