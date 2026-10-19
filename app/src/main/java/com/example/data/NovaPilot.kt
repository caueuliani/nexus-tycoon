package com.example.data

/** Pilot narrative state is part of the player's save, not an Activity preference. */
object NovaPilot {
    const val FIRST_INDUSTRY = 1
    const val FIRST_RESEARCHER = 2
    const val FIRST_ASCENSION = 4
    const val ALL = FIRST_INDUSTRY or FIRST_RESEARCHER or FIRST_ASCENSION

    fun queue(state: GameState, event: Int): GameState {
        require(event in listOf(FIRST_INDUSTRY, FIRST_RESEARCHER, FIRST_ASCENSION))
        if (state.novaSeenMask and event != 0 || state.novaPendingMask and event != 0) return state
        return state.copy(novaPendingMask = state.novaPendingMask or event)
    }

    fun acknowledge(state: GameState, event: Int): GameState {
        require(event in listOf(FIRST_INDUSTRY, FIRST_RESEARCHER, FIRST_ASCENSION))
        if (state.novaPendingMask and event == 0) return state
        return state.copy(novaPendingMask = state.novaPendingMask and event.inv(),
            novaSeenMask = state.novaSeenMask or event)
    }

    fun next(state: GameState): Int? = listOf(FIRST_INDUSTRY, FIRST_RESEARCHER, FIRST_ASCENSION)
        .firstOrNull { state.novaPendingMask and it != 0 }

    fun firstResearcherAcquired(before: List<ResearcherCard>, after: List<ResearcherCard>): Boolean =
        // Card 1 is granted in the starter save. The cue celebrates the first *new* discovery.
        before.none { it.id != 1 && (it.level > 0 || it.cardsCollected > 0) } &&
            after.any { it.id != 1 && (it.level > 0 || it.cardsCollected > 0) }
}
