package com.unciv.logic.battle.session

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleMovementPreviewUseCase
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.application.pathfinding.TroopMovementContext
import com.unciv.pure.domain.battle.Point

/** Uses the same queries as the local screen; call under the session's serialization boundary. */
object BattlePresentationReader {
    fun capture(
        manager: BattleManager,
        commands: List<BattleCommand>
    ): BattlePresentationSnapshot {
        if (!manager.isBattleOn()) return BattlePresentationSnapshot()
        val active = manager.getCurrentTroop() ?: return BattlePresentationSnapshot()
        val origin = manager.getTroopTile(active) ?: return BattlePresentationSnapshot()
        fun IBattleTile.point() = Point(position.x.toInt(), position.y.toInt())
        fun tile(point: Point): IBattleTile? = manager.battleField.getTileAt(
            Vector2(point.x.toFloat(), point.y.toFloat())
        ) as? IBattleTile
        val enemies = manager.getEnemies(active).toSet()
        val context = TroopMovementContext(active) { it in enemies }
        val living = (manager.getAttackerArmy().getAllTroops() +
            manager.getDefenderArmy().getAllTroops()).filterNotNull().filter { it.currentAmount > 0 }
        val ranges = living.associate { troop -> troop.id to manager.getReachableTiles(troop) }
        val appearances = ranges[active.id].orEmpty().map { target ->
            BattleTilePresentation(target.point(), BattleTileAppearanceUseCase.execute(
                isReachable = true,
                movementDistance = HexMath.getDistance(origin.position, target.position),
                maximumMovement = active.speed,
                control = context.controlStrength(target),
                canDisplayControl = manager.isTileFree(target) || target == origin
            ))
        }
        val movements = living.mapNotNull { troop ->
            val start = manager.getTroopTile(troop) ?: return@mapNotNull null
            BattleTroopMovementPresentation(troop.id, ranges[troop.id].orEmpty().map { target ->
                BattleMovementTilePresentation(target.point(), BattleMovementPreviewUseCase.execute(
                    isReachable = true,
                    movementDistance = HexMath.getDistance(start.position, target.position),
                    maximumMovement = troop.speed
                ))
            })
        }
        val attacks = commands.mapNotNull { command ->
            val targetPoint = when (command) {
                is BattleCommand.Attack -> command.target
                is BattleCommand.Shoot -> command.target
                else -> return@mapNotNull null
            }
            val target = tile(targetPoint)?.let(manager::getTroopOnTile) ?: return@mapNotNull null
            val approach = (command as? BattleCommand.Attack)?.attackFrom?.let(::tile)
            val preview = manager.getAttackPreview(active, target, approach) ?: return@mapNotNull null
            BattleAttackPresentation(command, preview)
        }
        return BattlePresentationSnapshot(appearances, movements, attacks)
    }
}
