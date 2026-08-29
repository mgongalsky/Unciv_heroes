// com/unciv/pure/domain/battle/TurnQueue.kt
package com.unciv.pure.domain.battle

import com.unciv.pure.domain.troop.Troop

class TurnQueue {
    private val queue: MutableList<Troop> = mutableListOf()
    private var currentIndex: Int = 0

    fun initialize(attackerTroops: List<Troop>, defenderTroops: List<Troop>) {
        val attackers = attackerTroops.toSet()
        val defenders = defenderTroops.toSet()
        val troopsBySpeed = (attackerTroops + defenderTroops)
            .groupBy { it.speed }
            .toSortedMap(compareByDescending { it })

        queue.clear()
        troopsBySpeed.values.forEach { sameSpeedTroops ->
            val sameSpeedAttackers = sameSpeedTroops.filter { it in attackers }
            val sameSpeedDefenders = sameSpeedTroops.filter { it in defenders }
            val pairedCount = minOf(sameSpeedAttackers.size, sameSpeedDefenders.size)

            repeat(pairedCount) { index ->
                queue += sameSpeedAttackers[index]
                queue += sameSpeedDefenders[index]
            }
            queue += sameSpeedAttackers.drop(pairedCount)
            queue += sameSpeedDefenders.drop(pairedCount)
        }

        currentIndex = 0
    }

    fun current(): Troop? {
        if (queue.isEmpty()) return null
        if (currentIndex >= queue.size) currentIndex = queue.size - 1
        return queue[currentIndex]
    }

    fun advance() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
    }

    fun remove(troop: Troop) {
        val index = queue.indexOf(troop)
        if (index != -1) {
            queue.removeAt(index)
            if (index < currentIndex) currentIndex--
            if (currentIndex >= queue.size) currentIndex = 0
        }
    }

    fun getAll(): List<Troop> {
        if (queue.isEmpty()) return emptyList()
        return queue.drop(currentIndex) + queue.take(currentIndex)
    }

    fun isEmpty(): Boolean = queue.isEmpty()
}
