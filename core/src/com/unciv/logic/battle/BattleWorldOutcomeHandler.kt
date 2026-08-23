package com.unciv.logic.battle

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
                    val attackerCiv = attacker?.getCivInfo() ?: return
                    defeated.city.moveToCiv(attackerCiv)
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
