package com.unciv.logic.story

import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.application.story.ProcessStoryEvent
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.StoryEvent
import com.unciv.pure.domain.story.StoryEventKind

/** Переводит факты карты в чистые события, не исполняя последствия внутри незавершённого шага движения. */
object StoryMapEvents {
    /** Как и вход в нейтральный город, сюжет доступен живым наземным военным героям участников. */
    fun isEligible(unit: MapUnit): Boolean =
        !unit.isDestroyed && unit.isMilitary() && unit.baseUnit.isLandUnit() &&
            !unit.civInfo.isBarbarian() && !unit.civInfo.isSpectator()

    /** Сохраняет значения, а не ссылки на юнит и клетку, для очереди и последующего сохранения. */
    fun event(unit: MapUnit, kind: StoryEventKind, tile: TileInfo, from: TileInfo? = null, targetKey: String = ""): StoryEvent {
        val game = unit.civInfo.gameInfo
        return StoryEvent(
            kind = kind, actorKey = StoryWorldEffects.actorKey(unit), heroId = unit.scenarioHeroId,
            ownerName = unit.owner,
            ownerSlotId = game.scenarioVictoryParticipants.entries.firstOrNull { it.value == unit.owner }?.key.orEmpty(),
            from = from?.let { CityPosition(it.position.x, it.position.y) },
            position = CityPosition(tile.position.x, tile.position.y), targetKey = targetKey
        )
    }

    /** Вызывается после обновления видимости на клетке, где герой может остановиться; применение выполняется после размещения. */
    fun queueStep(unit: MapUnit, from: TileInfo, previousVisible: Set<TileInfo>) {
        val game = unit.civInfo.gameInfo
        val scenario = game.tileMap.story ?: return
        if (!isEligible(unit)) return
        fun queue(event: StoryEvent) {
            game.storyProgress = ProcessStoryEvent.execute(scenario, game.storyProgress, event)
        }
        queue(event(unit, StoryEventKind.HeroMoved, unit.currentTile, from))
        // Явный порядок координат делает очередь независимой от обхода HashSet видимости.
        for (tile in unit.viewableTiles.filter { it !in previousVisible }
            .sortedWith(compareBy({ it.position.x }, { it.position.y }))) {
            if (tile.improvement != null || tile.isCityCenter())
                queue(event(unit, StoryEventKind.ObjectSeen, tile, from))
            for (seen in tile.getUnits().filter { it !== unit && !it.isInvisible(unit.civInfo) }
                .sortedBy { StoryWorldEffects.actorKey(it) }) {
                queue(event(unit, StoryEventKind.ArmySeen, tile, from, StoryWorldEffects.actorKey(seen)))
            }
        }
    }

    /** Посещение относится только к реально достигнутой клетке, а не к дальней цели маршрута. */
    fun visit(unit: MapUnit, tile: TileInfo) {
        if (unit.currentTile !== tile || !isEligible(unit) || unit.civInfo.gameInfo.tileMap.story == null) return
        StoryRuntime.emit(unit.civInfo.gameInfo, event(unit, StoryEventKind.ObjectVisited, tile))
    }
}
