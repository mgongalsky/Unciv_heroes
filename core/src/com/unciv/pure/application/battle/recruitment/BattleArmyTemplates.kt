package com.unciv.pure.application.battle.recruitment

import kotlin.random.Random

enum class BattleArmyTemplate(val title: String) {
    BALANCED("Balanced"), ARCHERS("More archers"), MILITIA("More peasants"),
    CAVALRY("Cavalry only"), NO_ARCHERS("No archers")
}

/** Builds editable compositions using the authoritative catalog and budget. */
object BattleArmyTemplates {
    fun create(
        selection: BattleArmySelection,
        template: BattleArmyTemplate = BattleArmyTemplate.BALANCED,
        random: Random? = null
    ): List<BattleArmyStack> {
        val affordable = selection.units.filter { it.cost <= selection.budget }
        require(affordable.isNotEmpty()) { "No affordable units in this catalog" }
        val pattern = when (template) {
            BattleArmyTemplate.BALANCED -> listOf("Spearman", "Archer", "Swordsman", "Horseman")
            BattleArmyTemplate.ARCHERS -> listOf("Archer", "Archer", "Archer", "Spearman")
            BattleArmyTemplate.MILITIA -> listOf("Peasant", "Peasant", "Peasant", "Spearman")
            BattleArmyTemplate.CAVALRY -> listOf("Horseman", "Horseman", "Horseman", "Horseman")
            BattleArmyTemplate.NO_ARCHERS -> listOf("Spearman", "Swordsman", "Horseman", "Peasant")
        }
        val preferred = pattern.mapNotNull { name -> affordable.firstOrNull { it.unitName == name } }
        val candidates = if (random != null) affordable else preferred
        require(candidates.isNotEmpty()) { "This template is unavailable in this catalog" }
        val units = List(selection.maxStacks) { slot ->
            if (random == null) candidates[slot % candidates.size] else candidates[random.nextInt(candidates.size)]
        }
        val weights = List(units.size) { random?.nextInt(1, 5) ?: 1 }
        val totalWeight = weights.sum()
        val counts = units.indices.map { slot ->
            (selection.budget.toLong() * weights[slot] / totalWeight / units[slot].cost).toInt()
        }.toMutableList()
        var remaining = selection.budget.toLong() - units.indices.sumOf {
            counts[it].toLong() * units[it].cost
        }
        // Spend rounding leftovers without iterating once per soldier.
        val order = if (random == null) units.indices.sortedBy { units[it].cost }
            else units.indices.shuffled(random)
        for (slot in order) {
            val extra = remaining / units[slot].cost
            counts[slot] += extra.toInt()
            remaining -= extra * units[slot].cost
        }
        return selection.validate(units.indices.mapNotNull {
            if (counts[it] == 0) null else BattleArmyStack(units[it].unitName, counts[it])
        })
    }
}
