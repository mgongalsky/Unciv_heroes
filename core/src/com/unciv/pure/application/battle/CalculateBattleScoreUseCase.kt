package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.BattleScore

/** Shared valuation for local results and authoritative server reports. */
object CalculateBattleScoreUseCase {
    data class Stack(val unitName: String, val initialAmount: Int, val remainingAmount: Int)

    /** Missing or non-positive recruitment prices make the score unavailable. */
    fun execute(stacks: List<Stack>, unitPrice: (String) -> Int?): BattleScore? {
        if (stacks.isEmpty()) return null
        var initial = 0L
        var remaining = 0L
        for (stack in stacks) {
            require(stack.initialAmount > 0)
            require(stack.remainingAmount in 0..stack.initialAmount)
            val price = unitPrice(stack.unitName)?.takeIf { it > 0 } ?: return null
            val initialPart = stack.initialAmount.toLong() * price
            val remainingPart = stack.remainingAmount.toLong() * price
            require(initial <= Long.MAX_VALUE - initialPart) { "Army value overflow" }
            require(remaining <= Long.MAX_VALUE - remainingPart) { "Army value overflow" }
            initial += initialPart
            remaining += remainingPart
        }
        return BattleScore(initial, remaining)
    }
}
