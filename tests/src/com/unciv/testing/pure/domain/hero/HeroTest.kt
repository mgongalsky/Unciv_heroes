package com.unciv.testing.pure.domain.hero

import com.unciv.pure.domain.hero.Hero
import org.junit.Assert.*
import org.junit.Test

class HeroTest {

    private fun makeHero(
        currentFood: Float = 10f,
        baseFoodCapacity: Float = 15f,
        baseAttackSkill: Int = 5,
        baseDefenseSkill: Int = 5,
        morale: Int = 3,
        luck: Int = 3
    ) = Hero(
        baseAttackSkill = baseAttackSkill,
        baseDefenseSkill = baseDefenseSkill,
        baseFoodCapacity = baseFoodCapacity,
        currentFood = currentFood,
        morale = morale,
        luck = luck
    )

    // --- addFood ---

    @Test
    fun `addFood increases currentFood`() {
        val hero = makeHero(currentFood = 5f)
        hero.addFood(3f)
        assertEquals(8f, hero.currentFood, 0.001f)
    }

    @Test
    fun `addFood with zero does not change currentFood`() {
        val hero = makeHero(currentFood = 5f)
        hero.addFood(0f)
        assertEquals(5f, hero.currentFood, 0.001f)
    }

    @Test
    fun `addFood with negative value decreases currentFood`() {
        val hero = makeHero(currentFood = 5f)
        hero.addFood(-2f)
        assertEquals(3f, hero.currentFood, 0.001f)
    }

    // --- setFood ---

    @Test
    fun `setFood sets exact value`() {
        val hero = makeHero(currentFood = 10f)
        hero.setFood(4f)
        assertEquals(4f, hero.currentFood, 0.001f)
    }

    @Test
    fun `setFood to zero empties food`() {
        val hero = makeHero(currentFood = 10f)
        hero.setFood(0f)
        assertEquals(0f, hero.currentFood, 0.001f)
    }

    // --- baseFoodCapacity ---

    @Test
    fun `baseFoodCapacity is set correctly`() {
        val hero = makeHero(baseFoodCapacity = 20f)
        assertEquals(20f, hero.baseFoodCapacity, 0.001f)
    }

    // --- baseStats ---

    @Test
    fun `baseAttackSkill and baseDefenseSkill are set correctly`() {
        val hero = makeHero(baseAttackSkill = 7, baseDefenseSkill = 4)
        assertEquals(7, hero.baseAttackSkill)
        assertEquals(4, hero.baseDefenseSkill)
    }
}
