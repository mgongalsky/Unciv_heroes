package com.unciv.logic.story

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.pure.domain.city.CityId
import com.unciv.pure.domain.story.PendingStoryEffect
import com.unciv.pure.domain.story.StoryEffectKind
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

/** Граница игровых последствий: подготовка не меняет ресурсы, клетки или состав армий мира. */
object StoryWorldEffects {
    /** Возвращает краткую операцию публикации после проверки всех необходимых объектов. */
    fun prepare(game: GameInfo, pending: PendingStoryEffect): () -> Unit {
        val effect = pending.effect
        val ownerName = if (effect.recipientSlotId.isEmpty()) pending.event.ownerName
            else requireNotNull(game.scenarioVictoryParticipants[effect.recipientSlotId]) { "Unknown story recipient slot" }
        fun recipient() = requireNotNull(game.civilizations.firstOrNull { it.civName == ownerName }) {
            "Story recipient no longer exists: $ownerName"
        }
        return when (effect.kind) {
            StoryEffectKind.GrantGold -> {
                require(effect.amount > 0) { "Story reward must be positive" }
                val owner = recipient()
                val apply: () -> Unit = { owner.addGold(effect.amount) }
                apply
            }
            StoryEffectKind.GrantFood -> {
                require(effect.amount > 0) { "Story food reward must be positive" }
                val owner = recipient()
                val hero = requireNotNull(owner.getCivUnits().firstOrNull {
                    !it.isDestroyed && actorKey(it) == pending.event.actorKey
                }) { "Story food recipient hero no longer exists" }
                val food = hero.hero.currentFood + effect.amount.toFloat()
                require(food.isFinite()) { "Story food reward overflows" }
                val apply: () -> Unit = { hero.hero.setFood(food) }
                apply
            }
            StoryEffectKind.SetGarrison -> {
                val city = requireNotNull(game.cityRegistry.byId(CityId(effect.reference))) { "Story garrison city no longer exists" }
                require(effect.amount > 0 && effect.unitName in game.ruleSet.units) { "Invalid story garrison troops" }
                val army = ArmyInfo(city.ownerOrNull)
                require(army.maxSlots > 0) { "Garrison has no slots" }
                army.setTroopAt(0, TroopFactory.create(effect.unitName, effect.amount, RulesetTroopDefinitionSource(game.ruleSet)))
                val apply: () -> Unit = { city.garrisonInfo = army }
                apply
            }
            StoryEffectKind.SpawnArmy -> {
                val owner = recipient()
                require(!owner.isSpectator()) { "A spectator cannot receive a story army" }
                require(effect.amount > 0) { "Story army amount must be positive" }
                val definition = requireNotNull(game.ruleSet.units[effect.unitName]) { "Unknown story army unit" }
                require(definition.isLandUnit() && !definition.isCivilian()) { "Story army requires a land military unit" }
                val position = requireNotNull(effect.position) { "Story army has no position" }
                val vector = Vector2(position.x, position.y)
                require(vector in game.tileMap) { "Story army position is outside the map" }
                val tile = game.tileMap[vector]
                require(tile.getUnits().none() && tile.isLand && !tile.isImpassible()) { "Story army position is unavailable" }
                require(!tile.isCityCenter() || tile.getCity()?.ownerOrNull === owner) { "Story army cannot occupy another owner's city" }
                val unit = definition.getMapUnit(owner)
                unit.currentTile = tile
                require(unit.movement.canMoveTo(tile)) { "Story army cannot enter position" }
                val army = ArmyInfo(owner)
                require(army.maxSlots > 0) { "Story army has no slots" }
                army.setTroopAt(0, TroopFactory.create(effect.unitName, effect.amount, RulesetTroopDefinitionSource(game.ruleSet)))
                army.setTransients(owner, unit)
                unit.army = army
                unit.originalOwner = owner.civName
                unit.currentMovement = 0f
                // Прямое размещение не является движением и не запускает новое посещение или захват.
                val apply: () -> Unit = {
                    tile.militaryUnit = unit
                    unit.assignOwner(owner, false)
                    unit.addMovementMemory()
                    unit.refreshProtectedTiles()
                    unit.updateVisibleTiles()
                    owner.updateSightAndResources()
                    owner.updateStatsForNextTurn()
                }
                apply
            }
            else -> throw IllegalArgumentException("${effect.kind} is handled by the story coordinator")
        }
    }

    /** Сценарная личность переживает улучшение юнита; обычный герой использует его сохраняемый числовой ID. */
    fun actorKey(unit: MapUnit): String =
        if (unit.scenarioHeroId.isNotEmpty()) "hero:${unit.scenarioHeroId}" else "unit:${unit.id}"
}
