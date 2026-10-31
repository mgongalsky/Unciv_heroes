package com.unciv.testing.pure.application.scenario

import com.unciv.pure.application.scenario.PrepareScenarioHeroesUseCase
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.hero.ScenarioHero
import org.junit.Assert.*
import org.junit.Test

/** Проверяет подготовку героев на значениях, без мира, графики, файлов и глобальных зависимостей. */
class PrepareScenarioHeroesUseCaseTest {
    private val origin = CityPosition(0f, 0f)
    private val next = CityPosition(1f, 0f)
    private val slots = listOf(ScenarioPlayerSlot("main"), ScenarioPlayerSlot("ally"))
    private val participants = listOf("ally" to "Greece", "main" to "Rome")

    private fun hero() = ScenarioHero("guide", "Проводник", "Warrior", "main", origin)

    private fun prepare(
        heroes: List<ScenarioHero>,
        bindings: List<Pair<String, String>> = participants,
        occupied: Set<CityPosition> = emptySet()
    ) = PrepareScenarioHeroesUseCase.execute(
        heroes, slots, setOf(origin, next), bindings, setOf("Warrior"), occupied
    )

    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Ожидался отказ при некорректном сценарии")
        } catch (_: IllegalArgumentException) {
            // Проверяется контракт отказа, без привязки к формулировке сообщения.
        }
    }

    /** Порядок участников не определяет владельца; независимые запуски не разделяют изменяемые описания. */
    @Test
    fun resolvesStableSlotAndSeparatesCopies() {
        val source = hero()
        val first = prepare(listOf(source)).single()
        val second = prepare(listOf(source)).single()
        assertEquals("Rome", first.owner)
        assertEquals(origin, first.description.position)
        first.description.name = "Изменённое имя"
        assertEquals("Проводник", source.name)
        assertEquals("Проводник", second.description.name)
        assertNotSame(source, second.description)
    }

    /** Повтор ID или клетки отклоняется, но одинаковые отображаемые имена допустимы. */
    @Test
    fun rejectsDuplicateIdentityOrPositionButAllowsSameName() {
        val first = hero()
        rejects { prepare(listOf(first, first.copy(position = next))) }
        rejects { prepare(listOf(first, first.copy(id = "other"))) }
        assertEquals(2, prepare(listOf(first, first.copy(id = "other", position = next))).size)
    }

    /** Неизвестные ссылки, координаты и типы не изменяют ни одно описание даже при ошибке второго героя. */
    @Test
    fun rejectsInvalidReferencesWithoutMutatingInput() {
        val first = hero()
        val snapshot = first.copy()
        val second = first.copy(id = "other", position = next)
        for (invalid in listOf(
            second.copy(ownerSlotId = "missing"),
            second.copy(position = CityPosition(99f, 0f)),
            second.copy(unitName = "Missing"),
            second.copy(name = " "),
            second.copy(id = ""),
            second.copy(position = CityPosition(Float.NaN, 0f))
        )) {
            rejects { prepare(listOf(first, invalid)) }
            assertEquals(snapshot, first)
        }
    }

    /** Занятая клетка не заменяется соседней: сценарная расстановка имеет точные координаты. */
    @Test
    fun rejectsOccupiedPosition() {
        rejects { prepare(listOf(hero()), occupied = setOf(origin)) }
        assertEquals(origin, prepare(listOf(hero()), occupied = setOf(next)).single().description.position)
    }

    /** Неполное, повторное или неоднозначное назначение участников не создаёт подготовленных героев. */
    @Test
    fun rejectsInvalidParticipantBindings() {
        rejects { prepare(listOf(hero()), listOf("main" to "Rome")) }
        rejects { prepare(listOf(hero()), participants + ("main" to "Egypt")) }
        rejects { prepare(listOf(hero()), listOf("main" to "Rome", "ally" to "Rome")) }
        rejects { prepare(listOf(hero()), listOf("main" to "", "ally" to "Greece")) }
    }

    /** Пустая конфигурация старой карты не требует участников и не создаёт героев. */
    @Test
    fun emptyScenarioRemainsEmpty() {
        assertTrue(PrepareScenarioHeroesUseCase.execute(
            emptyList(), emptyList(), emptySet(), emptyList(), emptySet()
        ).isEmpty())
    }
}
