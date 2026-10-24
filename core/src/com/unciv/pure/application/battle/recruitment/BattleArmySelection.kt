package com.unciv.pure.application.battle.recruitment

/** One purchased stack; repeated unit types in different stacks are allowed. */
data class BattleArmyStack(val unitName: String, val count: Int)

data class BattleRecruitmentUnit(val unitName: String, val cost: Int)

/** Shared by local UI, remote UI and authoritative server validation. */
class BattleArmySelection(
    units: List<BattleRecruitmentUnit>,
    val budget: Int = DEFAULT_BUDGET,
    val maxStacks: Int = DEFAULT_MAX_STACKS
) {
    companion object {
        const val DEFAULT_BUDGET = 300
        const val DEFAULT_MAX_STACKS = 4
    }

    val units: List<BattleRecruitmentUnit> = units.toList()
    private val prices = this.units.associate { it.unitName to it.cost }

    init {
        require(budget > 0)
        require(maxStacks in 1..4)
        require(this.units.isNotEmpty()) { "No units available for recruitment" }
        require(prices.size == this.units.size) { "Duplicate recruitment unit" }
        require(this.units.all { it.unitName.isNotBlank() && it.cost > 0 }) {
            "Recruitment prices must be positive"
        }
    }

    /** Empty drafts are allowed for editing; ready armies must pass validate(). */
    fun cost(stacks: List<BattleArmyStack>): Long {
        require(stacks.size <= maxStacks) { "Too many stacks: maximum $maxStacks" }
        var total = 0L
        for (stack in stacks) {
            require(stack.count > 0) { "Stack count must be positive" }
            val price = requireNotNull(prices[stack.unitName]) {
                "Unit is not available for recruitment: ${stack.unitName}"
            }
            val subtotal = price.toLong() * stack.count.toLong()
            require(total <= Long.MAX_VALUE - subtotal) { "Army cost is too large" }
            total += subtotal
        }
        return total
    }

    /** Returns a detached selection so later UI edits cannot change a ready army. */
    fun validate(stacks: List<BattleArmyStack>): List<BattleArmyStack> {
        val copy = stacks.toList()
        require(copy.isNotEmpty()) { "Choose at least one stack" }
        require(cost(copy) <= budget.toLong()) { "Army exceeds the $budget coin budget" }
        return copy
    }
}
