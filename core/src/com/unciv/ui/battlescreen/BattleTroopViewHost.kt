package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.Stage
import com.unciv.pure.domain.troop.Troop

/** Presentation callbacks shared by local and remote battle screens. */
interface BattleTroopViewHost {
    val stage: Stage
    fun setHoveredTroop(troop: Troop?)
    fun isCurrentTroop(troop: Troop): Boolean
    fun hasFormationRecoveryChance(troop: Troop): Boolean
    fun getNormalRetaliationBudget(troop: Troop): Int
}

internal class LocalBattleTroopViewHost(private val screen: BattleScreen) : BattleTroopViewHost {
    override val stage: Stage get() = screen.stage
    override fun setHoveredTroop(troop: Troop?) = screen.setHoveredTroop(troop)
    override fun isCurrentTroop(troop: Troop): Boolean =
        screen.getCurrentTroopView()?.getTroopInfo() === troop
    override fun hasFormationRecoveryChance(troop: Troop): Boolean =
        screen.hasFormationRecoveryChance(troop)
    override fun getNormalRetaliationBudget(troop: Troop): Int =
        screen.getNormalRetaliationBudget(troop)
}
