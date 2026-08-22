package com.unciv.pure.application.battle

fun interface BattlePolicy {
    fun chooseCommand(troopId: Int): BattleCommand?
}
