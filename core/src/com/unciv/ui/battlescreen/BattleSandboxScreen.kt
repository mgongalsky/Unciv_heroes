package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.json.fromJsonFile
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

class BattleSandboxArmy {
    var unitName: String = ""
    var totalCount: Int = 0

    fun createArmy(): ArmyInfo = ArmyInfo().apply {
        fillArmy(unitName, totalCount)
    }
}

class BattleSandboxScenario {
    var id: String = ""
    var title: String = ""
    var description: String = ""
    var attacker: BattleSandboxArmy = BattleSandboxArmy()
    var defender: BattleSandboxArmy = BattleSandboxArmy()
}

class BattleSandboxCatalog {
    var scenarios: ArrayList<BattleSandboxScenario> = arrayListOf()
}

class BattleSandboxScreen(
    private val catalog: BattleSandboxCatalog = loadCatalog()
) : BaseScreen() {

    init {
        val content = Table().apply {
            defaults().pad(8f)
            add("Battle sandbox".toLabel()).colspan(2).padBottom(20f)
            row()

            catalog.scenarios.forEach { scenario ->
                val details = Table().apply {
                    add(scenario.title.toLabel()).left().growX()
                    row()
                    add(scenario.description.toLabel().apply { setWrap(true) })
                        .left().width(560f).growX()
                }
                val launchButton = "Start battle".toTextButton().onClick {
                    game.pushScreen(
                        BattleScreen.forTesting(
                            attackerArmy = scenario.attacker.createArmy(),
                            defenderArmy = scenario.defender.createArmy()
                        )
                    )
                }

                add(details).left().growX()
                add(launchButton).width(180f).height(55f)
                row()
            }
        }

        stage.addActor(
            Table().apply {
                setFillParent(true)
                add(content).width(800f)
            }
        )
    }

    companion object {
        private const val CatalogPath = "jsons/BattleScenarios.json"

        fun loadCatalog(): BattleSandboxCatalog =
            json().fromJsonFile(BattleSandboxCatalog::class.java, CatalogPath)
    }
}
