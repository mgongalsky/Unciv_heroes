package com.unciv.ui.mapeditor

import com.unciv.logic.map.MapParameters

private object MapGeneratorStepsHelpers {
    val applyLandmass = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.type = newParameters.type
        actualParameters.mountainZoneCount = newParameters.mountainZoneCount
        actualParameters.waterThreshold = newParameters.waterThreshold
        actualParameters.seed = newParameters.seed
    }
    val applyElevation = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.elevationExponent = newParameters.elevationExponent
        actualParameters.elevationScale = newParameters.elevationScale
    }
    /** Мировой диапазон переносится без формы и размеров текущей карты. */
    val applyLatitude = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.useLatitudeRange = newParameters.useLatitudeRange
        actualParameters.topLatitude = newParameters.topLatitude
        actualParameters.bottomLatitude = newParameters.bottomLatitude
    }
    val applyHumidityAndTemperature = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.temperatureExtremeness = newParameters.temperatureExtremeness
        actualParameters.temperatureShift = newParameters.temperatureShift
        actualParameters.tilesPerBiomeArea = newParameters.tilesPerBiomeArea
        applyLatitude(newParameters, actualParameters)
    }
    val applyLakesAndCoast = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.maxCoastExtension = newParameters.maxCoastExtension
    }
    val applyVegetation = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.vegetationRichness = newParameters.vegetationRichness
        actualParameters.vegetationScale = newParameters.vegetationScale
    }
    val applyRareFeatures = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.rareFeaturesRichness = newParameters.rareFeaturesRichness
    }
    val applyResources = fun(newParameters: MapParameters, actualParameters: MapParameters) {
        actualParameters.resourceRichness = newParameters.resourceRichness
    }
}

enum class MapGeneratorSteps(
    val label: String,
    val copyParameters: ((MapParameters, MapParameters) -> Unit)? = null
) {
    None(""),
    All("All"),
    Landmass("Generate landmass", MapGeneratorStepsHelpers.applyLandmass),
    Elevation("Raise mountains and hills", MapGeneratorStepsHelpers.applyElevation),
    HumidityAndTemperature("Humidity and temperature", MapGeneratorStepsHelpers.applyHumidityAndTemperature),
    LakesAndCoast("Lakes and coastline", MapGeneratorStepsHelpers.applyLakesAndCoast),
    Vegetation("Sprout vegetation", MapGeneratorStepsHelpers.applyVegetation),
    RareFeatures("Spawn rare features", MapGeneratorStepsHelpers.applyRareFeatures),
    // Лёд читает те же настройки температуры, но сохраняет собственную формулу смешивания шума.
    Ice("Distribute ice", MapGeneratorStepsHelpers.applyHumidityAndTemperature),
    Continents("Assign continent IDs"),
    NaturalWonders("Place Natural Wonders", MapGeneratorStepsHelpers.applyLatitude),
    Rivers("Let the rivers flow"),
    Resources("Spread Resources", MapGeneratorStepsHelpers.applyResources),
    AncientRuins("Create ancient ruins"),
}
