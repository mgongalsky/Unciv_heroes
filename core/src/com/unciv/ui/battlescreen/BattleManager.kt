package com.unciv.ui.battlescreen

import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.HexMath
import com.unciv.logic.battle.Battle
import com.unciv.logic.event.hero.Troop
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
//import com.unciv.logic.map.Monster
import com.unciv.ui.images.ImageGetter
import kotlin.random.Random

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
         defendingHero = defendingHero0
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

         attackingTroops.forEachIndexed { index, troop -> troop.enterBattle(attackingHero.civInfo, index, attacker = true)}

         // Initialize turns sequence for all troops
         // Here should be sorting by speed
         sequence = (attackingTroops + defendingTroops).toMutableList().sortedByDescending { it.baseUnit.speed }.toMutableList()
    //     sequence = (attackingTroops + defendingTroops).toMutableList()
         iterTroop = sequence.listIterator()
         currentTroop = iterTroop.next()

         screen = BattleScreen(this, attackingHero0)
         UncivGame.Current.pushScreen(screen!!)

         AIMove()
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
         // TODO: Add morale here

         //val morale = if (currentTroop.)
         if(Random.nextDouble() < 0.15) {
             currentTroop.showMoraleBird()
             AIMove()
             if (screen != null) {
                 screen?.pointerPosition = currentTroop.position
                 screen?.draw_pointer()
             }


         }
         else {

             // If we are at the end of the sequence, go to the beginning
             if (iterTroop.hasNext())
                 currentTroop = iterTroop.next()
             else {
                 iterTroop = sequence.listIterator()
                 currentTroop = iterTroop.next()
             }
             // if(!currentTroop.civInfo.isPlayerCivilization())
             if (screen != null)
                 screen?.movePointerToNextTroop()
         }
         AIMove()
     }

     fun isTroopAttacking(troop: Troop): Boolean{
         if(attackingTroops.contains(troop))
             return true
         if(defendingTroops.contains(troop))
             return false
         throw Exception("The troop is not attacker and defender!")


     }

     fun AIMove(){
         // If there is a Human player, do nothing
         if(currentTroop.civInfo.isPlayerCivilization())
             return

         if(attackingTroops.isEmpty() || defendingTroops.isEmpty())
             return

         var target: Troop?

         //checkBattleFinished()

         // Ranged units just shot at the best enemy
         if(currentTroop.baseUnit.isRanged()){
             if(!isTroopAttacking(currentTroop)) {
                 target = attackingTroops.maxByOrNull { troop -> troop.baseUnit.damage.toFloat() / troop.baseUnit.health.toFloat() }
             }
             else {
                 target = defendingTroops.maxByOrNull { troop -> troop.baseUnit.damage.toFloat() / troop.baseUnit.health.toFloat() }
             }

             if(target != null)
                attack(target)

             if(screen != null)
                 screen?.refreshTargetTroop(target?.position!!)

             //checkBattleFinished()
             nextTurn()

             return
         }

         var availableTiles = mutableListOf<Vector2>()

         //if(!isTroopAttacking(currentTroop)) {
         // First of all we find all positions in the radius of the troop

        // position = HexMath.evenQ2HexCoords(Vector2(6f, 3f-number.toFloat()*2))

         val radiusPositionsHex = HexMath.getVectorsInDistance(
             currentTroop.position,
             currentTroop.baseUnit.speed,
             worldWrap = false
         )

         // Let's check which tiles we can reach in this turn
         radiusPositionsHex.forEach { pos ->
             if (isHexAchievable(pos))
                 availableTiles.add(pos)

         }

         // Let's find all the enemy troop available for attack
         var availableTargets = mutableListOf<Troop>()
         availableTiles.forEach { pos ->
             if (isTroopOnHex(pos)) {
                 val troop = getTroopOnHex(pos)
                 if (isTroopAttacking(troop) != isTroopAttacking(currentTroop))
                     availableTargets.add(troop)
             }
         }

         if (availableTargets.isNotEmpty()) {
             // If we can attack somebody, we'll do it.
             // We'll pick up enemy with smallest damage to health ratio
             target = availableTargets.maxByOrNull { troop -> troop.baseUnit.damage.toFloat() / troop.baseUnit.health.toFloat() }
             // Now we need to find the proper tile to attack from

             val attackPositions = HexMath.getVectorsAtDistance(target?.position!!, 1, 1, worldWrap = false)
             var availableAttackPositions = mutableListOf<Vector2>()
             attackPositions.forEach { pos ->
                 if (isHexAchievable(pos) && !isTroopOnHex(pos))
                     availableAttackPositions.add(pos)
             }

             if(availableAttackPositions.isNotEmpty()){
                 val hexToMove = availableAttackPositions.first()
                 attack(target)

                 // Here we assume that screen == null, if quick battle goes or the battle is between two AIs
                 if(screen != null)
                     screen?.redrawMovedTroop(target?.position!!, hexToMove)


                 // Remember that moveCurrentTroop triggers nextTurn() also
                 moveCurrentTroop(hexToMove)

                 //nextTurn()
                 return
                 //screen?.
             }
         } else {
             // If there is nobody reachable by one turn, then go closer to the best potential target
             if(!isTroopAttacking(currentTroop)) {
                 target = attackingTroops.maxByOrNull { troop -> troop.baseUnit.damage.toFloat() / troop.baseUnit.health.toFloat() }
             }
             else {
                 target = defendingTroops.maxByOrNull { troop -> troop.baseUnit.damage.toFloat() / troop.baseUnit.health.toFloat() }
             }
             val hexToMove = availableTiles.filter { tile -> !isTroopOnHex(tile) }.minBy { tile -> HexMath.getDistance(target?.position!!, tile) }

             if(screen != null)
                 screen?.redrawMovedTroop(target?.position!!, hexToMove)

             // Remember that moveCurrentTroop triggers nextTurn() also
             moveCurrentTroop(hexToMove)

             //nextTurn()
             return

         }





         //}



         //HexMath. currentTroop.position

     }

     /** Moves current troop to the specified [position] */
     fun moveCurrentTroop(position: Vector2)
     {
         currentTroop.position = position
         nextTurn()

     }

     /** Checks if the specified hex is achievable by the current troop */
     fun isHexAchievable(positionHex: Vector2): Boolean{

         return (HexMath.getDistance(positionHex, currentTroop.position) <= currentTroop.baseUnit.speed) && isHexOnBattleField(positionHex)
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
         var damage = attacker.currentAmount * attacker.baseUnit.damage

         if(Random.nextDouble() < 0.15) {
             damage *= 2
             if (screen != null) {
                 currentTroop.showLuckRainbow()
             }
         }
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
         if(attackingTroops.isEmpty() || defendingTroops.isEmpty()) {
             if(defendingTroops.isEmpty())
             {
                 defendingHero?.destroy()
                 attackingTroops.forEach { troop -> troop.amount = troop.currentAmount }
                 attackingHero.troops = attackingTroops
             }
             if(attackingTroops.isEmpty())
             {
                 attackingHero.destroy()
                 defendingTroops.forEach { troop -> troop.amount = troop.currentAmount }
                 defendingHero?.troops = defendingTroops


             }

             finishBattle()
         }
     }
}
