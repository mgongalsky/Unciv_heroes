package com.unciv.testing.pure.application.scenario

import com.unciv.pure.application.scenario.ResolveVictoryOptionsUseCase
import com.unciv.pure.domain.scenario.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет авторские ограничения и различие начальных настроек и явного выбора игрока. */
class VictoryOptionsTest {
    private val catalog = listOf("CaptureCity", "Science", "Domination")

    /** Обязательный тип нельзя выключить, запрещённый нельзя включить, Optional выбирает игрок. */
    @Test fun restrictionsOverridePlayerSelectionWithoutMutatingIt() {
        val settings = ScenarioVictorySettings(rules = arrayListOf(
            ScenarioVictoryRule("CaptureCity", ScenarioVictoryMode.Required),
            ScenarioVictoryRule("Domination", ScenarioVictoryMode.Forbidden)
        ))
        val selected = arrayListOf("Science", "Domination")
        assertEquals(listOf("CaptureCity", "Science"), ResolveVictoryOptionsUseCase.execute(catalog, settings, selected))
        assertEquals(listOf("Science", "Domination"), selected)
        assertEquals(listOf("CaptureCity"), ResolveVictoryOptionsUseCase.execute(catalog, settings, emptyList()))
    }

    /** Значение Optional по умолчанию не возвращает тип, который игрок явно отключил. */
    @Test fun optionalDefaultsApplyOnlyToInitialSelection() {
        val settings = ScenarioVictorySettings(rules = arrayListOf(
            ScenarioVictoryRule("CaptureCity", ScenarioVictoryMode.Required),
            ScenarioVictoryRule("Science", defaultEnabled = false),
            ScenarioVictoryRule("Domination", defaultEnabled = true)
        ))
        assertEquals(listOf("CaptureCity", "Domination"), ResolveVictoryOptionsUseCase.execute(catalog, settings, null))
        assertEquals(listOf("CaptureCity", "Science"), ResolveVictoryOptionsUseCase.execute(catalog, settings, listOf("Science")))
    }

    /** Старая карта сохраняет известные выбранные типы, удаляя отсутствующие в текущем каталоге. */
    @Test fun absentSettingsPreserveLegacySelection() {
        assertEquals(listOf("Science"), ResolveVictoryOptionsUseCase.execute(catalog, null, listOf("Science", "Missing")))
        assertEquals(catalog, ResolveVictoryOptionsUseCase.execute(catalog, null, null))
    }

    /** Недоступный обязательный тип диагностируется вместо молчаливого удаления ограничения. */
    @Test(expected = IllegalArgumentException::class)
    fun missingRequiredVictoryIsRejected() {
        ResolveVictoryOptionsUseCase.execute(catalog, ScenarioVictorySettings(rules = arrayListOf(
            ScenarioVictoryRule("Missing", ScenarioVictoryMode.Required)
        )), null)
    }

    /** Явно пустой выбор не заменяется включением всех типов. */
    @Test(expected = IllegalArgumentException::class)
    fun emptyExplicitSelectionIsRejected() {
        ResolveVictoryOptionsUseCase.execute(catalog, null, emptyList())
    }

    /** Карта, запрещающая все доступные победы, не может запуститься. */
    @Test(expected = IllegalArgumentException::class)
    fun allForbiddenIsRejected() {
        ResolveVictoryOptionsUseCase.execute(catalog, ScenarioVictorySettings(rules = ArrayList(catalog.map {
            ScenarioVictoryRule(it, ScenarioVictoryMode.Forbidden)
        })), null)
    }

    /** Повторные настройки одного типа не получают неявного приоритета по порядку списка. */
    @Test(expected = IllegalArgumentException::class)
    fun duplicateRulesAreRejected() {
        ResolveVictoryOptionsUseCase.execute(catalog, ScenarioVictorySettings(rules = arrayListOf(
            ScenarioVictoryRule("Science"), ScenarioVictoryRule("Science", ScenarioVictoryMode.Forbidden)
        )), null)
    }
}
