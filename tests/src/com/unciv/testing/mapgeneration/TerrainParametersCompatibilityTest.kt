package com.unciv.testing.mapgeneration

import com.badlogic.gdx.utils.Json
import com.unciv.logic.map.MapParameters
import com.unciv.ui.mapeditor.MapGeneratorSteps
import org.junit.Assert.*
import org.junit.Test

/** Совместимость сериализуемых настроек и передачи параметров редактора, отдельно от pure-тестов. */
class TerrainParametersCompatibilityTest {
    /** Старые JSON без масштабов получают исторические значения без миграции карты. */
    @Test
    fun oldParametersUseHistoricalScales() {
        val parameters = Json().fromJson(MapParameters::class.java, """{"seed":42,"tilesPerBiomeArea":9}""")
        assertEquals(42L, parameters.seed)
        assertEquals(9, parameters.tilesPerBiomeArea)
        assertEquals(3.0, parameters.vegetationScale, 0.0)
        assertEquals(2.0, parameters.elevationScale, 0.0)
    }

    /** Сериализация сохраняет дробные масштабы и явный нулевой seed. */
    @Test
    fun newParametersRoundTrip() {
        val parameters = MapParameters().apply {
            seed = 0L
            vegetationScale = 7.5
            elevationScale = 12.25
            tilesPerBiomeArea = 13
        }
        val json = Json()
        val restored = json.fromJson(MapParameters::class.java, json.toJson(parameters))
        assertEquals(0L, restored.seed)
        assertEquals(7.5, restored.vegetationScale, 0.0)
        assertEquals(12.25, restored.elevationScale, 0.0)
        assertEquals(13, restored.tilesPerBiomeArea)
    }

    /** Копия настроек сохраняет масштабы, но не разделяет изменяемые размеры и моды. */
    @Test
    fun clonedParametersAreIndependent() {
        val parameters = MapParameters().apply { vegetationScale = 8.0; elevationScale = 9.0 }
        val copy = parameters.clone()
        copy.mapSize.width += 1
        copy.mods.add("Test")
        copy.vegetationScale = 11.0
        assertEquals(8.0, parameters.vegetationScale, 0.0)
        assertEquals(9.0, copy.elevationScale, 0.0)
        assertFalse("Test" in parameters.mods)
        assertNotEquals(parameters.mapSize.width, copy.mapSize.width)
    }

    /** Сброс возвращает три исторических масштаба независимо от предыдущих настроек. */
    @Test
    fun resetRestoresHistoricalScales() {
        val parameters = MapParameters().apply {
            tilesPerBiomeArea = 12; vegetationScale = 15.0; elevationScale = 20.0
        }
        parameters.resetAdvancedSettings()
        assertEquals(6, parameters.tilesPerBiomeArea)
        assertEquals(3.0, parameters.vegetationScale, 0.0)
        assertEquals(2.0, parameters.elevationScale, 0.0)
    }

    /** Каждый частичный этап копирует свой масштаб и не меняет масштабы остальных слоёв. */
    @Test
    fun partialStepsCopyOnlyTheirLayerScales() {
        val requested = MapParameters().apply {
            tilesPerBiomeArea = 12; vegetationScale = 15.0; elevationScale = 20.0
        }
        val climate = MapParameters()
        MapGeneratorSteps.HumidityAndTemperature.copyParameters!!.invoke(requested, climate)
        assertEquals(12, climate.tilesPerBiomeArea)
        assertEquals(3.0, climate.vegetationScale, 0.0)
        assertEquals(2.0, climate.elevationScale, 0.0)
        val vegetation = MapParameters()
        MapGeneratorSteps.Vegetation.copyParameters!!.invoke(requested, vegetation)
        assertEquals(15.0, vegetation.vegetationScale, 0.0)
        assertEquals(6, vegetation.tilesPerBiomeArea)
        assertEquals(2.0, vegetation.elevationScale, 0.0)
        val elevation = MapParameters()
        MapGeneratorSteps.Elevation.copyParameters!!.invoke(requested, elevation)
        assertEquals(20.0, elevation.elevationScale, 0.0)
        assertEquals(6, elevation.tilesPerBiomeArea)
        assertEquals(3.0, elevation.vegetationScale, 0.0)
    }
}
