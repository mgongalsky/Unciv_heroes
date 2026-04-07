package com.unciv.pure.domain.troop

import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.UniqueType

class RulesetTroopDefinitionSource(private val ruleset: Ruleset) : ITroopDefinitionSource {
    override fun getSpeed(unitName: String) = ruleset.units[unitName]?.speed ?: 0
    override fun getDamage(unitName: String) = ruleset.units[unitName]?.damage ?: 0
    override fun getMaxHealth(unitName: String) = ruleset.units[unitName]?.health ?: 0
    override fun getRangedStrength(unitName: String) = ruleset.units[unitName]?.rangedStrength ?: 0
    override fun isSelfFeeding(unitName: String) =
            ruleset.units[unitName]?.hasUnique(UniqueType.SelfFeeding) ?: false
}
