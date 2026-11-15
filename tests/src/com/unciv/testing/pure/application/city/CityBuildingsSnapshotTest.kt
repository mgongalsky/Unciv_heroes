package com.unciv.testing.pure.application.city

import com.unciv.pure.application.city.CityBuiltBuilding
import com.unciv.pure.application.city.CityBuildingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверки границы снимка без модели игры, графики и файлов. */
class CityBuildingsSnapshotTest {
    /** Замена и удаление исходных записей не меняют показанные данные и разрешения. */
    @Test fun sourceChangesDoNotChangeSnapshot() {
        val building = CityBuiltBuilding("Workshop", "Effect", "Buildings/Workshop", false, 2, 10, true)
        val source = mutableListOf(building)
        val snapshot = CityBuildingsSnapshot(source, 2, 1f)
        source[0] = building.copy(canSell = false, baseMaintenance = 0)
        source.clear()
        assertEquals(listOf(building), snapshot.buildings)
        assertEquals(2, snapshot.baseMaintenance)
        assertEquals(1f, snapshot.effectiveMaintenance, 0f)
    }

    /** Даже приведение к изменяемому интерфейсу не позволяет удалить опубликованные записи. */
    @Test(expected = UnsupportedOperationException::class)
    fun publishedBuildingsCannotBeChanged() {
        val snapshot = CityBuildingsSnapshot(
            listOf(CityBuiltBuilding("Palace", "", "", true, 0, null, false)), 0, 0f
        )
        (snapshot.buildings as MutableList<CityBuiltBuilding>).clear()
    }
}
