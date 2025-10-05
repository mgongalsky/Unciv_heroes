package testsupport

import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.ruleset.Speed

object TestGameFactory {
    fun minimalSingleHumanGame(): GameInfo {
        // Load rulesets to satisfy CivilizationInfo init and any lightweight lookups
        RulesetCache.loadRulesets()
        val ruleset = RulesetCache.getVanillaRuleset()

        val g = GameInfo()
        g.gameParameters = GameParameters().apply {
            isOnlineMultiplayer = false
            noBarbarians = true
        }
        g.ruleSet = ruleset
        g.difficultyObject = ruleset.difficulties.values.first()
        g.speed = ruleset.speeds[Speed.DEFAULTFORSIMULATION] ?: ruleset.speeds.values.first()

        // Minimal 1x1 map
        g.tileMap = TileMap(1, 1, ruleset, false).apply { gameInfo = g }

        val civ = CivilizationInfo().apply {
            playerType = PlayerType.Human
            civName = "Rome"
            gameInfo = g
            // Do NOT call setNationTransient() here – we keep it minimal
        }
        g.civilizations.add(civ)
        g.currentPlayer = civ.civName
        g.currentPlayerCiv = civ
        // Initialize transients to satisfy nextTurn expectations
        g.setTransients()
        return g
    }
}
