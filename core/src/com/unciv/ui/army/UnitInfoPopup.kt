package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.BattleTroopInfoPanel
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode

/** Persistent version of the troop statistics panel, closed with OK or Escape. */
class UnitInfoPopup(
    screen: BaseScreen,
    troop: Troop,
    attacker: Boolean? = null
) : Popup(screen) {
    init {
        val panel = BattleTroopInfoPanel(troop, attacker).apply {
            background = null
            pad(0f)
        }
        add(panel).pad(12f).padTop(36f).row()
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
