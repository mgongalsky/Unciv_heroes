package com.unciv.pure.domain.battle

import com.unciv.pure.domain.troop.Troop

/** Round-based initiative shared by execution and the presentation forecast. */
class TurnQueue(private val penaltyPerTurn: Int = 1) {
    private val troops = mutableListOf<Troop>()
    private val attackers = mutableSetOf<Troop>()
    private val remaining = mutableListOf<Troop>()
    private var attackerTurns = 0
    private var defenderTurns = 0
    private var active: Troop? = null

    init {
        require(penaltyPerTurn >= 0)
    }

    fun initialize(attackerTroops: List<Troop>, defenderTroops: List<Troop>) {
        troops.clear()
        troops.addAll((attackerTroops + defenderTroops).distinct())
        attackers.clear()
        attackers.addAll(attackerTroops)
        startRound()
        selectNext()
    }

    fun current(): Troop? = active

    /** Completes one activation, including any morale or move-and-shoot follow-ups. */
    fun advance() {
        val completed = active ?: return
        if (completed in attackers) attackerTurns++ else defenderTurns++
        remaining.remove(completed)
        if (remaining.isEmpty()) startRound()
        selectNext()
    }

    /** Removing the active troop completes its activation and selects its successor. */
    fun remove(troop: Troop) {
        if (!troops.remove(troop)) return
        if (active === troop) {
            advance()
        } else {
            remaining.remove(troop)
        }
        attackers.remove(troop)
    }

    /**
     * Forecasts the next [troops] count of activations, beginning with the current one.
     * May cross a round boundary; assumes no future casualties or extra actions.
     * Reads never consume an activation or mutate a troop.
     */
    fun getAll(): List<Troop> {
        val forecast = TurnQueue(penaltyPerTurn)
        forecast.troops.addAll(troops)
        forecast.attackers.addAll(attackers)
        forecast.remaining.addAll(remaining)
        forecast.attackerTurns = attackerTurns
        forecast.defenderTurns = defenderTurns
        forecast.active = active
        return buildList {
            repeat(troops.size) {
                forecast.current()?.let { add(it) }
                forecast.advance()
            }
        }
    }

    fun isEmpty(): Boolean = troops.isEmpty()

    private fun startRound() {
        attackerTurns = 0
        defenderTurns = 0
        remaining.clear()
        remaining.addAll(troops)
    }

    private fun selectNext() {
        val index = InitiativeOrder.nextIndex(
            remaining.map { InitiativeOrder.Entry(it.speed, it in attackers) },
            attackerTurns, defenderTurns, penaltyPerTurn
        )
        active = index?.let { remaining[it] }
    }

    /** Activations before the next round, including the current troop. */
    fun remainingInRound(): Int = remaining.size
}
