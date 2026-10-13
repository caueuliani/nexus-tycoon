package com.example.data

/** Stable IDs keep the original six researchers and their progress intact. */
internal object ResearcherCatalog {
    val industries = listOf(
        "Sol-Power Array", "Deep-Core Drill", "Alloy Smelter", "Quantum Assembler",
        "Biosphere Dome", "Cybernetics Lab", "Antimatter Reactor", "Galactic Shipyard"
    )

    val cards: List<ResearcherCard> = buildList {
        add(ResearcherCard(1, "Elon Dusk-V", "Reduz o custo total de upgrades do Painel Solar em -15% por nível.", 1, 1, 2, 1, "COST_REDUCTION", "COMMON"))
        add(ResearcherCard(2, "Marie Curie-ous", "Aumenta em +200% por nível a produção da Broca de Perfuração.", 0, 0, 2, 2, "PRODUCTION", "COMMON"))
        add(ResearcherCard(3, "Ada Lovelace-space", "Aumenta a velocidade de refino do Fundidor de Liga em +50% por nível.", 0, 0, 3, 3, "SPEED", "RARE"))
        add(ResearcherCard(4, "Albert Ein-stone", "Aumenta em +200% por nível a produção do Montador Quântico.", 0, 0, 3, 4, "PRODUCTION", "RARE"))
        add(ResearcherCard(5, "Karl Mark-IV", "Soma +1.0 Camarada/segundo de forma permanente na corporação.", 0, 0, 5, 0, "COMRADE_GEN", "EPIC"))
        add(ResearcherCard(6, "Nebula Quantum Core", "Multiplica a produção global de TODAS as fábricas em +100% por nível.", 0, 0, 5, 0, "PRODUCTION", "SUPREME"))

        var id = 7
        industries.forEachIndexed { index, industry ->
            val buildingId = index + 1
            val rarity = when (buildingId) {
                1, 2 -> "COMMON"
                3, 4 -> "RARE"
                5, 6 -> "EPIC"
                else -> "SUPREME"
            }
            val needed = when (rarity) { "COMMON" -> 2; "RARE" -> 3; else -> 5 }
            val title = listOf("Solar", "Mineral", "Metalúrgica", "Quântica", "Biológica",
                "Cibernética", "Antimatéria", "Galáctica")[index]
            val existing = this.filter { it.targetBuildingId == buildingId }.map { it.boostType }.toSet()
            for ((type, label, effect) in listOf(
                Triple("PRODUCTION", "Produção", "Aumenta em +200% por nível a produção de $industry."),
                Triple("SPEED", "Velocidade", "Aumenta em +50% por nível a velocidade de $industry."),
                Triple("COST_REDUCTION", "Eficiência", "Reduz em 15% por nível o custo em créditos das melhorias de $industry (até 80%).")
            )) {
                if (type !in existing) add(ResearcherCard(id++, "$title • $label", effect,
                    0, 0, needed, buildingId, type, rarity))
            }
        }
    }

    fun withMissing(saved: List<ResearcherCard>): List<ResearcherCard> {
        val existing = saved.map { it.id }.toSet()
        val corrected = saved.map { card ->
            when {
                card.id == 2 && card.description == "Duplica a taxa de faturamento da Broca de Perfuração." ->
                    card.copy(description = cards.first { it.id == 2 }.description)
                card.id == 4 && card.description == "Duplica a taxa de processamento do Montador Quântico." ->
                    card.copy(description = cards.first { it.id == 4 }.description)
                else -> card
            }
        }
        return (corrected + cards.filter { it.id !in existing }).sortedBy { it.id }
    }
}
