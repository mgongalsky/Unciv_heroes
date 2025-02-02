package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
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

    /**
     * Finds the troop located at the specified hexagonal position.
     *
     * @param positionHex The hexagonal position to check.
     * @return The [TroopInfo] located at the position or `null` if the hex is empty.
     */
    fun getTroopOnHex(positionHex: Vector2): TroopInfo? {
        return turnQueue.find { it.position == positionHex }
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
        val targetPosition = actionRequest.targetPosition

        // TODO: remove +1 when actual morale bonus for the same castle is introduced
        val isMorale = isMoraleTriggered(troop) // Add here +1 to morale just because all troops are from the same castle now )
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

                // Successful movement
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

                // Move attacker to attack position
                val oldPosition = troop.position
                troop.position = attackPosition
                if (verboseAttack) println("Troop moved to attack position $attackPosition")

                // Perform attack
                val isLuck = attack(defender, troop)

                if (verboseAttack) println("Attack performed on defender at $targetPosition")

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

                // Get defender
                val defender = getTroopOnHex(targetPosition)
                    ?: return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    ).also {
                        if (verboseAttack) println("Error: No troop found on target position: $targetPosition")
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

                if (!isHexOccupiedByEnemy(troop, targetPosition)) {
                    if (verboseAttack) println("Error: Target position $targetPosition is not occupied by an enemy")
                    return BattleActionResult(
                        actionType = ActionType.SHOOT,
                        success = false,
                        errorId = ErrorId.INVALID_TARGET
                    )
                }

                if (verboseAttack) println("Troop ${troop.unitName} is shooting at target: ${defender.unitName} on position: $targetPosition")

                // Perform shooting
                val isLuck = attack(defender, troop) // Use the same attack logic

                // Check if defender is defeated
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

    /**
     * Checks if the battle is still ongoing.
     *
     * @return True if both armies have surviving troops, false otherwise.
     */
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
     * Checks if a hex is free of any troop.
     *
     * @param targetPosition The position to check.
     * @return True if the position is free, false otherwise.
     */
    fun isHexFree(targetPosition: Vector2) = turnQueue.none { it.position == targetPosition }

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
    fun getReachableTiles(troop: TroopInfo): List<Vector2> {
        val reachableTiles = mutableListOf<Vector2>()

        // Iterate through all battlefield tiles
        for (x in -7..6) {  // X-coordinates of the battlefield
            for (y in -4..3) { // Y-coordinates of the battlefield
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

        // Hardcoded battlefield dimensions
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
     * Executes an attack by one troop on another troop.
     *
     * @param defender The defending troop.
     * @param attacker The attacking troop. Defaults to the current troop.
     * @return True if luck influenced the attack, false otherwise.
     */
    fun attack(defender: TroopInfo, attacker: TroopInfo? = getCurrentTroop()): Boolean {
        var isLuck = false
        if (attacker == null) {
            return isLuck
        }
        if (verboseAttack) {
            println("Starting attack: ${attacker.unitName} (Position: ${attacker.position}) attacking ${defender.unitName} (Position: ${defender.position})")
            println("Initial attacker amount: ${attacker.currentAmount}, Initial defender amount: ${defender.currentAmount}")
        }

        // Calculate maximum damage
        var damage = attacker.currentAmount * attacker.baseUnit.damage

        if (Random.nextDouble() < GameConstants.luckProbability) {
            damage *= 2
            isLuck = true
            if (verboseAttack) println("Troop ${attacker.baseUnit.name} has luck")
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
            if (verboseAttack) println("Defender ${defender.unitName} at position ${defender.position} has been defeated.")
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
     * @param troop The troop to remove.
     */
    fun removeTroop(troop: TroopInfo) {
        // Remove from turn queue
        turnQueue.remove(troop)

        // Remove from the respective army
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
