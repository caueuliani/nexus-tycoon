package com.example

import com.example.data.ResearcherCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearcherIdentityTest {
    @Test fun knownResearchersHaveArtworkAndUnknownIdsUseIconFallback() {
        ResearcherCatalog.cards.forEach { assertNotNull(ResearcherStyle.artwork(it.id)) }
        assertNull(ResearcherStyle.artwork(0))
        assertNull(ResearcherStyle.artwork(27))
    }

    @Test fun expansionHasDistinctCharacterNamesAndRenamesLegacyDisplayOnly() {
        val cards = ResearcherCatalog.cards
        assertEquals(26, cards.map { it.name }.distinct().size)
        assertTrue(cards.drop(6).none { it.name.contains(" • ") || it.name.startsWith("Indústria ") })
        val old = cards.first { it.id == 7 }.copy(
            name = "Solar • Produção",
            description = "Aumenta em +200% por nível a produção de Sol-Power Array.",
            level = 4, cardsCollected = 9
        )
        val renamed = ResearcherCatalog.withMissing(cards.map { if (it.id == old.id) old else it })
            .first { it.id == old.id }
        assertEquals(cards.first { it.id == old.id }.name, renamed.name)
        assertEquals(old.copy(name = renamed.name, description = renamed.description), renamed)
        assertEquals(cards.first { it.id == old.id }.description, renamed.description)
    }
}
