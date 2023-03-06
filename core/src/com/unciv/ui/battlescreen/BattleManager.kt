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
         //iterTroop = sequence.iterator()
       //  val iter = sequence.iterator()
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

     fun attackFrom(attackedHex: Vector2, attacker:Troop = currentTroop){
        // val attackedTroop = sequence.find { it.position == attackedHex }
         if (isTroopOnHex(attackedHex)) {
             //val positionMoveOffset = HexMath.hex2EvenQCoords(HexMath.oneStepTowards(HexMath.evenQ2HexCoords(attackedHex), direction))


             attack(getTroopOnHex(attackedHex), attacker)
             //moveCurrentTroop()
         }

     }
     /*fun attackFrom(attackedTroop: Troop, direction: Direction) {
         val positionMoveOffset = HexMath.hex2EvenQCoords(HexMath.oneStepTowards(HexMath.evenQ2HexCoords(attackedTroop.position), direction))

      //   moveCurrentTroop(positionMoveOffset)
         attack(currentTroop, attackedTroop)
     }

      */
     fun attack(defender: Troop, attacker:Troop = currentTroop){
         val damage = attacker.amount * attacker.baseUnit.damage
         val healthLack = defender.baseUnit.health - defender.currentHealth
         val perished = (damage + healthLack) / defender.baseUnit.health
         defender.currentHealth = defender.baseUnit.health - ((damage + healthLack) - perished * defender.baseUnit.health)
         defender.currentAmount -= perished
         if(defender.currentAmount <= 0)
             defender.currentAmount = 0

         if(defender.currentAmount <= 0)
             perishTroop(defender)


     }

     fun perishTroop(troop: Troop){
         troop.perish()
         // TODO: That is ugly, but that's how iterators in kotlin work ( We must redefine all iterators after removing the element.
         // Otherwise, everything crashes ( Ideally, we need to invent our own IteratableList class, or switch to Int iterators.
        // TODO: Maybe the solution is to use iterator.cursor, etc.
         val index = sequence.indexOf(troop)
         if(index != -1)
         {
             val currIndex = sequence.indexOf(currentTroop)
             sequence.removeAt(index)
             if(currIndex == -1){
                 iterTroop = sequence.listIterator()
                 currentTroop = iterTroop.next()
             }
             else{
                 iterTroop = if(currIndex <= index)
                    sequence.listIterator(currIndex)
                 else
//                 sequence.listIterator(currIndex-1)
                    sequence.listIterator(currIndex)
             }

         }
         val indexAttack = attackingTroops.indexOf(troop)
         if(indexAttack != -1)
             attackingTroops.removeAt(indexAttack)

         val indexDefend = defendingTroops.indexOf(troop)
         if(indexDefend != -1)
             defendingTroops.removeAt(indexDefend)

     }

     fun finishBattle()
     {
         isBattleOn = false
         defendingHero = null
         screen = null

     }
}
