package com.unciv.logic.story

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.automation.unit.BattleHelper
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.ICombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.battle.NeutralCityBattleSession
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.story.StoryEvent
import com.unciv.pure.domain.story.StoryEventKind

/** Откладывает конкретную атаку без сохранения callback или ссылок на объекты мира. */
object StoryBattleEvents {
    /** Возвращает разрешение только после завершения очереди и повторной проверки цели. */
    fun beforeAttack(attacker: ICombatant, defender: ICombatant): Boolean {
        val game = attacker.getCivInfo().gameInfo
        if (game.tileMap.story == null) return true
        if (StoryRuntime.isBlocked(game)) return false
        val unit = (attacker as? MapUnitCombatant)?.unit ?: return game.storyPendingAttack == null
        val waiting = game.storyPendingAttack
        if (waiting != null) {
            if (!matches(waiting, unit, defender)) return false
            val permitted = !game.storyAttackCancelled && isLegal(unit, defender)
            clear(game)
            return permitted
        }
        if (!StoryMapEvents.isEligible(unit)) return true
        if (!isLegal(unit, defender)) return false
        val event = StoryMapEvents.event(unit, StoryEventKind.BeforeAttack, defender.getTile(),
            unit.currentTile, targetKey(defender))
        game.storyPendingAttack = event
        game.storyAttackCancelled = false
        StoryRuntime.emit(game, event)
        if (StoryRuntime.isBlocked(game)) return false
        val permitted = !game.storyAttackCancelled && matches(event, unit, defender) && isLegal(unit, defender)
        clear(game)
        return permitted
    }

    /** Продолжает сохранённую атаку после ответа; исчезнувшая или изменившаяся цель отменяет её. */
    fun resume(game: GameInfo, ownerName: String) {
        val event = game.storyPendingAttack ?: return
        require(event.ownerName == ownerName) { "Pending attack belongs to another participant" }
        if (StoryRuntime.isBlocked(game)) return
        if (game.storyAttackCancelled) {
            clear(game)
            return
        }
        val owner = game.civilizations.firstOrNull { it.civName == event.ownerName }
        val unit = owner?.getCivUnits()?.firstOrNull { StoryWorldEffects.actorKey(it) == event.actorKey }
        val position = event.position
        val tile = if (position == null) null else game.tileMap.values.firstOrNull {
            it.position == Vector2(position.x, position.y)
        }
        val defender = tile?.let { Battle.getMapCombatantOfTile(it) }
        if (unit == null || defender == null || !matches(event, unit, defender) || !isLegal(unit, defender)) {
            clear(game)
            return
        }
        // Battle.attack повторно проходит beforeAttack и расходует сохранённое решение без нового события.
        Battle.attack(MapUnitCombatant(unit), defender)
    }

    private fun matches(event: StoryEvent, unit: MapUnit, defender: ICombatant): Boolean =
        event.ownerName == unit.owner && event.actorKey == StoryWorldEffects.actorKey(unit) &&
            event.targetKey == targetKey(defender) &&
            event.from?.let { it.x == unit.currentTile.position.x && it.y == unit.currentTile.position.y } == true &&
            event.position?.let { it.x == defender.getTile().position.x && it.y == defender.getTile().position.y } == true

    private fun targetKey(defender: ICombatant): String = when (defender) {
        is CityCombatant -> "city:${defender.city.id}"
        is MapUnitCombatant -> StoryWorldEffects.actorKey(defender.unit)
        else -> ""
    }

    /** Проверка использует текущие видимость, владельцев, движение и состояние армий. */
    private fun isLegal(unit: MapUnit, defender: ICombatant): Boolean {
        if (unit.isDestroyed || !unit.canAttack() || unit.hasUnique(UniqueType.CannotAttack)) return false
        val tile = defender.getTile()
        val actual = Battle.getMapCombatantOfTile(tile) ?: return false
        if (targetKey(actual) != targetKey(defender) || tile !in unit.civInfo.viewableTiles) return false
        if (defender is CityCombatant && defender.city.state.ownership == CityOwnership.Neutral)
            return NeutralCityBattleSession.canStart(unit, defender.city)
        if (unit.hasUnique(UniqueType.MustSetUp) && !unit.isSetUpForSiege()) return false
        return BattleHelper.getAttackableEnemies(unit, unit.movement.getDistanceToTiles(), stayOnTile = true)
            .any { it.tileToAttack == tile && it.tileToAttackFrom == unit.currentTile }
    }

    private fun clear(game: GameInfo) {
        game.storyPendingAttack = null
        game.storyAttackCancelled = false
    }
}
