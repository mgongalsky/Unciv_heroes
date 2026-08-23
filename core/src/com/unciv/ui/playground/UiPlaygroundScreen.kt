package com.unciv.ui.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.battlescreen.BattleResultPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

class UiPlaygroundScreen : BaseScreen() {
    private data class Scenario(
        val title: String,
        val description: String,
        val show: (BaseScreen) -> Unit
    )

    private val scenarios = listOf(
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
    }
}
