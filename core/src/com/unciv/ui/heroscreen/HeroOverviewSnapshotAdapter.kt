package com.unciv.ui.heroscreen

import com.unciv.logic.map.MapUnit
import com.unciv.pure.application.hero.HeroOverviewSnapshot

/** Общая граница чтения legacy-модели для города и отдельного обзора героя. */
object HeroOverviewSnapshotAdapter {
    /** Чтение не меняет героя; расход планируется вне города, как в прежнем обзоре. */
    fun read(hero: MapUnit, supplyEnabled: Boolean): HeroOverviewSnapshot {
        val food = hero.hero.currentFood
        val maintenance = hero.army.calculateFoodMaintenance(false)
        return HeroOverviewSnapshot(
            name = hero.displayName(),
            attack = hero.heroAttackSkill,
            defense = hero.heroDefenseSkill,
            morale = hero.morale,
            luck = hero.luck,
            supplyEnabled = supplyEnabled,
            food = food,
            overviewCapacity = hero.basicFoodCapacity,
            // Ручная передача использует другую историческую вместимость; не подменяем её.
            manualCapacity = hero.hero.baseFoodCapacity,
            outsideMaintenance = maintenance,
            foodDuration = if (maintenance > 0f) (food / maintenance).toInt() else Int.MAX_VALUE,
            settlementPopulation = hero.calculateArmyPopulation() + 1
        )
    }
}
