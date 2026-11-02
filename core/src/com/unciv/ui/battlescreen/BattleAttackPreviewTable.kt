package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.application.battle.BattleAttackPreview
import com.unciv.pure.domain.battle.RangedShotRules
import com.unciv.ui.utils.extensions.toLabel

/** Compact presentation of values produced by combat rules; the caller specifies its background. */
internal fun buildBattleAttackPreviewTable(
    preview: BattleAttackPreview,
    parchment: Boolean = false
): Table = Table().apply {
    val textColor = if (parchment) Color.valueOf("453925") else Color.WHITE
    val secondaryColor = if (parchment) Color.valueOf("655438") else Color.LIGHT_GRAY
    val headingColor = if (parchment) Color.valueOf("8a632e") else Color.valueOf("F5D77AFF")
    fun Table.line(label: String, value: String) {
        add(label.toLabel(fontColor = secondaryColor, fontSize = 14)).left().padRight(10f)
        add(value.toLabel(fontColor = textColor, fontSize = 14)).right().row()
    }

    fun hitTable(title: String, hit: BattleAttackPreview.Hit, retaliation: Boolean) =
        Table().apply {
            defaults().pad(2f)
            add(title.toLabel(fontColor = headingColor, fontSize = 17))
                .colspan(2).left().padBottom(5f).row()
            hit.preparation?.let { preparation ->
                if (preparation.supportBonusPercent != 0)
                    line("Support", "+${preparation.supportBonusPercent}%")
                if (preparation.penaltyPercent != 0)
                    line(
                        if (preview.isRanged && !retaliation) "Shot after movement" else "Ranged unit in melee",
                        "-${preparation.penaltyPercent}%"
                    )
            }
            if (!retaliation) preview.rangedShot?.let { shot ->
                // Movement is already shown in the preparation breakdown above.
                if (shot.shotType == RangedShotRules.ShotType.INDIRECT &&
                    shot.reason != RangedShotRules.IndirectReason.MOVEMENT
                )
                    line(
                        if (shot.reason == RangedShotRules.IndirectReason.DISTANCE)
                            "Indirect: distance" else "Indirect: blocked line",
                        "${shot.damageModifierPercent}%"
                    )
            }
            if (hit.antiCavalryDamageBonusPercent > 0)
                line("Against cavalry", "+${hit.antiCavalryDamageBonusPercent}%")
            if (hit.formationBefore > 0 && hit.formationReductionPercent > 0)
                line("Formation protection", "-${hit.formationReductionPercent}%")
            val bonus = preview.flankingBonusPercent(retaliation)
            if (bonus > 0.0) {
                val percent = if (bonus.isInfinite()) "∞" else
                    java.math.BigDecimal.valueOf(bonus).setScale(1, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString()
                line("Flanking bonus", "+$percent%")
            }
            line(
                if (retaliation) "Damage to us" else "Damage to enemy",
                hit.result.damageDealt.toString()
            )
            line(if (retaliation) "Our losses" else "Enemy losses", hit.result.perished.toString())
        }
    defaults().pad(3f)
    add("Forecast: no new luck".toLabel(fontColor = secondaryColor, fontSize = 12))
        .colspan(2).left().padBottom(4f).row()
    add(hitTable(if (preview.isRanged) "Our shot" else "Our attack", preview.attack, false)).top()
        .left()
    val retaliation = preview.retaliation
    if (retaliation != null && (preview.retaliationBudget ?: 0) > 0) {
        add(hitTable("Enemy retaliation", retaliation, true)).top().left().padLeft(18f)
    } else {
        add("No retaliation".toLabel(fontColor = secondaryColor, fontSize = 14))
            .top().left().padLeft(18f)
    }
}
