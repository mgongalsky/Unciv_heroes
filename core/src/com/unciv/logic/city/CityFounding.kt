package com.unciv.logic.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.Proximity
import com.unciv.logic.civilization.ReligionState
import com.unciv.logic.civilization.diplomacy.DiplomacyFlags
import com.unciv.models.ruleset.Nation
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import com.unciv.logic.map.TileInfo

/** Игровые эффекты основания. Доступность действия и дипломатическое подтверждение проверяет вызывающая сторона. */
object CityFounding {
    /** Создаёт и регистрирует город; выбор производства и уничтожение основателя выполняются отдельно. */
    fun create(civInfo: CivilizationInfo, cityLocation: Vector2, foundingUnit: MapUnit): CityInfo {
        val city = CityInfo()
        initialize(city, civInfo, cityLocation, foundingUnit)
        return city
    }

    /** Общий путь с совместимым конструктором CityInfo; вызывается однократно для нового города. */
    internal fun initialize(city: CityInfo, civInfo: CivilizationInfo, cityLocation: Vector2, foundingUnit: MapUnit) {
        with(city) {
            this.civInfo = civInfo
            foundingCiv = civInfo.civName
            turnAcquired = civInfo.gameInfo.turns
            location = cityLocation

            // Активная армия получает прежний стартовый состав; рефакторинг не меняет баланс основания.
            garrisonInfo = ArmyInfo(
                civInfo,
                "Archer" to 35,
                 "Peasant" to 20
                //"Horseman" to 8,
                //"Swordsman" to 20
            )

            // Старое представление гарнизона пока сохраняется отдельно и имеет свой исторический состав.
            garrison.add(TroopInfo(30, "Archer"))
            garrison.add(TroopInfo(20, "Spearman"))
            garrison.add(TroopInfo(10, "Horseman"))
            garrison.add(TroopInfo(15, "Swordsman"))

            setTransients()

            name = generateNewCityName(
                civInfo,
                civInfo.gameInfo.civilizations.asSequence().filter { civ -> civ.isAlive() }.toSet(),
                arrayListOf("New ", "Neo ", "Nova ", "Altera ")
            ) ?: "City Without A Name"

            isOriginalCapital = civInfo.citiesCreated == 0
            if (isOriginalCapital) {
                civInfo.hasEverOwnedOriginalCapital = true
                // Культура могла накопиться до первого города; теперь выбор первой политики становится возможен.
                civInfo.policies.shouldOpenPolicyPicker = true
            }
            civInfo.citiesCreated++

            // Регистрация предшествует стартовым зданиям: первый город определяется через список владельца.
            civInfo.gameInfo.cityRegistry.register(city)

            val startingEra = civInfo.gameInfo.gameParameters.startingEra

            addStartingBuildings(civInfo, startingEra)

            // Порядок расширения и установки населения сохранён: оба этапа могут влиять на итоговую территорию.
            expansion.reset()

            tryUpdateRoadStatus()

            val tile = getCenterTile()
            for (terrainFeature in tile.terrainFeatures.filter {
                getRuleset().tileImprovements.containsKey(
                    "Remove $it"
                )
            })
                tile.removeTerrainFeature(terrainFeature)

            tile.changeImprovement(null)
            tile.improvementInProgress = null

            val ruleset = civInfo.gameInfo.ruleSet
            workedTiles = hashSetOf() // Рабочие клетки будут назначены заново после изменения населения.

            // Начальное население складывается из значения эпохи и вклада армии основателя.
            population.setPopulation(ruleset.eras[startingEra]!!.settlerPopulation + foundingUnit.calculateArmyPopulation())

            if (civInfo.religionManager.religionState == ReligionState.Pantheon) {
                religion.addPressure(
                    civInfo.religionManager.religion!!.name,
                    200 * population.population
                )
            }

            population.autoAssignPopulation()

            // Новый город меняет близость цивилизаций; уже достигнутое соседство пересчитывать не требуется.
            for (otherCiv in civInfo.gameInfo.getAliveMajorCivs()) {
                if (civInfo.getProximity(otherCiv) != Proximity.Neighbors)
                    civInfo.updateProximity(otherCiv,
                    otherCiv.updateProximity(civInfo))
            }
            for (otherCiv in civInfo.gameInfo.getAliveCityStates()) {
                if (civInfo.getProximity(otherCiv) != Proximity.Neighbors)
                    civInfo.updateProximity(otherCiv,
                        otherCiv.updateProximity(civInfo))
            }

            triggerCitiesSettledNearOtherCiv()
        }
    }

    /** Назначает первую столицу и выдаёт здания эпохи с учётом национальных замен и бесплатных построек. */
    private fun CityInfo.addStartingBuildings(civInfo: CivilizationInfo, startingEra: String) {
        val ruleset = civInfo.gameInfo.ruleSet
        if (civInfo.cities.size == 1) {
            state.isCurrentCapital = true
            cityConstructions.addBuilding(capitalCityIndicator())
        }

        for (buildingName in ruleset.eras[startingEra]!!.settlerBuildings) {
            val building = ruleset.buildings[buildingName] ?: continue
            val uniqueBuilding = civInfo.getEquivalentBuilding(building)
            if (uniqueBuilding.isBuildable(cityConstructions))
                cityConstructions.addBuilding(uniqueBuilding.name)
        }

        civInfo.civConstructions.tryAddFreeBuildings()
        cityConstructions.addFreeBuildings()
    }

    /**
     * Выбирает свободное имя с учётом городов всех живых цивилизаций.
     * Сначала использует список основателя, затем заимствование при соответствующем свойстве,
     * иначе перебирает префиксы с повторением до десяти раз. null означает, что имя не найдено.
     * @param foundingCiv Цивилизация, основывающая город.
     * @param aliveCivs Живые цивилизации для проверки занятых имён и заимствования.
     * @param prefixes Префиксы в порядке предпочтения после исчерпания обычных имён.
     */
    private fun CityInfo.generateNewCityName(
        foundingCiv: CivilizationInfo,
        aliveCivs: Set<CivilizationInfo>,
        prefixes: List<String>
    ): String? {
        val usedCityNames: Set<String> =
            aliveCivs.asSequence().flatMap { civilization ->
                civilization.cities.asSequence().map { city -> city.name }
            }.toSet()

        for (cityName in foundingCiv.nation.cities) {
            if (cityName !in usedCityNames) return cityName
        }

        if (foundingCiv.hasUnique(UniqueType.BorrowsCityNames)) {
            return borrowCityName(foundingCiv, aliveCivs, usedCityNames)
        }

        // TODO: Вынести префиксы в настройки наций и рассмотреть поддержку суффиксов.
        var candidate: String?
        for (number in (1..10)) {
            for (prefix in prefixes) {
                val currentPrefix: String = prefix.repeat(number)
                candidate = foundingCiv.nation.cities.firstOrNull { cityName ->
                    (currentPrefix + cityName) !in usedCityNames
                }
                if (candidate != null) return currentPrefix + candidate
            }
        }

        // Например, мод может вообще не задавать имена городов.
        return null
    }

    /** Заимствует свободное имя у другой крупной нации; при отсутствии кандидатов возвращает null. */
    private fun CityInfo.borrowCityName(
        foundingCiv: CivilizationInfo,
        aliveCivs: Set<CivilizationInfo>,
        usedCityNames: Set<String>
    ): String? {
        val aliveMajorNations: Sequence<Nation> =
            aliveCivs.asSequence().filter { civ -> civ.isMajorCiv() }.map { civ -> civ.nation }

        // От каждой другой живой нации берём последнее свободное имя и случайно выбираем одно из них.
        val otherMajorNations: Sequence<Nation> =
            aliveMajorNations.filter { nation -> nation != foundingCiv.nation }
        var newCityNames: Set<String> =
            otherMajorNations.mapNotNull { nation ->
                nation.cities.lastOrNull { city -> city !in usedCityNames }
            }.toSet()
        if (newCityNames.isNotEmpty()) return newCityNames.random()

        // Если имена участников исчерпаны, используем свободные имена крупных наций, отсутствующих в партии.
        val absentMajorNations: Sequence<Nation> =
            getRuleset().nations.values.asSequence().filter { nation ->
                nation.isMajorCiv() && nation !in aliveMajorNations
            }
        newCityNames =
            absentMajorNations.flatMap { nation ->
                nation.cities.asSequence().filter { city -> city !in usedCityNames }
            }.toSet()
        if (newCityNames.isNotEmpty()) return newCityNames.random()

        return null
    }

    /**
     * Уведомляет знакомых крупных соседей об основании в пределах шести клеток от их городов.
     * Флаг запускает последующую дипломатическую реакцию AI; просьбы и последствия обещаний обрабатываются отдельно.
     */
    private fun CityInfo.triggerCitiesSettledNearOtherCiv() {
        val citiesWithin6Tiles =
            civInfo.gameInfo.civilizations.asSequence()
                .filter { it.isMajorCiv() && it != civInfo }
                .flatMap { it.cities }
                .filter { it.getCenterTile().aerialDistanceTo(getCenterTile()) <= 6 }
        val civsWithCloseCities =
            citiesWithin6Tiles
                .map { it.civInfo }
                .distinct()
                .filter { it.knows(civInfo) && it.hasExplored(location) }
        for (otherCiv in civsWithCloseCities)
            otherCiv.getDiplomacyManager(civInfo).setFlag(DiplomacyFlags.SettledCitiesNearUs, 30)
    }

    /** Полное игровое действие: создаёт город, оформляет центр и уничтожает основателя. Обучение вызывается до, UI — после. */
    fun complete(unit: MapUnit, tile: TileInfo) {
        unit.civInfo.addCity(tile.position, unit)
        if (tile.ruleset.tileImprovements.containsKey("City center")) {
            if (unit.civInfo.isMajorCiv())
                tile.changeImprovement("City center")
            else
                tile.changeImprovement("Village City center")
        }
        if (tile.roadStatus != RoadStatus.None)
            tile.removeRoad()
        unit.destroy()
    }
}
