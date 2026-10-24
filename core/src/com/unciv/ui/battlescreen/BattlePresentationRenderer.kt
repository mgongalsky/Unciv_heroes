package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase.Appearance
import com.unciv.pure.application.battle.session.BattleStateSnapshot
import com.unciv.pure.domain.battle.Point
import com.unciv.ui.tilegroups.TileGroup

/** Shared presentation of engine-supplied values, independent of local or remote execution. */
class BattlePresentationRenderer(
    groups: List<TileGroup>,
    private val highlights: BattleTileHighlightRenderer = BattleTileHighlightRenderer(),
    bindHover: Boolean = true
) {
    private val tiles = groups.associateBy {
        Point(it.tileInfo.position.x.toInt(), it.tileInfo.position.y.toInt())
    }
    private val movementOverlays = mutableMapOf<TileGroup, Image>()
    private var state: BattleStateSnapshot? = null

    init { if (bindHover) groups.forEach(highlights::bind) }

    fun update(value: BattleStateSnapshot) {
        state = value
        val appearances = value.presentation.tiles.associate { it.position to it.appearance }
        val occupants = value.troops.associateBy { it.position }
        tiles.forEach { (position, tile) ->
            highlights.update(tile, appearances[position] ?: Appearance())
            highlights.updateSupport(tile, (occupants[position]?.supportBonusPercent ?: 0) > 0)
        }
        hover(null, null)
    }

    fun hover(command: BattleCommand?, movementTroopId: Int?) {
        val value = state ?: return
        val forecast = value.presentation.attacks.firstOrNull { it.command == command }?.preview
        val target = (command as? BattleCommand.Attack)?.target
        val flank = forecast?.attack?.flanking
        val previewLevel = flank?.takeIf {
            it.formedDamageBonusPercent > 0 || it.formationDamageBonusPercent > 0 ||
                it.unformedDamageBonusPercent > 0
        }?.flankLevel
        val occupants = value.troops.associateBy { it.position }
        tiles.forEach { (position, tile) ->
            highlights.updateFlanking(tile,
                if (value.finished) 0 else occupants[position]?.flankingLevel ?: 0,
                if (position == target) previewLevel else null)
        }
        movementOverlays.values.forEach { it.isVisible = false }
        val inspected = value.troops.firstOrNull { it.id == movementTroopId }
        if (!value.finished && inspected != null && value.currentSide != null &&
            inspected.side != value.currentSide) {
            value.presentation.movementRanges.firstOrNull { it.troopId == inspected.id }?.tiles?.forEach {
                val tile = tiles[it.position] ?: return@forEach
                movementOverlays.getOrPut(tile) { createBattleMovementRangeOverlay(tile) }
                    .showBattleMovementStyle(it.style)
            }
        }
        highlights.restoreAll()
    }
}
