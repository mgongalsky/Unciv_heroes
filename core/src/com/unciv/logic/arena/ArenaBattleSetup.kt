package com.unciv.logic.arena

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.arena.ArenaGenerator
import com.unciv.pure.domain.arena.ArenaMatchup
import com.unciv.pure.domain.arena.ArenaRun
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

/** Initial tuning from boundary-1788699997865's directed, 20-attacker measurements.
 * Uses the lower integer boundary so rounding never strengthens the opponent.
 * The source used a simplified field; these counts are a starting estimate for live play.
 */
object ArenaBattleSetup {
    val matchups: List<ArenaMatchup> = listOf(
        ArenaMatchup("Peasant", "Swordsman", 20, 1),
        // Calibrated on the production 14x8 field with 4–5 squads; see docs/arena-peasant-balance.md.
        ArenaMatchup("Peasant", "Archer", 36, 5),
        ArenaMatchup("Peasant", "Spearman", 20, 2),
        ArenaMatchup("Peasant", "Horseman", 20, 1),
        ArenaMatchup("Swordsman", "Peasant", 20, 176),
        ArenaMatchup("Swordsman", "Archer", 20, 50),
        ArenaMatchup("Swordsman", "Spearman", 20, 27),
        ArenaMatchup("Swordsman", "Horseman", 20, 15),
        ArenaMatchup("Archer", "Peasant", 20, 68),
        ArenaMatchup("Archer", "Swordsman", 20, 7),
        ArenaMatchup("Archer", "Spearman", 20, 10),
        ArenaMatchup("Archer", "Horseman", 20, 6),
        ArenaMatchup("Spearman", "Peasant", 20, 129),
        ArenaMatchup("Spearman", "Swordsman", 20, 14),
        ArenaMatchup("Spearman", "Archer", 20, 36),
        ArenaMatchup("Spearman", "Horseman", 20, 11),
        ArenaMatchup("Horseman", "Peasant", 20, 221),
        ArenaMatchup("Horseman", "Swordsman", 20, 25),
        ArenaMatchup("Horseman", "Archer", 20, 64),
        ArenaMatchup("Horseman", "Spearman", 20, 34)
    )

    fun newRun(seed: Long): ArenaRun = ArenaRun.withMixedBattles(matchups, mixedMatchups, seed)

    fun ruleset(): Ruleset = requireNotNull(RulesetCache[BaseRuleset.Civ_V_GnK.fullName]) {
        "Arena ruleset has not been loaded"
    }

    fun createArmy(unitName: String, count: Int, ruleset: Ruleset, troopSlots: Int = 4): ArmyInfo {
        val stacks = com.unciv.pure.domain.arena.ArenaArmyDistribution.split(count, troopSlots)
        val unit = requireNotNull(ruleset.units[unitName]) { "Unknown arena unit: $unitName" }
        require(unit.speed > 0 && unit.health > 0 && unit.damage > 0)
        val source = RulesetTroopDefinitionSource(ruleset)
        return ArmyInfo(CivilizationInfo(), maxSlots = troopSlots).apply {
            stacks.forEachIndexed { index, amount ->
                setTroopAt(index, TroopFactory.create(unitName, amount, source))
            }
        }
    }
    fun createArmyFromStacks(
        stacks: List<com.unciv.pure.domain.arena.ArenaStack>,
        ruleset: Ruleset,
        troopSlots: Int
    ): ArmyInfo {
        require(troopSlots in 1..5)
        require(stacks.size in 1..troopSlots)
        stacks.forEach { stack ->
            val unit = requireNotNull(ruleset.units[stack.unitName]) {
                "Unknown arena unit: ${stack.unitName}"
            }
            require(unit.speed > 0 && unit.health > 0 && unit.damage > 0)
        }
        val source = RulesetTroopDefinitionSource(ruleset)
        return ArmyInfo(CivilizationInfo(), maxSlots = troopSlots).apply {
            stacks.forEachIndexed { index, stack ->
                setTroopAt(index, TroopFactory.create(stack.unitName, stack.count, source))
            }
        }
    }

    /** Symmetric base compositions for initial tuning on the production battlefield. */
    val mixedMatchups: List<com.unciv.pure.domain.arena.ArenaMixedMatchup> = run {
        fun army(vararg groups: com.unciv.pure.domain.arena.ArenaUnitGroup) =
                com.unciv.pure.domain.arena.ArenaArmyComposition(groups.toList())

        fun group(unit: String, count: Int, slots: Int) =
                com.unciv.pure.domain.arena.ArenaUnitGroup(unit, count, slots)

        fun matchup(id: String, composition: com.unciv.pure.domain.arena.ArenaArmyComposition) =
                com.unciv.pure.domain.arena.ArenaMixedMatchup(id, composition, composition)
        listOf(
            matchup("infantry-archers", army(group("Spearman", 12, 2), group("Archer", 10, 2))),
            matchup("infantry-cavalry", army(group("Swordsman", 10, 2), group("Horseman", 6, 2))),
            matchup(
                "combined-arms", army(
                    group("Spearman", 10, 2), group("Archer", 8, 2), group("Horseman", 4, 1)
                )
            )
        )
    }
}
