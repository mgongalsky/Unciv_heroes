package com.unciv.clean.application.ports.battle

import com.unciv.clean.application.usecases.battle.AttackCommand
import com.unciv.clean.application.usecases.battle.BattleResult

/**
 * Порт «как считать бой». На Stage 10 адаптируемся к legacy-логике 1:1.
 * Позже можно будет подменить на чистый доменный калькулятор.
 */
interface CombatResolver {
    fun resolve(command: AttackCommand): BattleResult
}
