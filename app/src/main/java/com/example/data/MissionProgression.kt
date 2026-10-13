package com.example.data

import kotlin.math.pow

internal fun missionLimitForRank(rank: Int): Int =
    if (rank <= 3) 3 else (4 + (rank - 4) / 2).coerceAtMost(12)

// Keep the existing three mission templates, targets and individual rewards.
internal fun missionForRank(rank: Int, id: Int): ActiveMission {
    if (rank == 1) return initialRankMissions()[(id - 1) % 3].copy(id = id, progress = 0.0)
    val template = (id - 1) % 3
    val resource = missionResourceForRank(rank)
    return when (template) {
        0 -> ActiveMission(id, "Possuir ${3 + rank * 2} Sol-Power Arrays", 0.0,
            (3 + rank * 2).toDouble(), false, false, "OWN_BUILDING", 1, "",
            (10 + rank * 5).toLong(), (40 + rank * 20).toLong(),
            if (rank % 3 == 0) "SUPREMA" else "EPICA")
        1 -> {
            val target = 500.0 * 2.5.pow(rank - 1)
            ActiveMission(id, "Coletar ${com.example.ui.compactNumber(target)} de $resource", 0.0,
                target, false, false, "COLLECT_RESOURCE", 0, resource,
                (15 + rank * 5).toLong(), (50 + rank * 20).toLong(), "EPICA")
        }
        else -> {
            val target = 8000.0 * 4.0.pow(rank - 1)
            ActiveMission(id, "Acumular ${com.example.ui.compactNumber(target)} de Créditos (C$)",
                0.0, target, false, false, "EARN_CASH", 0, "",
                (20 + rank * 5).toLong(), (60 + rank * 20).toLong(), "SUPREMA")
        }
    }
}

internal fun missingMissions(rank: Int, existing: List<ActiveMission>): List<ActiveMission> {
    val missing = (missionLimitForRank(rank) - existing.count { !it.isClaimed }).coerceAtLeast(0)
    val firstId = (existing.maxOfOrNull { it.id } ?: 0) + 1
    return (firstId until firstId + missing).map { missionForRank(rank, it) }
}

private fun missionResourceForRank(rank: Int): String = when (rank % 8) {
    1 -> "Energy Cell"
    2 -> "Iron Ore"
    3 -> "Hyperalloy"
    4 -> "Quantum Chip"
    5 -> "Organic Feedstock"
    6 -> "Neural Implant"
    7 -> "Antimatter Containment"
    else -> "Warp Drive"
}
