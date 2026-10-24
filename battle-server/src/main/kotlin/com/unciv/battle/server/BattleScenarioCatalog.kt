package com.unciv.battle.server

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.session.BattleFieldPreparation
import com.unciv.logic.battle.session.BattleSessionSetup
import com.unciv.logic.map.ClimateParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.session.BattleFieldSession
import com.unciv.pure.application.battle.session.BattleSession
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import java.io.File
import java.security.SecureRandom

/** Loads trusted server assets once. Network requests select IDs, never file paths or unit stats. */
class BattleScenarioCatalog(assetsDirectory: File) {
    private var recruitmentMoraleProbability = 0.0
    private var recruitmentLuckProbability = 0.0

    data class Scenario(
        val id: String,
        val title: String,
        val description: String,
        val attacker: BattleSessionSetup.Side,
        val defender: BattleSessionSetup.Side,
        val luckProbability: Double,
        val moraleProbability: Double
    )

    private val ruleset = Ruleset().apply {
        val directory = File(assetsDirectory, "jsons/Civ V - Gods & Kings")
        require(File(directory, "Units.json").isFile) { "Missing ruleset in $directory" }
        load(FileHandle(directory))
    }
    private val source = RulesetTroopDefinitionSource(ruleset)
    private val seedSource = SecureRandom()
    val scenarios: List<Scenario>
    private val byId: Map<String, Scenario>

    init {
        val constants = JsonReader().parse(FileHandle(File(assetsDirectory, "jsons/gameConstants.json")))
        val slots = constants.getInt("armySize")
        require(slots in 1..5) { "Battle armySize must be between 1 and 5" }
        val defaultLuck = constants.getDouble("luckProbability")
        val defaultMorale = constants.getDouble("moraleProbability")
        require(defaultLuck in 0.0..1.0 && defaultMorale in 0.0..1.0)
        recruitmentLuckProbability = defaultLuck
        recruitmentMoraleProbability = defaultMorale
        val catalog = JsonReader().parse(FileHandle(File(assetsDirectory, "jsons/BattleScenarios.json")))
        val entries = requireNotNull(catalog.get("scenarios")) { "Missing scenarios" }
        require(entries.isArray && entries.size > 0)
        scenarios = entries.map { node ->
            val effects = node.get("effects")?.takeUnless { it.isNull }
            val luck = effects?.getInt("luck", 1) ?: 1
            val morale = effects?.getInt("morale", 0) ?: 0
            fun probability(name: String, default: Double): Double {
                val value = effects?.get(name)?.takeUnless { it.isNull }?.asDouble() ?: default
                require(value in 0.0..1.0) { "Invalid $name" }
                return value
            }
            fun side(value: JsonValue): BattleSessionSetup.Side {
                val name = value.getString("unitName")
                val count = value.getInt("totalCount")
                require(count > 0)
                val unit = requireNotNull(ruleset.units[name]) { "Unknown scenario unit: $name" }
                require(unit.speed > 0 && unit.health > 0 && unit.damage >= 0)
                val stacks = (0 until slots).mapNotNull { index ->
                    val amount = count / slots + if (index < count % slots) 1 else 0
                    if (amount == 0) null else BattleSessionSetup.Stack(name, amount)
                }
                return BattleSessionSetup.Side(stacks, luck, morale)
            }
            val id = node.getString("id")
            require(id.matches(Regex("[a-z0-9][a-z0-9-]{0,63}"))) { "Invalid scenario ID" }
            Scenario(
                id, node.getString("title"), node.getString("description", ""),
                side(requireNotNull(node.get("attacker"))), side(requireNotNull(node.get("defender"))),
                probability("luckProbability", defaultLuck), probability("moraleProbability", defaultMorale)
            )
        }
        require(scenarios.map { it.id }.distinct().size == scenarios.size) { "Duplicate scenario IDs" }
        byId = scenarios.associateBy { it.id }
    }

    fun create(scenarioId: String): BattleSession = create(scenarioId, seedSource.nextLong())

    /** Explicit seed supports reproducible integration tests; the server chooses production seeds. */
    fun create(scenarioId: String, seed: Long): BattleSession {
        val scenario = requireNotNull(byId[scenarioId]) { "Unknown battle scenario" }
        return createConfigured(scenario.attacker, scenario.defender, scenario.luckProbability, scenario.moraleProbability, seed)
    }

    fun createPurchased(
        attacker: List<com.unciv.pure.application.battle.recruitment.BattleArmyStack>,
        defender: List<com.unciv.pure.application.battle.recruitment.BattleArmyStack>
    ): BattleSession = createConfigured(
        com.unciv.logic.battle.session.BattleRecruitmentRules.side(recruitment, attacker),
        com.unciv.logic.battle.session.BattleRecruitmentRules.side(recruitment, defender),
        recruitmentLuckProbability, recruitmentMoraleProbability, seedSource.nextLong()
    )

    private fun createConfigured(
        attacker: BattleSessionSetup.Side,
        defender: BattleSessionSetup.Side,
        luckProbability: Double,
        moraleProbability: Double,
        seed: Long
    ): BattleSession {
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        val prepared = BattleFieldPreparation.create(climate, climate) { name ->
            requireNotNull(ruleset.terrains[name]) { "Missing battlefield terrain definition: $name" }.impassable
        }
        val session = BattleSessionSetup.create(
            attacker, defender, source,
            prepared.field, SeededBattleRandom(seed),
            luckProbability, moraleProbability
        )
        return BattleFieldSession(session, prepared.description)
    }
    val recruitment by lazy { com.unciv.logic.battle.session.BattleRecruitmentRules.from(ruleset) }
}
