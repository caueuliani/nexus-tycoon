package com.example.data

import kotlin.math.pow
import kotlin.random.Random

// Transactions own the fresh reads and all writes. UI callbacks run after commit.
suspend fun GameRepository.buyAutoSellLicense() {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        saveGameState(current.copy(
        hasAutoSellLicense = true
        ))
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.buyAutoSellLicenseWithGems(onSuccess: () -> Unit, onError: (String) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        if (current.hasAutoSellLicense) {
            notifications.add { onError("Você já possui esta licença!") }
            return@runProgressTransaction
        }
        if (current.starGems < 300) {
            notifications.add { onError("Gemas Estelares insuficientes (necessário: 300 💎)!") }
            return@runProgressTransaction
        }
        saveGameState(current.copy(
        starGems = current.starGems - 300,
        hasAutoSellLicense = true
        ))
        notifications.add { onSuccess() }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.buySubscription() {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        if (current.isSubscribed) return@runProgressTransaction
        saveGameState(current.copy(
        isSubscribed = true,
        starGems = Math.addExact(current.starGems, 1000.toLong()) // VIP starter gift!
        ))
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.joinGuild(guildName: String) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        saveGameState(current.copy(guildName = guildName))
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.completeTutorial() {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        saveGameState(current.copy(hasCompletedTutorial = true))
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.resetTutorial() {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        saveGameState(current.copy(hasCompletedTutorial = false))
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.buyAndOpenCapsuleWithGems(type: String, onSuccess: (Long, List<String>) -> Unit, onError: (String) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        val cost = when (type) {
            "COMUM" -> 50
            "EPICA" -> 150
            "SUPREMA" -> 400
            else -> return@runProgressTransaction
        }
        if (current.starGems < cost) {
            notifications.add { onError("Gemas insuficientes para abrir Cápsula ${type}!") }
            return@runProgressTransaction
        }

        val scientists = getResearchersDirect()
        val scienceGain = when (type) {
            "COMUM" -> Random.nextLong(15, 40)
            "EPICA" -> Random.nextLong(50, 110)
            "SUPREMA" -> Random.nextLong(150, 350)
            else -> 10L
        }

        val cardsNumber = when (type) {
            "COMUM" -> 3
            "EPICA" -> 6
            "SUPREMA" -> 12
            else -> 1
        }

        val obtainedCards = mutableListOf<String>()
        val updatedScientists = scientists.map { s ->
            var gained = 0
            for (i in 0 until cardsNumber) {
                val roll = Random.nextDouble()
                val matchChance = when (s.rarity) {
                    "COMMON" -> 0.40
                    "RARE" -> 0.20
                    "EPIC" -> 0.10
                    "SUPREME" -> if (type == "SUPREMA") 0.15 else 0.03
                    else -> 0.15
                }
                if (roll < matchChance) gained++
            }

            if (gained > 0) {
                obtainedCards.add("${s.name} (x$gained)")
                s.copy(cardsCollected = Math.addExact(s.cardsCollected, gained))
            } else s
        }

        saveResearchers(updatedScientists)
        saveGameState(current.copy(
        starGems = current.starGems - cost,
        science = Math.addExact(current.science, scienceGain.toLong())
        ))

        notifications.add { onSuccess(scienceGain, obtainedCards) }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.upgradeResearcherCard(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        val scientists = getResearchersDirect()
        val researcher = scientists.find { it.id == id } ?: return@runProgressTransaction

        val cost = researcher.getUpgradeScienceCost()
        if (current.science < cost) {
            notifications.add { onError("Ciência insuficiente! Requer: ${cost} 🧪") }
            return@runProgressTransaction
        }
        if (researcher.cardsCollected < researcher.cardsNeededToUpgrade) {
            notifications.add { onError("Cartas insuficientes! Requer: ${researcher.cardsCollected}/${researcher.cardsNeededToUpgrade} 📇") }
            return@runProgressTransaction
        }

        val nextLevel = researcher.level + 1
        val cardsUsed = researcher.cardsNeededToUpgrade
        val nextCardsNeeded = researcher.cardsNeededToUpgrade + (4 * nextLevel)

        val updated = researcher.copy(
        level = nextLevel,
        cardsCollected = researcher.cardsCollected - cardsUsed,
        cardsNeededToUpgrade = nextCardsNeeded
        )

        updateResearcher(updated)
        saveGameState(current.copy(
        science = current.science - cost
        ))
        notifications.add { onSuccess() }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.claimMissionReward(missionId: Int, onSuccess: (Long, Long, String, List<String>) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val freshState = getGameStateDirect() ?: return@runProgressTransaction
        val m = getActiveMissionsDirect().find { it.id == missionId } ?: return@runProgressTransaction
        if (!m.isCompleted || m.isClaimed) return@runProgressTransaction

        // Mark as claimed
        updateActiveMission(m.copy(isClaimed = true))

        val scienceReward = m.rewardScience
        val gemsReward = m.rewardGems

        val scientists = getResearchersDirect()
        val cardsNumber = when (m.rewardCapsuleType) {
            "COMUM" -> 2
            "EPICA" -> 5
            "SUPREMA" -> 10
            else -> 1
        }

        val obtainedCards = mutableListOf<String>()
        val updatedScientists = scientists.map { s ->
            var gained = 0
            for (i in 0 until cardsNumber) {
                val roll = Random.nextDouble()
                val matchChance = when (s.rarity) {
                    "COMMON" -> 0.45
                    "RARE" -> 0.22
                    "EPIC" -> 0.12
                    "SUPREME" -> 0.05
                    else -> 0.15
                }
                if (roll < matchChance) gained++
            }

            if (gained > 0) {
                obtainedCards.add("${s.name} (x$gained)")
                s.copy(cardsCollected = Math.addExact(s.cardsCollected, gained))
            } else s
        }

        saveResearchers(updatedScientists)

        val nextCompleted = freshState.completedRankMissions + 1
        saveGameState(freshState.copy(
        starGems = Math.addExact(freshState.starGems, gemsReward.toLong()),
        science = Math.addExact(freshState.science, scienceReward.toLong()),
        completedRankMissions = nextCompleted
        ))

        notifications.add { onSuccess(gemsReward, scienceReward, m.rewardCapsuleType, obtainedCards) }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.rankUpPlayer(onSuccess: (Int, Long, List<String>) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val freshState = getGameStateDirect() ?: return@runProgressTransaction
        if (freshState.completedRankMissions < 3 || getActiveMissionsDirect().any { !it.isClaimed }) return@runProgressTransaction
        val nextRank = freshState.playerRank + 1
        val newMissions = listOf(
        ActiveMission(
        id = 1,
        description = "Possuir ${3 + nextRank * 2} Sol-Power Arrays",
        progress = 0.0,
        target = (3 + nextRank * 2).toDouble(),
        isCompleted = false,
        isClaimed = false,
        missionType = "OWN_BUILDING",
        targetId = 1,
        rewardGems = (10 + nextRank * 5).toLong(),
        rewardScience = (40 + nextRank * 20).toLong(),
        rewardCapsuleType = if (nextRank % 3 == 0) "SUPREMA" else "EPICA"
        ),
        ActiveMission(
        id = 2,
        description = "Coletar ${formatCredits(500.0 * 2.5.pow(nextRank - 1))} de ${getBuildingResourceNameByRank(nextRank)}",
        progress = 0.0,
        target = 500.0 * 2.5.pow(nextRank - 1),
        isCompleted = false,
        isClaimed = false,
        missionType = "COLLECT_RESOURCE",
        targetString = getBuildingResourceNameByRank(nextRank),
        rewardGems = (15 + nextRank * 5).toLong(),
        rewardScience = (50 + nextRank * 20).toLong(),
        rewardCapsuleType = "EPICA"
        ),
        ActiveMission(
        id = 3,
        description = "Acumular ${formatCredits(8000.0 * 4.0.pow(nextRank - 1))} de Créditos (C$)",
        progress = 0.0,
        target = 8000.0 * 4.0.pow(nextRank - 1),
        isCompleted = false,
        isClaimed = false,
        missionType = "EARN_CASH",
        rewardGems = (20 + nextRank * 5).toLong(),
        rewardScience = (60 + nextRank * 20).toLong(),
        rewardCapsuleType = "SUPREMA"
        )
        )
        saveActiveMissions(newMissions)

        val scientists = getResearchersDirect()
        val rewardScienceGain = (100 * nextRank).toLong()
        val rewardGemsGain = (50 + nextRank * 10).toLong()

        val obtainedCards = mutableListOf<String>()
        val updatedScientists = scientists.map { s ->
            val gained = Random.nextInt(1, 3)
            obtainedCards.add("${s.name} (x$gained)")
            s.copy(cardsCollected = Math.addExact(s.cardsCollected, gained))
        }
        saveResearchers(updatedScientists)

        saveGameState(freshState.copy(
        playerRank = nextRank,
        completedRankMissions = 0,
        starGems = Math.addExact(freshState.starGems, rewardGemsGain.toLong()),
        science = Math.addExact(freshState.science, rewardScienceGain.toLong())
        ))

        notifications.add { onSuccess(nextRank, rewardScienceGain, obtainedCards) }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.purchaseScienceUpgrade(upgradeType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val freshState = getGameStateDirect() ?: return@runProgressTransaction
        val currentLevel = when (upgradeType) {
            "PROD" -> freshState.scienceProdLevel
            "CREDITS" -> freshState.scienceCreditsLevel
            "COMRADE" -> freshState.scienceComradeLevel
            "COMBAT" -> freshState.scienceCombatLevel
            else -> return@runProgressTransaction
        }
        val cost = when (upgradeType) {
            "PROD" -> (100 * 1.5.pow(currentLevel)).toLong()
            "CREDITS" -> (150 * 1.6.pow(currentLevel)).toLong()
            "COMRADE" -> (80 * 1.5.pow(currentLevel)).toLong()
            "COMBAT" -> (200 * 1.7.pow(currentLevel)).toLong()
            else -> 999999L
        }
        if (freshState.science < cost) {
            notifications.add { onError("Ciência insuficiente! Requer: $cost 🧪") }
            return@runProgressTransaction
        }

        val updatedState = when (upgradeType) {
            "PROD" -> freshState.copy(science = freshState.science - cost, scienceProdLevel = currentLevel + 1)
            "CREDITS" -> freshState.copy(science = freshState.science - cost, scienceCreditsLevel = currentLevel + 1)
            "COMRADE" -> freshState.copy(science = freshState.science - cost, scienceComradeLevel = currentLevel + 1)
            "COMBAT" -> freshState.copy(science = freshState.science - cost, scienceCombatLevel = currentLevel + 1)
            else -> freshState
        }
        saveGameState(updatedState)
        notifications.add { onSuccess() }
    }
    notifications.forEach { it() }
}

private fun getBuildingResourceNameByRank(rank: Int): String {
    return when (rank % 8) {
        1 -> "Energy Cell"
        2 -> "Iron Ore"
        3 -> "Hyperalloy"
        4 -> "Quantum Chip"
        5 -> "Organic Feedstock"
        6 -> "Neural Implant"
        7 -> "Antimatter Containment"
        0 -> "Warp Drive"
        else -> "Energy Cell"
    }
}

private fun formatCredits(amount: Double): String = com.example.ui.compactNumber(amount)
