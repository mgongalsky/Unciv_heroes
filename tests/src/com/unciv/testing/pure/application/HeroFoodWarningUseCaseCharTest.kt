package com.unciv.testing.pure.application

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.HeroFoodWarningUseCase
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeroFoodWarningUseCaseCharTest {

    private lateinit var fakeCiv: FakeCivilizationInfo

    @Before
    fun setUp() {
        fakeCiv = FakeCivilizationInfo()
    }

    private fun execute(currentFood: Float, dailyConsumption: Float) {
        HeroFoodWarningUseCase.execute(
            currentFood = currentFood,
            dailyConsumption = dailyConsumption,
            unitDisplayName = "Hero",
            unitName = "Warrior",
            civInfo = fakeCiv,
            position = Vector2(0f, 0f)
        )
    }

    @Test
    fun `food for 1 turn sends warning`() {
        execute(currentFood = 2f, dailyConsumption = 2f)
        assertEquals(listOf("[Hero] has food for only 1 turn!"), fakeCiv.capturedNotifications)
    }

    @Test
    fun `no food sends warning`() {
        execute(currentFood = 0f, dailyConsumption = 2f)
        assertEquals(listOf("[Hero] has no food left!"), fakeCiv.capturedNotifications)
    }

    @Test
    fun `enough food sends no warning`() {
        execute(currentFood = 20f, dailyConsumption = 2f)
        assertTrue(fakeCiv.capturedNotifications.isEmpty())
    }

    @Test
    fun `zero consumption sends no warning`() {
        execute(currentFood = 5f, dailyConsumption = 0f)
        assertTrue(fakeCiv.capturedNotifications.isEmpty())
    }

    @Test
    fun `exactly 3 turns remaining sends warning`() {
        execute(currentFood = 6f, dailyConsumption = 2f)
        assertEquals(listOf("[Hero] has food for only 3 turns!"), fakeCiv.capturedNotifications)
    }
}
