package com.unciv.ui.worldscreen.bottombar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.models.translations.tr
import com.unciv.pure.application.battle.AssessBattleThreatUseCase
import com.unciv.pure.application.battle.AssessBattleThreatUseCase.ThreatLevel
import com.unciv.pure.application.battle.AssessBattleThreatUseCase.TroopSnapshot
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.toLabel

private const val TROOP_SPRITE_WIDTH = 72f
private const val TROOP_SPRITE_HEIGHT = 86f

internal data class BattleThreatArmyPreview(
    val attackSkill: Int,
    val defenseSkill: Int,
    val troops: List<Troop>
)

private data class AggregatedTroop(
    val representative: Troop,
    val totalAmount: Int
)

internal fun buildBattleThreatPreview(
    attacker: BattleThreatArmyPreview,
    defender: BattleThreatArmyPreview
): Table {
    val result = AssessBattleThreatUseCase.execute(
        attacker = attacker.troops.map { it.toThreatSnapshot() },
        defender = defender.troops.map { it.toThreatSnapshot() },
        attackerAttackSkill = attacker.attackSkill,
        attackerDefenseSkill = attacker.defenseSkill,
        defenderAttackSkill = defender.attackSkill,
        defenderDefenseSkill = defender.defenseSkill
    )
    val threatColor = when (result.level) {
        ThreatLevel.EASY -> Color(0.25f, 0.85f, 0.35f, 1f)
        ThreatLevel.FAVORABLE -> Color(0.65f, 0.9f, 0.25f, 1f)
        ThreatLevel.EVEN -> Color.GOLD
        ThreatLevel.DANGEROUS -> Color.ORANGE
        ThreatLevel.DEADLY -> Color.FIREBRICK
    }
    val threatText = when (result.level) {
        ThreatLevel.EASY -> "Easy fight"
        ThreatLevel.FAVORABLE -> "Favorable fight"
        ThreatLevel.EVEN -> "Even fight"
        ThreatLevel.DANGEROUS -> "Dangerous fight"
        ThreatLevel.DEADLY -> "Deadly fight"
    }
    val aggregatedDefenders = defender.troops
        .filter { it.amount > 0 }
        .groupBy { it.unitName }
        .map { (_, troops) -> AggregatedTroop(troops.first(), troops.sumOf { it.amount }) }

    return Table().apply {
        defaults().pad(4f)
        add("Enemy forces".tr().toLabel(fontSize = 18)).left()
        add(threatText.tr().toLabel(threatColor, fontSize = 18)).right().row()
        add(
            ImageGetter.getProgressBarHorizontal(
                width = 260f,
                height = 7f,
                percentComplete = threatBarFraction(result.defenderToAttackerRatio),
                progressColor = threatColor,
                backgroundColor = Color(0.08f, 0.1f, 0.16f, 1f)
            )
        ).colspan(2).fillX().row()
        addSeparator(threatColor).colspan(2).padBottom(5f).row()

        aggregatedDefenders.forEach { aggregated ->
            val troop = aggregated.representative
            add(createTroopSprite(troop.unitName)).size(TROOP_SPRITE_WIDTH, TROOP_SPRITE_HEIGHT)
                .left()
            val baseUnit = ImageGetter.ruleset.units[troop.unitName]
            val defense = baseUnit?.defenceSkill ?: defender.defenseSkill
            val range = if (troop.rangedStrength > 0) baseUnit?.range ?: 2 else 0
            add(
                Table().apply {
                    add("${troop.unitName.tr()}  ×${aggregated.totalAmount}".toLabel(fontSize = 17)).left()
                        .row()
                    add(
                        "ATK ${troop.damage}   DEF $defense   HP ${troop.maxHealth}   SPD ${troop.speed}   RNG $range"
                            .toLabel(fontSize = 13)
                    ).left()
                }
            ).left().growX().row()
        }
    }
}

private fun createTroopSprite(unitName: String): Group = Group().apply {
    setSize(TROOP_SPRITE_WIDTH, TROOP_SPRITE_HEIGHT)
    val layers = ImageGetter.getLayeredImageColored(
        "TileSets/AbsoluteUnits/Units/$unitName",
        null,
        null,
        null
    )
    layers.forEach { image ->
        val scale = minOf(
            TROOP_SPRITE_WIDTH / image.width,
            TROOP_SPRITE_HEIGHT / image.height
        )
        image.setScale(scale)
        image.setPosition(
            (TROOP_SPRITE_WIDTH - image.width * scale) / 2f,
            (TROOP_SPRITE_HEIGHT - image.height * scale) / 2f
        )
        addActor(image)
    }
}

private fun Troop.toThreatSnapshot() = TroopSnapshot(
    amount = amount,
    damage = damage,
    maxHealth = maxHealth,
    speed = speed,
    range = ImageGetter.ruleset.units[unitName]?.range ?: if (rangedStrength > 0) 2 else 0
)

private fun threatBarFraction(ratio: Double): Float = when {
    !ratio.isFinite() -> 1f
    else -> (ratio / 2.0).toFloat().coerceIn(0.05f, 1f)
}
