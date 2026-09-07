package com.unciv.testing.simulations

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.Ruleset
import java.io.File

/** Immutable combat inputs captured from the selected game definitions. */
data class BalanceUnit(
    val name: String,
    val speed: Int,
    val health: Int,
    val damage: Int,
    val rangedStrength: Int = 0,
    val formationHealthPercent: Int = 0,
    val formationDamageReductionPercent: Int = 0
) {
    init {
        require(name.isNotBlank())
        require(speed > 0 && health > 0 && damage >= 0 && rangedStrength >= 0) {
            "$name: speed/health must be positive; damage/rangedStrength must be non-negative"
        }
        require(formationHealthPercent >= 0 && formationDamageReductionPercent in 0..100)
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

    fun totalCells(unitCount: Int): Int =
            Math.multiplyExact(Math.multiplyExact(unitCount, unitCount), maxAmount * maxAmount)

    fun totalBattles(unitCount: Int): Int =
            Math.multiplyExact(totalCells(unitCount), battlesPerCell)

    fun seeds(): List<Long> = (0 until battlesPerCell).map { firstSeed + it }
}

object BalanceUnitSources {
    private val names = listOf("Peasant", "Swordsman", "Archer", "Spearman", "Horseman")

    private fun loadGameRuleset(): Ruleset {
        val relative = "jsons/${BaseRuleset.Civ_V_GnK.fullName}"
        val directory =
                generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .flatMap {
                        sequenceOf(
                            File(it, relative),
                            File(it, "android/assets/$relative")
                        )
                    }
                    .firstOrNull { File(it, "Units.json").isFile }
                    ?: error("Cannot locate game ruleset $relative")
        return Ruleset().apply { load(FileHandle(directory)) }
    }

    val preset: List<BalanceUnit>
        get() {
            val ruleset = loadGameRuleset()
            return names.map { name ->
                val unit = requireNotNull(ruleset.units[name]) { "Missing game unit: $name" }
                BalanceUnit(
                    name, unit.speed, unit.health, unit.damage, unit.rangedStrength,
                    unit.formationHealthPercent, unit.formationDamageReductionPercent
                )
            }
        }

    fun load(file: File): List<BalanceUnit> = parse(file.readText())

    fun parse(text: String): List<BalanceUnit> {
        val root = JsonReader().parse(text)
        require(root.isArray) { "Units.json must contain an array" }
        return names.map { name ->
            val matches = root.filter { it.getString("name", "") == name }
            require(matches.size == 1) { "Expected exactly one $name in Units.json" }
            val node = matches.single()
            fun number(field: String, default: String? = null): Int {
                val raw = node.getString(field, default)
                return raw?.toIntOrNull() ?: error("$name: missing or invalid $field")
            }
            BalanceUnit(
                name, number("speed"), number("health"), number("damage"),
                number("rangedStrength", "0"), number("formationHealthPercent", "0"),
                number("formationDamageReductionPercent", "0")
            )
        }
    }

    /** Retain real terrain and unit definitions; apply the experiment's selected combat values. */
    fun ruleset(units: List<BalanceUnit>): Ruleset = loadGameRuleset().apply {
        units.forEach { definition ->
            val unit =
                    requireNotNull(this.units[definition.name]) { "Unknown game unit: ${definition.name}" }
            unit.speed = definition.speed
            unit.health = definition.health
            unit.damage = definition.damage
            unit.rangedStrength = definition.rangedStrength
            unit.formationHealthPercent = definition.formationHealthPercent
            unit.formationDamageReductionPercent = definition.formationDamageReductionPercent
        }
    }
}
