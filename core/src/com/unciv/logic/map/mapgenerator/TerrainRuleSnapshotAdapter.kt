package com.unciv.logic.map.mapgenerator

import com.unciv.Constants
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.application.mapgeneration.TerrainRuleSnapshot
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule

/** Преобразует реальные правила в переносимые значения; не загружает файлы и не создаёт игровую партию. */
object TerrainRuleSnapshotAdapter {
    fun snapshot(ruleset: Ruleset): TerrainRuleSnapshot {
        val terrains = ruleset.terrains.values
        val climate = terrains.filter { it.type == TerrainType.Land }.flatMap { terrain ->
            terrain.getMatchingUniques(UniqueType.TileGenerationConditions).map { unique ->
                ClimateTerrainRange.inclusiveMinimums(terrain.name,
                    unique.params[0].toFloat(), unique.params[1].toFloat(),
                    unique.params[2].toFloat(), unique.params[3].toFloat())
            }.toList()
        }
        val mountains = terrains.filter { it.hasUnique(UniqueType.OccursInChains) }.map { it.name }
        // Сохраняем фактический порядок legacy HashSet в снимке: после экспорта он уже не зависит от identity hash.
        val vegetation = (Constants.vegetation.mapNotNull { ruleset.terrains[it] }
            + terrains.filter { it.hasUnique(UniqueType.Vegetation) }).toHashSet().map {
            VegetationRule(it.name, it.occursOn.toList())
        }
        return TerrainRuleSnapshot(
            ruleset.terrains.keys.toSet(),
            terrains.filter { it.type == TerrainType.Water }.mapTo(mutableSetOf()) { it.name },
            terrains.filter { it.impassable }.mapTo(mutableSetOf()) { it.name },
            terrains.first { it.type == TerrainType.Land }.name,
            terrains.firstOrNull { !it.impassable && it.type == TerrainType.Land && !it.hasUnique(UniqueType.RoughTerrain) }?.name,
            mountains.firstOrNull(),
            terrains.firstOrNull { it.hasUnique(UniqueType.OccursInGroups) }?.name,
            if (mountains.isEmpty()) setOf(Constants.mountain) else mountains.toSet(),
            climate, vegetation, Constants.rising, Constants.lowering
        )
    }
}
