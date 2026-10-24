package com.unciv.logic.battle.session

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.HexMath
import com.unciv.logic.map.ClimateParameters
import com.unciv.pure.application.battle.BattlefieldTerrain
import com.unciv.pure.application.battle.session.BattleFieldCell
import com.unciv.pure.application.battle.session.BattleFieldDescription
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import kotlin.random.Random

/** Creates presentation terrain and combat terrain from the same generated cells. */
object BattleFieldPreparation {
    data class Prepared(
        val description: BattleFieldDescription,
        val field: TacticalBattleField
    )

    fun create(
        attacker: ClimateParameters,
        defender: ClimateParameters,
        width: Int = 14,
        height: Int = 8,
        terrainSeed: Long = 42L,
        isTerrainImpassable: (String) -> Boolean
    ): Prepared {
        require(width > 1 && height > 0 && width.toLong() * height <= 4096L)
        val temperatureSeed = Random(terrainSeed).nextDouble()
        val cells = mutableListOf<BattleFieldCell>()
        for (x in -width / 2..(width - 1) / 2) {
            for (y in -height / 2..(height - 1) / 2) {
                val position = HexMath.evenQ2HexCoords(Vector2(x.toFloat(), y.toFloat()))
                val terrain = BattlefieldTerrain.at(position, width, attacker, defender, temperatureSeed)
                // Matches TileInfo and StandaloneBattleSetup: the last feature is effective terrain.
                val effectiveTerrain = terrain.features.lastOrNull() ?: terrain.baseTerrain
                cells.add(BattleFieldCell(
                    Point(position.x.toInt(), position.y.toInt()), terrain.baseTerrain,
                    terrain.features.toList(), terrain.temperature, terrain.humidity,
                    isTerrainImpassable(effectiveTerrain)
                ))
            }
        }
        val description = BattleFieldDescription(width, height, cells.toList())
        val field = TacticalBattleField(description.cells.map {
            TacticalBattleField.Cell(it.position, it.impassable)
        })
        return Prepared(description, field)
    }
}
