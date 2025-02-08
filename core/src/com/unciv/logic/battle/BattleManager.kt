package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.models.GameConstants
import kotlin.random.Random

/**
 * Handles the logical part of a battle between two armies.
 * Does not include visual representation or UI logic.
 *
 * @property attackerArmy The attacking army participating in the battle.
 * @property defenderArmy The defending army participating in the battle.
 */
class BattleManager(
    private var attackerArmy: ArmyInfo,
    private var defenderArmy: ArmyInfo,
    val battleField: TileMap // BattleField on use
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
     *
     * @return The current troop or `null` if the queue is empty.
     */
    fun getCurrentTroop(): TroopInfo? {
        if (turnQueue.isEmpty()) {
            println("Warning: turnQueue is empty. No troops left to process. Battle likely ended.")
            return null
        }

        // Ensure currentTurnIndex is within bounds
        if (currentTurnIndex >= turnQueue.size) {
            println("Warning: currentTurnIndex ($currentTurnIndex) is out of bounds. Adjusting to last valid index (${turnQueue.size - 1}).")
            currentTurnIndex = turnQueue.size - 1
        }

        return turnQueue[currentTurnIndex]
    }

    /**
     * Determines whether the morale bonus is triggered for a troop.
     *
     * Rule:
     * - If troopMorale is less than or equal to 3, the effective probability is calculated as:
     *      (GameConstants.moraleProbability / 3) * troopMorale
     * - If troopMorale is greater than 3, the effective probability is set to GameConstants.moraleProbability.
     *
     * @param troopMorale The morale value of the troop.
     * @return True if the morale bonus is triggered, false otherwise.
     */
    private fun isMoraleTriggered(troop: TroopInfo): Boolean {
        val troopMorale = troop.getHeroMorale()
        val effectiveProbability = if (troopMorale <= 3) {
            (GameConstants.moraleProbability / 3.0) * troopMorale
        } else {
            GameConstants.moraleProbability
        }

        // Verbose logging: output unit name, amount, hero presence, hero morale, and effective probability
        println(
            "Unit: ${troop.unitName}, " +
                    "Amount: ${troop.amount}, " +
                    "Hero present: ${troop.hasHero()}, " +
                    "Hero morale: $troopMorale, " +
                    "Effective morale probability: $effectiveProbability"
        )

        return Random.nextDouble() < effectiveProbability
    }

    /**
     * Determines whether the luck bonus is triggered for a troop.
     * Luck bonus doubles the damage.
     *
     * Rule:
     * - If hero's luck (troopLuck) is less than or equal to 3, effective probability = (GameConstants.luckProbability / 3) * troopLuck.
     * - Otherwise, effective probability = GameConstants.luckProbability.
     *
     * Verbose logging outputs unit name, amount, hero presence, hero luck, and effective probability.
     *
     * @param troop The troop to check.
     * @return True if the luck bonus is triggered, false otherwise.
     */
    private fun isLuckTriggered(troop: TroopInfo): Boolean {
        // Assume hero's luck value is stored in hero.luck; if no hero, default to 1.
        val troopLuck = troop.hero?.luck ?: 1
        val effectiveProbability = if (troopLuck <= 3) {
            (GameConstants.luckProbability / 3.0) * troopLuck
        } else {
            GameConstants.luckProbability
        }
        println(
            "Unit: ${troop.unitName}, Amount: ${troop.amount}, " +
                    "Hero present: ${troop.hasHero()}, Hero luck: $troopLuck, " +
                    "Effective luck probability: $effectiveProbability"
        )
        return Random.nextDouble() < effectiveProbability
    }


    /**
     * Finishes the battle and cleans up resources or state.
     * Placeholder for additional logic.
     */
    fun finishBattle() {
        // Add logic to clean up battle state or notify the screen to close.
        attackerArmy.finishBattle()
        defenderArmy.finishBattle()
    }

    /**
     * Determines the result of the battle.
     *
     * @return A [BattleTotalResult] indicating the winning army or `null` if the battle is ongoing.
     */
    fun getBattleResult(): BattleTotalResult? {
        val remainingAttackers = attackerArmy.getAllTroops().filterNotNull().filter { it.currentAmount > 0 }
        val remainingDefenders = defenderArmy.getAllTroops().filterNotNull().filter { it.currentAmount > 0 }

        return when {
            remainingAttackers.isNotEmpty() && remainingDefenders.isEmpty() -> BattleTotalResult(attackerArmy)
            remainingDefenders.isNotEmpty() && remainingAttackers.isEmpty() -> BattleTotalResult(defenderArmy)
            else -> null
        }
    }

    //fun getTroopOnHex() {}
    /**
     * Finds the troop located at the specified tile.
     *
     * @param tile The tile to check.
     * @return The [TroopInfo] located at the tile or `null` if the tile is empty.
     */
    private fun getTroopOnTile(tile: TileInfo): TroopInfo? {
        return tile.troopUnit  // Теперь получаем юнита напрямую из клетки
    }


    private val verboseAttack = true // Флаг для включения/выключения вербозинга атак

    /**
     * Processes a turn for the current troop based on the given action request.
     *
     * @param actionRequest The requested action to perform.
     * @return The result of the action as a [BattleActionResult].
     */
    fun performTurn(actionRequest: BattleActionRequest): BattleActionResult {
        val troop = actionRequest.troop
        //val targetPosition = actionRequest.targetPosition

        // TODO: remove +1 when actual morale bonus for the same castle is introduced
        val isMorale = isMoraleTriggered(troop) // Add here +1 to morale just because all troops are from the same castle now )
        if (verboseAttack && isMorale) println("Troop ${troop.baseUnit.name} has morale")

        when (actionRequest.actionType) {
            ActionType.SKIP -> {
                return BattleActionResult(
                    actionType = ActionType.SKIP,
                    success = true,
                    movedFrom = troop.currentTile,
                    movedTo = troop.currentTile,
                    isMorale = false,
                    battleEnded = !isBattleOn()
                )
            }
            ActionType.MOVE -> {
                if (!isTileAchievable(troop, actionRequest.targetPosition)) {
                    if (verboseAttack) println("Hex not achievable for troop at position $actionRequest.targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.TOO_FAR
                    )
                }
                if (isTileOccupiedByAlly(troop, actionRequest.targetPosition)) {
                    if (verboseAttack) println("Hex occupied by ally at position $actionRequest.targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.OCCUPIED_BY_ALLY
                    )
                }
                if (!isTileFree(actionRequest.targetPosition)) {
                    if (verboseAttack) println("Hex occupied by another troop $actionRequest.targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.MOVE,
                        success = false,
                        errorId = ErrorId.HEX_OCCUPIED
                    )
                }

                // Successful movement
                val oldTile = troop.currentTile
                //troop.moveToPosition(targetPosition)
                troop.moveToTile(actionRequest.targetPosition)

                if (verboseAttack) println("Troop moved from $oldTile to $actionRequest.targetPosition")

                return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = true,
                    movedFrom = oldTile,
                    movedTo = actionRequest.targetPosition,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.ATTACK -> {
                /*
                val direction = actionRequest.direction
                    ?: return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )

                 */

                //if (verboseAttack) println("Attempting attack with direction $direction")

                val defender = getTroopOnTile(actionRequest.targetPosition)
                    ?: return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )

                if (!isTileOccupiedByEnemy(troop, actionRequest.targetPosition)) {
                    if (verboseAttack) println("No enemy found at $actionRequest.targetPosition")
                    return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                //val attackPosition = HexMath.oneStepTowards(targetPosition, direction)
                //val attackTile = battleField.getNeighborTile(actionRequest.targetPosition, direction)

                if (actionRequest.attackTile == null) {
                    if (verboseAttack) println("Invalid attackTile")
                    return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                // If troop:
                // 1. Cannot achieve hex for attack
                // 2. That hex is occupied, but not by that troop
                if (!isTileAchievable(
                            troop,
                            actionRequest.attackTile
                        ) || (!isTileFree(actionRequest.attackTile) && troop.currentTile != actionRequest.attackTile)
                ) {
                    if (verboseAttack) {
                        println("Attack position $actionRequest.attackTile not achievable or not free")
                    }
                    return BattleActionResult(
                        actionType = ActionType.ATTACK,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                // Move attacker to attack position
                val oldTile = troop.currentTile
                troop.moveToTile(actionRequest.attackTile)

                if (verboseAttack) println("Troop moved to attack position $actionRequest.attackTile")

                // Perform attack
                val isLuck = attack(defender, troop)

                if (verboseAttack) println("Attack performed on defender at $actionRequest.targetPosition")

                return BattleActionResult(
                    actionType = ActionType.ATTACK,
                    success = true,
                    movedFrom = oldTile,
                    movedTo = actionRequest.attackTile,
                    isLuck = isLuck,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.SHOOT -> {
                if (verboseAttack) println("Starting ActionType.SHOOT for troop: ${troop.unitName} at position: ${troop.currentTile.position}")

                // Get defender
                val defender = getTroopOnTile(actionRequest.targetPosition)
                    ?: return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    ).also {
                        if (verboseAttack) println("Error: No troop found on target position: $actionRequest.targetPosition")
                    }

                // Check shooting capability
                if (!canShoot(troop)) {
                    if (verboseAttack) println("Error: Troop ${troop.unitName} cannot shoot")
                    return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.NOT_IMPLEMENTED
                    )
                }

                if (!isTileOccupiedByEnemy(troop, actionRequest.targetPosition)) {
                    if (verboseAttack) println("Error: Target position $actionRequest.targetPosition is not occupied by an enemy")
                    return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                if (verboseAttack) println("Troop ${troop.unitName} is shooting at target: ${defender.unitName} on position: $actionRequest.targetPosition")

                // Perform shooting
                val isLuck = attack(defender, troop) // Use the same attack logic

                // Check if defender is defeated
                if (defender.currentAmount <= 0) {
                    if (verboseAttack) println("Defender ${defender.unitName} at position $actionRequest.targetPosition defeated.")
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

    /**
     * Checks if the battle is still ongoing.
     *
     * @return True if both armies have surviving troops, false otherwise.
     */
    fun isBattleOn(): Boolean {
        val attackerHasTroops = attackerArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 }
        val defenderHasTroops = defenderArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 }

        return attackerHasTroops && defenderHasTroops
    }

    /**
     * Checks if the target tile is occupied by an allied troop.
     *
     * @param troop The troop attempting to move.
     * @param targetTile The tile to check.
     * @return True if the tile is occupied by an allied troop, false otherwise.
     */
    fun isTileOccupiedByAlly(troop: TroopInfo, targetTile: TileInfo): Boolean {
        val targetTroop = targetTile.troopUnit ?: return false  // Если клетка пустая, значит не занята союзником

        // Определяем, к какой армии относится юнит
        val isAlly = if (attackerArmy.contains(troop)) {
            attackerArmy.contains(targetTroop)
        } else {
            defenderArmy.contains(targetTroop)
        }

        return isAlly && targetTroop != troop  // Союзный, но не сам себе союзник
    }

/*
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

 */

    /**
     * Checks if the target tile is occupied by an enemy troop.
     *
     * @param troop The troop attempting to move.
     * @param targetTile The tile to check.
     * @return True if the tile is occupied by an enemy troop, false otherwise.
     */
    fun isTileOccupiedByEnemy(troop: TroopInfo, targetTile: TileInfo): Boolean {
        val targetTroop = targetTile.troopUnit ?: return false  // Если клетка пустая, значит нет врага

        // Определяем, к какой армии относится юнит и является ли цель врагом
        return if (attackerArmy.contains(troop)) {
            defenderArmy.contains(targetTroop)  // Если юнит из атакующей армии, то ищем врага в защитниках
        } else {
            attackerArmy.contains(targetTroop)  // И наоборот
        }
    }


    /**
     * Checks if a hex is free of any troop.
     *
     * @param targetPosition The position to check.
     * @return True if the position is free, false otherwise.
     */
    //fun isHexFree(targetPosition: Vector2) = turnQueue.none { it.position == targetPosition }

    /**
     * Checks if a tile is free of any troop.
     *
     * @param targetTile The tile to check.
     * @return True if the tile is free, false otherwise.
     */
    fun isTileFree(targetTile: TileInfo): Boolean {
        return targetTile.troopUnit == null  // Если юнита нет, клетка свободна
    }


    /**
     * Returns a list of enemies for the given troop.
     *
     * @param troop The troop for which to get enemies.
     * @return A list of enemy troops.
     */
    fun getEnemies(troop: TroopInfo): List<TroopInfo> {
        return if (attackerArmy.contains(troop)) {
            defenderArmy.getAllTroops().filterNotNull().toList()
        } else if (defenderArmy.contains(troop)) {
            attackerArmy.getAllTroops().filterNotNull().toList()
        } else {
            emptyList() // No enemies
        }
    }



    /**
     * Returns a list of reachable tiles for the given troop.
     *
     * @param troop The troop for which to calculate reachable tiles.
     * @return A list of reachable tiles as Vector2.
     */
    /**
     * Получает список клеток, доступных для перемещения отряда в текущем ходу.
     *
     * @param troop Отряд, для которого определяется доступность клеток.
     * @return Список доступных клеток (`TileInfo`).
     */
    fun getReachableTiles(troop: TroopInfo): List<TileInfo> {
        return troop.movement.getReachableTilesInCurrentTurn().toList()
    }

    /*
    fun getReachableTiles(troop: TroopInfo): List<Vector2> {
        return troop.movement.getReachableTilesInCurrentTurn().map { it.position }.toList()

        val reachableTiles = mutableListOf<Vector2>()

        // Iterate through all battlefield tiles
        for (x in -7..6) {  // X-coordinates of the battlefield
            for (y in -4..3) { // Y-coordinates of the battlefield
                val tilePosition = HexMath.evenQ2HexCoords(Vector2(x.toFloat(), y.toFloat()))
               // if (isTileAchievable(troop, tilePosition) && isHexFree(tilePosition)) {
               //     reachableTiles.add(tilePosition)
               // }
            }
        }

        return reachableTiles


    }

     */

    /**
     * Проверяет, достижима ли целевая клетка для данного отряда.
     *
     * @param troop Отряд, совершающий перемещение.
     * @param targetTile Целевая клетка.
     * @return `true`, если клетка достижима юнитом в этот ход, иначе `false`.
     */
    fun isTileAchievable(troop: TroopInfo, targetTile: TileInfo): Boolean {
        // Проверяем, находится ли клетка на поле битвы
        if (!battleField.contains(targetTile)) {
            if (verboseAttack) println("Target tile ${targetTile.position} is outside the battlefield.")
            return false
        }

        // Проверяем, может ли юнит дойти до клетки в текущем ходу
        val reachableTiles = troop.movement.getReachableTilesInCurrentTurn(targetTile = targetTile)
        if (!reachableTiles.contains(targetTile)) {
            if (verboseAttack) println("Target tile ${targetTile.position} is not reachable for ${troop.unitName} in this turn.")
            return false
        }

        return true
    }

    fun getAttackerArmy() =  attackerArmy
    fun getDefenderArmy() =  defenderArmy

    /**
     * Executes an attack by one troop on another troop.
     *
     * @param defender The defending troop.
     * @param attacker The attacking troop. Defaults to the current troop.
     * @return True if luck influenced the attack (damage doubled), false otherwise.
     */
    fun attack(defender: TroopInfo, attacker: TroopInfo? = getCurrentTroop()): Boolean {
        var isLuck = false
        if (attacker == null) {
            return isLuck
        }
        if (verboseAttack) {
            println("Starting attack: ${attacker.unitName} (Position: ${attacker.currentTile.position}) attacking ${defender.unitName} (Position: ${defender.currentTile.position})")
            println("Initial attacker amount: ${attacker.currentAmount}, Initial defender amount: ${defender.currentAmount}")
        }

        // Calculate maximum damage
        var damage = attacker.currentAmount * attacker.baseUnit.damage

        // Determine if luck is triggered for the attacker
        isLuck = isLuckTriggered(attacker)
        if (isLuck) {
            damage *= 2  // Double the damage if luck triggers
            if (verboseAttack) println("Troop ${attacker.baseUnit.name} has luck, doubling damage")
        }

        if (verboseAttack) println("Base damage calculated: $damage")

        // Include health deficit in the calculation
        val healthDeficit = defender.baseUnit.health - defender.currentHealth
        if (verboseAttack) println("Defender health deficit: $healthDeficit")

        val totalDamage = damage + healthDeficit
        if (verboseAttack) println("Total damage after health deficit adjustment: $totalDamage")

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
            if (verboseAttack) println("Defender ${defender.unitName} at position ${defender.currentTile.position} has been defeated.")
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
        removeTroop(troop)
        println("Troop ${troop.baseUnit.name} has perished.")
    }

    /**
     * Removes a troop from the battle.
     *
     * This function removes the troop from the turn queue and updates the currentTurnIndex accordingly.
     *
     * @param troop The troop to remove.
     */
    fun removeTroop(troop: TroopInfo) {
        // Find the index of the troop in the turn queue
        val index = turnQueue.indexOf(troop)
        if (index != -1) {
            // Remove the troop from the turn queue
            turnQueue.removeAt(index)
            // Adjust currentTurnIndex:
            // If the removed troop was located before the current turn,
            // decrement currentTurnIndex to keep the turn order consistent.
            if (index < currentTurnIndex) {
                currentTurnIndex--
            }
            // If currentTurnIndex is now out of bounds, wrap it around to the start.
            if (currentTurnIndex >= turnQueue.size) {
                currentTurnIndex = 0
            }
        }

        // Remove the troop from its respective army
        if (attackerArmy.contains(troop)) {
            attackerArmy.removeTroop(troop)
        } else if (defenderArmy.contains(troop)) {
            defenderArmy.removeTroop(troop)
        }

        println("Troop ${troop.baseUnit.name} removed from the battle.")
    }

    /**
     * Advances the turn to the next troop in the queue.
     * If the end of the queue is reached, it loops back to the start.
     */
    fun advanceTurn() {
        if (turnQueue.isEmpty()) {
            println("The turn queue is empty. Ending battle...")
            finishBattle()
            return
        }

        currentTurnIndex = (currentTurnIndex + 1) % turnQueue.size
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
     *
     * @return A list of troops in the turn queue.
     */
    fun getTurnQueue(): List<TroopInfo> {
        return turnQueue
    }
}
