package com.unciv.testing.simulations

import kotlin.math.abs

/** Best measured army counts for one directed matchup; does not interpolate untested ratios. */
data class BalanceEquilibrium(
    val closest: BalanceCell?,
    val completedCells: Int,
    val expectedCells: Int
) {
    val complete: Boolean get() = completedCells == expectedCells
    val found: Boolean get() = closest?.shade == 0
    val description: String
        get() = when {
            completedCells == 0 -> "Ожидает расчёта"
            !complete && closest == null -> "Предварительно: нет решающих боёв"
            !complete && !found -> "Предварительно: равновесие пока не найдено"
            !complete -> "Предварительное соотношение"
            closest == null -> "Нет решающих боёв"
            !found -> "Равновесие не найдено"
            else -> "Найдено в диапазоне 45–55%"
        }

    override fun toString(): String {
        val result = closest ?: return "$description\n$completedCells/$expectedCells сочетаний"
        return "${result.attackerAmount}:${result.defenderAmount} · %.1f%% · %d битв\n%s\n%d/%d сочетаний".format(
            result.decisiveRate!! * 100.0, result.simulations,
            description, completedCells, expectedCells
        )
    }

    companion object {
        fun select(cells: Iterable<BalanceCell>, expectedCells: Int): BalanceEquilibrium {
            require(expectedCells > 0)
            val measured = cells.toList()
            require(measured.size <= expectedCells)
            val closest = measured.filter { it.decisiveRate != null }.minWithOrNull(
                Comparator<BalanceCell> { a, b ->
                    val aDecisive = a.attackerWins.toLong() + a.defenderWins
                    val bDecisive = b.attackerWins.toLong() + b.defenderWins
                    // Compare exact fractions so 40% and 60% tie despite floating-point rounding.
                    val distance = (abs(2L * a.attackerWins - aDecisive) * bDecisive)
                        .compareTo(abs(2L * b.attackerWins - bDecisive) * aDecisive)
                    if (distance != 0) distance else {
                        val amount = (a.attackerAmount + a.defenderAmount)
                            .compareTo(b.attackerAmount + b.defenderAmount)
                        if (amount != 0) amount else a.attackerAmount.compareTo(b.attackerAmount)
                    }
                }
            )
            return BalanceEquilibrium(closest, measured.size, expectedCells)
        }
    }
}
