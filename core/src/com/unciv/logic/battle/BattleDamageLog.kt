package com.unciv.logic.battle

import com.unciv.pure.application.battle.PreparedBattleDamage
import com.unciv.pure.domain.battle.DamageResult
import com.unciv.pure.domain.battle.FlankingCalculator

/** Formats captured combat values; never executes a rule or draws randomness. */
object BattleDamageLog {
    fun describe(
        label: String,
        preparation: PreparedBattleDamage?,
        rawDamage: Int,
        formationBefore: Int,
        formationAfter: Int,
        reductionPercent: Int,
        flanking: FlankingCalculator.Result,
        result: DamageResult
    ): String = buildString {
        append(label)
        if (preparation == null) append(" stored raw budget")
        else {
            append(" base=${preparation.amount}*${preparation.damagePerSoldier}")
            append(" luck=*${preparation.luckMultiplier} -> ${preparation.afterLuck}")
            append(" support=+${preparation.supportBonusPercent}% -> ${preparation.afterSupport}")
            append(" penalty=-${preparation.penaltyPercent}% -> ${preparation.incomingDamage}")
        }
        append("; appliedRaw=$rawDamage")
        append("; flankScore=${flanking.flankScore} level=${flanking.flankLevel}")
        append(" bonuses(formed/formation/unformed)=")
        append("${flanking.formedDamageBonusPercent}/${flanking.formationDamageBonusPercent}/${flanking.unformedDamageBonusPercent}%")
        append("; F=$formationBefore reduction=$reductionPercent%")
        val hasBonus = flanking.flankLevel > 0 &&
            (flanking.formedDamageBonusPercent > 0 || flanking.formationDamageBonusPercent > 0 ||
                flanking.unformedDamageBonusPercent > 0)
        if (!hasBonus) {
            append("; absorbed=min(F,floor(raw*$reductionPercent/100)); soldiers=raw-absorbed")
        } else if (formationBefore == 0) {
            append("; soldiers=ceil(raw*(1+${flanking.unformedDamageBonusPercent}/100))")
        } else {
            append("; cohesionRate=($reductionPercent/100)*(1+${flanking.formationDamageBonusPercent}/100)")
            append("; formedRaw=min(raw,F/cohesionRate) [raw if rate=0]")
            append("; remainder=raw-formedRaw")
            append("; soldiers=ceil(formedRaw*(1-$reductionPercent/100)*(1+${flanking.formedDamageBonusPercent}/100)")
            append("+remainder*(1+${flanking.unformedDamageBonusPercent}/100))")
        }
        append("; formation=$formationBefore->$formationAfter")
        append("; soldierDamage=${result.damageDealt} killed=${result.perished}")
        append(" remaining=${result.remainingAmount} lastSoldierHP=${result.remainingHealth}")
    }
}
