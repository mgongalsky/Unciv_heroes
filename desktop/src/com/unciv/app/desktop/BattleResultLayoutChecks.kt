package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleScore
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.battlescreen.BattleResultPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate
import com.unciv.ui.utils.extensions.stageBoundingBox

/** Exercises layout at constrained logical sizes, then leaves a normal popup for capture. */
internal object BattleResultLayoutChecks {
    fun open(screen: BaseScreen) {
        val report = BattleReport(
            winner = BattleSide.DEFENDER,
            attackerLosses = List(24) { BattleLoss(it, "Peasant", 12345) },
            defenderLosses = emptyList(),
            winnerScore = BattleScore(10000L, 7500L)
        )
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                // Only construction reads stage width. The test explicitly sizes the popup below,
                // independently of the desktop window and the platform's visible-area events.
                viewport.setWorldSize(width, 480f)
                var continued = 0
                val popup = BattleResultPopup(screen, report) { continued++ }
                try {
                    screen.stage.addActor(popup)
                    popup.setFillParent(false)
                    popup.setSize(width, 480f)
                    popup.setPosition(0f, 0f)
                    popup.isVisible = true
                    popup.validate()
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    scroll.validate()
                    popup.innerTable.validate()
                    check(popup.innerTable.width <= width + 1f) { "Result exceeds logical width $width" }
                    check(scroll.maxX <= 1f) { "Result requires horizontal scrolling at $width" }
                    val attacker = popup.innerTable.findActor<Table>("battleResultAttacker")
                    val defender = popup.innerTable.findActor<Table>("battleResultDefender")
                    val attackerBounds = attacker.stageBoundingBox
                    val defenderBounds = defender.stageBoundingBox
                    if (width == 360f) {
                        check(attackerBounds.y >= defenderBounds.y + defenderBounds.height - 1f) {
                            "Narrow result must stack the sides"
                        }
                    } else {
                        check(attackerBounds.x + attackerBounds.width <= defenderBounds.x + 1f) {
                            "Wide result must place sides next to each other"
                        }
                    }
                    check(scroll.maxY > 0f) { "Long loss list should exercise vertical scrolling" }
                    scroll.scrollY = scroll.maxY
                    scroll.updateVisualScroll()
                    scroll.layout()
                    val button = popup.innerTable.findActor<TextButton>("battleResultContinue")
                    val buttonBounds = button.stageBoundingBox
                    val scrollBounds = scroll.stageBoundingBox
                    check(buttonBounds.y >= scrollBounds.y - 1f &&
                        buttonBounds.y + buttonBounds.height <= scrollBounds.y + scrollBounds.height + 1f) {
                        "Continue is not reachable after scrolling at $width"
                    }
                    button.activate()
                    check(continued == 1 && popup.parent == null) {
                        "Continue must close the popup and invoke its callback once"
                    }
                } finally {
                    popup.close()
                }
            }
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
        }
        BattleResultPopup(screen, report) {}.open(force = true)
    }
}
