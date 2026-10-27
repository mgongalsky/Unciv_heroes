package com.unciv.testing

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.testing.city.CityTestFixture
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SerializationTests {
    private lateinit var game: GameInfo
    private var fixture: CityTestFixture? = null
    private var previousMonsterGame: GameInfo? = null
    private var previousInput: Input? = null
    private var inputReplaced = false

    private val classSynchronizedLazyImpl: Class<*> by lazy {
        class TestWithLazy { val test: Int by lazy { 0 } }
        val instance = TestWithLazy()
        val field = instance::class.java.declaredFields.first { it.name.endsWith("\$delegate") }
        field.isAccessible = true
        field.get(instance)::class.java
    }

    @Before
    fun prepareGame() {
        val environment = CityTestFixture()
        fixture = environment
        previousMonsterGame = CivilizationInfo.monsterGameInfo
        CivilizationInfo.monsterGameInfo = GameInfo()
        previousInput = Gdx.input
        inputReplaced = true
        Gdx.input = MockInput()

        environment.foundCity()
        game = environment.game.clone()
        val other = CivilizationInfo("Greece").apply {
            tech.techsResearched.addAll(environment.civilization.tech.techsResearched)
        }
        game.civilizations.add(other)
        game.setTransients()
        UncivGame.Current.startSimulation(game)
        // Meeting remains real, but the already completed tutorial cannot write settings.
        UncivGame.Current.settings.tutorialTasksCompleted.add("Meet another civilization")
        val civ = game.getCurrentPlayerCivilization()
        civ.makeCivilizationsMeet(other)
        assertNotNull(civ.placeUnitNearTile(civ.cities.single().location, "Warrior"))
    }

    @Test
    fun canSerializeGame() {
        val encoded = json().toJson(game)
        assertTrue("The game must serialize without swallowing exceptions", encoded.isNotEmpty())
        assertFalse(encoded.contains("FakeCivilizationInfo"))
    }

    @Test
    fun serializedLaziesTest() {
        val serializer = json().apply {
            setDeprecated(classSynchronizedLazyImpl, "initializer", true)
            setDeprecated(classSynchronizedLazyImpl, "lock", true)
        }
        val encoded = serializer.toJson(game)
        assertFalse("Lazy implementation objects must not enter saves",
            encoded.contains("kotlin.SynchronizedLazyImpl"))
    }

    @After
    fun cleanup() {
        try {
            previousMonsterGame?.let { CivilizationInfo.monsterGameInfo = it }
            if (inputReplaced) Gdx.input = previousInput
        } finally {
            fixture?.close()
        }
    }
}
