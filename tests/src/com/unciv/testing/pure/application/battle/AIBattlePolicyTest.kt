package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.Point
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AIBattlePolicyTest {
    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerTile: FakeBattleTile
    private lateinit var defenderTile: FakeBattleTile

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            unitTypes["Ranged"] = UnitType().apply { name = "Ranged" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Melee"; speed = 5; health = 40; damage = 10
                formationHealthPercent = 50
                formationDamageReductionPercent = 60
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"; unitType = "Ranged"; speed = 3; health = 15; damage = 5
                rangedStrength = 7
                formationHealthPercent = 20
                formationDamageReductionPercent = 25
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        defenderTile = FakeBattleTile(Vector2(1f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        val civ = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 10) }
        manager = TestableBattleManager(
            attackerArmy, defenderArmy,
            FakeBattleField(listOf(attackerTile, defenderTile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        manager.placeTroop(attackerArmy.getAllTroops().filterNotNull().first(), attackerTile)
        manager.placeTroop(defenderArmy.getAllTroops().filterNotNull().first(), defenderTile)
        manager.initializeTurnQueue()
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `adjacent melee chooses ATTACK without executing it`() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        val command = AIBattlePolicy(manager).chooseCommand(attacker.id)
        assertEquals(BattleCommand.Attack(attacker.id, Point(1, 0), Point(0, 0)), command)
        assertEquals(10, defender.currentAmount)
    }

    @Test
    fun `ranged policy prioritizes ranged enemy`() {
        val civ = FakeCivilizationInfo()
        val archerArmy = ArmyInfo(civ, 5).apply { addUnits("Archer", 10) }
        val mixedArmy = ArmyInfo(civ, 5).apply {
            addUnits("Spearman", 5)
            addUnits("Archer", 5)
        }
        val shooterTile = FakeBattleTile(Vector2(0f, 0f))
        val meleeTile = FakeBattleTile(Vector2(1f, 0f))
        val rangedTile = FakeBattleTile(Vector2(2f, 0f))
        val rangedManager = TestableBattleManager(
            archerArmy,
            mixedArmy,
            FakeBattleField(listOf(shooterTile, meleeTile, rangedTile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        val shooter = archerArmy.getAllTroops().filterNotNull().first()
        val enemies = mixedArmy.getAllTroops().filterNotNull()
        rangedManager.placeTroop(shooter, shooterTile)
        rangedManager.placeTroop(enemies[0], meleeTile)
        rangedManager.placeTroop(enemies[1], rangedTile)
        val command = AIBattlePolicy(rangedManager).chooseCommand(shooter.id)
        assertEquals(BattleCommand.Shoot(shooter.id, Point(2, 0)), command)
    }

    @Test
    fun `policy returns null when troop has no enemies`() {
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        defenderArmy.getAllTroops().filterNotNull().forEach(manager::removeTroop)
        assertNull(AIBattlePolicy(manager).chooseCommand(attacker.id))
    }

    @Test
    fun `ranged policy focuses enemy with broken formation without mutating it`() {
        val civ = FakeCivilizationInfo()
        val archerArmy = ArmyInfo(civ, 5).apply { addUnits("Archer", 10) }
        val mixedArmy = ArmyInfo(civ, 5).apply {
            addUnits("Archer", 5)
            addUnits("Spearman", 5)
        }
        val shooterTile = FakeBattleTile(Vector2(0f, 0f))
        val intactRangedTile = FakeBattleTile(Vector2(1f, 0f))
        val brokenMeleeTile = FakeBattleTile(Vector2(2f, 0f))
        val rangedManager = TestableBattleManager(
            archerArmy,
            mixedArmy,
            FakeBattleField(listOf(shooterTile, intactRangedTile, brokenMeleeTile)),
            FakeBattleRandom(List(100) { 0.0 })
        )
        val shooter = archerArmy.getAllTroops().filterNotNull().first()
        val enemies = mixedArmy.getAllTroops().filterNotNull()
        val brokenEnemy = enemies.first { !it.isRanged }.apply { formation.current = 0 }
        rangedManager.placeTroop(shooter, shooterTile)
        rangedManager.placeTroop(enemies.first { it.isRanged }, intactRangedTile)
        rangedManager.placeTroop(brokenEnemy, brokenMeleeTile)
        val command = AIBattlePolicy(rangedManager).chooseCommand(shooter.id)
        assertEquals(BattleCommand.Shoot(shooter.id, Point(2, 0)), command)
        assertEquals(0, brokenEnemy.formation.current)
    }
}
