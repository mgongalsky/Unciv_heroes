package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath


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

    /**
     * Checks if the target position is occupied by an allied troop.
     *
     * @param troop The troop attempting to move.
     * @param targetPosition The position to check.
     * @return True if the position is occupied by an allied troop, false otherwise.
     */
    fun isHexOccupiedByAlly(troop: TroopInfo, targetPosition: Vector2): Boolean {
        // Get all allied troops
        val alliedTroops = if (attackerArmy.contains(troop)) {
            attackerArmy.getAllTroops()
        } else {
            defenderArmy.getAllTroops()
        }

        // Check if any allied troop occupies the target position
        return alliedTroops.any { alliedTroop ->
            alliedTroop != null && alliedTroop != troop && alliedTroop.position == targetPosition
        }
    }

    /**
     * Checks if the target position is occupied by an enemy troop.
     *
     * @param troop The troop attempting to move.
     * @param targetPosition The position to check.
     * @return True if the position is occupied by an enemy troop, false otherwise.
     */
    fun isHexOccupiedByEnemy(troop: TroopInfo, targetPosition: Vector2): Boolean {
        // Get all enemy troops
        val enemyTroops = if (attackerArmy.contains(troop)) {
            defenderArmy.getAllTroops()
        } else {
            attackerArmy.getAllTroops()
        }

        // Check if any enemy troop occupies the target position
        return enemyTroops.any { enemyTroop ->
            enemyTroop != null && enemyTroop.position == targetPosition
        }
    }


    /**
     * Checks if hex is free.
     */
    fun isHexFree(targetPosition: Vector2) = turnQueue.none { it.position == targetPosition }



    /**
     * Checks if the target position is achievable by the given troop.
     *
     * @param troop The troop attempting to move.
     * @param targetPosition The target position to check.
     * @return True if the target position is within movement range and on the battlefield, false otherwise.
     */
    fun isHexAchievable(troop: TroopInfo, targetPosition: Vector2): Boolean {
        // Check if the target position is within the troop's movement range
        val distance = HexMath.getDistance(troop.position, targetPosition)
        if (distance > troop.baseUnit.speed) {
            println("Target position is too far: distance=$distance, speed=${troop.baseUnit.speed}")
            return false
        }

        // Check if the target position is on the battlefield
        if (!isHexOnBattleField(targetPosition)) {
            println("Target position $targetPosition is outside the battlefield.")
            return false
        }

        // If all checks pass, the hex is achievable
        return true
    }

    /**
     * Checks if the specified hex is inside the battlefield boundaries.
     *
     * @param positionHex The position in hexagonal coordinates to check.
     * @return True if the position is within the predefined battlefield bounds, false otherwise.
     */
    fun isHexOnBattleField(positionHex: Vector2): Boolean {
        // Convert hexagonal coordinates to offset coordinates (even-q)
        val positionOffset = HexMath.hex2EvenQCoords(positionHex)

        // Hardcoded battlefield dimensions (same as in the old version)
        val minX = -7f
        val maxX = 6f
        val minY = -4f
        val maxY = 3f

        // Check if the position is within bounds
        val isWithinBounds = positionOffset.x in minX..maxX && positionOffset.y in minY..maxY

        if (!isWithinBounds) {
            println("Position $positionOffset is outside the hardcoded battlefield bounds.")
        }

        return isWithinBounds
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
     * Checks if the given troop can shoot.
     *
     * @param troop The troop to check.
     * @return True if the troop can shoot, false otherwise.
     */
    fun canShoot(troop: TroopInfo): Boolean {
        return troop.baseUnit.rangedStrength != 0
    }


    /**
     * Returns the current turn queue (for debugging or visualization).
     */
    fun getTurnQueue(): List<TroopInfo> {
        return turnQueue
    }
}
