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
    override fun getFormationHealthPercent(unitName: String) =
        ruleset.units[unitName]?.formationHealthPercent ?: 0
    override fun getFormationDamageReductionPercent(unitName: String) =
            ruleset.units[unitName]?.formationDamageReductionPercent ?: 0
    override fun getSupportBonusPercent(unitName: String): Int =
        ruleset.units[unitName]?.supportBonusPercent ?: 0
    override fun getFlankingProfile(unitName: String): com.unciv.pure.domain.battle.FlankingProfile? =
        ruleset.units[unitName]?.flanking
    override fun getRangedShotProfile(unitName: String): com.unciv.pure.domain.battle.RangedShotProfile? =
            ruleset.units[unitName]?.rangedShot
    override fun ignoresMovementFormationPenalty(unitName: String): Boolean =
        ruleset.units[unitName]?.ignoresMovementFormationPenalty ?: false

    override fun isCavalry(unitName: String): Boolean =
            ruleset.units[unitName]?.isCavalry ?: false
    override fun getAntiCavalryDamageBonusPercent(unitName: String): Int =
            (ruleset.units[unitName]?.antiCavalryDamageBonusPercent ?: 0).also {
                require(it >= 0) { "Negative anti-cavalry damage bonus for $unitName" }
            }
}
