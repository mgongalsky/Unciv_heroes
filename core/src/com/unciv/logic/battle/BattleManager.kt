package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import kotlin.random.Random


/** Logical part of a battle. No visual part here. */
class BattleManager(
    private var attackerArmy: ArmyInfo,
    private var defenderArmy: ArmyInfo
) {
    private val turnQueue: MutableList<TroopInfo> = mutableListOf() // Queue of troops for turn order
    private var currentTurnIndex: Int = 0 // Index of the current troop's turn


    companion object {
        const val LUCK_PROBABILITY = 0.15
        const val MORALE_PROBABILITY = 0.15
    }

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
    fun getCurrentTroop(): TroopInfo? {
        if (turnQueue.isEmpty()) {
            println("Warning: turnQueue is empty. No troops left to process. Battle likely ended.")
            return null // Возвращаем null, если очередь пуста
        }

        // Проверяем, находится ли currentTurnIndex в допустимых границах
        if (currentTurnIndex >= turnQueue.size) {
            println("Warning: currentTurnIndex ($currentTurnIndex) is out of bounds. Adjusting to last valid index (${turnQueue.size - 1}).")
            currentTurnIndex = turnQueue.size - 1 // Корректируем индекс, чтобы он не выходил за границы
        }

        return turnQueue[currentTurnIndex]
    }

    private fun finishBattle() {
        // Add logic to clean up battle state or notify the screen to close.
    }


    /**
     * Возвращает юнит, находящийся на заданной позиции, или null, если клетка пуста.
     *
     * @param positionHex Позиция в гексагональных координатах.
     * @return Найденный юнит [TroopInfo] или null, если юнита нет.
     */
    fun getTroopOnHex(positionHex: Vector2): TroopInfo? {
        return turnQueue.find { it.position == positionHex }
    }

    private val verboseAttack = true // Флаг для включения/выключения вербозинга атак

    fun performTurn(actionRequest: BattleActionRequest): BattleActionResult {
        val troop = actionRequest.troop
        val targetPosition = actionRequest.targetPosition

        val isMorale = (Random.nextDouble() < MORALE_PROBABILITY)
        if (verboseAttack && isMorale) println("Troop ${troop.baseUnit.name} has morale")


        when (actionRequest.actionType) {
            ActionType.MOVE -> {
                if (!isHexAchievable(troop, targetPosition)) {
                    if (verboseAttack) println("Hex not achievable for troop at position $targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.TOO_FAR
                    )
                }
                if (isHexOccupiedByAlly(troop, targetPosition)) {
                    if (verboseAttack) println("Hex occupied by ally at position $targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.OCCUPIED_BY_ALLY
                    )
                }
                if (!isHexFree(targetPosition)) {
                    if (verboseAttack) println("Hex occupied by another troop $targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.HEX_OCCUPIED
                    )
                }


                // Успешное перемещение
                val oldPosition = troop.position
                troop.position = targetPosition
                if (verboseAttack) println("Troop moved from $oldPosition to $targetPosition")

                return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = true,
                    movedFrom = oldPosition,
                    movedTo = targetPosition,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.ATTACK -> {
                val direction = actionRequest.direction
                    ?: return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )

                if (verboseAttack) println("Attempting attack with direction $direction")

                val defender = getTroopOnHex(targetPosition)
                    ?: return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )

                if (!isHexOccupiedByEnemy(troop, targetPosition)) {
                    if (verboseAttack) println("No enemy found at $targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                val attackPosition = HexMath.oneStepTowards(targetPosition, direction)
                // If troop:
                // 1. Cannot achieve hex for attack
                // 2. That hex is occupied, but not by that troop
                if (!isHexAchievable(troop, attackPosition) || (!isHexFree(attackPosition) && troop.position != attackPosition)) {
                    if (verboseAttack) {
                        println("Attack position $attackPosition not achievable or not free")
                    }
                    return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                // Перемещаем атакующего юнита в позицию атаки
                val oldPosition = troop.position
                troop.position = attackPosition
                if (verboseAttack) println("Troop moved to attack position $attackPosition")

                // Выполняем атаку
                val isLuck = attack(defender, troop)

                if (verboseAttack) println("Attack performed on defender at $targetPosition")

                //if (defender.currentAmount <= 0) {
                //    removeTroop(defender)
                //    if (verboseAttack) {
                //        println("Defender defeated at $targetPosition")
                //    }
                //}

                return BattleActionResult(
                    actionType = ActionType.ATTACK,
                    success = true,
                    movedFrom = oldPosition,
                    movedTo = attackPosition,
                    isLuck = isLuck,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.SHOOT -> {
                if (verboseAttack) println("Starting ActionType.SHOOT for troop: ${troop.unitName} at position: ${troop.position}")

                // Получаем защищающегося
                val defender = getTroopOnHex(targetPosition)
                    ?: return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    ).also {
                        if (verboseAttack) println("Error: No troop found on target position: $targetPosition")
                    }

                // Проверяем возможность стрельбы
                if (!canShoot(troop)) {
                    if (verboseAttack) println("Error: Troop ${troop.unitName} cannot shoot")
                    return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.NOT_IMPLEMENTED
                    )
                }

                if (!isHexOccupiedByEnemy(troop, targetPosition)) {
                    if (verboseAttack) println("Error: Target position $targetPosition is not occupied by an enemy")
                    return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                if (verboseAttack) println("Troop ${troop.unitName} is shooting at target: ${defender.unitName} on position: ${targetPosition}")

                // Выполняем стрельбу
                val isLuck = attack(defender, troop) // Используем ту же логику атаки

                // Проверяем, уничтожен ли юнит
                if (defender.currentAmount <= 0) {
                    if (verboseAttack) println("Defender ${defender.unitName} at position $targetPosition defeated.")
                    removeTroop(defender)
                } else {
                    if (verboseAttack) println("Defender ${defender.unitName} survived with ${defender.currentAmount} units.")
                }

                return BattleActionResult(
                    actionType = ActionType.SHOOT,
                    success = true,
                    movedFrom = null,
                    movedTo = null,
                    isLuck = isLuck,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                ).also {
                    if (verboseAttack) println("ActionType.SHOOT completed successfully for troop: ${troop.unitName}")
                }
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


    fun getEnemies(troop: TroopInfo): List<TroopInfo> {
        return if (attackerArmy.contains(troop)) {
            defenderArmy.getAllTroops().filterNotNull().toList()
        } else if (defenderArmy.contains(troop)) {
            attackerArmy.getAllTroops().filterNotNull().toList()
        } else {
            emptyList() // Пустой список напрямую
        }
    }


    /**
     * Returns a list of reachable tiles for the given troop based on its speed and current position.
     *
     * @param troop The troop for which to calculate reachable tiles.
     * @return A list of reachable tiles as Vector2.
     */
    /**
     * Returns a list of reachable tiles for the given troop by checking all battlefield tiles.
     *
     * @param troop The troop for which to calculate reachable tiles.
     * @return A list of reachable tiles as Vector2.
     */
    fun getReachableTiles(troop: TroopInfo): List<Vector2> {
        val reachableTiles = mutableListOf<Vector2>()

        // Перебираем все клетки на поле боя
        for (x in -7..6) {  // X-координаты поля
            for (y in -4..3) { // Y-координаты поля
                val tilePosition = HexMath.evenQ2HexCoords(Vector2(x.toFloat(), y.toFloat()))
                if (isHexAchievable(troop, tilePosition) && isHexFree(tilePosition)) {
                    reachableTiles.add(tilePosition)
                }
            }
        }

        return reachableTiles
    }


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
            //println("Target position is too far: distance=$distance, speed=${troop.baseUnit.speed}")
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

    fun getAttackerArmy() =  attackerArmy
    fun getDefenderArmy() =  defenderArmy

    /**
     * Handles an attack by the current troop on a specified hex position.
     *
     * @param defenderHex The position of the defending troop.
     * @param attacker The attacking troop. Defaults to the current troop.
     */
    /*
    fun attack(defenderHex: Vector2, attacker: TroopInfo = manager.getCurrentTroop()) {
        val defender = manager.getTroopAt(defenderHex)
        if (defender != null) {
            attack(defender, attacker)
        } else {
            println("Error: No troop found at position $defenderHex.")
        }
    }

     */

    /**
     * Executes an attack by one troop on another troop.
     *
     * @param defender The defending troop.
     * @param attacker The attacking troop. Defaults to the current troop.
     */
    fun attack(defender: TroopInfo, attacker: TroopInfo? = getCurrentTroop()) : Boolean{
        var isLuck = false
        if (attacker == null){

            return isLuck
        }
        if (verboseAttack) {
            println("Starting attack: ${attacker.unitName} (Position: ${attacker.position}) attacking ${defender.unitName} (Position: ${defender.position})")
            println("Initial attacker amount: ${attacker.currentAmount}, Initial defender amount: ${defender.currentAmount}")
        }

        // Calculate maximum damage
        var damage = attacker.currentAmount * attacker.baseUnit.damage

        if(Random.nextDouble() < LUCK_PROBABILITY) {
            damage *= 2
            isLuck = true
            if (verboseAttack) println("Troop ${attacker.baseUnit.name} has luck")
        }


            if (verboseAttack) println("Base damage calculated: $damage")

        // Include health lack in the calculation
        val healthLack = defender.baseUnit.health - defender.currentHealth
        if (verboseAttack) println("Defender health lack: $healthLack")

        val totalDamage = damage + healthLack
        if (verboseAttack) println("Total damage after health lack adjustment: $totalDamage")

        // Calculate the number of perished units
        val perished = (totalDamage / defender.baseUnit.health).toInt()
        defender.currentAmount -= perished
        defender.currentHealth = defender.baseUnit.health - (totalDamage % defender.baseUnit.health)

        if (verboseAttack) {
            println("Perished units: $perished")
            println("Defender remaining amount: ${defender.currentAmount}, Remaining health: ${defender.currentHealth}")
        }

        if (defender.currentAmount <= 0) {
            defender.currentAmount = 0
            if (verboseAttack) println("Defender ${defender.unitName} at position ${defender.position} has been defeated.")
            //defender.perish()
            perishTroop(defender)
        }

        if (verboseAttack) println("Attack complete: ${attacker.unitName} caused $damage damage to ${defender.unitName}.")
        return isLuck
    }

    /**
     * Handles the removal of a perished troop.
     *
     * @param troop The troop to remove.
     */
    fun perishTroop(troop: TroopInfo) {
        //troop.perish() // Trigger any visual or logical effects for troop death
        removeTroop(troop)
        println("Troop ${troop.baseUnit.name} has perished.")
    }



    /**
     * Removes a troop from the battle.
     *
     * @param troop The troop to remove.
     */
    fun removeTroop(troop: TroopInfo) {
        // Удаляем из очереди хода
        turnQueue.remove(troop)

        // Удаляем из армии атакующих или защищающихся
        if (attackerArmy.contains(troop)) {
            attackerArmy.removeTroop(troop)
        } else if (defenderArmy.contains(troop)) {
            defenderArmy.removeTroop(troop)
        }

        // TODO: here we need to handle the situation when current troop is killed (weird)
        // Обновляем итератор очереди
        //if (getCurrentTroop() == troop) {
        //    advanceTurn() // Переход к следующему юниту
        //}

        println("Troop ${troop.baseUnit.name} removed from the battle.")
    }


    /**
     * Advances the turn to the next troop in the queue.
     * If the end of the queue is reached, it loops back to the start.
     */
    fun advanceTurn() {
        //if (turnQueue.isNotEmpty()) {
        //    currentTurnIndex = (currentTurnIndex + 1) % turnQueue.size
       // }

        if (turnQueue.isEmpty()) {
            println("The turn queue is empty. Ending battle...")
            finishBattle()
            return
        }

        currentTurnIndex = (currentTurnIndex + 1) % turnQueue.size
        //currentTroop = turnQueue[currentTroopIndex]
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
