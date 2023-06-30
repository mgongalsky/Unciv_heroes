package com.unciv.ui.battlescreen

import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.HexMath
import com.unciv.logic.event.hero.Troop
import com.unciv.logic.map.MapUnit
//import com.unciv.logic.map.Monster
import com.unciv.ui.images.ImageGetter

// TODO: This class is logic. And should be moved to "logic" package. However, there is internal visibility with BattleScreen class, which is UI.

/** Specifies one of 6 directions of a hex (from center of an edge to center of the hex). [DirError] is handle for error */
enum class Direction(val num: Int) {
    TopRight(0), CenterRight(1), BottomRight(2), BottomLeft(3), CenterLeft(4), TopLeft(5), DirError(6)
}

/** Logical part of a battle. No visial part here (it is in [BattleScreen] class).
 * That class must handle AI duel.
 * Here we use hex coordinates, despite that the battle map is rectagular.
 */
class BattleManager()
 {

     /** Current status of the battle */
     internal var isBattleOn = false
     /** Reference to a hero unit on a world map.
      * Currently used only for determining of terrain land and a handle to hero parameters */
     internal lateinit var attackingHero: MapUnit
     internal lateinit var attackingTroops: MutableList<Troop>
     internal var defendingHero: MapUnit? = null
     internal lateinit var defendingTroops: MutableList<Troop>
     internal lateinit var sequence: MutableList<Troop>
     internal var screen: BattleScreen? = null
     /** Iterator to a currently active troop */
     private lateinit var iterTroop: MutableListIterator<Troop>
     /** Reference to a currently active troop. Must be changed just after iterator [iterTroop] changed ( */
     internal lateinit var currentTroop: Troop

     /** Initialization of the battle */
     fun startBattle(attackingHero0: MapUnit, defendingHero0: MapUnit? = null) {
         attackingHero = attackingHero0
         if(attackingHero.troops.isEmpty())
         {
                 attackingHero.troops.add(Troop(10, "Horseman"))
                 attackingHero.troops.add(Troop(20, "Archer"))
                 attackingHero.troops.add(Troop(15, "Spearman"))
                 attackingHero.troops.add(Troop(5, "Swordsman"))
         }
         attackingTroops = attackingHero.troops.toMutableList()
         isBattleOn = true

         // Initialize model armies

         var monster = MapUnit(40, "Crossbowman") // Crossbowman means elf in our case ))
         defendingTroops = monster.troops

         if (defendingHero0 != null) {
             /*
             if (defendingHero0 is Monster) {
                 defendingHero = defendingHero0

                 //defendingTroops = mutableListOf()
                 //defendingTroops = defendingHero0.troops
                 defendingHero.apply {
                     if (this is Monster) {
                         troops = mutableListOf()
                         troops.clear()
                         baseUnit = ImageGetter.ruleset.units[monsterName]!!
                         baseUnit.ruleset = ImageGetter.ruleset

                         //   amount = amount0
                         val amountOfTroops = 4
                         for (i in 1..amountOfTroops) {
                             troops.add(Troop(amount / amountOfTroops, monsterName))

                         }
                         val imageString = "TileSets/AbsoluteUnits/Units/" + monsterName

                         monsterImages = ImageGetter.getLayeredImageColored(imageString, null)
                     }
                 }
                 defendingTroops = (defendingHero as Monster).troops

              */
             //}
             defendingTroops = defendingHero0.troops.toMutableList()
         } else {
             defendingTroops = monster.troops
         }

         defendingTroops.forEachIndexed { index, troop ->
             troop.enterBattle(
                 attackingHero.civInfo.gameInfo.civilizations.first(),
                 index,
                 attacker = false
             )
         }
/*
         attackingTroops.clear()
         attackingTroops.add(Troop(10, "Horseman"))
         attackingTroops.add(Troop(20, "Archer"))
         attackingTroops.add(Troop(15, "Spearman"))
         attackingTroops.add(Troop(5, "Swordsman"))

 */

         attackingTroops.forEachIndexed { index, troop -> troop.enterBattle(attackingHero.civInfo, index, attacker = true)}

         // Initialize turns sequence for all troops
         // Here should be sorting by speed
         sequence = (attackingTroops + defendingTroops).toMutableList().sortedByDescending { it.baseUnit.speed }.toMutableList()
    //     sequence = (attackingTroops + defendingTroops).toMutableList()
         iterTroop = sequence.listIterator()
         currentTroop = iterTroop.next()

         screen = BattleScreen(this, attackingHero0)
         UncivGame.Current.pushScreen(screen!!)
     }

     /** Check if specified hex has a troop on it */
     fun isTroopOnHex(positionHex: Vector2): Boolean {
         return (sequence.find { it.position == positionHex } != null)
     }

     /** Get a troop on the specified hex. Check if there is any by call [isTroopOnHex] first */
     fun getTroopOnHex(positionHex: Vector2): Troop {
         return sequence.first { it.position == positionHex }
     }

     /** Gives turn to next troop in the sequence */
     fun nextTurn()
     {
         // If we are at the end of the sequence, go to the beginning
         if(iterTroop.hasNext())
             currentTroop = iterTroop.next()
         else{
             iterTroop = sequence.listIterator()
             currentTroop = iterTroop.next()
         }
     }

     /** Moves current troop to the specified [position] */
     fun moveCurrentTroop(position: Vector2)
     {
         currentTroop.position = position
         nextTurn()

     }

     /** Checks if the specified hex is achievable by the current troop */
     fun isHexAchievable(positionHex: Vector2): Boolean{

         return HexMath.getDistance(positionHex, currentTroop.position) <= currentTroop.baseUnit.speed
     }

     /** Checks if the specified hex is inside the battlefield */
     fun isHexOnBattleField(positionHex: Vector2): Boolean{
         val positionOffset = HexMath.hex2EvenQCoords(positionHex)
         // TODO: Change to variable parameters of the BattleField
         return -7f <= positionOffset.x  &&
                 positionOffset.x <= 6f &&
                 -4f <= positionOffset.y &&
                 positionOffset.y <= 3f

     }

     /** Attack the target by current troop by specified [defenderHex] position */
     fun attack(defenderHex: Vector2, attacker: Troop = currentTroop){
         if (isTroopOnHex(defenderHex)) {
             attack(getTroopOnHex(defenderHex), attacker)
         }
     }

     /** Attack the target by current troop by specified [defender] troop handle */
     fun attack(defender: Troop, attacker: Troop = currentTroop){
         // Calculate maximum damage
         val damage = attacker.currentAmount * attacker.baseUnit.damage
         // We add lack of health just to simplify calculations. We add it to total amount of health
         val healthLack = defender.baseUnit.health - defender.currentHealth
         // Calculate amount of perished units
         val perished = (damage + healthLack) / defender.baseUnit.health
         // Update parameters after attack:
         defender.currentHealth = defender.baseUnit.health - ((damage + healthLack) - perished * defender.baseUnit.health)
         defender.currentAmount -= perished
         if(defender.currentAmount <= 0)
             defender.currentAmount = 0

         if(defender.currentAmount <= 0)
             perishTroop(defender)


     }

     /** Routine for perished troops. Removal from all lists. */
     fun perishTroop(troop: Troop){
         troop.perish()
         // TODO: That is ugly, but that's how iterators in kotlin work ( We must redefine all iterators after removing the element.
         // Otherwise, everything crashes ( Ideally, we need to invent our own IteratableList class, or switch to Int iterators.
        // TODO: Maybe the solution is to use iterator.cursor, etc.
         val indexPerish = sequence.indexOf(troop)
         if(indexPerish != -1)
         {
             val indexCurr = sequence.indexOf(currentTroop)
             sequence.removeAt(indexPerish)
             if(indexCurr == -1){
                 iterTroop = sequence.listIterator()
                 currentTroop = iterTroop.next()
             }
             else{
                 iterTroop = if(indexCurr <= indexPerish)
                     sequence.listIterator(indexCurr+1)
                 else
                     sequence.listIterator(indexCurr)
             }

         }

         val indexAttack = attackingTroops.indexOf(troop)
         if(indexAttack != -1)
             attackingTroops.removeAt(indexAttack)

         val indexDefend = defendingTroops.indexOf(troop)
         if(indexDefend != -1)
             defendingTroops.removeAt(indexDefend)

         checkBattleFinished()
     }

     fun finishBattle()
     {
         isBattleOn = false
         defendingHero = null
         screen?.shutdownScreen(calledFromManager = true)
         screen = null

     }

     /** Checks if battle is finished. If all troops of one army is gone, then finish the battle */
     fun checkBattleFinished()
     {
         if(attackingTroops.isEmpty() || defendingTroops.isEmpty())
             finishBattle()
     }
}
