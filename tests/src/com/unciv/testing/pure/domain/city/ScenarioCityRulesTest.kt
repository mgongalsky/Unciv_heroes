package com.unciv.testing.pure.domain.city

import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Локальные инварианты проверяются без карты и назначений участников. */
class ScenarioCityRulesTest {
    private fun city() = ScenarioCity(id = "fort", name = "Fort",
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle")

    /** Для проверки описания игрока не требуется искать его слот в мире. */
    @Test fun acceptsLocalDescriptionWithoutResolvingExternalReferences() {
        val input = city().apply { owner = ScenarioCityOwner.PlayerSlot; ownerSlotId = "external" }
        val before = input.deepCopy()
        ScenarioCityRules.validate(input)
        assertEquals(before, input)
    }

    /** Некорректные население и гарнизон запрещены независимо от окружения. */
    @Test fun rejectsInvalidPopulationAndGarrison() {
        rejects { ScenarioCityRules.validate(city().apply { population = 0 }) }
        rejects { ScenarioCityRules.validate(city().apply { foodStored = -1 }) }
        rejects { ScenarioCityRules.validate(city().apply { garrison.add(ScenarioCityTroop("Guard", 0)) }) }
    }

    /** Нейтральный город требует своего типа и не может ссылаться на владельца-слот. */
    @Test fun rejectsInconsistentNeutralOwnership() {
        rejects { ScenarioCityRules.validate(city().apply { ownerSlotId = "slot" }) }
        rejects { ScenarioCityRules.validate(city().apply {
            typeMode = ScenarioCityTypeMode.FromStartingPlayer; fixedType = ""
        }) }
    }

    /** Территория одного города включает центр и не содержит повторных клеток. */
    @Test fun rejectsMissingCenterAndRepeatedTerritory() {
        rejects { ScenarioCityRules.validate(city().apply { territory.add(CityPosition(1f, 0f)) }) }
        rejects { ScenarioCityRules.validate(city().apply { territory.addAll(listOf(position, position)) }) }
    }

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Expected invalid city description to be rejected") }
        catch (_: IllegalArgumentException) { }
    }
}
