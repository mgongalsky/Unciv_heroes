package com.unciv.clean.adapters.barbarians

import com.unciv.clean.application.ports.BarbariansPort
import com.unciv.logic.BarbarianManager

class BarbariansAdapter(private val manager: BarbarianManager) : BarbariansPort {
    override fun updateEncampments() = manager.updateEncampments()
}
