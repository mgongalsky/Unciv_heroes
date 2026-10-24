package com.unciv.testing

import com.unciv.pure.application.battle.recruitment.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class BattleArmyTemplatesTest {
    private fun selection(budget: Int = 300, slots: Int = 4) = BattleArmySelection(
        listOf(
            BattleRecruitmentUnit("Peasant", 1),
            BattleRecruitmentUnit("Archer", 3),
            BattleRecruitmentUnit("Spearman", 8),
            BattleRecruitmentUnit("Swordsman", 11),
            BattleRecruitmentUnit("Horseman", 11)
        ), budget, slots
    )

    @Test
    fun templatesRespectTheirCompositionAndBudget() {
        val rules = selection()
        BattleArmyTemplate.values().forEach { template ->
            val army = BattleArmyTemplates.create(rules, template)
            assertEquals(army, rules.validate(army))
            assertEquals(4, army.size)
            assertTrue(rules.cost(army) <= 300L)
            when (template) {
                BattleArmyTemplate.CAVALRY -> {
                    assertTrue(army.all { it.unitName == "Horseman" })
                    assertEquals(27, army.sumOf { it.count })
                }
                BattleArmyTemplate.NO_ARCHERS -> assertTrue(army.none { it.unitName == "Archer" })
                BattleArmyTemplate.ARCHERS -> assertTrue(
                    rules.cost(army.filter { it.unitName == "Archer" }) > 150L
                )
                BattleArmyTemplate.MILITIA -> assertTrue(
                    rules.cost(army.filter { it.unitName == "Peasant" }) > 150L
                )
                BattleArmyTemplate.BALANCED -> assertEquals(4, army.map { it.unitName }.distinct().size)
            }
        }
    }

    @Test
    fun randomArmiesAreValidVariedAndReproducible() {
        val rules = selection()
        val armies = (0 until 100).map { seed ->
            val army = BattleArmyTemplates.create(rules, random = Random(seed))
            assertEquals(army, rules.validate(army))
            assertEquals(army, BattleArmyTemplates.create(rules, random = Random(seed)))
            army
        }
        assertTrue(armies.distinct().size > 20)
    }

    @Test
    fun usesCatalogPricesAndSlotLimits() {
        for (slots in 1..4) {
            val rules = BattleArmySelection(listOf(BattleRecruitmentUnit("Horseman", 17)), 91, slots)
            val army = BattleArmyTemplates.create(rules, BattleArmyTemplate.CAVALRY)
            assertTrue(army.size <= slots)
            assertEquals(5, army.sumOf { it.count })
            assertEquals(85L, rules.cost(army))
        }
        val tiny = selection(budget = 2)
        val army = BattleArmyTemplates.create(tiny, random = Random(42))
        assertEquals(2L, tiny.cost(army))
        assertTrue(army.all { it.unitName == "Peasant" })
    }

    @Test
    fun unavailableTemplateDoesNotSilentlyIntroduceForbiddenUnits() {
        val rules = BattleArmySelection(listOf(BattleRecruitmentUnit("Archer", 3)))
        for (template in listOf(BattleArmyTemplate.CAVALRY, BattleArmyTemplate.NO_ARCHERS)) {
            try {
                BattleArmyTemplates.create(rules, template)
                fail("Expected unavailable template: $template")
            } catch (_: IllegalArgumentException) {
                // The editor reports the unavailable template and keeps the existing army.
            }
        }
    }

    @Test
    fun templateCanBeEditedAndLockedWithoutExceedingBudget() {
        val rules = selection()
        val draft = BattleArmyDraft(rules)
        draft.restore(BattleArmyTemplates.create(rules))
        draft.adjust(0, -10)
        draft.choose(0, "Horseman")
        draft.fill(0)
        assertEquals(draft.stacks(), rules.validate(draft.stacks()))
        val before = draft.stacks()
        draft.locked = true
        draft.adjust(0, 10)
        draft.clear(1)
        assertEquals(before, draft.stacks())
    }
}
