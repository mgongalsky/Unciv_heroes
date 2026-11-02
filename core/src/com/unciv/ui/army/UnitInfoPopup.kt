package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.BattleTroopInfoPanel
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RoyalPanelDrawable

/** Постоянное окно характеристик отряда, закрываемое через OK или Escape. */
class UnitInfoPopup(
    screen: BaseScreen,
    troop: Troop,
    attacker: Boolean? = null
) : Popup(screen) {
    init {
        val royal = UncivGame.Current.settings.skin == "Minimal"
        val panel = BattleTroopInfoPanel(troop, attacker).apply {
            background = null
            pad(0f)
        }
        if (royal) {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
        }
        add(panel).pad(if (royal) 4f else 12f).padTop(if (royal) 4f else 36f).row()
        addCloseButton(
            text = "OK",
            additionalKey = KeyCharAndCode.RETURN,
            style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        ).apply {
            actor.name = "troopInfoOK"
            actor.label.setFontScale(0.4f)
            actor.label.setAlignment(Align.center)
            actor.pad(5f, 10f, 5f, 10f)
            width(190f).height(58f).padTop(12f)
        }
    }
}
