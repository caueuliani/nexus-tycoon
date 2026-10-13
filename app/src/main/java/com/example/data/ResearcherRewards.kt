package com.example.data

import kotlin.random.Random

internal data class CardGrant(val researchers: List<ResearcherCard>, val labels: List<String>)

/** Preserve the old expected number of drops per rarity as the collection expands. */
internal object ResearcherRewards {
    private val originalPool = mapOf("COMMON" to 2, "RARE" to 2, "EPIC" to 1, "SUPREME" to 1)

    fun roll(researchers: List<ResearcherCard>, attempts: Int, capsule: String?, random: Random = Random.Default): CardGrant {
        val byRarity = researchers.groupingBy { it.rarity }.eachCount()
        val labels = mutableListOf<String>()
        val updated = researchers.map { card ->
            val baseChance = when (card.rarity) {
                "COMMON" -> if (capsule == null) 0.45 else 0.40
                "RARE" -> if (capsule == null) 0.22 else 0.20
                "EPIC" -> if (capsule == null) 0.12 else 0.10
                "SUPREME" -> if (capsule == null) 0.05 else if (capsule == "SUPREMA") 0.15 else 0.03
                else -> 0.0
            }
            val chance = baseChance * (originalPool[card.rarity] ?: 0).toDouble() /
                (byRarity[card.rarity] ?: 1).toDouble()
            var gained = 0
            repeat(attempts) { if (random.nextDouble() < chance) gained++ }
            if (gained > 0) {
                labels.add("${card.name} (x$gained)")
                card.copy(cardsCollected = Math.addExact(card.cardsCollected, gained))
            } else card
        }
        return CardGrant(updated, labels)
    }

    fun promotion(researchers: List<ResearcherCard>, random: Random = Random.Default): CardGrant {
        val grants = originalPool.flatMap { (rarity, count) ->
            researchers.filter { it.rarity == rarity }.shuffled(random).take(count)
        }.associate { it.id to random.nextInt(1, 3) }
        val labels = mutableListOf<String>()
        val updated = researchers.map { card ->
            val gained = grants[card.id] ?: 0
            if (gained > 0) {
                labels.add("${card.name} (x$gained)")
                card.copy(cardsCollected = Math.addExact(card.cardsCollected, gained))
            } else card
        }
        return CardGrant(updated, labels)
    }
}
