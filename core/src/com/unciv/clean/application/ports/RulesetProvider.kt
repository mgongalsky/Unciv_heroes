package com.unciv.clean.application.ports

import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.Ruleset

interface RulesetProvider {
    fun loadFor(params: GameParameters): Ruleset
    fun missingMods(params: GameParameters): List<String>
}
