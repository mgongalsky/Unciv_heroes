package com.unciv.pure.domain.arena

import kotlin.random.Random

/** A directed matchup at the measured army scale, before the player's bonus. */
data class ArenaMatchup(
    val playerUnit: String,
    val opponentUnit: String,
    val playerCount: Int,
    val opponentCount: Int
) {
    init {
        require(playerUnit.isNotBlank() && opponentUnit.isNotBlank())
        require(playerUnit != opponentUnit)
        require(playerCount > 0 && opponentCount > 0)
    }
}

data class ArenaEncounter(
    val matchup: ArenaMatchup,
    val playerCount: Int,
    val troopSlots: Int = 4
) {
    init {
        require(playerCount > 0)
        require(troopSlots in 1..5)
    }

    val playerStacks: List<Int> get() = ArenaArmyDistribution.split(playerCount, troopSlots)
    val opponentStacks: List<Int>
        get() = ArenaArmyDistribution.split(matchup.opponentCount, troopSlots)
}

data class ArenaTierConfig(val battleCount: Int = 3, val playerBonusPercent: Int = 30) {
    init {
        require(battleCount > 0)
        require(playerBonusPercent in 0..1000)
    }
}

object ArenaGenerator {
    fun bonusForTier(tier: Int): Int {
        require(tier > 0)
        return if (tier >= 4) 0 else 30 - (tier - 1) * 10
    }

    fun generate(
        matchups: List<ArenaMatchup>,
        seed: Long,
        config: ArenaTierConfig = ArenaTierConfig()
    ): List<ArenaEncounter> {
        require(matchups.map { it.playerUnit to it.opponentUnit }.distinct().size == matchups.size)
        require(matchups.size >= config.battleCount)
        return matchups.shuffled(Random(seed)).take(config.battleCount)
            .mapIndexed { index, matchup ->
                val count =
                    (matchup.playerCount.toLong() * (100L + config.playerBonusPercent) + 99L) / 100L
                require(count <= Int.MAX_VALUE)
                ArenaEncounter(matchup, count.toInt(), troopSlots = 4 + index % 2)
            }
    }
}

/** Progress belongs to the run; mutable combat armies are recreated for every attempt. */
class ArenaRun(
    encounters: List<ArenaBattleDefinition>,
    private val nextTierEncounters: ((Int) -> List<ArenaBattleDefinition>)? = null
) {
    companion object {
        fun generated(matchups: List<ArenaMatchup>, seed: Long): ArenaRun {
            val pool = matchups.toList()
            fun generateTier(tier: Int) = ArenaGenerator.generate(
                pool, seed + tier.toLong() - 1L,
                ArenaTierConfig(playerBonusPercent = ArenaGenerator.bonusForTier(tier))
            ).map { ArenaBattleDefinition.homogeneous(it) }
            return ArenaRun(generateTier(1), ::generateTier)
        }

        fun withMixedBattles(
            matchups: List<ArenaMatchup>,
            mixedMatchups: List<ArenaMixedMatchup>,
            seed: Long
        ): ArenaRun {
            val pool = matchups.toList()
            val mixedPool = mixedMatchups.toList()
            fun generateTier(tier: Int) = ArenaTierGenerator.generate(
                pool, mixedPool, seed + tier.toLong() - 1L, ArenaGenerator.bonusForTier(tier)
            )
            return ArenaRun(generateTier(1), ::generateTier)
        }
    }

    var encounters: List<ArenaBattleDefinition> = encounters.toList()
        private set
    var tier: Int = 1
        private set
    val playerBonusPercent: Int get() = ArenaGenerator.bonusForTier(tier)
    var completedBattles: Int = 0
        private set
    val isComplete: Boolean get() = completedBattles == encounters.size
    var lastOutcome: Outcome? = null
        private set
    private var nextAttemptId = 0L
    private var activeAttempt: Attempt? = null

    init {
        require(this.encounters.isNotEmpty())
    }

    enum class Outcome { VICTORY, DEFEAT, DRAW, ABANDONED }
    data class Attempt(val id: Long, val battleIndex: Int, val encounter: ArenaBattleDefinition)

    fun beginBattle(): Attempt? {
        if (isComplete || activeAttempt != null) return null
        return Attempt(++nextAttemptId, completedBattles, encounters[completedBattles]).also {
            activeAttempt = it
        }
    }

    fun finishBattle(attempt: Attempt, outcome: Outcome): Boolean {
        if (activeAttempt !== attempt) return false
        activeAttempt = null
        lastOutcome = outcome
        if (outcome == Outcome.VICTORY) completedBattles++
        return true
    }

    fun advanceTier(): Boolean {
        if (!isComplete || activeAttempt != null || tier == Int.MAX_VALUE) return false
        val factory = nextTierEncounters ?: return false
        val next = factory(tier + 1).toList()
        require(next.size == 3)
        encounters = next
        tier++
        completedBattles = 0
        lastOutcome = null
        return true
    }
}
