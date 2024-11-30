package com.unciv.logic.battle

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.ui.battlescreen.BattleScreen

/** Specifies one of 6 directions of a hex (from center of an edge to center of the hex). [DirError] is handle for error */
enum class Direction(val num: Int) {
    TopRight(0), CenterRight(1), BottomRight(2), BottomLeft(3), CenterLeft(4), TopLeft(5), DirError(6)
}

/** Logical part of a battle. No visial part here (it is in [BattleScreen] class).
 * That class must handle AI duel.
 * Here we use hex coordinates, despite that the battle map is rectagular.
 */
class NewBattleManager(

    private var attackerArmy: ArmyInfo,
    private var defenderArmy: ArmyInfo

)
{


}

