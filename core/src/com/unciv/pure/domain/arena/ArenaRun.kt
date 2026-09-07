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
        get() = ArenaArmyDistribution.split(
            matchup.opponentCount,
            troopSlots
        )
}

data class ArenaTierConfig(val battleCount: Int = 5, val playerBonusPercent: Int = 30) {
    init {
        require(battleCount > 0)
        require(playerBonusPercent in 0..1000)
    }
}

object ArenaGenerator {
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
class ArenaRun(encounters: List<ArenaEncounter>) {
    val encounters: List<ArenaEncounter> = encounters.toList()
    var completedBattles: Int = 0
        private set
    val isComplete: Boolean get() = completedBattles == encounters.size
    var lastOutcome: Outcome? = null
        private set
    private var nextAttemptId = 0L
    private var activeAttempt: Attempt? = null

    init {
        require(this.encounters.isNotEmpty())
        require(this.encounters.all { it.playerCount > 0 })
    }

    enum class Outcome { VICTORY, DEFEAT, DRAW, ABANDONED }
    data class Attempt(val id: Long, val battleIndex: Int, val encounter: ArenaEncounter)

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
}
