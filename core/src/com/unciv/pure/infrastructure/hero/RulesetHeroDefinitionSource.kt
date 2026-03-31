package com.unciv.pure.infrastructure.hero

import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.hero.IHeroDefinitionSource

class RulesetHeroDefinitionSource(
    private val ruleset: Ruleset
) : IHeroDefinitionSource {
    override fun getAttackSkill(unitName: String) =
            ruleset.units[unitName]?.attackSkill ?: 5
    override fun getDefenseSkill(unitName: String) =
            ruleset.units[unitName]?.defenceSkill ?: 5
    override fun getFoodCapacity(unitName: String) = 15f
}
