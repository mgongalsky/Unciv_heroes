package com.unciv.ui.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.ui.battlescreen.BattleResultPopup
import com.unciv.ui.battlescreen.populateBattleTroopGroup
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.popup.Popup
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.utils.Align
import com.unciv.ui.utils.extensions.onActivation
import kotlin.math.abs
import kotlin.math.roundToInt

class UiPlaygroundScreen : BaseScreen() {
    private data class Scenario(
        val title: String,
        val description: String,
        val show: (BaseScreen) -> Unit
    )

    private val scenarios = listOf(
        troopBattleViewScenario(),
        battleResultScenario(
            title = "Battle result — attacker victory",
            description = "All six supported Heroic Civs troop sprites in one compact result.",
            report = BattleReport(
                winner = BattleSide.ATTACKER,
                attackerLosses = listOf(
                    loss(1, "Peasant", 13),
                    loss(2, "Spearman", 8),
                    loss(3, "Archer", 6)
                ),
                defenderLosses = listOf(
                    loss(4, "Swordsman", 13),
                    loss(5, "Horseman", 12),
                    loss(6, "Crossbowman", 7)
                )
            )
        ),
        battleResultScenario(
            title = "Battle result — defender victory",
            description = "A compact result with one side taking no losses.",
            report = BattleReport(
                winner = BattleSide.DEFENDER,
                attackerLosses = listOf(
                    loss(1, "Peasant", 20),
                    loss(2, "Spearman", 14),
                    loss(3, "Archer", 5)
                ),
                defenderLosses = emptyList()
            )
        ),
        battleResultScenario(
            title = "Battle result — mutual defeat",
            description = "Both armies are destroyed, using only the supported troop sprites.",
            report = BattleReport(
                winner = null,
                attackerLosses = listOf(
                    loss(1, "Swordsman", 10),
                    loss(2, "Horseman", 15)
                ),
                defenderLosses = listOf(
                    loss(3, "Crossbowman", 12),
                    loss(4, "Peasant", 18)
                )
            )
        )
    )

    init {
        val selector = SelectBox<String>(BaseScreen.skin).apply {
            setItems(*scenarios.map { it.title }.toTypedArray())
        }
        val description = Label(scenarios.first().description, BaseScreen.skin).apply {
            setWrap(true)
        }
        selector.onChange {
            description.setText(scenarios[selector.selectedIndex].description)
        }

        val content = Table(BaseScreen.skin).apply {
            defaults().pad(10f)
            add("UI playground".toLabel(fontSize = 30)).padBottom(20f)
            row()
            add("Choose a window and preview it with deterministic fake data.".toLabel())
                .padBottom(10f)
            row()
            add(selector).width(520f).height(50f)
            row()
            add(description).width(520f).minHeight(60f)
            row()
            add("Preview".toTextButton().onClick {
                scenarios[selector.selectedIndex].show(this@UiPlaygroundScreen)
            }).width(200f).height(55f).padTop(10f)
            row()
            add("Exit".toTextButton().onClick { Gdx.app.exit() })
                .width(200f).height(50f)
        }

        stage.addActor(Table().apply {
            setFillParent(true)
            add(content).center()
        })
    }

    private companion object {
        fun loss(id: Int, unitName: String, amount: Int) =
                BattleLoss(troopId = id, unitName = unitName, amount = amount)

        fun battleResultScenario(
            title: String,
            description: String,
            report: BattleReport
        ) = Scenario(title, description) { screen ->
            BattleResultPopup(screen, report) {}.open(force = true)
        }

        fun troopBattleViewScenario() = Scenario(
            title = "Battle troops — grass field",
            description = "Attacker and defender on a real 5 × 3 grass hex field."
        ) { screen ->
            val popup = Popup(screen.stage, scrollable = false)
            popup.defaults().pad(10f)
            popup.add("Swordsman attacks Crossbowman".toLabel(fontSize = 24))
                .padTop(28f)
                .padBottom(62f)
            popup.row()
            popup.add(battleFieldPreview()).padLeft(24f).padRight(24f).padBottom(28f)
            popup.row()
            popup.add(fantasyCloseButton(popup)).width(190f).height(58f).padBottom(18f)
            popup.open(force = true)
        }

        fun battleFieldPreview(): Group {
            val ruleset = RulesetCache.getVanillaRuleset()
            val field = TileMap(
                width = 5,
                height = 3,
                ruleset = ruleset,
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
            val attacker = TroopFactory.create("Swordsman", 18, source)
            val defender = TroopFactory.create("Crossbowman", 12, source)

            val middleRow = tileGroups
                .groupBy { it.y.roundToInt() }
                .minBy { (rowY, _) -> abs(rowY - fieldView.height / 2f) }
                .value
                .sortedBy { it.x }
            val attackerTile = middleRow.first()
            val defenderTile = middleRow.last()

            populateBattleTroopGroup(
                target = attackerTile,
                troop = attacker,
                attacker = true,
                parentWidth = attackerTile.width,
                parentHeight = attackerTile.height,
                originX = attackerTile.originX,
                originY = attackerTile.originY
            )
            populateBattleTroopGroup(
                target = defenderTile,
                troop = defender,
                attacker = false,
                parentWidth = defenderTile.width,
                parentHeight = defenderTile.height,
                originX = defenderTile.originX,
                originY = defenderTile.originY
            )

            return fieldView
        }
    }
}

fun fantasyCloseButton(popup: Popup) =
        "Close".toTextButton(BaseScreen.skin.get("fantasy", TextButtonStyle::class.java))
            .apply {
                label.setFontScale(0.4f)
                label.setAlignment(Align.center)
                pad(5f, 10f, 5f, 10f)
                onActivation { popup.close() }
            }
