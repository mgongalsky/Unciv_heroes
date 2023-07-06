package com.unciv.logic.map

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.event.hero.Troop

enum class Visitability{none, once_per_hero, once_per_civ, regular, once, next_battle}

class Visitable(var visitability: Visitability = Visitability.none) : IsPartOfGameInfoSerialization {
    var visitedHeroesIDs = mutableSetOf<Int>()
    var visitedCivsIDs = mutableSetOf<Int>()
    var turnsToRefresh: Int = 0

    fun clone(): Visitable {
        val toReturn = Visitable(visitability)
        toReturn.visitedHeroesIDs = visitedHeroesIDs
        toReturn.visitedCivsIDs = visitedCivsIDs
        //visitedHeroesIDs.forEach {it -> toReturn.visitedHeroesIDs.add(it)}
        //toReturn.visitedHeroesIDs = HashSet<Int>(visitedHeroesIDs)
        //toReturn.visitedCivsIDs = HashSet<Int>(visitedCivsIDs)
        toReturn.turnsToRefresh = turnsToRefresh
        return toReturn
    }

}
