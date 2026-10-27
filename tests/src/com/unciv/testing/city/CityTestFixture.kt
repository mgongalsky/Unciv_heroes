package com.unciv.testing.city

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileMap
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSettings
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.Closeable
import java.io.File

/** Owns test globals and restores them even when construction or the test fails. */
internal class CityTestFixture : Closeable {
    private val globals = mutableListOf<() -> Unit>()
    private val cachedRulesets = HashMap(RulesetCache)
    private var ownsKoin = false
    private var closed = false

    lateinit var ruleset: Ruleset
        private set
    lateinit var game: GameInfo
        private set
    lateinit var civilization: FakeCivilizationInfo
        private set

    init {
        check(GlobalContext.getOrNull() == null) {
            "CityTestFixture requires an isolated test process without an active Koin context"
        }
        try {
            // Preserve backing fields without triggering lazy/default getters.
            rememberField(UncivGame::class.java, "Current", null)
            rememberField(MapUnit::class.java, "_monsterCivInfo", null)
            rememberField(MapUnit::class.java, "currID", null)
            rememberField(GameConstants::class.java, "_testData", GameConstants)
            rememberField(ArmyInfo::class.java, "_defaultMaxSlots", null)
            rememberField(TroopFactory::class.java, "nextId", TroopFactory)

            UncivGame.Current = UncivGame().apply { settings = GameSettings() }
            GameConstants.setTestingInstance(GameConstantsData(armySize = 4))
            ArmyInfo.setTestingMaxSlots(4)
            TroopFactory.resetIdCounter()
            MapUnit.currID = 1
            MapUnit.setTestingInstance(FakeCivilizationInfo())

            val assets = findAssets()
            RulesetCache.clear()
            for (base in BaseRuleset.values()) {
                val loaded = Ruleset().apply {
                    load(FileHandle(File(assets, "jsons/${base.fullName}")))
                    name = base.fullName
                }
                RulesetCache[base.fullName] = loaded
            }
            ruleset = RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName)
            for (unit in ruleset.units.values) unit.ruleset = ruleset

            ownsKoin = true
            startKoin {
                modules(module {
                    single { ruleset }
                    single<ITroopDefinitionSource> {
                        HardcodedTroopDefinitionSource(5, 10, 100, 0, false)
                    }
                })
            }

            game = GameInfo().apply {
                ruleSet = ruleset
                difficulty = "Prince"
                difficultyObject = ruleset.difficulties.getValue(difficulty)
                speed = ruleset.speeds.getValue(gameParameters.speed)
                gameParameters.numberOfCityStates = 0
                gameParameters.noBarbarians = true
                tileMap = TileMap(6, ruleset, false)
                tileMap.gameInfo = this
            }
            civilization = FakeCivilizationInfo().apply {
                civName = "Rome"
                nation = ruleset.nations.getValue(civName)
                gameInfo = game
                playerType = PlayerType.Human
                isCurrentPlayerOverride = true
            }
            game.civilizations.add(civilization)
            game.currentPlayer = civilization.civName
            game.currentPlayerCiv = civilization
            val firstTech = ruleset.technologies.values.minByOrNull { it.era() }
            if (firstTech != null) civilization.tech.techsResearched.add(firstTech.name)
            civilization.setTransients()
        } catch (failure: Throwable) {
            try {
                close()
            } catch (cleanupFailure: Throwable) {
                failure.addSuppressed(cleanupFailure)
            }
            throw failure
        }
    }

    fun foundCity(position: Vector2 = Vector2(0f, 0f)): CityInfo {
        val founder = MapUnit().apply {
            civInfo = civilization
            owner = civilization.civName
            army.setTransients(civilization, this)
        }
        return CityInfo(civilization, position, founder)
    }

    override fun close() {
        if (closed) return
        closed = true
        val failures = mutableListOf<Throwable>()
        fun restore(action: () -> Unit) {
            try { action() } catch (failure: Throwable) { failures.add(failure) }
        }
        restore { if (ownsKoin) stopKoin() }
        restore {
            RulesetCache.clear()
            RulesetCache.putAll(cachedRulesets)
        }
        for (action in globals.asReversed()) restore(action)
        if (failures.isNotEmpty()) {
            val first = failures.first()
            failures.drop(1).forEach { first.addSuppressed(it) }
            throw first
        }
    }

    private fun rememberField(type: Class<*>, name: String, receiver: Any?) {
        val field = type.getDeclaredField(name).apply { isAccessible = true }
        val previous = field.get(receiver)
        globals.add { field.set(receiver, previous) }
    }

    private fun findAssets(): File {
        val candidates = listOf(File("."), File("android/assets"), File("../android/assets"))
        return candidates.firstOrNull { File(it, "jsons/Civ V - Gods & Kings").isDirectory }
            ?: error("Cannot locate game assets from ${File(".").absolutePath}")
    }
}
