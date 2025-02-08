package com.unciv.ai

import BattleActionResult
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.map.TileInfo
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

class AIBattle(private val battleManager: BattleManager) {

    companion object {
        var AI_verbose = true // Флаг для включения/выключения вербозинга
    }

    /** Выполняет ход для указанного отряда AI */
    fun performTurn(troop: TroopInfo): BattleActionResult {
        if (AI_verbose) println("AI Turn: ${troop.unitName} at ${troop.currentTile.position}")
        return if (troop.baseUnit.isRanged()) {
            performRangedAction(troop)
        } else {
            performMeleeAction(troop)
        }
    }

    /** Логика для ближнего боя */
    private fun performMeleeAction(troop: TroopInfo): BattleActionResult {
        val enemies = battleManager.getEnemies(troop)

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for melee troop ${troop.unitName} at ${troop.currentTile.position}")
            return BattleActionResult(
                actionType = ActionType.ATTACK,
                success = false,
                errorId = ErrorId.AI_NO_ENEMIES
            )
        }

        val closestEnemy = enemies.minByOrNull { HexMath.getDistance(troop.currentTile.position, it.currentTile.position) }
        if (AI_verbose) println("Closest enemy for ${troop.unitName}: ${closestEnemy?.unitName} at ${closestEnemy?.currentTile?.position}")

        val attackTile = closestEnemy?.currentTile?.let { findAttackTile(troop, it) }

        if (attackTile != null) {
            //val attackTile = battleManager.battleField.getNeighborTile(closestEnemy.currentTile, attackDirection)

            if (AI_verbose) println("Attacking direction for ${troop.unitName}: $attackTile")
            return battleManager.performTurn(
                BattleActionRequest(
                    troop = troop,
                    targetPosition = closestEnemy.currentTile,
                    actionType = ActionType.ATTACK,
                    attackTile = attackTile
                )
            )
        } else {
            val moveTarget = closestEnemy?.currentTile?.let { findBestMoveTarget(troop, it) }
            if (moveTarget != null) {
                if (AI_verbose) println("${troop.unitName} moving to $moveTarget")
                return battleManager.performTurn(
                    BattleActionRequest(
                        troop = troop,
                        targetPosition = moveTarget,
                        actionType = ActionType.MOVE
                    )
                )
            } else {
                if (AI_verbose) println("${troop.unitName} cannot find a valid move target.")
                return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = false,
                    errorId = ErrorId.AI_NO_VALID_MOVE
                )
            }
        }
    }

    /*
    /**
     * Найти направление атаки для юнита
     *
     * @param troop Отряд, совершающий атаку.
     * @param targetTile Целевая клетка атаки.
     * @return Направление атаки (`Direction`) или `null`, если подходящего нет.
     */
    private fun findAttackDirection(troop: TroopInfo, targetTile: TileInfo): Direction? {
        val defaultDirection = HexMath.getDirection(troop.currentTile.position, targetTile.position)
        if (AI_verbose) println("Default direction for attack: $defaultDirection")

        if (isDirectionValid(troop, targetTile, defaultDirection)) {
            return defaultDirection
        }

        for (i in 1..5) {
            val direction = HexMath.rotateClockwise(defaultDirection, i)
            if (isDirectionValid(troop, targetTile, direction)) {
                if (AI_verbose) println("Found valid attack direction: $direction")
                return direction
            }
        }

        if (AI_verbose) println("No valid attack direction found for ${troop.unitName}")
        return null
    }

     */

    /**
     * Найти клетку для атаки юнита.
     *
     * @param troop Отряд, совершающий атаку.
     * @param targetTile Целевая клетка атаки.
     * @return Клетка (`TileInfo`), с которой можно атаковать, или `null`, если подходящей нет.
     */
    private fun findAttackTile(troop: TroopInfo, targetTile: TileInfo): TileInfo? {
        if (AI_verbose) println("Finding attack tile for ${troop.unitName} attacking ${targetTile.position}")

        // Проверяем, находится ли текущий тайл юнита в соседях цели
        if (targetTile.neighbors.contains(troop.currentTile)) {
            if (AI_verbose) println("Troop is already in a valid attack position: ${troop.currentTile.position}")
            return troop.currentTile
        }

        // Ищем ближайший соседний тайл, с которого можно атаковать
        val attackTile = targetTile.neighbors.firstOrNull { battleManager.isTileAchievable(troop, it) && battleManager.isTileFree(it) }

        if (attackTile != null) {
            if (AI_verbose) println("Found attack tile: ${attackTile.position}")
        } else {
            if (AI_verbose) println("No valid attack tile found for ${troop.unitName}")
        }

        return attackTile
    }


    /*

    /** Найти направление атаки для юнита */
    private fun findAttackDirection(troop: TroopInfo, targetPosition: Vector2): Direction? {
        val defaultDirection = HexMath.getDirection(troop.position, targetPosition)
        if (AI_verbose) println("Default direction for attack: $defaultDirection")

        if (isDirectionValid(troop, targetPosition, defaultDirection)) {
            return defaultDirection
        }

        for (i in 1..5) {
            val direction = HexMath.rotateClockwise(defaultDirection, i)
            if (isDirectionValid(troop, targetPosition, direction)) {
                if (AI_verbose) println("Found valid attack direction: $direction")
                return direction
            }
        }

        if (AI_verbose) println("No valid attack direction found for ${troop.unitName}")
        return null
    }


     */
    /** Логика для стреляющих юнитов */
    private fun performRangedAction(troop: TroopInfo): BattleActionResult {
        val enemies = battleManager.getEnemies(troop)

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ranged troop ${troop.unitName} at ${troop.currentTile.position}")
            return BattleActionResult(
                actionType = ActionType.SHOOT,
                success = false,
                errorId = ErrorId.AI_NO_ENEMIES
            )
        }

        val target = enemies
            .sortedWith(
                compareByDescending<TroopInfo> { it.baseUnit.isRanged() }
                    .thenByDescending { it.baseUnit.speed }
            )
            .firstOrNull()

        if (AI_verbose) println("Selected ranged target for ${troop.unitName}: ${target?.unitName} at ${target?.currentTile?.position}")

        return if (target != null) {
            battleManager.performTurn(
                BattleActionRequest(
                    troop = troop,
                    targetPosition = target.currentTile,
                    actionType = ActionType.SHOOT
                )
            )
        } else {
            if (AI_verbose) println("No valid targets in range for ${troop.unitName}")
            BattleActionResult(
                actionType = ActionType.SHOOT,
                success = false,
                errorId = ErrorId.AI_NO_TARGET
            )
        }
    }

    /*
    /** Проверить, валидна ли клетка для атаки */
    private fun isDirectionValid(troop: TroopInfo, targetPosition: Vector2, direction: Direction): Boolean {
        //val attackTile =
        val attackPosition = HexMath.oneStepTowards(targetPosition, direction)
        return battleManager.isHexAchievable(troop, attackPosition) && battleManager.isTileFree(attackPosition)
    }

     */

    /** Проверить, валидна ли клетка для атаки */
    private fun isDirectionValid(troop: TroopInfo, targetTile: TileInfo, direction: Direction): Boolean {
        // Получаем клетку, с которой можно атаковать
        val attackTile = battleManager.battleField.getNeighborTile(targetTile, direction) ?: return false

        return battleManager.isTileAchievable(troop, attackTile) && battleManager.isTileFree(attackTile)
    }


    /**
     * Найти лучшую клетку для перемещения к цели.
     *
     * @param troop Отряд, который перемещается.
     * @param targetTile Целевая клетка.
     * @return Лучшая клетка для перемещения или `null`, если перемещение невозможно.
     */
    private fun findBestMoveTarget(troop: TroopInfo, targetTile: TileInfo): TileInfo? {
        val reachableTiles = battleManager.getReachableTiles(troop)
        if (reachableTiles.isEmpty()) {
            if (AI_verbose) println("No reachable tiles for ${troop.unitName}")
            return null
        }

        // Выбираем ближайшую клетку к цели
        val bestTile = reachableTiles.minByOrNull { HexMath.getDistance(it.position, targetTile.position) }
        if (AI_verbose) println("Best move target for ${troop.unitName}: ${bestTile?.position}")
        return bestTile
    }

    /*

    /** Найти лучшую клетку для перемещения к цели */
    private fun findBestMoveTarget(troop: TroopInfo, targetPosition: Vector2): Vector2? {
        val reachableTiles = battleManager.getReachableTiles(troop).filterNotNull()
        if (reachableTiles.isEmpty()) {
            if (AI_verbose) println("No reachable tiles for ${troop.unitName}")
            return null
        }

        val bestTile = reachableTiles.minByOrNull { HexMath.getDistance(it, targetPosition) }
        if (AI_verbose) println("Best move target for ${troop.unitName}: $bestTile")
        return bestTile
    }

     */
}



