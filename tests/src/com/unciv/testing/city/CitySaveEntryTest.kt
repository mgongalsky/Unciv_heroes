package com.unciv.testing.city

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.city.CitySaveEntry
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class CitySaveEntryTest {
    @Test
    fun ownershipEnvelopePreservesPlayerAndNeutralWithoutBindingRuntimeOwner() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val originalOwner = city.state.ownership
            try {
                for (owner in listOf(CityOwnership.Player("Rome"), CityOwnership.Neutral)) {
                    city.state.ownership = owner
                    val text = json().toJson(CitySaveEntry(city))
                    val tree = JsonReader().parse(text)
                    assertTrue(tree.has("ownership"))
                    assertTrue(tree.has("city"))
                    val restored = json().fromJson(CitySaveEntry::class.java, text).city
                    assertEquals(owner, restored.state.ownership)
                    assertEquals(city.id, restored.id)
                    assertEquals(city.location, restored.location)
                    assertEquals(city.tiles, restored.tiles)
                    assertNotSame(city.state.garrison, restored.state.garrison)
                    assertEquals(city.garrisonInfo.getAllTroops().map { it?.amount },
                        restored.garrisonInfo.getAllTroops().map { it?.amount })
                }
            } finally {
                city.state.ownership = originalOwner
            }
        }
    }

    @Test
    fun missingIdentityLocationOrOwnershipIsRejected() {
        CityTestFixture().use { fixture ->
            val text = json().toJson(CitySaveEntry(fixture.foundCity()))
            for (field in listOf("id", "location", "ownership")) {
                val tree = JsonReader().parse(text)
                if (field == "ownership") tree.remove(field) else tree.get("city").remove(field)
                var rejected = false
                try {
                    json().readValue(CitySaveEntry::class.java, tree)
                } catch (_: Exception) {
                    rejected = true
                }
                assertTrue("Missing $field must be rejected", rejected)
            }
        }
    }
}
