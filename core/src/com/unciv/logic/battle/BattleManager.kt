package com.unciv.logic.battle

import BattleActionResult
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.RealBattleRandom
import com.unciv.logic.HexMath
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.UnitMovementAlgorithms
import com.unciv.models.GameConstants
import com.unciv.pure.application.battle.CalculateDamageUseCase
import com.unciv.pure.application.battle.IsMoraleTriggeredUseCase
import com.unciv.pure.application.battle.PerformAttackUseCase
import com.unciv.pure.application.battle.PerformMoveUseCase
import com.unciv.pure.application.pathfinding.TroopMovementAdapter
import com.unciv.pure.application.pathfinding.TroopMovementContext
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.TurnQueue
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop
import kotlin.random.Random

/**
 * Handles the logical part of a battle between two armies.
 * Does not include visual representation or UI logic.
 *
 * @property attackerArmy The attacking army participating in the battle.
 * @property defenderArmy The defending army participating in the battle.
 */
open class BattleManager(
    private var attackerArmy: ArmyInfo,
    private var defenderArmy: ArmyInfo,
    val battleField: IBattleField, // BattleField on use
    private val random: IBattleRandom = RealBattleRandom(),
    private val moraleProbability: Double = GameConstants.moraleProbability,
    private val luckProbability: Double = GameConstants.luckProbability
) {
    protected val troopPositions = mutableMapOf<Troop, IBattleTile>()
    private val turnQueue = TurnQueue()

    private fun makeAdapter(troop: Troop, tile: TileInfo): TroopMovementAdapter {
        val armyCivInfo = getArmyOf(troop)?.civInfo ?: CivilizationInfo()
        return TroopMovementAdapter(troop, tile, enemyChecker(troop), armyCivInfo)
    }

    fun initializeBattle() {
        val attackerTroops = attackerArmy.getAllTroops().filterNotNull()
        val defenderTroops = defenderArmy.getAllTroops().filterNotNull()

        // Расставляем атакующих
        attackerTroops.forEachIndexed { index, troop ->
            val position = HexMath.evenQ2HexCoords(Vector2(-7f, 3f - index.toFloat() * 2))
            val rawTile = battleField.getTileAt(position)
            println("Attacker $index: position=$position, tile=$rawTile, isBattleTile=${rawTile is IBattleTile}")
            val tile = rawTile as? IBattleTile ?: run {
                println("WARNING: tile is null or not IBattleTile for attacker $index at $position")
                return@forEachIndexed
            }
            troopPositions[troop] = tile
            tile.receiveTroop(troop)
        }

        // Расставляем защитников
        defenderTroops.forEachIndexed { index, troop ->
            val position = HexMath.evenQ2HexCoords(Vector2(6f, 3f - index.toFloat() * 2))
            val tile = battleField.getTileAt(position) as? IBattleTile ?: return@forEachIndexed
            troopPositions[troop] = tile
            tile.receiveTroop(troop)
        }

        initializeTurnQueue()
    }

    /**
     * Initializes the turn queue based on troop speed and priority rules.
     * Attacker troops have priority in case of equal speed.
     */
    fun initializeTurnQueue() {
        turnQueue.initialize(
            attackerArmy.getAllTroops().filterNotNull(),
            defenderArmy.getAllTroops().filterNotNull()
        )
    }

    fun getTroopTile(troop: Troop): IBattleTile? = troopPositions[troop]
    protected open fun getTroopCurrentTile(troop: Troop): IBattleTile? = troopPositions[troop]

    protected open fun moveTroop(troop: Troop, targetTile: IBattleTile) {
        // Очищаем старый тайл
        troopPositions[troop]?.clearTroop()
        // Обновляем позицию
        troopPositions[troop] = targetTile
        // Ставим на новый тайл
        targetTile.receiveTroop(troop)
    }
    /**
     * Returns the troop currently taking its turn.
     *
     * @return The current troop or `null` if the queue is empty.
     */
    fun getCurrentTroop(): Troop? = turnQueue.current()

    private fun getArmyOf(troop: Troop): ArmyInfo? {
        return when {
            attackerArmy.contains(troop) -> attackerArmy
            defenderArmy.contains(troop) -> defenderArmy
            else -> null
        }
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
    private fun isMoraleTriggered(troop: Troop) =
            IsMoraleTriggeredUseCase.execute(getArmyOf(troop)?.hero?.morale ?: 0, random, moraleProbability)

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
    private fun isLuckTriggered(troop: Troop): Boolean {
        // Assume hero's luck value is stored in hero.luck; if no hero, default to 1.
        val troopLuck = getArmyOf(troop)?.hero?.luck ?: 1
        val effectiveProbability = if (troopLuck <= 3) {
            (luckProbability / 3.0) * troopLuck
        } else {
            luckProbability
        }
        println(
            "Unit: ${troop.unitName}, Amount: ${troop.amount}, " +
                    "Hero present: ${getArmyOf(troop)?.hero != null}, Hero luck: $troopLuck, " +
                    "Effective luck probability: $effectiveProbability"
        )
        return random.nextDouble() < effectiveProbability
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
    fun getTroopOnTile(tile: IBattleTile): Troop? =
            troopPositions.entries.find { it.value == tile }?.key

    fun setTroopPosition(troop: Troop, tile: IBattleTile) {
        troopPositions[troop] = tile
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
        if (verboseAttack && isMorale) println("Troop ${troop.unitName} has morale")

        when (actionRequest.actionType) {
            ActionType.SKIP -> {
                return BattleActionResult(
                    actionType = ActionType.SKIP,
                    success = true,
                    movedFrom = null,
                    movedTo = null,
                    isMorale = false,
                    battleEnded = !isBattleOn()
                )
            }
            ActionType.MOVE -> {
                val output = PerformMoveUseCase.execute(
                    PerformMoveUseCase.Input(
                        troop = troop,
                        targetTile = actionRequest.targetPosition,
                        currentTile = getTroopCurrentTile(troop),
                        isTileAchievable = isTileAchievable(troop, actionRequest.targetPosition),
                        isTileOccupiedByAlly = isTileOccupiedByAlly(troop, actionRequest.targetPosition),
                        isTileFree = isTileFree(actionRequest.targetPosition)
                    )
                )
                if (output.success) moveTroop(troop, actionRequest.targetPosition)
                if (verboseAttack) println("Troop moved from ${output.movedFrom} to ${output.movedTo}")
                return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = output.success,
                    errorId = output.errorId,
                    movedFrom = output.movedFrom,
                    movedTo = output.movedTo,
                    isMorale = isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.ATTACK -> {
                val defender = getTroopOnTile(actionRequest.targetPosition)
                val attackTile = actionRequest.attackTile
                val currentTile = getTroopCurrentTile(troop)

                // все флаги вычисляем ДО moveTroop
                val isTargetOccupiedByEnemy = defender != null &&
                        isTileOccupiedByEnemy(troop, actionRequest.targetPosition)
                val isAttackTileAchievable = attackTile != null &&
                        isTileAchievable(troop, attackTile)
                val isAttackTileFree = attackTile != null &&
                        isTileFree(attackTile)

                val canAttack = defender != null &&
                        attackTile != null &&
                        isTargetOccupiedByEnemy &&
                        isAttackTileAchievable &&
                        (isAttackTileFree || currentTile == attackTile)

                val isLuck: Boolean
                val defenderRemainingAmount: Int
                val defenderDied: Boolean

                if (canAttack) {
                    moveTroop(troop, attackTile!!)
                    isLuck = attack(defender!!, troop)
                    defenderRemainingAmount = defender.currentAmount
                    defenderDied = defender.currentAmount <= 0
                } else {
                    isLuck = false
                    defenderRemainingAmount = 0
                    defenderDied = false
                }

                val output = PerformAttackUseCase.execute(
                    PerformAttackUseCase.Input(
                        attacker = troop,
                        defender = defender,
                        attackTile = attackTile,
                        currentTile = currentTile,
                        isTargetOccupiedByEnemy = isTargetOccupiedByEnemy,
                        isAttackTileAchievable = isAttackTileAchievable,
                        isAttackTileFree = isAttackTileFree,
                        isLuck = isLuck,
                        isMorale = isMorale,
                        defenderRemainingAmount = defenderRemainingAmount,
                        defenderDied = defenderDied
                    )
                )
                return BattleActionResult(
                    actionType = ActionType.ATTACK,
                    success = output.success,
                    errorId = output.errorId,
                    movedFrom = output.movedFrom,
                    movedTo = output.movedTo,
                    isLuck = output.isLuck,
                    isMorale = output.isMorale,
                    battleEnded = !isBattleOn()
                )
            }

            ActionType.SHOOT -> {
                if (verboseAttack) println("Starting ActionType.SHOOT for troop: ${troop.unitName}.")

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
    fun isTileOccupiedByAlly(troop: Troop, targetTile: IBattleTile): Boolean {
        //val targetTroop = targetTile.troopUnit ?: return false  // Если клетка пустая, значит не занята союзником
        val targetTroop = targetTile.getTroop() ?: return false

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
    fun isTileOccupiedByEnemy(troop: Troop, targetTile: IBattleTile): Boolean {
        //val targetTroop = targetTile.troopUnit ?: return false  // Если клетка пустая, значит нет врага
        val targetTroop = targetTile.getTroop() ?: return false

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
    fun isTileFree(targetTile: INavigableTile): Boolean {
        //return (targetTile as? TileInfo)?.troopUnit == null  // Если юнита нет, клетка свободна
        return (targetTile as? IBattleTile)?.getTroop() == null
    }


    /**
     * Returns a list of enemies for the given troop.
     *
     * @param troop The troop for which to get enemies.
     * @return A list of enemy troops.
     */
    fun getEnemies(troop: Troop): List<Troop> {
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
    //fun getReachableTiles(troop: Troop): List<TileInfo> {
    //    return troop.movement.getReachableTilesInCurrentTurn(context = TroopMovementContext(troop)).toList()
    //}


    private fun enemyChecker(troop: Troop): (Troop) -> Boolean = { other ->
        attackerArmy.contains(troop) && defenderArmy.contains(other) ||
                defenderArmy.contains(troop) && attackerArmy.contains(other)
    }

    fun getReachableTiles(troop: Troop): List<TileInfo> {
        val currentTile = getTroopTile(troop) as? TileInfo ?: return emptyList()
        val movement = UnitMovementAlgorithms(makeAdapter(troop, currentTile))
        return movement.getReachableTilesInCurrentTurn(
            context = TroopMovementContext(troop)
        ).toList()
    }

    /*
    fun getReachableTiles(troop: TroopInfo): List<Vector2> {
        return troop.movement.getReachableTilesInCurrentTurn(context = TroopMovementContext(troop))().map { it.position }.toList()

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
    fun isTileAchievable(troop: Troop, targetTile: INavigableTile): Boolean {
        if (!battleField.contains(targetTile)) return false
        if (!isReachableInCurrentTurn(troop, targetTile)) return false
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
    fun attack(defender: Troop, attacker: Troop? = getCurrentTroop()): Boolean {
        if (attacker == null) return false

        val isLuck = isLuckTriggered(attacker)

        val result = CalculateDamageUseCase.execute(
            attackerAmount = attacker.currentAmount,
            attackerDamage = attacker.damage,
            defenderAmount = defender.currentAmount,
            defenderHealth = defender.currentHealth,
            defenderMaxHealth = defender.maxHealth,
            isLuck = isLuck
        )

        defender.currentAmount = result.remainingAmount
        defender.currentHealth = result.remainingHealth

        if (defender.currentAmount <= 0) perishTroop(defender)

        return result.isLuck
    }

    /**
     * Handles the removal of a perished troop.
     *
     * @param troop The troop to remove.
     */
    fun perishTroop(troop: Troop) {
        removeTroop(troop)
        println("Troop ${troop.unitName} has perished.")
    }

    /**
     * Removes a troop from the battle.
     *
     * This function removes the troop from the turn queue and updates the currentTurnIndex accordingly.
     *
     * @param troop The troop to remove.
     */
    fun removeTroop(troop: Troop) {
        turnQueue.remove(troop)  // queue-логика

        // остаётся в BattleManager
        if (attackerArmy.contains(troop)) {
            attackerArmy.removeTroop(troop)
            troopPositions.remove(troop)
        } else if (defenderArmy.contains(troop)) {
            defenderArmy.removeTroop(troop)
            troopPositions.remove(troop)
        }
    }

    /**
     * Advances the turn to the next troop in the queue.
     * If the end of the queue is reached, it loops back to the start.
     */
    fun advanceTurn() {
        if (turnQueue.isEmpty()) {
            finishBattle()
            return
        }
        turnQueue.advance()
    }

    /**
     * Checks if the given troop can shoot.
     *
     * @param troop The troop to check.
     * @return True if the troop can shoot, false otherwise.
     */
    fun canShoot(troop: Troop): Boolean {
        return troop.rangedStrength != 0
    }

    /**
     * Returns the current turn queue (for debugging or visualization).
     *
     * @return A list of troops in the turn queue.
     */
    fun getTurnQueue(): List<Troop> = turnQueue.getAll()

    protected open fun isReachableInCurrentTurn(troop: Troop, targetTile: INavigableTile): Boolean {
        val currentTile = getTroopTile(troop) as? TileInfo ?: return false
        val movement = UnitMovementAlgorithms(makeAdapter(troop, currentTile))
        val reachableTiles = movement.getReachableTilesInCurrentTurn(
            context = TroopMovementContext(troop),
            targetTile = targetTile as TileInfo
        )
        return reachableTiles.contains(targetTile)
    }
}
