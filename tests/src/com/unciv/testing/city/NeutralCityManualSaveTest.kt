package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.NeutralCityBattleSession
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Builds the user-requested manual smoke-test save and validates it before publishing. */
class NeutralCityManualSaveTest {
    @Test
    fun prepareLoadableSaveWithEmptyAndDefendedNeutralCities() {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                val previousMonsterGame = CivilizationInfo.monsterGameInfo
                CivilizationInfo.monsterGameInfo = GameInfo()
                try {
                    fixture.foundCity().name = "Home"
                    val game = fixture.game
                    game.turns = 1
                    game.gameParameters.victoryTypes.clear()
                    game.gameParameters.noBarbarians = true
                    game.gameParameters.nuclearWeaponsEnabled = false
                    val source = RulesetTroopDefinitionSource(fixture.ruleset)

                    fun army(amount: Int, neutral: Boolean) = ArmyInfo(
                        if (neutral) null else fixture.civilization, 4
                    ).apply {
                        if (amount > 0) setTroopAt(0, TroopFactory.create("Spearman", amount, source))
                    }
                    fun neutralCity(cityName: String, position: Vector2, defenders: Int) = CityInfo().apply {
                        name = cityName
                        state.ownership = CityOwnership.Neutral
                        state.isCurrentCapital = false
                        location = position
                        tiles.add(position.cpy())
                        health = 91
                        population.foodStored = 17
                        cityConstructions.builtBuildings.add("Granary")
                        cityConstructions.constructionQueue.add("Scout")
                        cityConstructions.inProgressConstructions["Scout"] = 23
                        garrisonInfo = army(defenders, true)
                        bindNeutralWorld(game)
                        game.cityRegistry.register(this)
                        setTransients()
                    }
                    val empty = neutralCity("Emptyford", Vector2(4f, 0f), 0)
                    val defended = neutralCity("Guardford", Vector2(-4f, 0f), 5)
                    for (position in listOf(Vector2(3f, 0f), Vector2(-3f, 0f))) {
                        val hero = fixture.civilization.placeUnitNearTile(position, "Warrior")!!
                        assertEquals(position, hero.currentTile.position)
                        hero.hero = com.unciv.pure.domain.hero.HeroFactory.create(
                            source = com.unciv.pure.infrastructure.hero.RulesetHeroDefinitionSource(fixture.ruleset),
                            unitName = "Warrior", currentFood = 150f, morale = 2, luck = 3
                        )
                        hero.army = army(50, false)
                        hero.army.setTransients(fixture.civilization, hero)
                        hero.currentMovement = 2f
                        hero.attacksThisTurn = 0
                    }
                    fixture.civilization.exploredTiles = game.tileMap.values.map { it.position.cpy() }.toHashSet()
                    fixture.civilization.updateSightAndResources()

                    val save = UncivFiles.gameInfoToString(game.clone(), false)
                    val loaded = UncivFiles.gameInfoFromString(save)
                    val restoredEmpty = loaded.cityRegistry.byId(empty.state.id)!!
                    val restoredDefended = loaded.cityRegistry.byId(defended.state.id)!!
                    val emptyHero = loaded.tileMap[Vector2(3f, 0f)].militaryUnit!!
                    val battleHero = loaded.tileMap[Vector2(-3f, 0f)].militaryUnit!!
                    for (hero in listOf(emptyHero, battleHero)) {
                        val originalHero = game.tileMap[hero.currentTile.position].militaryUnit!!.hero
                        assertEquals(originalHero.baseAttackSkill, hero.hero.baseAttackSkill)
                        assertEquals(originalHero.baseDefenseSkill, hero.hero.baseDefenseSkill)
                        assertEquals(originalHero.baseFoodCapacity, hero.hero.baseFoodCapacity, 0f)
                        assertNotSame(originalHero, hero.hero)
                        assertEquals(150f, hero.hero.currentFood, 0f)
                        assertEquals(2, hero.hero.morale)
                        assertEquals(3, hero.hero.luck)
                        assertEquals(50, hero.army.getTroopAt(0)!!.currentAmount)
                    }
                    assertEquals(3, loaded.getCities().count())
                    assertEquals(1, loaded.civilizations.single().cities.size)
                    assertEquals(Decision.ACQUIRE, NeutralCityEntry.decision(restoredEmpty, emptyHero))
                    assertTrue(emptyHero.movement.canMoveTo(restoredEmpty.getCenterTile()))
                    assertEquals(Decision.FIGHT_GARRISON, NeutralCityEntry.decision(restoredDefended, battleHero))
                    assertFalse(battleHero.movement.canMoveTo(restoredDefended.getCenterTile()))
                    assertTrue(NeutralCityBattleSession.canStart(battleHero, restoredDefended))
                    assertTrue(restoredDefended.getCenterTile() in battleHero.civInfo.viewableTiles)
                    for (city in listOf(restoredEmpty, restoredDefended)) {
                        assertNull(city.ownerOrNull)
                        assertNull(city.garrisonInfo.ownerOrNull)
                        assertEquals(91, city.health)
                        assertEquals(17, city.population.foodStored)
                        assertEquals(23, city.cityConstructions.inProgressConstructions["Scout"])
                    }

                    var root = File(".").canonicalFile
                    while (!File(root, "core/src/com/unciv").isDirectory) {
                        root = root.parentFile ?: error("Cannot locate project root")
                    }
                    val output = File(root, "tests/resources/manual/neutral-cities.json")
                    check(output.parentFile.isDirectory || output.parentFile.mkdirs())
                    output.writeText(save, Charsets.UTF_8)
                    val fromDisk = UncivFiles.gameInfoFromString(output.readText(Charsets.UTF_8))
                    assertEquals(3, fromDisk.getCities().count())
                    println("Manual neutral-city save: ${output.absolutePath}")
                } finally {
                    CivilizationInfo.monsterGameInfo = previousMonsterGame
                }
            }
        } finally {
            Gdx.input = previousInput
        }
    }
}
