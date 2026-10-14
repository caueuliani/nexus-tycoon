package com.example.data

import kotlin.math.pow

internal fun missionLimitForRank(rank: Int): Int =
    if (rank <= 3) 3 else (4 + (rank - 4) / 2).coerceAtMost(12)

internal fun missionActiveSlotLimitForRank(rank: Int): Int = missionLimitForRank(rank).coerceAtMost(4)

internal fun missionVisibleLimit(rank: Int, completed: Int): Int =
    (missionLimitForRank(rank) - completed).coerceAtLeast(0).coerceAtMost(4)

internal fun visibleMissions(rank: Int, completed: Int, existing: List<ActiveMission>): List<ActiveMission> =
    existing.filterNot { it.isClaimed }
        .sortedWith(compareByDescending<ActiveMission> { it.isCompleted }.thenBy { it.id })
        .take(missionVisibleLimit(rank, completed))

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

internal fun missingMissions(rank: Int, completed: Int, existing: List<ActiveMission>,
    minimumNextId: Int = 1): List<ActiveMission> {
    val missing = (missionVisibleLimit(rank, completed) - existing.count { !it.isClaimed }).coerceAtLeast(0)
    val firstId = maxOf((existing.maxOfOrNull { it.id } ?: 0) + 1, minimumNextId)
    return (firstId until firstId + missing).map { missionForRank(rank, it) }
}

internal fun withCurrentMissionProgress(mission: ActiveMission, cash: Double,
    resources: Map<String, Double>, buildingLevels: Map<Int, Int>): ActiveMission {
    val current = when (mission.missionType) {
        "OWN_BUILDING" -> buildingLevels[mission.targetId]?.toDouble() ?: 0.0
        "COLLECT_RESOURCE" -> resources[mission.targetString] ?: 0.0
        "EARN_CASH" -> cash
        else -> mission.progress
    }
    val progress = current.coerceIn(0.0, mission.target)
    return mission.copy(progress = progress, isCompleted = progress >= mission.target)
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
