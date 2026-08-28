package com.unciv.pure.domain.battle

import com.unciv.pure.domain.troop.Troop

object ZoneOfControl {
    fun canLeaveTile(adjacentTroops: Sequence<Troop?>, isEnemy: (Troop) -> Boolean): Boolean =
        adjacentTroops.none { it != null && isEnemy(it) }
}
