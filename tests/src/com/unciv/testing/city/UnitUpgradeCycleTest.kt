package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Test

/** Защищает построение пути улучшений от зацикливания на ошибочных связях в правилах. */
class UnitUpgradeCycleTest {
    /** Временно задаёт граф улучшений, проверяет путь реального юнита и восстанавливает исходные определения. */
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

    /** Ссылка на собственный тип даёт пустой путь; таймаут выявляет бесконечный обход. */
    @Test(timeout = 10000)
    fun selfUpgradeDoesNotIncludeCurrentUnit() {
        checkPath(mapOf("Scout" to "Scout"), emptyList())
    }

    /** Цикл между последующими типами обрывается до повторного добавления уже посещённого юнита. */
    @Test(timeout = 10000)
    fun cycleStopsBeforeRepeatedUnit() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Spearman", "Spearman" to "Archer"),
            listOf("Archer", "Spearman"))
    }

    /** Защита от циклов не сокращает корректную цепочку, оканчивающуюся отсутствием улучшения. */
    @Test(timeout = 10000)
    fun ordinaryChainRemainsComplete() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Spearman", "Spearman" to null),
            listOf("Archer", "Spearman"))
    }

    /** Возврат к исходному типу завершает обход, не добавляя сам исходный юнит в путь улучшений. */
    @Test(timeout = 10000)
    fun cycleReturningToCurrentUnitExcludesIt() {
        checkPath(mapOf("Scout" to "Archer", "Archer" to "Scout"), listOf("Archer"))
    }
}
