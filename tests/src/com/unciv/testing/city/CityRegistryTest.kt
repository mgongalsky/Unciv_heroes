package com.unciv.testing.city

import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

class CityRegistryTest {
    private class City(val state: CityState)

    private fun city(id: String, x: Float, owner: String = "Rome") = City(
        CityState(CityId(id), ownership = CityOwnership.Player(owner), position = CityPosition(x, 0f))
    )

    private fun registry() = CityRegistry<City> { it.state }

    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Expected invalid registration to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun lookupsReturnOriginalObjectsAndOwnershipFollowsState() {
        val registry = registry()
        val first = city("first", 0f)
        val second = city("second", 2f, "Greece")
        registry.register(first)
        registry.register(second)
        assertSame(first, registry.byId(CityId("first")))
        assertSame(second, registry.at(CityPosition(2f, 0f)))
        assertEquals(listOf(first), registry.ownedBy(CityOwnership.Player("Rome")))
        first.state.ownership = CityOwnership.Player("Greece")
        assertTrue(registry.ownedBy(CityOwnership.Player("Rome")).isEmpty())
        assertEquals(listOf(first, second), registry.ownedBy(CityOwnership.Player("Greece")))
        assertSame(first, registry.byId(CityId("first")))
    }

    @Test
    fun duplicateIdentityPositionAndUnboundOwnerLeaveRegistryUnchanged() {
        val registry = registry()
        val original = city("first", 0f)
        registry.register(original)
        rejects { registry.register(original) }
        rejects { registry.register(city("first", 2f)) }
        rejects { registry.register(city("second", 0f)) }
        rejects { registry.register(city("unbound", 3f).also { it.state.ownership = null }) }
        assertEquals(listOf(original), registry.all())
    }

    @Test
    fun replacementIsValidatedBeforeChangingMembership() {
        val registry = registry()
        val original = city("original", 0f)
        registry.register(original)
        rejects { registry.replaceAll(listOf(city("new", 1f), city("new", 2f))) }
        assertEquals(listOf(original), registry.all())
        val replacement = city("replacement", 3f)
        registry.replaceAll(listOf(replacement))
        assertNull(registry.byId(original.state.id))
        assertSame(replacement, registry.at(replacement.state.position))
    }

    @Test
    fun removalUsesObjectIdentityAndPreviousSnapshotsRemainStable() {
        val registry = registry()
        val original = city("first", 0f)
        registry.register(original)
        val snapshot = registry.all()
        assertFalse(registry.remove(city("first", 0f)))
        assertTrue(registry.contains(original))
        assertTrue(registry.remove(original))
        assertFalse(registry.remove(original))
        assertTrue(registry.all().isEmpty())
        assertNull(registry.byId(original.state.id))
        assertEquals(listOf(original), snapshot)
    }
}
