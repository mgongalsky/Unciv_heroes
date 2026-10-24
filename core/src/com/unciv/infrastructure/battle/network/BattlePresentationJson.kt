package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.JsonValue
import com.unciv.battle.protocol.BattleProtocol
import com.unciv.infrastructure.battle.network.BattleReplyJson.bool
import com.unciv.infrastructure.battle.network.BattleReplyJson.int
import com.unciv.infrastructure.battle.network.BattleReplyJson.optional
import com.unciv.infrastructure.battle.network.BattleReplyJson.required
import com.unciv.infrastructure.battle.network.BattleReplyJson.text
import com.unciv.pure.application.battle.*
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.*

/** Explicit value decoding; no class names or engine execution from network data. */
internal object BattlePresentationJson {
    fun decode(value: JsonValue): BattlePresentationSnapshot {
        require(value.isObject)
        val tiles = array(value, "tiles", 4096).map { node ->
            val alpha = node.required("terrainAlpha")
            require(alpha.isDouble || alpha.isLong)
            val opacity = alpha.asFloat()
            require(opacity.isFinite() && opacity in 0f..1f)
            BattleTilePresentation(point(node.required("position")),
                BattleTileAppearanceUseCase.Appearance(opacity,
                    ZoneOfControlTransition.Strength.valueOf(node.required("control").text())))
        }
        require(tiles.map { it.position }.distinct().size == tiles.size)
        val ranges = array(value, "movementRanges", 4096).map { node ->
            val troopId = node.int("troopId").also { require(it > 0) }
            val cells = array(node, "tiles", 4096).map {
                BattleMovementTilePresentation(point(it.required("position")),
                    BattleMovementPreviewUseCase.TileStyle.valueOf(it.required("style").text()))
            }
            require(cells.map { it.position }.distinct().size == cells.size)
            BattleTroopMovementPresentation(troopId, cells)
        }
        require(ranges.map { it.troopId }.distinct().size == ranges.size)
        val attacks = array(value, "attacks", 32768).map {
            val command = BattleAvailableCommandJson.decode(it.required("command"))
            require(command is BattleCommand.Attack || command is BattleCommand.Shoot)
            val preview = preview(it.required("preview"))
            require(preview.isRanged == (command is BattleCommand.Shoot))
            BattleAttackPresentation(command, preview)
        }
        require(attacks.map { it.command }.distinct().size == attacks.size)
        return BattlePresentationSnapshot(tiles, ranges, attacks)
    }

    private fun array(value: JsonValue, name: String, maximum: Int): JsonValue =
        value.required(name).also { require(it.isArray && it.size <= maximum) }

    private fun point(value: JsonValue): Point {
        require(value.isObject)
        return Point(value.int("x"), value.int("y")).also {
            require(it.x in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
            require(it.y in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
        }
    }

    private fun optionalInt(value: JsonValue, name: String): Int? =
        if (value.optional(name) == null) null else value.int(name)

    private fun flank(value: JsonValue): FlankingCalculator.Result {
        require(value.isObject)
        return FlankingCalculator.Result(value.int("flankScore"), value.int("flankLevel"),
            value.int("formedDamageBonusPercent"), value.int("formationDamageBonusPercent"),
            value.int("unformedDamageBonusPercent"))
    }

    private fun shot(value: JsonValue): RangedShotRules.Result {
        require(value.isObject)
        return RangedShotRules.Result(
            RangedShotRules.ShotType.valueOf(value.required("shotType").text()),
            value.optional("reason")?.let { RangedShotRules.IndirectReason.valueOf(it.text()) },
            value.int("distance"), value.int("directShotRange"),
            value.optional("lineBlocked")?.let { require(it.isBoolean); it.asBoolean() },
            value.int("damageModifierPercent")
        )
    }

    private fun preview(value: JsonValue): BattleAttackPreview {
        require(value.isObject)
        return BattleAttackPreview(
            attack = hit(value.required("attack")),
            retaliation = value.optional("retaliation")?.let(::hit),
            retaliationBudget = optionalInt(value, "retaliationBudget"),
            remainingRetaliationBudget = optionalInt(value, "remainingRetaliationBudget"),
            usesStoredRetaliation = value.bool("usesStoredRetaliation"),
            isRanged = value.bool("isRanged"),
            rangedShot = value.optional("rangedShot")?.let(::shot)
        )
    }

    private fun hit(value: JsonValue): BattleAttackPreview.Hit {
        require(value.isObject)
        val preparation = value.optional("preparation")?.let {
            require(it.isObject)
            PreparedBattleDamage(it.int("amount"), it.int("damagePerSoldier"),
                it.int("luckMultiplier"), it.int("afterLuck"), it.int("supportBonusPercent"),
                it.int("afterSupport"), it.int("penaltyPercent"), it.int("incomingDamage"))
        }
        val result = value.required("result")
        require(result.isObject)
        return BattleAttackPreview.Hit(
            preparation, flank(value.required("flanking")), value.int("formationBefore"),
            value.int("formationReductionPercent"), value.int("remainingFormation"),
            DamageResult(result.int("damageDealt"), result.int("perished"),
                result.int("remainingAmount"), result.int("remainingHealth"), result.bool("isLuck"))
        )
    }
}
