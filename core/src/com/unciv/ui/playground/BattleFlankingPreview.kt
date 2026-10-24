package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.Constants
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.application.battle.BattleFlankingAppearance
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase.Appearance
import com.unciv.ui.battlescreen.BattleTileHighlightRenderer
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.popup.Popup
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.abs

/** Fixed animation phases keep screenshots independent of frame timing. */
fun openBattleFlankingPreview(screen: BaseScreen) {
    val field = TileMap(5, 3, RulesetCache.getVanillaRuleset(), fillTerra = Constants.grassland)
    val strings = TileSetStrings(tileSet = Constants.defaultTileset, unitSet = Constants.defaultUnitset)
    val tiles = field.values.sortedWith(compareBy({ it.position.y }, { it.position.x }))
        .map { TileGroup(it, strings).apply { showEntireMap = true; update() } }
    val renderer = BattleTileHighlightRenderer()
    tiles.forEach { renderer.bind(it); renderer.update(it, Appearance()) }

    fun overlay(tile: TileGroup): Image =
        tile.highlightFogCrosshairLayerGroup.findActor("battleFlankingOverlay")
    fun expectAlpha(image: Image, expected: Float) {
        check(abs(image.color.a - expected) < 0.0001f) {
            "Flanking alpha: expected $expected, got ${image.color.a}"
        }
    }

    val probe = tiles.first()
    renderer.updateFlanking(probe, 0, 1)
    val animated = overlay(probe)
    animated.act(BattleFlankingAppearance.PULSE_SECONDS / 4f)
    expectAlpha(animated, BattleFlankingAppearance.intensity(1) / 2f)
    renderer.updateFlanking(probe, 0, 1)
    animated.act(BattleFlankingAppearance.PULSE_SECONDS / 4f)
    expectAlpha(animated, BattleFlankingAppearance.intensity(1))
    renderer.updateFlanking(probe, 0)
    renderer.restoreAll()
    check(!animated.isVisible)
    renderer.updateFlanking(probe, 1, 2)
    animated.act(BattleFlankingAppearance.PULSE_SECONDS / 2f)
    expectAlpha(animated, BattleFlankingAppearance.intensity(2))
    renderer.updateFlanking(probe, 1)
    expectAlpha(animated, BattleFlankingAppearance.intensity(1))
    check(animated.isVisible)

    tiles.forEachIndexed { index, tile ->
        val column = index % 5
        val row = index / 5
        val phase = column / 4f
        val current = when (row) { 0 -> 0; 1 -> 1; else -> column }
        val preview = when (row) { 0 -> 1; 1 -> 2; else -> null }
        if (index == tiles.lastIndex) renderer.updateSupport(tile, true)
        renderer.updateFlanking(tile, current, preview, fixedPhase = phase)
        if (current > 0 || preview != null) {
            val image = overlay(tile)
            expectAlpha(image, BattleFlankingAppearance.alpha(current, preview, phase))
            image.act(10f)
            expectAlpha(image, BattleFlankingAppearance.alpha(current, preview, phase))
            check(image.touchable == Touchable.disabled)
            check(tile.highlightFogCrosshairLayerGroup.children.count {
                it.name == "battleFlankingOverlay"
            } == 1)
        }
    }

    Popup(screen.stage, scrollable = false).apply {
        defaults().pad(10f)
        add("Flanking: current encirclement and attack preview".toLabel(fontSize = 24)).row()
        add("Waves: transparent to level 1; level 1 to level 2.".toLabel()).row()
        add("Five phases: start, rising, peak, falling, end.".toLabel()).row()
        add("Static levels 0-4; strongest tile also has Support.".toLabel()).row()
        add(TileGroupMap(tiles)).pad(30f).row()
        open(force = true)
    }
}
