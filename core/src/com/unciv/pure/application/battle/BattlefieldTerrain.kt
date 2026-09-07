package com.unciv.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.logic.map.ClimateParameters
import com.unciv.logic.map.Perlin

/** Shared battlefield terrain generation for graphical and unattended battles. */
object BattlefieldTerrain {
    data class TileTerrain(
        val baseTerrain: String,
        val features: List<String>,
        val temperature: Double,
        val humidity: Double
    )

    fun at(
        position: Vector2,
        width: Int,
        attacker: ClimateParameters,
        defender: ClimateParameters,
        temperatureSeed: Double
    ): TileTerrain {
        require(width > 1)
        val t = (HexMath.hex2EvenQCoords(position).x + width / 2) / (width - 1).toFloat()
        val noiseTemperature = Perlin.noise3d(
            position.x.toDouble(), position.y.toDouble(), temperatureSeed,
            nOctaves = 3, persistence = 0.5, lacunarity = 2.0, scale = 2.0
        )
        val elevation = attacker.averageElevation * (1 - t) + defender.averageElevation * t
        val temperature =
            attacker.averageTemperature * (1 - t) + defender.averageTemperature * t + noiseTemperature
        val humidity = attacker.averageHumidity * (1 - t) + defender.averageHumidity * t
        val features = mutableListOf<String>()
        val terrain = if (elevation >= 0.8) {
            "Mountain"
        } else {
            if (elevation >= 0.5) features.add("Hill")
            when {
                temperature < -0.4 -> if (humidity < 0.5) "Snow" else "Tundra"
                temperature < 0.8 -> if (humidity < 0.5) "Plains" else "Grassland"
                temperature <= 1.0 -> if (humidity < 0.7) "Desert" else "Plains"
                else -> "Plains"
            }
        }
        return TileTerrain(terrain, features.toList(), temperature, humidity)
    }
}
