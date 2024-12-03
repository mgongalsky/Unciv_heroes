package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2


/** Logical part of a battle. No visual part here. */
class NewBattleManager(
    private var attackerArmy: ArmyInfo,
    private var defenderArmy: ArmyInfo
) {
    private val turnQueue: MutableList<TroopInfo> = mutableListOf() // Queue of troops for turn order
    private var currentTurnIndex: Int = 0 // Index of the current troop's turn

    /**
     * Initializes the turn queue based on troop speed and priority rules.
     * Attacker troops have priority in case of equal speed.
     */
    fun initializeTurnQueue() {
        val allTroops = mutableListOf<TroopInfo>()

        // Collect all troops from both armies
        allTroops.addAll(attackerArmy.getAllTroops().filterNotNull())
        allTroops.addAll(defenderArmy.getAllTroops().filterNotNull())

        // Sort troops by speed (descending). Attacker troops have priority for equal speed.
        turnQueue.clear()
        turnQueue.addAll(
            allTroops.sortedWith(
                compareByDescending<TroopInfo> { it.baseUnit.speed }
                    .thenByDescending { attackerArmy.getAllTroops().contains(it) }
            )
        )

        // Set the first troop as the current turn
        currentTurnIndex = 0
    }

    /**
     * Returns the troop currently taking its turn.
     */
    fun getCurrentTroop(): TroopInfo {
        if (turnQueue.isEmpty()) {
            finishBattle()
            throw IllegalStateException("Battle has ended. No troops left in the queue.")
        }
        return turnQueue[currentTurnIndex]
    }

    private fun finishBattle() {
        // Add logic to clean up battle state or notify the screen to close.
    }

    fun performTurn(actionRequest: BattleActionRequest): BattleActionResult {
        val troop = actionRequest.troop
        val targetPosition = actionRequest.targetPosition

        when (actionRequest.actionType) {
            ActionType.MOVE -> {
                if (!isHexAchievable(troop, targetPosition)) {
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.TOO_FAR
                    )
                }
                if (isHexOccupiedByAlly(troop, targetPosition)) {
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.OCCUPIED_BY_ALLY
                    )
                }
                // Успешное перемещение
                val oldPosition = troop.position
                troop.position = targetPosition
                println("Manager moves to position: (${targetPosition.x}, ${targetPosition.y})")

                return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = true,
                    movedFrom = oldPosition,
                    movedTo = targetPosition
                )
            }

            ActionType.ATTACK -> {
                // Здесь позже добавим логику атаки
                return BattleActionResult(
                    actionType = ActionType.ATTACK,
                    success = false,
                    errorId = ErrorId.NOT_IMPLEMENTED
                )
            }
        }
    }

    // Проверка, идет ли еще битва
    fun isBattleOn(): Boolean {
        val attackerHasTroops = attackerArmy.getAllTroops().any { it?.currentAmount ?: 0 > 0 }
        val defenderHasTroops = defenderArmy.getAllTroops().any { it?.currentAmount ?: 0 > 0 }

        return attackerHasTroops && defenderHasTroops
    }

    private fun isHexOccupiedByAlly(troop: TroopInfo, targetPosition: Vector2): Boolean {
        return false
    }

    private fun isHexAchievable(troop: TroopInfo, targetPosition: Vector2): Boolean {
        return true
    }


    /**
     * Advances the turn to the next troop in the queue.
     * If the end of the queue is reached, it loops back to the start.
     */
    fun advanceTurn() {
        if (turnQueue.isNotEmpty()) {
            currentTurnIndex = (currentTurnIndex + 1) % turnQueue.size
        }
    }

    /**
     * Returns the current turn queue (for debugging or visualization).
     */
    fun getTurnQueue(): List<TroopInfo> {
        return turnQueue
    }
}
