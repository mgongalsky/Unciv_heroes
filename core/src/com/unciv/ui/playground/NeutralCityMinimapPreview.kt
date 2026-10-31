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

/** Проверяет настоящие GL-акторы нейтрального города: спрайт замка, цвета миникарты и отсутствие национальных границ. */
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
    val strings = com.unciv.ui.tilegroups.TileSetStrings()
    val castle = com.unciv.ui.tilegroups.TileGroup(center, strings).apply {
        showEntireMap = true
        update()
    }
    // Отрисовка выбирает спрайт центра, не записывая улучшение в данные карты.
    check(center.improvement == null) { "Rendering must not write a city improvement into the map" }
    check(castle.tileBaseImages.any { it.name?.contains("City center") == true }) {
        "A runtime neutral city must render the existing castle sprite without a founding side effect"
    }
    val implicitImages = castle.tileBaseImages.map { it.name }
    center.improvement = "City center"
    castle.update()
    check(castle.tileBaseImages.map { it.name } == implicitImages) {
        "City rendering must agree with the existing founded-city improvement rendering"
    }
    center.improvement = null
    castle.setPosition(screen.stage.width / 2 - 100f, screen.stage.height / 2)
    screen.stage.addActor(castle)
    val layer = Group()
    val centerTile = MinimapTile(center, 40f) {}
    layer.addActor(centerTile.image)
    // Переход от скрытой клетки к открытой должен работать и без изменения владельца (null → null).
    centerTile.updateColor(true)
    centerTile.updateColor(false)
    check(centerTile.image.isVisible && centerTile.image.color == Color.LIGHT_GRAY)
    centerTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 2)
    centerTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 2) { "Refreshing a city circle must replace the previous actor" }
    centerTile.updateBorders().updateActorsIn(layer)
    check(layer.children.size == 2) { "Neutral territory must not acquire national borders" }
    // Обычная клетка территории имеет серый цвет, но не получает круг центра города.
    val territoryTile = MinimapTile(territory, 40f) {}
    layer.addActor(territoryTile.image)
    territoryTile.updateColor(false)
    check(territoryTile.image.color == Color.GRAY)
    territoryTile.updateCityCircle().updateActorsIn(layer)
    check(layer.children.size == 3) { "Only the city center gets a city circle" }
    layer.setPosition(screen.stage.width / 2, screen.stage.height / 2)
    screen.stage.addActor(layer)
}
