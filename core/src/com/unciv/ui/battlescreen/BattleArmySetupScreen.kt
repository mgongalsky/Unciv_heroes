package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.session.BattleRecruitmentRules
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.application.battle.recruitment.BattleArmyStack
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.ui.utils.extensions.setFontSize

/** Local setup uses exactly the recruitment rules and editor used by network rooms. */
class BattleArmySetupScreen : BaseScreen() {
    private val ruleset = ArenaBattleSetup.ruleset()
    private val selection = BattleRecruitmentRules.from(ruleset)
    private val title = "Choose your army".toLabel()
    private val next = "Choose opponent army".toTextButton()
    private var attacker: List<BattleArmyStack>? = null
    private val picker = BattleArmyPicker(selection) { valid -> next.isDisabled = !valid }

    init {
        title.setFontSize(24)
        next.label.setFontSize(18)
        val content = Table().apply {
            defaults().pad(4f)
            add(title).row()
            add(picker).row()
            add(Table().apply {
                defaults().pad(6f).minHeight(42f)
                add("Back".toTextButton().apply {
                    label.setFontSize(18)
                    onClick { back() }
                }).minWidth(100f)
                add(next).minWidth(230f)
            }).row()
        }
        stage.addActor(Table().apply {
            setFillParent(true)
            add(AutoScrollPane(content)).grow()
        })
        next.onClick {
            if (!next.isDisabled) {
                val chosen = picker.trySelectedArmy()
                if (chosen != null) {
                    val first = attacker
                    if (first == null) {
                        attacker = chosen
                        title.setText("Choose opponent army")
                        next.setText("Start battle")
                        picker.useTemplate()
                    } else {
                        next.isDisabled = true
                        val battle = BattleScreen.forTesting(
                            attackerArmy = createArmy(first),
                            defenderArmy = createArmy(chosen)
                        )
                        game.replaceCurrentScreen(battle)
                    }
                }
            }
        }
        globalShortcuts.add(KeyCharAndCode.BACK) { back() }
    }

    private fun back() {
        val previous = attacker
        if (previous == null) game.popScreen()
        else {
            attacker = null
            title.setText("Choose your army")
            next.setText("Choose opponent army")
            picker.setArmy(previous)
        }
    }

    private fun createArmy(stacks: List<BattleArmyStack>): ArmyInfo {
        val checked = selection.validate(stacks)
        val definitions = RulesetTroopDefinitionSource(ruleset)
        return ArmyInfo(CivilizationInfo(), maxSlots = selection.maxStacks).apply {
            checked.forEachIndexed { index, stack ->
                setTroopAt(index, TroopFactory.create(stack.unitName, stack.count, definitions))
            }
        }
    }
}
