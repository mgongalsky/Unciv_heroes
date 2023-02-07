package com.unciv.ui.battlescreen

import com.unciv.UncivGame
import com.unciv.logic.map.MapUnit

class BattleManager()
 {

     private var isBattleOn = false
     private var attackingHero: MapUnit? = null
     private var defendingHero: MapUnit? = null
     private var screen: BattleScreen? = null
//     UncivGame.Current.pushScreen(BattleScreen(attackingHero))
     fun startBattle(attackingHero0: MapUnit)
     {
         attackingHero = attackingHero0
         isBattleOn = true
         val screen2Push = BattleScreen(this, attackingHero0)
         screen = screen2Push
         UncivGame.Current.pushScreen(screen2Push)
     }

     fun finishBattle()
     {
         isBattleOn = false
         attackingHero = null
         defendingHero = null
         screen = null

     }
}
