package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.Constants
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase.Appearance
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import com.unciv.ui.battlescreen.BattleTileHighlightRenderer
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.popup.Popup
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel

fun openBattleTileHighlightPreview(screen: BaseScreen) {
    val field = TileMap(
        width = 5,
        height = 3,
        ruleset = RulesetCache.getVanillaRuleset(),
        fillTerra = Constants.grassland
    )
    val strings = TileSetStrings(
        tileSet = Constants.defaultTileset,
        unitSet = Constants.defaultUnitset
    )
    val tiles = field.values.sortedWith(
        compareBy({ it.position.y }, { it.position.x })
    ).map { TileGroup(it, strings).apply { showEntireMap = true; update() } }
    val fieldView = TileGroupMap(tiles)
    val renderer = BattleTileHighlightRenderer()
    val styles = listOf(
        Appearance(),
        BattleTileAppearanceUseCase.execute(true, 1, 4, Strength.NONE, true),
        BattleTileAppearanceUseCase.execute(true, 3, 4, Strength.NONE, true),
        BattleTileAppearanceUseCase.execute(true, 1, 4, Strength.NORMAL, true),
        BattleTileAppearanceUseCase.execute(true, 3, 4, Strength.REINFORCED, true)
    )

    fun pointer(tile: TileGroup, type: InputEvent.Type) {
        tile.fire(InputEvent().apply {
            this.type = type
            this.pointer = -1
        })
    }

    tiles.forEachIndexed { index, tile ->
        renderer.bind(tile)
        val expected = styles[index % styles.size]
        renderer.update(tile, expected)
        check(tile.baseLayerGroup.color.a > 0.3f)
        pointer(tile, InputEvent.Type.enter)
        check(tile.baseLayerGroup.color.a == 0.3f)
        pointer(tile, InputEvent.Type.exit)
        check(tile.baseLayerGroup.color.a == expected.terrainAlpha)
        if (expected.control != Strength.NONE) {
            check(
                tile.highlightFogCrosshairLayerGroup
                    .findActor<Image>("battleZoneOfControlOverlay").isVisible
            )
        }
    }

    // An action changes the cached appearance while the pointer stays on the tile.
    val changingTile = tiles.first()
    pointer(changingTile, InputEvent.Type.enter)
    renderer.update(changingTile, styles[3])
    check(changingTile.baseLayerGroup.color.a == 0.3f)
    val marker = changingTile.highlightFogCrosshairLayerGroup
        .findActor<Image>("battleZoneOfControlOverlay")
    check(marker.isVisible)
    pointer(changingTile, InputEvent.Type.exit)
    check(changingTile.baseLayerGroup.color.a == 0.5f)
    check(marker.isVisible)
    renderer.update(changingTile, Appearance())
    check(!marker.isVisible)
    check(changingTile.baseLayerGroup.color.a == 1f)
    renderer.restoreAll()
    check(!marker.isVisible)

    // Support is independent of hover, movement shading and control markers.
    tiles.takeLast(5).forEach { tile ->
        renderer.updateSupport(tile, true)
        val support = tile.highlightFogCrosshairLayerGroup.findActor<Image>("battleSupportOverlay")
        check(support.isVisible)
        check(support.touchable == com.badlogic.gdx.scenes.scene2d.Touchable.disabled)
        check(support.color.a == 0.3f)
        pointer(tile, InputEvent.Type.enter)
        check(support.isVisible && support.color.a == 0.3f)
        renderer.updateSupport(tile, false)
        check(!support.isVisible)
        pointer(tile, InputEvent.Type.exit)
        renderer.restoreAll()
        check(!support.isVisible)
        renderer.updateSupport(tile, true)
        check(support.isVisible)
        check(tile.highlightFogCrosshairLayerGroup.children.count {
            it.name == "battleSupportOverlay"
        } == 1)
    }

    Popup(screen.stage, scrollable = false).apply {
        defaults().pad(12f)
        add("Battle movement, control and support".toLabel(fontSize = 24)).row()
        add("Dark: formation preserved. Light: formation penalty.".toLabel()).row()
        add("Amber: normal control. Red: reinforced control.".toLabel()).row()
        add("Translucent blue: mutual support (+25% damage).".toLabel()).row()
        add(fieldView).pad(30f).row()
        open(force = true)
    }
}
