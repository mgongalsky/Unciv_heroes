package com.unciv.logic.battle.session

import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.recruitment.BattleArmySelection
import com.unciv.pure.application.battle.recruitment.BattleArmyStack
import com.unciv.pure.application.battle.recruitment.BattleRecruitmentUnit

/** Reads tactical prices from the same ruleset that supplies combat statistics. */
object BattleRecruitmentRules {
    fun from(ruleset: Ruleset): BattleArmySelection {
        require(ruleset.units.values.none { it.battleCost < 0 }) { "Negative battleCost in ruleset" }
        val units = ruleset.units.values.filter { it.battleCost > 0 }.map { unit ->
            require(unit.speed > 0 && unit.health > 0 && unit.damage >= 0) {
                "Invalid recruitable battle unit: ${unit.name}"
            }
            BattleRecruitmentUnit(unit.name, unit.battleCost)
        }.sortedWith(compareBy<BattleRecruitmentUnit> { it.cost }.thenBy { it.unitName })
        return BattleArmySelection(units)
    }

    fun side(selection: BattleArmySelection, stacks: List<BattleArmyStack>): BattleSessionSetup.Side =
        BattleSessionSetup.Side(selection.validate(stacks).map {
            BattleSessionSetup.Stack(it.unitName, it.count)
        })
}
