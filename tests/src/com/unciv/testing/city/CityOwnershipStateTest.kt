package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityId
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.CityState
import org.junit.Assert.*
import org.junit.Test

/** Проверяет границу между чистым владением, обязательным legacy-владельцем и восстановлением JSON. */
class CityOwnershipStateTest {
    /** Отсутствующая привязка отличается от Neutral; изменение снимка не меняет владельца оригинала. */
    @Test
    fun unboundAndNeutralAreDistinctAndSnapshotsKeepOwnership() {
        val state = CityState(CityId("city-1"))
        assertNull(state.ownership)
        state.ownership = CityOwnership.Neutral
        val snapshot = state.deepCopy()
        assertSame(CityOwnership.Neutral, snapshot.ownership)
        snapshot.ownership = CityOwnership.Player("slot-1")
        assertSame(CityOwnership.Neutral, state.ownership)
        assertEquals(CityOwnership.Player("slot-1"), snapshot.ownership)
    }

    /** Обращение к обязательному civInfo до привязки должно явно завершиться ошибкой. */
    @Test(expected = UninitializedPropertyAccessException::class)
    fun emptyCityHasNoLegacyOwner() {
        val city = CityInfo()
        assertNull(city.state.ownership)
        city.civInfo
    }

    /** Основание привязывает владельца; отдельный старый JSON города ждёт владельца от контейнера игры. */
    @Test
    fun foundingBindsCoreOwnerAndLegacyJsonWaitsForContainerBinding() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val owner = fixture.civilization
            assertEquals(CityOwnership.Player(owner.civName), city.state.ownership)
            assertSame(owner, city.civInfo)
            val serialized = json().toJson(city)
            val tree = com.badlogic.gdx.utils.JsonReader().parse(serialized)
            assertFalse(tree.has("ownerAdapter"))
            assertFalse(tree.has("ownership"))
            val restored = json().fromJson(CityInfo::class.java, serialized)
            assertNull(restored.state.ownership)
            restored.civInfo = owner
            assertEquals(CityOwnership.Player(owner.civName), restored.state.ownership)
            assertSame(owner, restored.civInfo)
            restored.setTransients()
            assertEquals(city.location, restored.getCenterTile().position)
        }
    }
}
