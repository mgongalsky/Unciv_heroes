package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.execute
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BattleManagerCommandEventBridgeTest {
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerArmy: ArmyInfo

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 5
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
        attackerArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Spearman", 10) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Spearman", 8) }
        val tile = FakeBattleTile(Vector2(0f, 0f))
        manager = TestableBattleManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(tile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        manager.initializeTurnQueue()
        manager.placeTroop(attackerArmy.getAllTroops().filterNotNull().first(), tile)
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun skipCommandPublishesBothEventContractsAndRestoresLegacyHandler() {
        val legacyEvents = mutableListOf<com.unciv.pure.domain.battle.BattleEvent>()
        val applicationEvents = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val legacyHandler: (com.unciv.pure.domain.battle.BattleEvent) -> Unit = { event ->
            legacyEvents.add(event)
        }
        manager.onEvent = legacyHandler
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()

        manager.execute(BattleCommand.Skip(attacker.id)) { event ->
            applicationEvents.add(event)
        }

        assertEquals(listOf(com.unciv.pure.domain.battle.BattleEvent.TurnSkipped), legacyEvents)
        assertEquals(
            listOf(com.unciv.pure.application.battle.BattleEvent.TurnSkipped),
            applicationEvents
        )
        assertSame(legacyHandler, manager.onEvent)
    }
}
