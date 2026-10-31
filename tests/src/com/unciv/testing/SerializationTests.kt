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

/** Проверяет сериализацию работающей игры в изолированном окружении без записи пользовательских настроек. */
class SerializationTests {
    private lateinit var game: GameInfo
    private var fixture: CityTestFixture? = null
    private var previousMonsterGame: GameInfo? = null
    private var previousInput: Input? = null
    private var inputReplaced = false

    /** Получает фактический класс Kotlin lazy через делегат, не привязываясь к его доступности в исходном коде. */
    private val classSynchronizedLazyImpl: Class<*> by lazy {
        class TestWithLazy { val test: Int by lazy { 0 } }
        val instance = TestWithLazy()
        val field = instance::class.java.declaredFields.first { it.name.endsWith("\$delegate") }
        field.isAccessible = true
        field.get(instance)::class.java
    }

    /** Создаёт обычные цивилизации, город, контакт и юнита, чтобы сериализация охватила реальные игровые связи. */
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
        // Контакт остаётся настоящим, но уже пройденное обучение не вызывает запись настроек.
        UncivGame.Current.settings.tutorialTasksCompleted.add("Meet another civilization")
        val civ = game.getCurrentPlayerCivilization()
        civ.makeCivilizationsMeet(other)
        assertNotNull(civ.placeUnitNearTile(civ.cities.single().location, "Warrior"))
    }

    /** Игра сериализуется без подавления исключений и без тестового класса цивилизации в формате сохранения. */
    @Test
    fun canSerializeGame() {
        val encoded = json().toJson(game)
        assertTrue("The game must serialize without swallowing exceptions", encoded.isNotEmpty())
        assertFalse(encoded.contains("FakeCivilizationInfo"))
    }

    /** Внутренний объект lazy не должен попадать в JSON даже после исключения его служебных полей. */
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

    /** Восстанавливает дополнительные глобальные ссылки и обязательно закрывает заготовку города. */
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
