package com.unciv.ui.cityscreen

import com.unciv.logic.map.MapUnit
import com.unciv.logic.civilization.HeroAction
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.application.TransferFoodUseCase
import com.unciv.pure.application.SupplyHeroMaximumUseCase
import com.unciv.pure.application.city.CityHeroSupplySnapshot
import com.unciv.pure.domain.supply.SupplyMechanic
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Граница legacy-модели: чтение не меняет игру, команды используют существующие операции снабжения. */
internal class CityHeroSupplyAdapter(private val screen: CityScreen) : KoinComponent {
    private val mechanic: SupplyMechanic by inject()
    private var revision = 0L
    private var displayedHero: MapUnit? = null
    private var displayed: CityHeroSupplySnapshot? = null

    /** Ручная и автоматическая вместимость отражают ограничения соответствующих игровых путей. */
    fun read(): CityHeroSupplySnapshot {
        val city = screen.city
        val hero = city.getVisitingHero()
        displayedHero = hero
        val enabled = mechanic.isEnabled
        val food = hero?.hero?.currentFood ?: 0f
        val manual = hero?.hero?.baseFoodCapacity ?: 0f
        val bonus = city.getMatchingUniques(UniqueType.FoodCapacityBonus)
            .sumOf { it.params[0].toInt() }
        val automatic = (hero?.basicFoodCapacity ?: 0f) * (1f + bonus / 100f)
        val citySpace = (city.population.getFoodToNextPopulation() - city.population.foodStored)
            .toFloat().coerceAtLeast(0f)
        return CityHeroSupplySnapshot(
            revision = ++revision,
            enabled = enabled,
            hasHero = hero != null,
            canChange = enabled && hero != null && screen.context.canChangeState() &&
                screen.canCityBeChanged() && screen.isPlayersTurn,
            cityFood = city.population.foodStored,
            heroFood = food,
            manualCapacity = manual,
            automaticCapacity = automatic,
            maintenance = hero?.army?.calculateFoodMaintenance(isInCity = false) ?: 0f,
            morale = hero?.morale ?: 0,
            autoSupply = city.autoFeedHero,
            minimumTransfer = -minOf(food.coerceAtLeast(0f), citySpace),
            maximumTransfer = minOf((manual - food).coerceAtLeast(0f),
                city.population.foodStored.toFloat().coerceAtLeast(0f))
        ).also { displayed = it }
    }

    /** Устаревшая панель не может передать еду другому герою или применить прежние границы запасов. */
    private fun allowed(expectedRevision: Long): MapUnit? {
        val snapshot = displayed ?: return null
        val city = screen.city
        val hero = city.getVisitingHero() ?: return null
        if (snapshot.revision != expectedRevision || hero !== displayedHero ||
            !mechanic.isEnabled || !screen.context.canChangeState() ||
            !screen.canCityBeChanged() || !screen.isPlayersTurn ||
            hero.hero.currentFood != snapshot.heroFood ||
            city.population.foodStored != snapshot.cityFood) return null
        return hero
    }

    /** Передаёт выбранное количество один раз; округление остаётся в действующем игровом методе. */
    fun transfer(expectedRevision: Long, amount: Float): CityHeroSupplySnapshot {
        val hero = allowed(expectedRevision)
        val snapshot = displayed
        if (hero != null && snapshot != null && amount.isFinite()) {
            val citySpace = (screen.city.population.getFoodToNextPopulation() -
                screen.city.population.foodStored).toFloat().coerceAtLeast(0f)
            val minimum = -minOf(hero.hero.currentFood.coerceAtLeast(0f), citySpace)
            val maximum = minOf((hero.hero.baseFoodCapacity - hero.hero.currentFood)
                .coerceAtLeast(0f), screen.city.population.foodStored.toFloat().coerceAtLeast(0f))
            TransferFoodUseCase.execute(hero.hero, screen.city, amount.coerceIn(minimum, maximum))
        }
        return read()
    }

    /** Max не генерирует событий ползунка и не возвращает еду при уже превышенной вместимости. */
    fun supplyMaximum(expectedRevision: Long): CityHeroSupplySnapshot {
        val hero = allowed(expectedRevision)
        if (hero != null && hero.hero.currentFood < hero.hero.baseFoodCapacity &&
            screen.city.population.foodStored > 0) {
            val previousFood = hero.hero.currentFood
            val transferred = SupplyHeroMaximumUseCase.execute(hero.hero, screen.city)
            // Сохраняем прежний порог уведомления, который учитывает городские бонусы.
            val capacity = (hero.basicFoodCapacity * (1f + screen.city
                .getMatchingUniques(UniqueType.FoodCapacityBonus)
                .sumOf { it.params[0].toInt() } / 100f))
            if (transferred > 0f && previousFood < capacity && hero.hero.currentFood >= capacity) {
                screen.city.civInfo.addNotification(
                    "Hero in [${screen.city.name}] is fully supplied and ready!",
                    HeroAction(screen.city.location), hero.displayName(), NotificationIcon.Food
                )
            }
        }
        return read()
    }

    /** Меняет сохранённый флаг; само автоматическое пополнение остаётся в игровом ходе. */
    fun toggleAutomatic(expectedRevision: Long): CityHeroSupplySnapshot {
        if (allowed(expectedRevision) != null) screen.city.autoFeedHero = !screen.city.autoFeedHero
        return read()
    }
}
