package com.example.data

import kotlin.math.pow
import kotlin.random.Random

// Transactions own the fresh reads and all writes. UI callbacks run after commit.
suspend fun GameRepository.buyAutoSellLicense() {
    if (!com.example.BuildConfig.DEBUG) return // Demo grants must never act as production purchases.
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        if (current.hasAutoSellLicense) return@runProgressTransaction
        saveGameState(current.copy(
        hasAutoSellLicense = true,
        autoSellLicenseSource = "DEMO"
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
        hasAutoSellLicense = true,
        autoSellLicenseSource = "GEMS"
        ))
        notifications.add { onSuccess() }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.buySubscription() {
    if (!com.example.BuildConfig.DEBUG) return // Demo grants must never act as production purchases.
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val current = getGameStateDirect() ?: return@runProgressTransaction
        if (current.isSubscribed) return@runProgressTransaction
        saveGameState(current.copy(
        isSubscribed = true // Debug demonstration only. Paid starter gift requires a verified token.
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
        saveGameState(NovaPilot.queue(current.copy(hasCompletedTutorial = true), NovaPilot.FIRST_INDUSTRY))
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

        val grant = ResearcherRewards.roll(scientists, cardsNumber, type)
        saveResearchers(grant.researchers)
        val updatedState = if (NovaPilot.firstResearcherAcquired(scientists, grant.researchers))
            NovaPilot.queue(current, NovaPilot.FIRST_RESEARCHER) else current
        saveGameState(updatedState.copy(
        starGems = current.starGems - cost,
        science = Math.addExact(current.science, scienceGain.toLong())
        ))

        notifications.add { onSuccess(scienceGain, grant.labels) }
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

        // Retain the claimed ID so delayed duplicate callbacks cannot match a replacement.
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

        val grant = ResearcherRewards.roll(scientists, cardsNumber, null)
        saveResearchers(grant.researchers)

        val nextCompleted = (freshState.completedRankMissions + 1)
            .coerceAtMost(missionLimitForRank(freshState.playerRank))
        val updatedState = if (NovaPilot.firstResearcherAcquired(scientists, grant.researchers))
            NovaPilot.queue(freshState, NovaPilot.FIRST_RESEARCHER) else freshState
        saveGameState(updatedState.copy(
        starGems = Math.addExact(freshState.starGems, gemsReward.toLong()),
        science = Math.addExact(freshState.science, scienceReward.toLong()),
        completedRankMissions = nextCompleted
        ))
        fillMissionSlots(freshState.playerRank, nextCompleted)

        notifications.add { onSuccess(gemsReward, scienceReward, m.rewardCapsuleType, grant.labels) }
    }
    notifications.forEach { it() }
}

suspend fun GameRepository.rankUpPlayer(onSuccess: (Int, Long, List<String>) -> Unit) {
    val notifications = mutableListOf<() -> Unit>()
    runProgressTransaction {
        val freshState = getGameStateDirect() ?: return@runProgressTransaction
        if (freshState.completedRankMissions < missionLimitForRank(freshState.playerRank))
            return@runProgressTransaction
        val nextRank = freshState.playerRank + 1
        // The completed rank's finite batch is over; new IDs prevent late callbacks
        // for old missions from matching missions in the next rank.

        val scientists = getResearchersDirect()
        val rewardScienceGain = (100 * nextRank).toLong()
        val rewardGemsGain = (50 + nextRank * 10).toLong()

        val grant = ResearcherRewards.promotion(scientists)
        saveResearchers(grant.researchers)

        val updatedState = if (NovaPilot.firstResearcherAcquired(scientists, grant.researchers))
            NovaPilot.queue(freshState, NovaPilot.FIRST_RESEARCHER) else freshState
        saveGameState(updatedState.copy(
        playerRank = nextRank,
        completedRankMissions = 0,
        starGems = Math.addExact(freshState.starGems, rewardGemsGain.toLong()),
        science = Math.addExact(freshState.science, rewardScienceGain.toLong())
        ))
        startNewMissionCycle(nextRank)

        notifications.add { onSuccess(nextRank, rewardScienceGain, grant.labels) }
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
