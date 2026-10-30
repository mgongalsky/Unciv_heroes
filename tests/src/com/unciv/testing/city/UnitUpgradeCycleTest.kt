package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitUpgradeCycleTest {
    private fun checkPath(links: Map<String, String?>, expected: List<String>) {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                val originals = links.keys.associateWith { fixture.ruleset.units.getValue(it).upgradesTo }
                try {
                    for ((name, target) in links)
                        fixture.ruleset.units.getValue(name).upgradesTo = target
                    val unit = fixture.civilization.placeUnitNearTile(Vector2.Zero, "Scout")!!
                    assertEquals(expected, unit.getUpgradePath().map { it.name })
                } finally {
                    for ((name, target) in originals)
                        fixture.ruleset.units.getValue(name).upgradesTo = target
                }
            }
        } finally {
            Gdx.input = previousInput
        }
    }

    @Test(timeout = 10000)
    fun selfUpgradeDoesNotIncludeCurrentUnit() {
        checkPath(mapOf("Scout" to "Scout"), emptyList())
    }

    @Test(timeout = 10000)
    fun cycleStopsBeforeRepeatedUnit() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Spearman", "Spearman" to "Archer"),
            listOf("Archer", "Spearman"))
    }

    @Test(timeout = 10000)
    fun ordinaryChainRemainsComplete() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Spearman", "Spearman" to null),
            listOf("Archer", "Spearman"))
    }

    @Test(timeout = 10000)
    fun cycleReturningToCurrentUnitExcludesIt() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Scout"), listOf("Archer"))
    }
}
