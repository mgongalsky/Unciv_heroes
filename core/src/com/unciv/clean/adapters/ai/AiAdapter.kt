package com.unciv.clean.adapters.ai

import com.unciv.clean.application.ports.AiPort
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.civilization.CivilizationInfo

class AiAdapter : AiPort {
    override fun playAiTurn(civ: CivilizationInfo) {
        NextTurnAutomation.automateCivMoves(civ)
    }
}
