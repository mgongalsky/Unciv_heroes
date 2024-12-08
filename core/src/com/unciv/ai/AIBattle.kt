package com.unciv.ai

import BattleActionResult
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

class AIBattle(private val battleManager: BattleManager) {

    /** Выполняет ход для указанного отряда AI */
    fun performTurn(troop: TroopInfo): BattleActionResult {
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
            println("No enemies left for troop ${troop.unitName} at ${troop.position}")
            return BattleActionResult(
                actionType = ActionType.ATTACK,
                success = false,
                errorId = ErrorId.AI_NO_ENEMIES
            )
        }

        // Найти ближайшего врага
        val closestEnemy = enemies.minByOrNull { HexMath.getDistance(troop.position, it.position) }
            ?: return BattleActionResult(
                actionType = ActionType.MOVE,
                success = false,
                errorId = ErrorId.AI_NO_TARGET
            )

        // Определить направление атаки
        val attackDirection = findAttackDirection(troop, closestEnemy.position)

        if (attackDirection != null) {
            // Если нашли направление атаки, атакуем
            //val attackPosition = HexMath.oneStepTowards(closestEnemy.position, attackDirection)

            return battleManager.performTurn(
                BattleActionRequest(
                    troop = troop,
                    targetPosition = closestEnemy.position,
                    actionType = ActionType.ATTACK,
                    direction = attackDirection
                )
            )
        } else {
            // Если атаковать нельзя, двигаемся к ближайшему врагу
            val moveTarget = findBestMoveTarget(troop, closestEnemy.position)
                ?: return BattleActionResult(
                    actionType = ActionType.MOVE,
                    success = false,
                    errorId = ErrorId.AI_NO_VALID_MOVE
                )

            return battleManager.performTurn(
                BattleActionRequest(
                    troop = troop,
                    targetPosition = moveTarget,
                    actionType = ActionType.MOVE
                )
            )
        }
    }

    /** Найти направление атаки для юнита */
    private fun findAttackDirection(troop: TroopInfo, targetPosition: Vector2): Direction? {
        // Вычислить дефолтное направление
        val defaultDirection = HexMath.getDirection(troop.position, targetPosition)

        // Проверить, достижима ли клетка в дефолтном направлении
        if (isDirectionValid(troop, targetPosition, defaultDirection)) {
            return defaultDirection
        }

        // Перебор направлений по часовой стрелке
        for (i in 1..5) {
            val direction = HexMath.rotateClockwise(defaultDirection, i)
            if (isDirectionValid(troop, targetPosition, direction)) {
                return direction
            }
        }

        // Если ни одно направление не подходит, возвращаем null
        return null
    }

    /** Проверить, валидна ли клетка для атаки */
    private fun isDirectionValid(troop: TroopInfo, targetPosition: Vector2, direction: Direction): Boolean {
        val attackPosition = HexMath.oneStepTowards(targetPosition, direction)
        return battleManager.isHexAchievable(troop, attackPosition) && battleManager.isHexFree(attackPosition)
    }

    /** Логика для стреляющих юнитов */
    private fun performRangedAction(troop: TroopInfo): BattleActionResult {
        val enemies = battleManager.getEnemies(troop)

        if (enemies.isEmpty()) {
            println("No enemies left for ranged troop ${troop.unitName} at ${troop.position}")
            return BattleActionResult(
                actionType = ActionType.SHOOT,
                success = false,
                errorId = ErrorId.AI_NO_ENEMIES
            )
        }

        // Выбрать цель: сначала стреляющих врагов, затем самых быстрых
        val target = enemies
            .filter { HexMath.getDistance(troop.position, it.position) <= troop.baseUnit.speed }
            .sortedWith(
                compareByDescending<TroopInfo> { it.baseUnit.isRanged() }
                    .thenByDescending { it.baseUnit.speed }
            )
            .firstOrNull()

        if (target == null) {
            println("No valid targets in range for troop ${troop.unitName} at ${troop.position}")
            return BattleActionResult(
                actionType = ActionType.SHOOT,
                success = false,
                errorId = ErrorId.AI_NO_TARGET
            )
        }

        // Если враг в радиусе атаки, стреляем
        return battleManager.performTurn(
            BattleActionRequest(
                troop = troop,
                targetPosition = target.position,
                actionType = ActionType.SHOOT
            )
        )
    }

    /** Найти лучшую клетку для перемещения к цели */
    private fun findBestMoveTarget(troop: TroopInfo, targetPosition: Vector2): Vector2? {
        // Все достижимые клетки
        val reachableTiles = battleManager.getReachableTiles(troop).filterNotNull()
        if (reachableTiles.isEmpty()) return null

        // Найти клетку, минимизирующую расстояние до цели
        return reachableTiles.minByOrNull { tile -> HexMath.getDistance(tile, targetPosition) }
    }
}
