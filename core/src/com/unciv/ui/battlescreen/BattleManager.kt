package com.unciv.ui.battlescreen

import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.hero.Monster
import com.unciv.logic.hero.Troop
import com.unciv.logic.map.MapUnit
import java.text.FieldPosition

class BattleManager()
 {

     internal var isBattleOn = false
     internal var attackingHero: MapUnit? = null
     internal var attackingTroops: MutableList<Troop>? = null
    // typealias attackingTroops = attackingHero.troops
     internal var defendingHero: MapUnit? = null
     internal var defendingTroops: MutableList<Troop>? = null
     internal var screen: BattleScreen? = null
     internal var currentTroop: Troop? = null
//     UncivGame.Current.pushScreen(BattleScreen(attackingHero))
     fun startBattle(attackingHero0: MapUnit)
     {
         attackingHero = attackingHero0
         attackingTroops = attackingHero!!.troops
         isBattleOn = true


         var monster = Monster(40, "Crossbowman")
         defendingTroops = monster.troops
         defendingTroops!!.forEachIndexed { index, troop -> troop.enterBattle(attackingHero!!.civInfo.gameInfo.civilizations.first(), index, attacker = false)}

         attackingTroops!!.clear()
         attackingTroops!!.add(Troop(10, "Warrior"))
         attackingTroops!!.add(Troop(20, "Archer"))
         attackingTroops!!.add(Troop(15, "Spearman"))
         attackingTroops!!.add(Troop(5, "Swordsman"))
         attackingTroops!!.forEachIndexed { index, troop -> troop.enterBattle(attackingHero!!.civInfo, index, attacker = true)}

         currentTroop = attackingTroops!!.first()

         screen = BattleScreen(this, attackingHero0)
         UncivGame.Current.pushScreen(screen!!)
     }

     fun moveCurrentTroop(position: Vector2)
     {
         currentTroop?.position = position
     }

     fun finishBattle()
     {
         isBattleOn = false
         attackingHero = null
         defendingHero = null
         screen = null

     }
}
