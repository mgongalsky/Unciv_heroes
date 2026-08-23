package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.json.fromJsonFile
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

class BattleSandboxEffects {
    var luck: Int = 1
    var morale: Int = 0
    var luckProbability: Double? = null
    var moraleProbability: Double? = null
}

class BattleSandboxArmy {
    var unitName: String = ""
    var totalCount: Int = 0

    fun createArmy(effects: BattleSandboxEffects?): ArmyInfo = ArmyInfo().apply {
        fillArmy(unitName, totalCount)
        if (effects != null) {
            hero = MapUnit().apply {
                luck = effects.luck
                morale = effects.morale
            }
        }
    }
}

class BattleSandboxScenario {
    var id: String = ""
    var title: String = ""
    var description: String = ""
    var attacker: BattleSandboxArmy = BattleSandboxArmy()
    var defender: BattleSandboxArmy = BattleSandboxArmy()
    var effects: BattleSandboxEffects? = null
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
                            attackerArmy = scenario.attacker.createArmy(scenario.effects),
                            defenderArmy = scenario.defender.createArmy(scenario.effects),
                            luckProbability = scenario.effects?.luckProbability,
                            moraleProbability = scenario.effects?.moraleProbability
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
