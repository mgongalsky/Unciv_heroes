// com/unciv/pure/domain/battle/TurnQueue.kt
package com.unciv.pure.domain.battle

import com.unciv.pure.domain.troop.Troop

class TurnQueue {
    private val queue: MutableList<Troop> = mutableListOf()
    private var currentIndex: Int = 0

    fun initialize(attackerTroops: List<Troop>, defenderTroops: List<Troop>) {
        val allTroops = mutableListOf<Troop>()
        allTroops.addAll(attackerTroops)
        allTroops.addAll(defenderTroops)

        queue.clear()
        queue.addAll(
            allTroops.sortedWith(
                compareByDescending<Troop> { it.speed }
                    .thenByDescending { attackerTroops.contains(it) }
            )
        )

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

    fun getAll(): List<Troop> = queue.toList()

    fun isEmpty(): Boolean = queue.isEmpty()
}
