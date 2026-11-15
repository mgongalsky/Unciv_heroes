package com.unciv.ui.cityscreen

import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.StatTreeNode
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.pure.application.city.CityStatResource
import com.unciv.pure.application.city.CityStatSource
import com.unciv.pure.application.city.CityStatsSnapshot
import com.unciv.pure.application.city.CityStatValues

/** Граница legacy: копирует уже рассчитанную статистику, не обновляя и не изменяя город. */
internal object CityStatsSnapshotAdapter {
    fun read(city: CityInfo): CityStatsSnapshot {
        val stats = city.cityStats
        val happiness = stats.happinessList.toMap()
        val resources = CityStatResource.values().filter {
            it != CityStatResource.Faith || city.civInfo.gameInfo.isReligionEnabled()
        }

        fun values(source: Stats, happinessOverride: Float? = null): CityStatValues =
            CityStatValues(CityStatResource.values().associateWith { resource ->
                if (resource == CityStatResource.Happiness && happinessOverride != null)
                    happinessOverride
                else source[Stat.valueOf(resource.name)]
            })

        // Детализация счастья в дереве зданий может отличаться из-за городского округления.
        // Для него показываем только авторитетный агрегат happinessList, без ложной суммы детей.
        fun treeSource(name: String, node: StatTreeNode, topLevel: Boolean): CityStatSource =
            CityStatSource(
                name,
                values(node.totalStats, if (topLevel) happiness[name] ?: 0f else 0f),
                node.children.map { (childName, child) -> treeSource(childName, child, false) }
            )

        val base = stats.baseStatTree.children.map { (name, node) -> treeSource(name, node, true) }
            .toMutableList()
        for ((name, amount) in happiness) {
            if (name !in stats.baseStatTree.children)
                base.add(CityStatSource(name, CityStatValues(mapOf(CityStatResource.Happiness to amount))))
        }

        fun percentageSource(name: String, node: StatTreeNode): CityStatSource = CityStatSource(
            name,
            values(node.totalStats),
            node.children.map { (childName, child) -> percentageSource(childName, child) }
        )
        val percentages = stats.statPercentBonusTree.children.map { (name, node) ->
            percentageSource(name, node)
        }

        val finalSources = stats.finalStatList.map { (name, source) ->
            CityStatSource(name, values(source, happiness[name] ?: 0f))
        }.toMutableList()
        for ((name, amount) in happiness) {
            if (name !in stats.finalStatList)
                finalSources.add(CityStatSource(name, CityStatValues(mapOf(CityStatResource.Happiness to amount))))
        }

        return CityStatsSnapshot(
            population = city.population.population,
            foodStored = city.population.foodStored,
            civilizationGold = city.civInfo.gold,
            resources = resources,
            baseSources = base,
            percentageSources = percentages,
            finalSources = finalSources,
            totals = values(stats.currentCityStats, happiness.values.sum())
        )
    }
}
