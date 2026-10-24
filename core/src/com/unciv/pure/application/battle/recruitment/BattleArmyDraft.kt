package com.unciv.pure.application.battle.recruitment

/** Budget-constrained edits shared by all recruitment screens. */
class BattleArmyDraft(val selection: BattleArmySelection) {
    private val types = MutableList(selection.maxStacks) { selection.units.first().unitName }
    private val counts = MutableList(selection.maxStacks) { 0 }
    var locked = false

    fun unitName(slot: Int): String = types[slot]
    fun count(slot: Int): Int = counts[slot]
    fun unitCost(slot: Int): Int = selection.units.first { it.unitName == types[slot] }.cost
    fun stacks(): List<BattleArmyStack> = types.indices.mapNotNull {
        if (counts[it] == 0) null else BattleArmyStack(types[it], counts[it])
    }
    fun cost(): Long = selection.cost(stacks())
    fun remaining(): Long = selection.budget.toLong() - cost()
    fun maximum(slot: Int): Int =
        ((remaining() + counts[slot].toLong() * unitCost(slot)) / unitCost(slot)).toInt()

    fun choose(slot: Int, unitName: String) {
        if (locked) return
        val price = requireNotNull(selection.units.firstOrNull { it.unitName == unitName }).cost
        val available = remaining() + counts[slot].toLong() * unitCost(slot)
        types[slot] = unitName
        counts[slot] = minOf(counts[slot].toLong(), available / price).toInt()
    }

    fun adjust(slot: Int, delta: Int) {
        if (locked) return
        counts[slot] = (counts[slot].toLong() + delta.toLong()).coerceIn(0L, maximum(slot).toLong()).toInt()
    }
    fun fill(slot: Int) { if (!locked) counts[slot] = maximum(slot) }
    fun clear(slot: Int) { if (!locked) counts[slot] = 0 }

    /** Server-confirmed selections may be restored even while editing is locked. */
    fun restore(stacks: List<BattleArmyStack>) {
        val checked = if (stacks.isEmpty()) emptyList() else selection.validate(stacks)
        types.indices.forEach {
            types[it] = checked.getOrNull(it)?.unitName ?: selection.units.first().unitName
            counts[it] = checked.getOrNull(it)?.count ?: 0
        }
    }
}
