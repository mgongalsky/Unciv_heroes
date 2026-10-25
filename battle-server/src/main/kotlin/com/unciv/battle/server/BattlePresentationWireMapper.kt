package com.unciv.battle.server

import com.unciv.battle.protocol.*
import com.unciv.pure.application.battle.BattleAttackPreview
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattlePresentationSnapshot
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.RangedShotRules

/** Maps captured values without consulting or mutating the engine. */
object BattlePresentationWireMapper {
    fun encode(
        value: BattlePresentationSnapshot,
        command: (BattleCommand) -> WireAvailableCommand
    ): WireBattlePresentation = WireBattlePresentation(
        tiles = value.tiles.map {
            WireTilePresentation(it.position.wire(), it.appearance.terrainAlpha,
                WireControlStrength.valueOf(it.appearance.control.name))
        },
        movementRanges = value.movementRanges.map { range ->
            WireTroopMovementPresentation(range.troopId, range.tiles.map {
                WireMovementTilePresentation(it.position.wire(), WireMovementStyle.valueOf(it.style.name))
            })
        },
        attacks = value.attacks.map { WireAttackPresentation(command(it.command), preview(it.preview)) }
    )

    private fun Point.wire() = WirePoint(x, y)

    private fun FlankingCalculator.Result.wire() = WireFlanking(
        flankScore, flankLevel, formedDamageBonusPercent,
        formationDamageBonusPercent, unformedDamageBonusPercent
    )

    private fun RangedShotRules.Result.wire() = WireRangedShot(
        WireShotType.valueOf(shotType.name), reason?.let { WireIndirectReason.valueOf(it.name) },
        distance, directShotRange, lineBlocked, damageModifierPercent
    )

    private fun preview(value: BattleAttackPreview) = WireAttackPreview(
        attack = hit(value.attack),
        retaliation = value.retaliation?.let(::hit),
        retaliationBudget = value.retaliationBudget,
        remainingRetaliationBudget = value.remainingRetaliationBudget,
        usesStoredRetaliation = value.usesStoredRetaliation,
        isRanged = value.isRanged,
        rangedShot = value.rangedShot?.wire()
    )

    private fun hit(value: BattleAttackPreview.Hit) = WirePreviewHit(
        preparation = value.preparation?.let {
            WirePreparedDamage(it.amount, it.damagePerSoldier, it.luckMultiplier, it.afterLuck,
                it.supportBonusPercent, it.afterSupport, it.penaltyPercent, it.incomingDamage)
        },
        flanking = value.flanking.wire(),
        formationBefore = value.formationBefore,
        formationReductionPercent = value.formationReductionPercent,
        remainingFormation = value.remainingFormation,
        result = value.result.let {
            WirePreviewDamageResult(it.damageDealt, it.perished, it.remainingAmount,
                it.remainingHealth, it.isLuck)
        },
        antiCavalryDamageBonusPercent = value.antiCavalryDamageBonusPercent
    )
}
