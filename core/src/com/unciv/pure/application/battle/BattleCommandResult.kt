package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point

data class BattleCommandResult(
    val success: Boolean,
    val movedFrom: Point? = null,
    val movedTo: Point? = null,
    val rejection: BattleRejection? = null,
    val isLuck: Boolean = false,
    val isMorale: Boolean = false,
    val battleEnded: Boolean = false,
    val hasFollowUpShot: Boolean = false,
    val attackFlanking: FlankingCalculator.Result = FlankingCalculator.Result(),
    val retaliationFlanking: FlankingCalculator.Result = FlankingCalculator.Result()
)
