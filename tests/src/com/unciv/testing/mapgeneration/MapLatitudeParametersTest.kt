package com.unciv.testing.mapgeneration

import com.badlogic.gdx.utils.Json
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.pure.domain.mapgeneration.LatitudeRange
import org.junit.Assert.*
import org.junit.Test

/** Контракты сериализуемых параметров широт; доменная арифметика проверяется отдельно. */
class MapLatitudeParametersTest {
    /** Отсутствующие поля старой карты не включают новый климат. */
    @Test
    fun legacyJsonKeepsHistoricalMode() {
        val parameters = Json().fromJson(MapParameters::class.java, """{"seed":42}""")
        assertFalse(parameters.useLatitudeRange)
        assertNull(parameters.validatedLatitudeRange())
        assertEquals(42L, parameters.seed)
    }

    /** JSON сохраняет включение диапазона, южные и дробные широты. */
    @Test
    fun rangeSurvivesJsonRoundTrip() {
        val source = MapParameters().apply {
            useLatitudeRange = true
            topLatitude = -20.5
            bottomLatitude = -65.25
        }
        val json = Json()
        val restored = json.fromJson(MapParameters::class.java, json.toJson(source))
        assertEquals(LatitudeRange(-20.5, -65.25), restored.validatedLatitudeRange())
    }

    /** Копия сохраняет диапазон, а её редактирование и сброс не затрагивают исходник. */
    @Test
    fun cloneAndResetKeepIndependentSettings() {
        val source = MapParameters().apply {
            useLatitudeRange = true
            topLatitude = 40.0
            bottomLatitude = -10.0
        }
        val copy = source.clone()
        assertEquals(source.validatedLatitudeRange(), copy.validatedLatitudeRange())
        copy.topLatitude = 80.0
        assertEquals(40.0, source.topLatitude, 0.0)
        copy.resetAdvancedSettings()
        assertNull(copy.validatedLatitudeRange())
        assertEquals(60.0, copy.topLatitude, 0.0)
        assertEquals(20.0, copy.bottomLatitude, 0.0)
        assertEquals(LatitudeRange(40.0, -10.0), source.validatedLatitudeRange())
    }

    /** Ошибочные активные границы и Flat Earth отклоняются без исправления исходных параметров. */
    @Test
    fun invalidActiveRangeIsRejectedWithoutMutation() {
        for ((top, bottom) in listOf(100.0 to 20.0, 10.0 to 20.0, Double.NaN to 0.0)) {
            val parameters = MapParameters().apply {
                useLatitudeRange = true
                topLatitude = top
                bottomLatitude = bottom
            }
            rejects { parameters.validatedLatitudeRange() }
            assertEquals(top.toBits(), parameters.topLatitude.toBits())
            assertEquals(bottom.toBits(), parameters.bottomLatitude.toBits())
        }
        val flat = MapParameters().apply { shape = MapShape.flatEarth; useLatitudeRange = true }
        rejects { flat.validatedLatitudeRange() }
        flat.useLatitudeRange = false
        assertNull(flat.validatedLatitudeRange())
    }

    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Invalid latitude settings accepted")
        } catch (_: IllegalArgumentException) { }
    }

    /** Partial переносит нужные настройки, не подменяя форму и размеры действующей карты. */
    @Test
    fun partialStepsCopyLatitudeWithoutGeometry() {
        val requested = MapParameters().apply {
            shape = MapShape.rectangular
            useLatitudeRange = true
            topLatitude = 20.0
            bottomLatitude = -20.0
            temperatureShift = 0.2f
            tilesPerBiomeArea = 12
        }
        for (step in listOf(
            com.unciv.ui.mapeditor.MapGeneratorSteps.HumidityAndTemperature,
            com.unciv.ui.mapeditor.MapGeneratorSteps.Ice,
            com.unciv.ui.mapeditor.MapGeneratorSteps.NaturalWonders
        )) {
            val actual = MapParameters()
            val size = actual.mapSize
            step.copyParameters!!.invoke(requested, actual)
            assertEquals(LatitudeRange(20.0, -20.0), actual.validatedLatitudeRange())
            assertEquals(MapShape.hexagonal, actual.shape)
            assertSame(size, actual.mapSize)
            if (step == com.unciv.ui.mapeditor.MapGeneratorSteps.NaturalWonders) {
                assertEquals(0f, actual.temperatureShift, 0f)
                assertEquals(6, actual.tilesPerBiomeArea)
            } else {
                assertEquals(0.2f, actual.temperatureShift, 0f)
                assertEquals(12, actual.tilesPerBiomeArea)
            }
        }
        val vegetation = MapParameters()
        com.unciv.ui.mapeditor.MapGeneratorSteps.Vegetation.copyParameters!!.invoke(
            requested,
            vegetation
        )
        assertNull(vegetation.validatedLatitudeRange())
        val flat = MapParameters().apply { shape = MapShape.flatEarth }
        com.unciv.ui.mapeditor.MapGeneratorSteps.HumidityAndTemperature.copyParameters!!.invoke(
            requested,
            flat
        )
        rejects { flat.validatedLatitudeRange() }
    }
}
