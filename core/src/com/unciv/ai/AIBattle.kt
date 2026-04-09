package com.unciv.ai

import BattleActionResult
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

class AIBattle(private val battleManager: BattleManager) {

    companion object {
        var AI_verbose = true // Флаг для включения/выключения вербозинга
    }


    fun performTurn(troop: Troop) {
        if (AI_verbose) println("AI Turn: ${troop.unitName} at ${battleManager.getTroopTile(troop)?.position}")
        if (troop.isRanged) performRangedAction(troop)
        else performMeleeAction(troop)
    }

    private fun performMeleeAction(troop: Troop) {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return
        }

        val closestEnemy = enemies.minByOrNull {
            battleManager.getTroopTile(it)?.let { tile ->
                HexMath.getDistance(currentTile.position, (tile as TileInfo).position)
            } ?: Int.MAX_VALUE
        } ?: return

        val closestEnemyTile = battleManager.getTroopTile(closestEnemy) ?: return
        if (AI_verbose) println("Closest enemy for ${troop.unitName}: ${closestEnemy.unitName} at ${closestEnemyTile.position}")

        val attackTile = findAttackTile(troop, closestEnemyTile as TileInfo)
        if (attackTile != null) {
            battleManager.performTurn(BattleActionRequest(
                troop = troop,
                targetPosition = closestEnemyTile,
                actionType = ActionType.ATTACK,
                attackTile = attackTile
            ))
        } else {
            val moveTarget = findBestMoveTarget(troop, closestEnemyTile)
            if (moveTarget != null) {
                if (AI_verbose) println("${troop.unitName} moving to $moveTarget")
                battleManager.performTurn(BattleActionRequest(
                    troop = troop,
                    targetPosition = moveTarget,
                    actionType = ActionType.MOVE
                ))
            } else {
                if (AI_verbose) println("${troop.unitName} cannot find a valid move target.")
            }
        }
    }

    private fun performRangedAction(troop: Troop) {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return
        }

        val target = enemies
            .sortedWith(compareByDescending<Troop> { it.isRanged }.thenByDescending { it.speed })
            .firstOrNull() ?: return

        val targetTile = battleManager.getTroopTile(target) ?: return
        if (AI_verbose) println("Selected ranged target for ${troop.unitName}: ${target.unitName} at ${targetTile.position}")

        battleManager.performTurn(BattleActionRequest(
            troop = troop,
            targetPosition = targetTile,
            actionType = ActionType.SHOOT
        ))
    }

    /**
     * Найти клетку для атаки юнита.
     *
     * @param troop Отряд, совершающий атаку.
     * @param targetTile Целевая клетка атаки.
     * @return Клетка (`TileInfo`), с которой можно атаковать, или `null`, если подходящей нет.
     */
    private fun findAttackTile(troop: Troop, targetTile: TileInfo): TileInfo? {
        if (AI_verbose) println("Finding attack tile for ${troop.unitName} attacking ${targetTile.position}")

        val currentTile = battleManager.getTroopTile(troop)
        if(currentTile == null)
            return null

        // Проверяем, находится ли текущий тайл юнита в соседях цели
        if (targetTile.neighbors.contains(currentTile)) {
            if (AI_verbose) println("Troop is already in a valid attack position: ${currentTile.position}")
            return currentTile as TileInfo?
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

    /*
    /** Проверить, валидна ли клетка для атаки */
    private fun isDirectionValid(troop: TroopInfo, targetPosition: Vector2, direction: Direction): Boolean {
        //val attackTile =
        val attackPosition = HexMath.oneStepTowards(targetPosition, direction)
        return battleManager.isHexAchievable(troop, attackPosition) && battleManager.isTileFree(attackPosition)
    }

     */

    /** Проверить, валидна ли клетка для атаки */
    private fun isDirectionValid(troop: Troop, targetTile: TileInfo, direction: Direction): Boolean {
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
    private fun findBestMoveTarget(troop: Troop, targetTile: TileInfo): TileInfo? {
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



