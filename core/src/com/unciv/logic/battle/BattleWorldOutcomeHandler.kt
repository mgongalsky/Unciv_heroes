package com.unciv.logic.battle

import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision

/** Применяет завершённый тактический бой к мировой карте: удаляет проигравшие юниты или передаёт город. */
class BattleWorldOutcomeHandler(
    private val attacker: ICombatant?,
    private val defender: ICombatant?
) {
    /** true — победа нападающего, false — защитника, null — взаимное поражение юнитов. */
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
                        // Результат боя не заменяет проверку: finishBattle должен уже убрать живых защитников.
                        require(NeutralCityEntry.decision(defeated.city, hero) == Decision.ACQUIRE) {
                            "Neutral city victory requires an eligible hero and no living defenders"
                        }
                        val center = defeated.city.getCenterTile()
                        require(hero.currentTile.aerialDistanceTo(center) == 1 && hero.movement.canMoveTo(center)) {
                            "Neutral city victory requires an accessible adjacent center"
                        }
                        NeutralCityOwnership.acquireUndefended(defeated.city, hero.civInfo)
                        // Атака и движение уже учтены сессией; здесь только переносим героя между клетками.
                        hero.removeFromTile()
                        hero.putInTile(center)
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

    /** Убирает юнит и с клетки, и из списка владельца, затем обновляет видимость. */
    private fun removeUnit(combatant: MapUnitCombatant) {
        val unit = combatant.unit
        unit.removeFromTile()
        unit.civInfo.removeUnit(unit)
        unit.civInfo.updateViewableTiles()
    }
}
