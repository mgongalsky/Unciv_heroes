package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector
import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.HexMath
import com.unciv.logic.hero.Monster
import com.unciv.logic.hero.Troop
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo

enum class Direction(val num: Int) {
    TopRight(0), CenterRight(1), BottomRight(2), BottomLeft(3), CenterLeft(4), TopLeft(5), DirError(6)
}

// Remember that: troop coordinates are offset, but all calculations and tileinfo are hex coords
class BattleManager()
 {

     internal var isBattleOn = false
     internal lateinit var attackingHero: MapUnit
     internal lateinit var attackingTroops: MutableList<Troop>
     internal var defendingHero: MapUnit? = null
     internal lateinit var defendingTroops: MutableList<Troop>
     internal lateinit var sequence: MutableList<Troop>
     internal var screen: BattleScreen? = null
     private lateinit var iterTroop: MutableListIterator<Troop>
     internal lateinit var currentTroop: Troop

     fun startBattle(attackingHero0: MapUnit)
     {
         attackingHero = attackingHero0
         attackingTroops = attackingHero.troops
         isBattleOn = true


         var monster = Monster(40, "Crossbowman")
         defendingTroops = monster.troops
         defendingTroops.forEachIndexed { index, troop -> troop.enterBattle(attackingHero.civInfo.gameInfo.civilizations.first(), index, attacker = false)}

         attackingTroops.clear()
         attackingTroops.add(Troop(10, "Warrior"))
         attackingTroops.add(Troop(20, "Archer"))
         attackingTroops.add(Troop(15, "Spearman"))
         attackingTroops.add(Troop(5, "Swordsman"))
         attackingTroops.forEachIndexed { index, troop -> troop.enterBattle(attackingHero.civInfo, index, attacker = true)}

         // Here should be sorting by speed
  //       sequence = (attackingTroops + defendingTroops).toMutableList().sortedByDescending { it.amount }.toMutableList()
         sequence = (attackingTroops + defendingTroops).toMutableList()
         iterTroop = sequence.listIterator()
         currentTroop = iterTroop.next()


         screen = BattleScreen(this, attackingHero0)
         UncivGame.Current.pushScreen(screen!!)
/*
         fun highlightTile(tile: TileInfo, color: Color = Color.WHITE) {
             for (group in mapHolder.tileGroups[tile] ?: return) {
                 group.showHighlight(color)
                 highlightedTileGroups.add(group)
             }
         }


 */
     }

     fun isTroopOnHex(position: Vector2): Boolean {
         return (sequence.find { HexMath.evenQ2HexCoords(it.position) == position } != null)
     }

     fun getTroopOnHex(position: Vector2): Troop {
         return sequence.first { HexMath.evenQ2HexCoords(it.position) == position }
     }

     fun moveCurrentTroop(position: Vector2)
     {
         currentTroop.position = position
         if(iterTroop.hasNext())
             currentTroop = iterTroop.next()
         else{
             iterTroop = sequence.listIterator()
             currentTroop = iterTroop.next()
         }
     }

     fun isHexAchievable(positionHex: Vector2): Boolean{

         return HexMath.getDistance(positionHex, currentTroop.positionHex()) <= currentTroop.baseUnit.speed
     }

     fun attackFrom(attackedHex: Vector2, direction: Direction){
        // val attackedTroop = sequence.find { it.position == attackedHex }
         if (isTroopOnHex(attackedHex))
             attackFrom(getTroopOnHex(attackedHex), direction)


     }
     fun attackFrom(attackedTroop: Troop, direction: Direction) {
         val positionMoveOffset = HexMath.hex2EvenQCoords(HexMath.oneStepTowards(HexMath.evenQ2HexCoords(attackedTroop.position), direction))

         moveCurrentTroop(positionMoveOffset)
     }

     fun finishBattle()
     {
         isBattleOn = false
         defendingHero = null
         screen = null

     }
}
