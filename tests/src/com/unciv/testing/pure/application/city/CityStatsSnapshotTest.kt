package com.unciv.testing.pure.application.city

import com.unciv.pure.application.city.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет изоляцию снимка от изменяемых коллекций вызывающего кода без движка. */
class CityStatsSnapshotTest {
    /** Последующая очистка источников не меняет значения, дерево и состав снимка. */
    @Test fun snapshotKeepsCapturedValues() {
        val amounts = linkedMapOf(CityStatResource.Food to -2.5f)
        val values = CityStatValues(amounts)
        val child = CityStatSource("Population", values)
        val children = mutableListOf(child)
        val parent = CityStatSource("Source", values, children)
        val sources = mutableListOf(parent)
        val resources = mutableListOf(CityStatResource.Food)
        val snapshot = CityStatsSnapshot(3, 12, 40, resources, sources, sources, sources, values)
        amounts[CityStatResource.Food] = 99f
        children.clear()
        sources.clear()
        resources.clear()
        assertEquals(-2.5f, snapshot.totals[CityStatResource.Food], 0f)
        assertEquals(0f, snapshot.totals[CityStatResource.Gold], 0f)
        assertEquals(listOf(CityStatResource.Food), snapshot.resources)
        for (section in listOf(snapshot.baseSources, snapshot.percentageSources, snapshot.finalSources)) {
            assertEquals(1, section.size)
            assertEquals("Population", section.single().children.single().name)
            assertEquals(-2.5f, section.single().children.single().values[CityStatResource.Food], 0f)
        }
    }

    /** Приведение интерфейса списка к изменяемому не позволяет повредить опубликованный снимок. */
    @Test(expected = UnsupportedOperationException::class)
    fun publishedSourcesCannotBeCleared() {
        val source = CityStatSource("Tiles", CityStatValues(emptyMap()))
        val snapshot = CityStatsSnapshot(1, 0, 0, emptyList(), listOf(source), emptyList(), emptyList(), source.values)
        (snapshot.baseSources as MutableList<CityStatSource>).clear()
    }

    /** Дочерние источники также защищены от изменения получателем DTO. */
    @Test(expected = UnsupportedOperationException::class)
    fun publishedChildrenCannotBeCleared() {
        val child = CityStatSource("Granary", CityStatValues(emptyMap()))
        val parent = CityStatSource("Buildings", child.values, listOf(child))
        (parent.children as MutableList<CityStatSource>).clear()
    }
}
