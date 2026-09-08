package com.unciv.pure.domain.arena

import kotlin.random.Random

/** Fully resolved deployment, including the player's tier bonus. */
data class ArenaBattleDefinition(
    val id: String,
    val playerArmy: List<ArenaStack>,
    val opponentArmy: List<ArenaStack>,
    val troopSlots: Int
) {
    init {
        require(id.isNotBlank())
        require(troopSlots in 1..5)
        require(playerArmy.size in 1..troopSlots)
        require(opponentArmy.size in 1..troopSlots)
    }

    val isMixed: Boolean
        get() = playerArmy.map { it.unitName }.distinct().size > 1 ||
                opponentArmy.map { it.unitName }.distinct().size > 1

    companion object {
        fun homogeneous(encounter: ArenaEncounter): ArenaBattleDefinition = ArenaBattleDefinition(
            "homogeneous:${encounter.matchup.playerUnit}:${encounter.matchup.opponentUnit}",
            encounter.playerStacks.map { ArenaStack(encounter.matchup.playerUnit, it) },
            encounter.opponentStacks.map { ArenaStack(encounter.matchup.opponentUnit, it) },
            encounter.troopSlots
        )

        fun mixed(matchup: ArenaMixedMatchup, bonusPercent: Int): ArenaBattleDefinition {
            val slots = maxOf(
                matchup.player.groups.sumOf { it.slots },
                matchup.opponent.groups.sumOf { it.slots }
            )
            return ArenaBattleDefinition(
                "mixed:${matchup.id}", matchup.player.stacks(bonusPercent),
                matchup.opponent.stacks(), slots
            )
        }
    }
}

/** Every tier contains one homogeneous and two distinct mixed encounters. */
object ArenaTierGenerator {
    fun generate(
        matchups: List<ArenaMatchup>,
        mixedMatchups: List<ArenaMixedMatchup>,
        seed: Long,
        bonusPercent: Int
    ): List<ArenaBattleDefinition> {
        require(mixedMatchups.size >= 2)
        require(mixedMatchups.map { it.id }.distinct().size == mixedMatchups.size)
        val random = Random(seed)
        val homogeneous = ArenaGenerator.generate(
            matchups, random.nextLong(), ArenaTierConfig(1, bonusPercent)
        ).single()
        val mixed = mixedMatchups.shuffled(random).take(2).map {
            ArenaBattleDefinition.mixed(it, bonusPercent)
        }
        return (listOf(ArenaBattleDefinition.homogeneous(homogeneous)) + mixed).shuffled(random)
    }
}
