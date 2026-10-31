package com.unciv.logic.scenario

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.ScenarioMapValidation
import com.unciv.pure.application.scenario.PrepareScenarioHeroesUseCase
import com.unciv.pure.domain.city.CityPosition

/** Создаёт героев только при старте миссии; загрузка сохранения не вызывает эту операцию. */
object InstantiateScenarioHeroesUseCase {
    /** Проверяет весь набор до размещения; старт не является посещением клетки или захватом города. */
    fun execute(game: GameInfo): List<MapUnit> {
        val map = game.tileMap
        if (map.scenarioHeroes.isEmpty()) return emptyList()
        ScenarioMapValidation.validateStructure(map)
        val errors = ScenarioMapValidation.definitionErrors(map, game.ruleSet)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val plans = PrepareScenarioHeroesUseCase.execute(
            map.scenarioHeroes, map.scenarioPlayerSlots,
            map.values.mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) },
            game.scenarioVictoryParticipants.toList(), game.ruleSet.units.keys,
            map.values.filter { it.getUnits().any() }
                .mapTo(HashSet()) { CityPosition(it.position.x, it.position.y) }
        )
        val existingIds = game.civilizations.flatMap { it.getCivUnits().toList() }
            .mapTo(HashSet()) { it.scenarioHeroId }
        val prepared = plans.map { plan ->
            val description = plan.description
            require(description.id !in existingIds) { "Scenario hero ${description.id} already exists" }
            val owner = game.getCivilization(plan.owner)
            require(!owner.isBarbarian() && !owner.isSpectator()) {
                "Scenario hero ${description.id}: invalid owner"
            }
            val tile = map[Vector2(description.position.x, description.position.y)]
            require(tile.isLand && !tile.isImpassible() && !tile.isNaturalWonder()) {
                "Scenario hero ${description.id}: unsuitable position"
            }
            require(!tile.isCityCenter() || tile.getCity()?.ownerOrNull === owner) {
                "Scenario hero ${description.id}: city belongs to another owner"
            }
            // getMapUnit подготавливает характеристики, но не регистрирует юнит в цивилизации или клетке.
            val unit = game.ruleSet.units.getValue(description.unitName).getMapUnit(owner)
            unit.currentTile = tile
            require(unit.movement.canMoveTo(tile)) { "Scenario hero ${description.id}: cannot occupy position" }
            unit.scenarioHeroId = description.id
            unit.instanceName = description.name
            unit.originalOwner = owner.civName
            unit.currentMovement = unit.getMaxMovement().toFloat()
            unit to tile
        }
        // Не вызываем putInTile: он запускает эффекты движения и посещения во время создания мира.
        for ((unit, tile) in prepared) {
            tile.militaryUnit = unit
            unit.assignOwner(unit.civInfo, false)
            unit.addMovementMemory()
        }
        for ((unit, _) in prepared) {
            for (promotion in unit.baseUnit.promotions) unit.promotions.addPromotion(promotion, true)
            unit.refreshProtectedTiles()
            unit.updateVisibleTiles()
        }
        for (owner in prepared.map { it.first.civInfo }.distinct()) {
            owner.updateSightAndResources()
            owner.updateStatsForNextTurn()
        }
        return prepared.map { it.first }
    }
}
