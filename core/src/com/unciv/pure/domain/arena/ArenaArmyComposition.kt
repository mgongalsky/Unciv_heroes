package com.unciv.pure.domain.arena

/** One occupied deployment slot. */
data class ArenaStack(val unitName: String, val count: Int) {
    init {
        require(unitName.isNotBlank())
        require(count > 0)
    }
}

/** Total fighters of one kind and the slots reserved for them. */
data class ArenaUnitGroup(val unitName: String, val count: Int, val slots: Int) {
    init {
        require(unitName.isNotBlank())
        require(count > 0)
        require(slots in 1..5)
    }
}

/** Input shared by live army creation and simulations. */
class ArenaArmyComposition(groups: List<ArenaUnitGroup>) {
    val groups: List<ArenaUnitGroup> = groups.toList()

    init {
        require(this.groups.isNotEmpty())
        require(this.groups.map { it.unitName }.distinct().size == this.groups.size)
        require(this.groups.sumOf { it.slots.toLong() } <= 5L)
    }

    val isMixed: Boolean get() = groups.size > 1

    /** Round once per kind before splitting: extra slots grant no extra fighters. */
    fun stacks(bonusPercent: Int = 0): List<ArenaStack> {
        require(bonusPercent in 0..1000)
        return groups.flatMap { group ->
            val total = (group.count.toLong() * (100L + bonusPercent) + 99L) / 100L
            require(total <= Int.MAX_VALUE) { "Arena troop bonus overflows ${group.unitName}" }
            ArenaArmyDistribution.split(total.toInt(), group.slots).map {
                ArenaStack(group.unitName, it)
            }
        }
    }
}

/** Base strengths before applying the tier bonus. */
data class ArenaMixedMatchup(
    val id: String,
    val player: ArenaArmyComposition,
    val opponent: ArenaArmyComposition
) {
    init {
        require(id.isNotBlank())
        require(player.isMixed && opponent.isMixed)
    }
}
