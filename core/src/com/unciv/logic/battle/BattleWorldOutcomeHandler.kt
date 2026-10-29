package com.unciv.logic.battle

import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision

class BattleWorldOutcomeHandler(
    private val attacker: ICombatant?,
    private val defender: ICombatant?
) {
    fun apply(winnerIsAttacker: Boolean?) {
        if (winnerIsAttacker == null) {
            if (attacker is MapUnitCombatant) removeUnit(attacker)
            if (defender is MapUnitCombatant) removeUnit(defender)
            return
        }
        if (winnerIsAttacker) {
            when (val defeated = defender) {
                is MapUnitCombatant -> removeUnit(defeated)
                is CityCombatant -> {
                    if (defeated.city.state.ownership == CityOwnership.Neutral) {
                        val hero = (attacker as? MapUnitCombatant)?.unit
                            ?: throw IllegalArgumentException("A neutral city requires a hero attacker")
                        require(NeutralCityEntry.decision(defeated.city, hero) == Decision.ACQUIRE) {
                            "Neutral city victory requires an eligible hero and no living defenders"
                        }
                        NeutralCityOwnership.acquireUndefended(defeated.city, hero.civInfo)
                    } else {
                        val attackerCiv = attacker?.getCivInfo() ?: return
                        defeated.city.moveToCiv(attackerCiv)
                    }
                }
            }
            return
        }

        val defeated = attacker
        if (defeated is MapUnitCombatant) removeUnit(defeated)
    }

    private fun removeUnit(combatant: MapUnitCombatant) {
        val unit = combatant.unit
        unit.removeFromTile()
        unit.civInfo.removeUnit(unit)
        unit.civInfo.updateViewableTiles()
    }
}
