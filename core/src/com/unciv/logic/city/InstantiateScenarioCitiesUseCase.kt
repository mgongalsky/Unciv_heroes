package com.unciv.logic.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.ScenarioMapValidation
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

/**
 * Создаёт обычные игровые CityInfo из исходных описаний карты только при запуске миссии.
 * Загрузка партии восстанавливает текущие города из сохранения и эту операцию не вызывает.
 * Основание и завоевание не выполняются: начальные данные берутся из описания без наград и потерь.
 */
object InstantiateScenarioCitiesUseCase {
    /** participants связывает стабильные ID слотов карты с уже созданными цивилизациями запуска. */
    fun execute(
        game: GameInfo,
        participants: Map<String, CivilizationInfo>,
        resolveType: (ScenarioCity, CivilizationInfo?) -> String = { description, owner ->
            when (description.typeMode) {
                ScenarioCityTypeMode.Fixed -> description.fixedType
                ScenarioCityTypeMode.FromStartingPlayer -> requireNotNull(owner).civName
            }
        }
    ): List<CityInfo> {
        val map = game.tileMap
        ScenarioMapValidation.validateStructure(map)
        val errors = ScenarioMapValidation.definitionErrors(map, game.ruleSet)
        require(errors.isEmpty()) { errors.joinToString("\n") }
        val bindings = ScenarioPlayerBindings.resolve(map.scenarioPlayerSlots, participants.toList())
        for (owner in bindings.values) {
            require(owner.gameInfo === game && game.civilizations.any { it === owner } &&
                !owner.isBarbarian() && !owner.isSpectator()) { "Invalid scenario participant ${owner.civName}" }
        }
        require(bindings.values.distinct().size == bindings.size) { "A participant occupies multiple scenario slots" }
        // Столичные предпочтения разрешаются на копиях: исходная миссия должна оставаться неизменной.
        val descriptions = ScenarioCityCapitals.resolve(map.scenarioCities)
        val resolved = descriptions.map { description ->
            val owner = if (description.owner == ScenarioCityOwner.PlayerSlot)
                bindings.getValue(description.ownerSlotId) else null
            val type = resolveType(description.deepCopy(), owner)
            require(type.isNotBlank()) { "Scenario city ${description.id}: unresolved type" }
            require(game.cityRegistry.byId(CityId(description.id)) == null) {
                "Scenario city ${description.id} already exists"
            }
            val center = map[Vector2(description.position.x, description.position.y)]
            require(center.isLand && !center.isImpassible() && !center.isNaturalWonder()) {
                "Scenario city ${description.id}: unsuitable center"
            }
            require(center.getUnits().all { owner != null && it.civInfo === owner }) {
                "Scenario city ${description.id}: center is occupied by another participant"
            }
            for (position in description.territory.ifEmpty { listOf(description.position) }) {
                require(map[Vector2(position.x, position.y)].getCity() == null) {
                    "Scenario city ${description.id}: territory is already occupied"
                }
            }
            val army = ArmyInfo(owner)
            require(description.garrison.size <= army.maxSlots) {
                "Scenario city ${description.id}: garrison exceeds ${army.maxSlots} slots"
            }
            Triple(owner, type, army)
        }
        // Это внутренний инвариант результата resolve, а не требование двух галочек от автора карты.
        for ((slotId, owner) in bindings) {
            val owned = descriptions.filter { it.owner == ScenarioCityOwner.PlayerSlot && it.ownerSlotId == slotId }
            if (owned.isEmpty()) continue
            require(owned.count { it.isCapital } == 1 && owner.getCapital() == null) {
                "Scenario slot $slotId requires exactly one current capital"
            }
            require(owned.count { it.isOriginalCapital } == 1 && owner.cities.none { it.isOriginalCapital }) {
                "Scenario slot $slotId requires exactly one original capital"
            }
        }
        val source = RulesetTroopDefinitionSource(game.ruleSet)
        // Сначала строим все объекты вне реестра: ошибка подготовки не должна опубликовать часть городов.
        val cities = descriptions.mapIndexed { index, description ->
            val (owner, type, army) = resolved[index]
            description.garrison.forEachIndexed { slot, troop ->
                army.setTroopAt(slot, TroopFactory.create(troop.unitName, troop.amount, source))
            }
            CityInfo().apply {
                id = description.id
                name = description.name
                state.position = description.position
                state.resolvedCityType = type
                state.isCurrentCapital = description.isCapital
                isOriginalCapital = description.isOriginalCapital
                foundingCiv = owner?.civName ?: ""
                turnAcquired = game.turns
                state.ownership = CityOwnership.Neutral
                if (owner == null) bindNeutralWorld(game) else civInfo = owner
                garrisonInfo = army
                // Инициализация без роста, голодания и побочных эффектов обычного изменения населения.
                population.initializeForScenario(description.population, description.foodStored)
                cityConstructions.builtBuildings.addAll(description.buildings)
                tiles = description.territory.ifEmpty { listOf(description.position) }
                    .mapTo(HashSet()) { Vector2(it.x, it.y) }
            }
        }
        for (city in cities) game.cityRegistry.register(city)
        for (city in cities) city.setTransients()
        // После публикации городов согласуем производные данные владельцев и первоначальные назначения.
        for ((_, owner) in bindings) {
            val owned = cities.filter { it.ownerOrNull === owner }
            if (owned.isEmpty()) continue
            owner.citiesCreated += owned.size
            owner.hasEverOwnedOriginalCapital = true
            owner.initialSetCitiesConnectedToCapitalTransients()
            owner.updateSightAndResources()
            for (city in owned) city.population.autoAssignPopulation()
            owner.updateStatsForNextTurn()
        }
        return cities
    }
}
