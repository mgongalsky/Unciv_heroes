package com.unciv.clean.adapters.ruleset

import com.unciv.clean.application.ports.RulesetProvider
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache

class RulesetCacheProvider : RulesetProvider {
    override fun loadFor(params: GameParameters): Ruleset =
        RulesetCache.getComplexRuleset(params)

    override fun missingMods(params: GameParameters): List<String> {
        val rs = RulesetCache.getComplexRuleset(params)
        val declared = (params.mods + params.baseRuleset).toSet()
        val installed = rs.mods.toSet()
        return declared.filterNot { it in installed }
    }
}
