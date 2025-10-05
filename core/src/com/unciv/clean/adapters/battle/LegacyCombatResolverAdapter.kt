package com.unciv.clean.adapters.battle

import com.unciv.clean.application.ports.battle.CombatResolver
import com.unciv.clean.application.usecases.battle.*
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.logic.GameInfo
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.MapUnitCombatant

/**
 * Адаптер, который вызывает «как было» в GameInfo/Unit и собирает результат.
 * Цель Stage 10 — оставить формулы и сайд-эффекты прежними.
 */
class LegacyCombatResolverAdapter(
    private val game: GameInfo
) : CombatResolver {

    override fun resolve(command: AttackCommand): BattleResult {
        val attackerUnit = findUnitAt(command.attackerTile) ?: findByRef(command.attacker)
        val defenderUnit = findUnitAt(command.defenderTile) ?: findByRef(command.defender)

        val attackerCombatant = MapUnitCombatant(attackerUnit)
        val defenderCombatant = MapUnitCombatant(defenderUnit)

        // Delegate to legacy battle logic (this will show BattleScreen etc.)
        Battle.attack(attackerCombatant, defenderCombatant)

        val afterA = attackerCombatant.getHealth()
        val afterD = defenderCombatant.getHealth()

        val events: List<DomainEvent> = emptyList()
        return BattleResult(
            attackerRemainingHp = afterA,
            defenderRemainingHp = afterD,
            attackerDefeated = afterA <= 0,
            defenderDefeated = afterD <= 0,
            events = events
        )
    }

    private fun findUnitAt(pos: com.badlogic.gdx.math.Vector2) =
        game.tileMap[pos].militaryUnit ?: game.tileMap[pos].civilianUnit

    private fun findByRef(ref: UnitRef) =
        game.getCivilization(ref.civId).getCivUnits().first { it.id == ref.unitId }
}
