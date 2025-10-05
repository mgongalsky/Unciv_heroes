package com.unciv.clean.application.usecases.battle

import com.badlogic.gdx.math.Vector2

/** Лёгкие ссылки, чтобы не тянуть тяжёлые сущности в Application. */
data class UnitRef(val civId: String, val unitId: Int)

enum class AttackType { Melee, Ranged }

data class AttackCommand(
    val attacker: UnitRef,
    val defender: UnitRef,
    val attackerTile: Vector2,
    val defenderTile: Vector2,
    val type: AttackType
)
