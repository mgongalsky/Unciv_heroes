package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.json.json
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityPosition
import org.junit.Assert.*
import org.junit.Test

class CityPositionStateTest {
    @Test
    fun boundaryVectorsCannotMutateCoreCoordinates() {
        val city = CityInfo()
        val input = Vector2(2f, -1f)
        city.location = input
        input.set(8f, 9f)
        assertEquals(CityPosition(2f, -1f), city.state.position)
        val returned = city.location
        returned.set(4f, 5f)
        assertEquals(Vector2(2f, -1f), city.location)
        city.state.position = CityPosition(-3f, 1f)
        assertEquals(Vector2(-3f, 1f), city.location)
        val snapshot = city.state.deepCopy()
        snapshot.position = CityPosition(7f, 7f)
        assertEquals(CityPosition(-3f, 1f), city.state.position)
    }

    @Test
    fun clonedCityKeepsCoordinatesWithoutSharingMutableVectors() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val originalPosition = city.state.position
            val copy = city.clone()
            assertEquals(originalPosition, copy.state.position)
            copy.location.set(9f, 9f)
            assertEquals(originalPosition, copy.state.position)
            copy.location = Vector2(2f, 1f)
            assertEquals(CityPosition(2f, 1f), copy.state.position)
            assertEquals(originalPosition, city.state.position)
        }
    }

    @Test
    fun legacyLocationJsonRestoresCoreCoordinates() {
        val codec = json()
        val city = CityInfo()
        city.location = Vector2(2f, -1f)
        val serialized = codec.toJson(city)
        val tree = com.badlogic.gdx.utils.JsonReader().parse(serialized)
        assertTrue(tree.has("location"))
        assertFalse(tree.has("position"))
        assertFalse(tree.has("state"))
        val restored = codec.fromJson(CityInfo::class.java, serialized)
        assertEquals(city.state.position, restored.state.position)
        assertEquals(city.location, restored.location)
        val withoutLocation = codec.fromJson(CityInfo::class.java, "{}")
        assertEquals(CityPosition(0f, 0f), withoutLocation.state.position)
    }
}
