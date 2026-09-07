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
        ArenaMatchup("Peasant", "Archer", 20, 5),
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

    fun newRun(seed: Long): ArenaRun = ArenaRun.generated(matchups, seed)

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
}
