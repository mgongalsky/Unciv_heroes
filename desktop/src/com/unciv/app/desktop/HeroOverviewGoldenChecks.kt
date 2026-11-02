package com.unciv.app.desktop

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.hero.HeroFactory
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.pure.infrastructure.hero.RulesetHeroDefinitionSource
import com.unciv.ui.heroscreen.HeroOverviewScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable

/** Builds a local world without saving it or replacing the running application's DI context. */
internal object HeroOverviewGoldenChecks {
    fun screen(): BaseScreen {
        val ruleset = RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName)
        for (unit in ruleset.units.values) unit.ruleset = ruleset
        val world = GameInfo().apply {
            ruleSet = ruleset
            difficulty = "Prince"
            difficultyObject = ruleset.difficulties.getValue(difficulty)
            speed = ruleset.speeds.getValue(gameParameters.speed)
            gameParameters.numberOfCityStates = 0
            gameParameters.noBarbarians = true
            tileMap = TileMap(3, ruleset, false)
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
        val hero = checkNotNull(civilization.placeUnitNearTile(Vector2.Zero, "Warrior"))
        hero.hero = HeroFactory.create(
            source = RulesetHeroDefinitionSource(ruleset),
            unitName = "Warrior", currentFood = 150f, morale = 2, luck = 3
        )
        val troopSource = RulesetTroopDefinitionSource(ruleset)
        hero.army = ArmyInfo(civilization, 7).apply {
            repeat(7) { setTroopAt(it, TroopFactory.create("Spearman", 10 + it, troopSource)) }
        }
        hero.army.setTransients(civilization, hero)
        val amounts = hero.army.getAllTroops().map { it?.amount }
        val food = hero.hero.currentFood
        val first = HeroOverviewScreen(hero)
        val recreated = try {
            first.stage.root.children.filterIsInstance<Layout>().forEach { it.validate() }
            first.recreate() as HeroOverviewScreen
        } finally {
            first.dispose()
        }
        try {
            recreated.stage.root.children.filterIsInstance<Layout>().forEach { it.validate() }
            val stats = checkNotNull(recreated.stage.root.findActor<Table>("heroOverviewStats"))
            stats.validate()
            check(stats.prefWidth <= recreated.stage.width + 1f) { "Hero statistics exceed stage width" }
            if (recreated.game.settings.skin == "Minimal")
                check(stats.background is RoyalPanelDrawable)
            check(hero.army.getAllTroops().map { it?.amount } == amounts)
            check(hero.hero.currentFood == food && hero.morale == 2 && hero.luck == 3)
            return recreated
        } catch (failure: Throwable) {
            recreated.dispose()
            throw failure
        }
    }
}
