package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.ui.battlescreen.populateBattleTroopGroup
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.popup.Popup
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.math.abs
import kotlin.math.roundToInt
import com.unciv.pure.application.battle.BattleMovementPreviewUseCase.TileStyle
import com.unciv.ui.battlescreen.createBattleMovementRangeOverlay
import com.unciv.ui.battlescreen.showBattleMovementStyle
import com.unciv.ui.battlescreen.createActiveTroopOutline

/** Opens the deterministic battle-troop scene shared by the playground and golden tests. */
fun openBattleTroopPreview(screen: BaseScreen) {
    val popup = Popup(screen.stage, scrollable = false)
    popup.defaults().pad(10f)
    popup.add("Swordsman attacks Crossbowman".toLabel(fontSize = 24))
        .padTop(28f)
        .padBottom(62f)
    popup.row()
    popup.add(createBattleFieldPreview()).padLeft(24f).padRight(24f).padBottom(28f)
    popup.row()
    popup.add(
        "Close".toTextButton(BaseScreen.skin.get("fantasy", TextButtonStyle::class.java)).apply {
            label.setFontScale(0.4f)
            label.setAlignment(Align.center)
            pad(5f, 10f, 5f, 10f)
            onActivation { popup.close() }
        }
    ).width(190f).height(58f).padBottom(18f)
    popup.open(force = true)
}

private fun createBattleFieldPreview(): Group {
    val field = TileMap(
        width = 5,
        height = 3,
        ruleset = RulesetCache.getVanillaRuleset(),
        fillTerra = Constants.grassland
    )
    val tileSetStrings = TileSetStrings(
        tileSet = Constants.defaultTileset,
        unitSet = Constants.defaultUnitset
    )
    val tileGroups = field.values.map { tile ->
        TileGroup(tile, tileSetStrings).apply {
            showEntireMap = true
            update()
        }
    }
    val fieldView = TileGroupMap(tileGroups)
    val source = HardcodedTroopDefinitionSource(
        speed = 3,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0
    )
    val attacker = TroopFactory.create("Swordsman", 18, source).apply {
        formation.current = formation.maximum / 2
    }
    val defender = TroopFactory.create("Crossbowman", 12, source).apply {
        formation.current = 0
    }
    val middleRow = tileGroups
        .groupBy { it.y.roundToInt() }
        .minBy { (rowY, _) -> abs(rowY - fieldView.height / 2f) }
        .value
        .sortedBy { it.x }
    val attackerTile = middleRow.first()
    val defenderTile = middleRow.last()

    val attackerGroup = Group()
    populateBattleTroopGroup(
        attackerGroup, attacker, true,
        attackerTile.width, attackerTile.height,
        attackerTile.originX, attackerTile.originY
    )
    attackerGroup.addActorAt(
        0,
        createActiveTroopOutline(
            attacker,
            true,
            attackerTile.width,
            attackerTile.height,
            attackerTile.originX,
            attackerTile.originY,
            animated = false
        )
    )
    attackerTile.addActor(attackerGroup)

    populateBattleTroopGroup(
        defenderTile, defender, false,
        defenderTile.width, defenderTile.height,
        defenderTile.originX, defenderTile.originY
    )
    middleRow.drop(1).dropLast(1).forEachIndexed { index, tileGroup ->
        createBattleMovementRangeOverlay(tileGroup).showBattleMovementStyle(
            if (index == 0) TileStyle.SAFE else TileStyle.FORMATION_PENALTY
        )
    }
    return fieldView
}

fun openBattleThreatPreview(screen: BaseScreen) {
    val source = HardcodedTroopDefinitionSource(
        speed = 3,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 2
    )
    val attacker = listOf(
        TroopFactory.create("Swordsman", 18, source),
        TroopFactory.create("Archer", 12, source)
    )
    val defender = listOf(
        TroopFactory.create("Archer", 5, source),
        TroopFactory.create("Archer", 7, source),
        TroopFactory.create("Spearman", 15, source),
        TroopFactory.create("Crossbowman", 10, source)
    )
    val popup = Popup(screen.stage, scrollable = false)
    popup.defaults().pad(10f)
    popup.add("Battle reconnaissance".toLabel(fontSize = 24)).padTop(22f).row()
    popup.add(
        com.unciv.ui.worldscreen.bottombar.buildBattleThreatPreview(
            com.unciv.ui.worldscreen.bottombar.BattleThreatArmyPreview(5, 5, attacker),
            com.unciv.ui.worldscreen.bottombar.BattleThreatArmyPreview(5, 5, defender)
        )
    ).width(560f).pad(20f)
    popup.open(force = true)
}
