package com.unciv.ui.cityscreen

import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.hero.HeroFactory
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.pure.infrastructure.hero.RulesetHeroDefinitionSource
import com.unciv.ui.images.ImageGetter

/** Адаптер автономного просмотра: настоящее окружение города без мирового экрана и цикла ходов. */
object CitySandbox {
    /** Вызывается на GL-потоке после загрузки ruleset, шрифтов и Skin приложением. */
    fun createScreen(): CityScreen {
        val game = UncivGame.Current
        check(game.worldScreen == null && game.gameInfo == null) {
            "City sandbox must start independently of an active game"
        }
        val city = createCity()
        ImageGetter.setNewRuleset(city.getRuleset())
        // Модель принадлежит экрану: не вызываем loadGame и не включаем автосохранение партии.
        return CityScreen(
            city,
            context = CityScreenContext(
                canChangeState = { true },
                isPlayersTurn = { true },
                recordTutorialTasks = false
            )
        )
    }

    /** Создаёт каждый раз отдельную небольшую модель, используя определения действующей игры. */
    private fun createCity(): CityInfo {
        val ruleset = RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName)
        for (unit in ruleset.units.values) unit.ruleset = ruleset
        val world = GameInfo().apply {
            ruleSet = ruleset
            gameParameters.baseRuleset = BaseRuleset.Civ_V_GnK.fullName
            gameParameters.numberOfCityStates = 0
            gameParameters.noBarbarians = true
            difficulty = "Prince"
            difficultyObject = ruleset.difficulties.getValue(difficulty)
            speed = ruleset.speeds.getValue(gameParameters.speed)
            turns = 1
            tileMap = TileMap(6, ruleset, false)
            tileMap.gameInfo = this
        }
        val civilization = CivilizationInfo().apply {
            civName = "Rome"
            nation = ruleset.nations.getValue(civName)
            gameInfo = world
            playerType = PlayerType.Human
        }
        world.civilizations.add(civilization)
        world.currentPlayer = civilization.civName
        world.currentPlayerCiv = civilization
        ruleset.technologies.values.minByOrNull { it.era() }?.let {
            civilization.tech.techsResearched.add(it.name)
        }
        civilization.setTransients()
        val founder = MapUnit().apply {
            civInfo = civilization
            owner = civilization.civName
            army.setTransients(civilization, this)
        }
        val city = CityInfo(civilization, Vector2(0f, 0f), founder).apply {
            name = "City Sandbox"
            cityConstructions.constructionQueue.clear()
            cityConstructions.constructionQueue.add("Granary")
        }
        civilization.exploredTiles = world.tileMap.values.map { it.position.cpy() }.toHashSet()
        world.setTransients()
        val hero = checkNotNull(civilization.placeUnitNearTile(city.location, "Warrior"))
        check(city.getVisitingHero() === hero) { "Sandbox hero must visit the city" }
        hero.hero = HeroFactory.create(
            source = RulesetHeroDefinitionSource(ruleset),
            unitName = "Warrior", currentFood = 10f, morale = 2, luck = 3
        )
        // Вместимость берётся из настроек армии игры, а не из демонстрационной модели бандла.
        hero.army = ArmyInfo(civilization).apply {
            setTroopAt(0, TroopFactory.create("Spearman", 10, RulesetTroopDefinitionSource(ruleset)))
        }
        hero.army.setTransients(civilization, hero)
        city.cityStats.update()
        return city
    }
}
