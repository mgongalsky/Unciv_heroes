package com.unciv.testing.simulations

import com.badlogic.gdx.utils.JsonReader
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import java.io.File

/** Immutable inputs: one experiment uses a snapshot even if the source file changes. */
data class BalanceUnit(
    val name: String,
    val speed: Int,
    val health: Int,
    val damage: Int,
    val rangedStrength: Int = 0
) {
    init {
        require(name.isNotBlank())
        require(speed > 0 && health > 0 && damage >= 0 && rangedStrength >= 0) {
            "$name: speed/health must be positive; damage/rangedStrength must be non-negative"
        }
    }
}

data class BalanceExperimentConfig(
    val battlesPerCell: Int = 20,
    val maxAmount: Int = 20,
    val firstSeed: Long = 0
) {
    init {
        require(battlesPerCell in 1..10000) { "Battles per cell must be 1..10000" }
        require(maxAmount in 1..20) { "Maximum army amount must be 1..20" }
        require(firstSeed >= 0 && firstSeed <= Long.MAX_VALUE - battlesPerCell) { "Invalid seed range" }
    }

    fun totalCells(unitCount: Int): Int = Math.multiplyExact(
        Math.multiplyExact(unitCount, unitCount), maxAmount * maxAmount
    )

    fun totalBattles(unitCount: Int): Int =
        Math.multiplyExact(totalCells(unitCount), battlesPerCell)

    fun seeds(): List<Long> = (0 until battlesPerCell).map { firstSeed + it }
}

object BalanceUnitSources {
    val preset = listOf(
        BalanceUnit("Peasant", 4, 5, 2),
        BalanceUnit("Swordsman", 7, 50, 15),
        BalanceUnit("Archer", 3, 15, 5, 7),
        BalanceUnit("Spearman", 5, 40, 10),
        BalanceUnit("Horseman", 10, 60, 20)
    )

    fun load(file: File): List<BalanceUnit> = parse(file.readText())

    fun parse(text: String): List<BalanceUnit> {
        val root = JsonReader().parse(text)
        require(root.isArray) { "Units.json must contain an array" }
        return preset.map { expected ->
            val matches = root.filter { it.getString("name", "") == expected.name }
            require(matches.size == 1) { "Expected exactly one ${expected.name} in Units.json" }
            val node = matches.single()
            fun number(field: String, default: String? = null): Int {
                val raw = node.getString(field, default)
                return raw?.toIntOrNull() ?: error("${expected.name}: missing or invalid $field")
            }
            BalanceUnit(
                expected.name,
                number("speed"),
                number("health"),
                number("damage"),
                number("rangedStrength", "0")
            )
        }
    }

    fun ruleset(units: List<BalanceUnit>): Ruleset = Ruleset().apply {
        unitTypes["Melee"] = UnitType().apply { name = "Melee" }
        unitTypes["Ranged"] = UnitType().apply { name = "Ranged" }
        units.forEach { definition ->
            this.units[definition.name] = BaseUnit().apply {
                name = definition.name
                unitType = if (definition.rangedStrength > 0) "Ranged" else "Melee"
                speed = definition.speed
                health = definition.health
                damage = definition.damage
                rangedStrength = definition.rangedStrength
            }
        }
    }
}
