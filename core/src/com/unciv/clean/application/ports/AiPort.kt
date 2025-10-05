package com.unciv.clean.application.ports

import com.unciv.logic.civilization.CivilizationInfo

interface AiPort {
    fun playAiTurn(civ: CivilizationInfo)
}
