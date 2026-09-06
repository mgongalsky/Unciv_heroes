package com.unciv.ui.playground

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.ui.army.ArmyView
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.army.UnitInfoPopup
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.SplitTroopPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel

private class ArmyPopupFixture(screen: BaseScreen, targetAmount: Int) {
    private val source = HardcodedTroopDefinitionSource(
        speed = 4, damage = 8, maxHealth = 60, rangedStrength = 7
    )
    val army = ArmyInfo(civInfo = CivilizationInfo(), maxSlots = 3).apply {
        setTroopAt(0, TroopFactory.create("Archer", 35, source))
        if (targetAmount > 0) setTroopAt(1, TroopFactory.create("Archer", targetAmount, source))
    }
    val manager = ArmyManager(army, troopDefinitionSource = source)
    val view = ArmyView(army, manager, screen)
}

fun openArmyInteractionPreview(screen: BaseScreen) {
    val fixture = ArmyPopupFixture(screen, 12)
    val overlay = com.badlogic.gdx.scenes.scene2d.ui.Table(BaseScreen.skin).apply {
        setFillParent(true)
        touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
        background = BaseScreen.skin.get(
            "fantasy_background",
            com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable::class.java
        )
        defaults().pad(10f)
    }
    overlay.add("Army interactions".toLabel(fontSize = 24)).row()
    overlay.add("Double-click a troop for properties; hold right mouse for a quick view.".toLabel())
        .row()
    overlay.add("Select a troop, then Shift-click another slot to split.".toLabel()).row()
    overlay.add(fixture.view).pad(20f).row()
    val close = com.badlogic.gdx.scenes.scene2d.ui.TextButton("Close", BaseScreen.skin)
    close.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
        override fun clicked(
            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
            x: Float,
            y: Float
        ) {
            overlay.remove()
        }
    })
    overlay.add(close).width(190f).height(58f)
    screen.stage.addActor(overlay)
}

/** Opens the production split dialog with fixed data and its real split callback. */
fun openSplitTroopPreview(screen: BaseScreen, targetAmount: Int = 0) {
    val fixture = ArmyPopupFixture(screen, targetAmount)
    val slots = fixture.view.children.filterIsInstance<TroopArmyView>()
    SplitTroopPopup(screen, slots[0], slots[1]) { left, right ->
        check(left + right == 35 + targetAmount)
        check(fixture.manager.splitTroop(fixture.army, 0, fixture.army, 1, left))
        fixture.view.updateView()
        val result = Popup(screen)
        result.add("Split result: $left / $right".toLabel(fontSize = 22)).row()
        result.add(fixture.view).pad(20f).row()
        result.add(fantasyCloseButton(result)).width(190f).height(58f)
        result.open(force = true)
    }.open(force = true)
}

fun openArmyTroopInfoPreview(screen: BaseScreen) {
    val fixture = ArmyPopupFixture(screen, 0)
    UnitInfoPopup(screen, checkNotNull(fixture.army.getTroopAt(0))).open(force = true)
}
