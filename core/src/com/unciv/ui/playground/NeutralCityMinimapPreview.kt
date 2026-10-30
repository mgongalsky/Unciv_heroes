package com.unciv.ui.playground

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.unciv.logic.GameInfo
import com.unciv.logic.city.InstantiateScenarioCitiesUseCase
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioCity
import com.unciv.pure.domain.city.ScenarioCityTypeMode
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.worldscreen.minimap.MinimapTile

/** Exercises the real minimap actors for an ownerless city in a GL session. */
fun openNeutralCityMinimapPreview(screen: BaseScreen) {
    val ruleset = RulesetCache.getVanillaRuleset()
    val game = GameInfo().apply {
        ruleSet = ruleset
        tileMap = TileMap(3, ruleset)
        tileMap.gameInfo = this
    }
    val center = game.tileMap[Vector2.Zero]
    val territory = center.neighbors.first { it.isLand && !it.isImpassible() }
    game.tileMap.scenarioCities.add(ScenarioCity(
        id = "minimap-neutral", name = "Neutral town",
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Default",
        territory = arrayListOf(CityPosition(), CityPosition(territory.position.x, territory.position.y))
    ))
    val city = InstantiateScenarioCitiesUseCase.execute(game, emptyMap()).single()
    check(city.ownerOrNull == null && center.getOwner() == null)
    val layer = Group()
    val centerTile = MinimapTile(center, 40f) {}
    layer.addActor(centerTile.image)
    centerTile.updateColor(true)
    centerTile.updateColor(false)
    check(centerTile.image.isVisible && centerTile.image.color == Color.LIGHT_GRAY)
    centerTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 2)
    centerTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 2) { "Refreshing a city circle must replace the previous actor" }
    centerTile.updateBorders().updateActorsIn(layer)
    check(layer.children.size == 2) { "Neutral territory must not acquire national borders" }
    val territoryTile = MinimapTile(territory, 40f) {}
    layer.addActor(territoryTile.image)
    territoryTile.updateColor(false)
    check(territoryTile.image.color == Color.GRAY)
    territoryTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 3) { "Only the city center gets a city circle" }
    layer.setPosition(screen.stage.width / 2, screen.stage.height / 2)
    screen.stage.addActor(layer)
}
