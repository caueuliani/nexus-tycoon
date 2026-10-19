package com.example.data

/** Validation runs before the first write, including for direct/local restores. */
object BackupValidation {
    private fun finite(value: Double) = require(value.isFinite() && value >= 0) { "Valor inválido no backup." }
    fun snapshot(s: GameState, resources: List<ResourceInventory>, buildings: List<BusinessBuilding>,
        units: List<CombatUnit>, researchers: List<ResearcherCard>, missions: List<ActiveMission>) {
        state(s)
        val names = ProductionEngine.basePrices.keys.toList()
        require(resources.map { it.name }.sorted() == names.sorted())
        resources.forEach { finite(it.quantity) }
        require(buildings.map { it.id }.sorted() == (1..8).toList())
        buildings.forEach { b ->
            require(b.level >= 0 && b.level < Int.MAX_VALUE && b.resourceProduced == names[b.id - 1])
            require(b.inputResource == null || b.inputResource in names.take(b.id - 1))
            finite(b.baseCost); finite(b.costMultiplier); finite(b.productionRatePerLevel); finite(b.inputAmountPerSec)
            require(b.baseCost > 0 && b.costMultiplier >= 1)
            finite(b.getActualProductionRate())
            require(b.cycleStartedAtMs >= 0 && b.cycleEndsAtMs >= 0)
            finite(b.cycleOutput); finite(b.cycleSalePrice)
            require((b.cycleEndsAtMs == 0L && b.cycleStartedAtMs == 0L && b.cycleOutput == 0.0 && b.cycleSalePrice == 0.0) ||
                (b.level > 0 && b.cycleStartedAtMs > 0L && b.cycleEndsAtMs > b.cycleStartedAtMs &&
                    b.cycleEndsAtMs - b.cycleStartedAtMs in 500L..15_000L && b.cycleOutput > 0.0))
        }
        require(units.map { it.id }.sorted() == (1..4).toList())
        units.forEach { u ->
            require(u.level >= 1 && u.level < Int.MAX_VALUE && u.health > 0 && u.attack > 0)
            require(u.type in setOf("Heavy Mech", "Shield Troop", "Ranged DPS", "Disrupter", "Vanguard", "Shield", "Striker", "Interceptor"))
            finite(u.upgradeCost); finite(u.costMultiplier)
            require(u.upgradeCost > 0 && u.costMultiplier >= 1)
        }
        require(researchers.map { it.id }.distinct().size == researchers.size &&
            researchers.map { it.id }.containsAll((1..6).toList()) &&
            researchers.all { r -> ResearcherCatalog.cards.any { it.id == r.id &&
                it.targetBuildingId == r.targetBuildingId && it.boostType == r.boostType } })
        researchers.forEach { r ->
            require(r.level >= 0 && r.level < Int.MAX_VALUE && r.cardsCollected >= 0 && r.cardsNeededToUpgrade > 0)
            require(r.targetBuildingId in 0..8 && r.rarity in setOf("COMMON", "RARE", "EPIC", "SUPREME"))
            require(r.boostType in setOf("PRODUCTION", "SPEED", "COST_REDUCTION", "COMRADE_GEN", "GLOBAL"))
        }
        require(missions.isNotEmpty() && missions.all { it.id > 0 } &&
            missions.map { it.id }.distinct().size == missions.size &&
            missions.count { !it.isClaimed } <= missionLimitForRank(s.playerRank))
        missions.forEach { m ->
            finite(m.progress); finite(m.target)
            require(m.target > 0 && m.progress <= m.target && (!m.isClaimed || m.isCompleted))
            require(m.rewardGems >= 0 && m.rewardScience >= 0)
            require(m.missionType in setOf("OWN_BUILDING", "COLLECT_RESOURCE", "COMBAT_WIN", "EARN_CASH"))
            require(m.rewardCapsuleType in setOf("COMUM", "EPICA", "SUPREMA"))
            if (m.missionType == "OWN_BUILDING") require(m.targetId in 1..8)
            if (m.missionType == "COLLECT_RESOURCE") require(m.targetString in names)
        }
        val claims = s.claimedCoreEvents.split("|").filter { it.isNotEmpty() }
        require(claims.distinct().size == claims.size && claims.all {
            it in setOf("Energy Supplier", "Celestial Capitalist", "Mega Titan Upgrader") })
        // Also catch finite inputs whose products overflow under the actual rules.
        ProductionEngine.step(s, buildings, resources, researchers)
    }

    fun state(s: GameState) {
        StellarLab.validate(s)
        require(s.autoSellLicenseSource in setOf("NONE", "DEMO", "GEMS", "PLAY", "LEGACY"))
        require(s.hasAutoSellLicense || s.autoSellLicenseSource == "NONE")
        require(s.id == 1 && s.companyName.isNotBlank())
        finite(s.cash); finite(s.comrades); finite(s.comradesPerSec)
        require(listOf(s.starGems, s.guildTokens, s.science, s.lastSavedTime).all { it >= 0 })
        require(listOf(s.scienceProdLevel, s.scienceCreditsLevel, s.scienceComradeLevel, s.scienceCombatLevel,
            s.stage1CompletedCount, s.stage2CompletedCount, s.stage3CompletedCount, s.stage4CompletedCount).all { it >= 0 && it < Int.MAX_VALUE })
        require(s.playerRank > 0 && s.playerRank < Int.MAX_VALUE &&
            s.completedRankMissions in 0..missionLimitForRank(s.playerRank))
        require(s.highestCombatStage in 1..5 && s.selectedSkinId in 0..2 && s.selectedAvatarId in 0..7)
        require(s.novaSeenMask in 0..NovaPilot.ALL && s.novaPendingMask in 0..NovaPilot.ALL &&
            (s.novaSeenMask and s.novaPendingMask) == 0)
    }

    /** Do not let Number.toInt()/toLong() truncate fractions or saturate overflows. */
    fun payload(payload: Map<String, Any>) {
        fun check(map: Map<*, *>, ints: String = "", longs: String = "", doubles: String = "", strings: String = "", bools: String = "") {
            fun fields(value: String) = value.split(" ").filter { it.isNotEmpty() }
            for (key in fields(ints) + fields(longs)) {
                val n = map[key] as? Number ?: throw IllegalArgumentException("Campo numérico ausente: $key")
                val exact = n.toString().toBigDecimal().toBigIntegerExact()
                val maximum = if (key in fields(ints)) Int.MAX_VALUE.toLong() else Long.MAX_VALUE
                require(exact <= java.math.BigInteger.valueOf(maximum)) { "Campo fora do intervalo: $key" }
                require(exact.signum() >= 0) { "Campo negativo: $key" }
            }
            for (key in fields(doubles)) finite((map[key] as? Number ?: throw IllegalArgumentException("Campo ausente: $key")).toDouble())
            for (key in fields(strings)) require(map[key] is String) { "Texto ausente: $key" }
            for (key in fields(bools)) require(map[key] is Boolean) { "Booleano ausente: $key" }
        }
        check(payload, ints = "backupVersion selectedSkinId selectedAvatarId highestCombatStage stage1CompletedCount stage2CompletedCount stage3CompletedCount stage4CompletedCount playerRank completedRankMissions scienceProdLevel scienceCreditsLevel scienceComradeLevel scienceCombatLevel",
            longs = "starGems nebulaCores guildTokens science lastSavedTime", doubles = "cash comrades comradesPerSec",
            strings = "companyName guildName", bools = "isSubscribed hasAutoSellLicense hasCompletedTutorial")
        if ((payload["backupVersion"] as Number).toInt() >= 3) check(payload,
            ints = "industrialEngineeringLevel tacticalLogisticsLevel offlineReserveLevel", longs = "availableCores", strings = "claimedCoreEvents")
        if ((payload["backupVersion"] as Number).toInt() >= 5) check(payload,
            strings = "autoSellLicenseSource")
        if ((payload["backupVersion"] as Number).toInt() >= 7) check(payload,
            ints = "novaSeenMask novaPendingMask")
        fun rows(key: String, validate: (Map<*, *>) -> Unit) {
            val list = payload[key] as? List<*> ?: throw IllegalArgumentException("Tabela ausente: $key")
            list.forEach { validate(it as? Map<*, *> ?: throw IllegalArgumentException("Registro inválido: $key")) }
        }
        if ((payload["backupVersion"] as Number).toInt() >= 4) rows("adReceipts") { check(it, longs = "rewardedAt", strings = "token") }
        rows("resources") { check(it, doubles = "quantity", strings = "name") }
        rows("buildings") {
            check(it, ints = "id level", doubles = "baseCost costMultiplier productionRatePerLevel inputAmountPerSec",
                strings = "name resourceProduced", bools = "isAutomated isAutoSelling")
            if ((payload["backupVersion"] as Number).toInt() >= 6) check(it,
                longs = "cycleStartedAtMs cycleEndsAtMs", doubles = "cycleOutput cycleSalePrice")
            require(it["inputResource"] == null || it["inputResource"] == org.json.JSONObject.NULL || it["inputResource"] is String)
        }
        rows("combatUnits") { check(it, ints = "id level health attack", doubles = "upgradeCost costMultiplier", strings = "name type") }
        rows("researchers") { check(it, ints = "id level cardsCollected cardsNeededToUpgrade targetBuildingId", strings = "name description boostType rarity") }
        rows("missions") { check(it, ints = "id targetId", longs = "rewardGems rewardScience", doubles = "progress target",
            strings = "description missionType targetString rewardCapsuleType", bools = "isCompleted isClaimed") }
    }
}
