package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.unciv.Constants
import com.unciv.logic.civilization.HeroAction
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.logic.map.MapUnit
import com.unciv.models.translations.tr
import com.unciv.pure.application.CalculateFoodDistributionUseCase
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.math.ceil
import kotlin.math.min
import com.unciv.pure.application.SupplyHeroMaximumUseCase
import com.unciv.pure.application.TransferFoodUseCase
import com.unciv.pure.domain.hero.FoodDistributionState

class HeroSupplyTable(val cityScreen: CityScreen) : Table(BaseScreen.skin) {
    val city = cityScreen.city

    private var currFoodCity = 0f
    private var currFoodHero = 0f
    private var maxFoodCity = 0f
    private var maxFoodHero = 0f
    private var freeFoodCity = 0f
    private var freeFoodHero = 0f
    private var minHero = 0f
    private var minCity = 0f
    private var foodRange = 0f

    // UI elements that need to be updated
    private lateinit var foodToVisitingHeroLabel: Label
    private lateinit var foodOfVisitingHeroLabel: Label
    private lateinit var foodDurationLabel: Label
    private lateinit var leftCountLabel: Label
    private lateinit var rightCountLabel: Label
    private lateinit var foodSlider: Slider
    private lateinit var turnsToPopLabel: Label

    fun update() {
        clear()

        if (cityScreen.visitingHero == null) return

        val hero = cityScreen.visitingHero

        // Display basic hero food information
        val heroFoodConsumption = "City has ${city.population.foodStored}${Fonts.food}, hero consumes ${ceil(hero.army.calculateFoodMaintenance(isInCity = false)).toInt()}${Fonts.food}."
        foodToVisitingHeroLabel = add(heroFoodConsumption.toLabel()).actor as Label
        row()

        val heroCurrentFood = "Hero has ${hero.hero.currentFood.toInt()}${Fonts.food}, hero max ${hero.basicFoodCapacity.toInt()}${Fonts.food}."
        foodOfVisitingHeroLabel = add(heroCurrentFood.toLabel()).actor as Label
        row()

        // Calculate and display food duration for hero
        val heroFoodMaintenance = hero.army.calculateFoodMaintenance(isInCity = false)
        val heroCurrentFoodValue = hero.hero.currentFood
        val foodDuration = if (heroFoodMaintenance > 0) {
            (heroCurrentFoodValue / heroFoodMaintenance).toInt()
        } else {
            Int.MAX_VALUE // Infinite if no consumption
        }

        val durationString = if (foodDuration == Int.MAX_VALUE) {
            "Hero food will last indefinitely."
        } else {
            "Hero food will last for $foodDuration${Fonts.turn}."
        }

        foodDurationLabel = add(durationString.toLabel()).actor as Label
        row()

        val moraleText = "Hero has morale ${hero.morale}."
        add(moraleText.toLabel()).row()

        // Calculate food capacities and current amounts
        calculateFoodValues()

        // Create food exchange slider interface
        createFoodExchangeInterface()

        // Create Auto-Supply Hero toggle
        createAutoSupplyToggle()

        pack()
    }

    private fun calculateFoodValues() {
        val hero = cityScreen.visitingHero!!

        // Calculate hero food capacity with bonuses
        maxFoodHero = hero.basicFoodCapacity
        val foodBonuses = city.getMatchingUniques(com.unciv.models.ruleset.unique.UniqueType.FoodCapacityBonus)
        var totalBonusPercent = 0f
        for (unique in foodBonuses) {
            totalBonusPercent += unique.params[0].toInt()
        }
        maxFoodHero *= (1f + totalBonusPercent / 100f)

        maxFoodCity = city.population.getFoodToNextPopulation().toFloat()
        currFoodHero = hero.hero.currentFood
        currFoodCity = city.population.foodStored.toFloat()

        freeFoodHero = maxFoodHero - currFoodHero
        if (freeFoodHero < 0f) {
            println("Hero has current food more than maximum!")
            freeFoodHero = 0f
        }
        freeFoodCity = maxFoodCity - currFoodCity

        minHero = min(currFoodHero, freeFoodCity)
        minCity = min(currFoodCity, freeFoodHero)

        foodRange = minHero + minCity
        if (foodRange < 0) {
            println("minHero $minHero, minCity $minCity")
            println("currFoodHero $currFoodHero, maxFoodHero $maxFoodHero, freeFoodHero $freeFoodHero")
            println("currFoodCity $currFoodCity, maxFoodCity $maxFoodCity, freeFoodCity $freeFoodCity")
            foodRange = 0f
        }

        foodState = FoodDistributionState(
            currFoodHero = cityScreen.visitingHero.hero.currentFood,
            currFoodCity = city.population.foodStored.toFloat(),
            maxFoodHero = maxFoodHero,
            maxFoodCity = maxFoodCity
        )
    }

    private fun createFoodExchangeInterface() {
        leftCountLabel = Label("City: " + currFoodCity.toInt().toString(), BaseScreen.skin)
        rightCountLabel = Label(currFoodHero.toInt().toString() + " :Hero", BaseScreen.skin)

        foodSlider = Slider(0f, foodRange, 1f, false, BaseScreen.skin)
        foodSlider.value = minHero // Initial position

        foodSlider.addListener { _ ->
            updateFoodDistribution()
            false
        }

        // Manual "Max" Supply Hero button
        val manualSupplyButton = "Max".toTextButton()
        manualSupplyButton.onClick {
            supplyHeroMaximum()
        }

        val foodExchangeTable = Table()
        foodExchangeTable.defaults().pad(5f)
        foodExchangeTable.add(leftCountLabel).padRight(10f)
        foodExchangeTable.add(foodSlider).growX().pad(5f)
        foodExchangeTable.add(rightCountLabel).padLeft(10f)
        foodExchangeTable.add(manualSupplyButton).padLeft(10f)
        add(foodExchangeTable).growX().colspan(2).row()
    }

    private fun createAutoSupplyToggle() {
        val autoSupplyToggleText = if (city.autoFeedHero) "Auto-Supply: ON" else "Auto-Supply: OFF"
        val autoSupplyToggleColor = if (city.autoFeedHero) Color.GREEN else Color.GRAY
        val autoSupplyToggle = autoSupplyToggleText.toTextButton()
        autoSupplyToggle.color = autoSupplyToggleColor
        autoSupplyToggle.onClick {
            city.autoFeedHero = !city.autoFeedHero
            val newText = if (city.autoFeedHero) "Auto-Supply: ON" else "Auto-Supply: OFF"
            val newColor = if (city.autoFeedHero) Color.GREEN else Color.GRAY
            autoSupplyToggle.setText(newText)
            autoSupplyToggle.color = newColor
        }

        val autoSupplyTable = Table()
        autoSupplyTable.defaults().pad(5f)
        autoSupplyTable.add(autoSupplyToggle)
        add(autoSupplyTable).growX().colspan(2).row()
    }

    private var foodState: FoodDistributionState? = null

    // updateFoodDistribution() — только UI
    private fun updateFoodDistribution() {

        val hero = cityScreen.visitingHero!!

        val state = foodState ?: return

        val result = CalculateFoodDistributionUseCase.execute(
            state = state,
            sliderValue = foodSlider.value,
            heroMaintenance = hero.army.calculateFoodMaintenance(isInCity = false)
        )

        foodState = result.state
        TransferFoodUseCase.execute(hero.hero, city,
            result.state.currFoodHero - state.currFoodHero)

        leftCountLabel.setText("City: ${result.state.currFoodCity.toInt()}")
        rightCountLabel.setText("${result.state.currFoodHero.toInt()} :Hero")
        foodDurationLabel.setText(
            if (result.foodDurationTurns == Int.MAX_VALUE)
                "Hero food will last indefinitely."
            else "Hero food will last for ${result.foodDurationTurns}${Fonts.turn}."
        )
    }

    private fun supplyHeroMaximum() {
        val hero = cityScreen.visitingHero!!

        val transferred = SupplyHeroMaximumUseCase.execute(hero.hero, city)
        if (transferred <= 0f) return

        foodSlider.value = minHero + transferred
        foodSlider.fire(ChangeListener.ChangeEvent())

        // TODO: уведомление не должно быть в UI — перенести в логику хода
        checkHeroFullySuppliedNotification(hero, transferred)
    }

    private fun checkHeroFullySuppliedNotification(hero: MapUnit, transferred: Float) {
        val previousFood = hero.hero.currentFood - transferred
        if (hero.hero.currentFood >= maxFoodHero && previousFood < maxFoodHero) {
            city.civInfo.addNotification(
                "Hero in [${city.name}] is fully supplied and ready!",
                HeroAction(city.location),
                hero.displayName(),
                NotificationIcon.Food
            )
        }
    }

    private fun updateTurnsToPopString(): String {
        val city = cityScreen.city
        var turnsToPopString =
            when {
                city.isStarving() -> "[${city.getNumTurnsToStarvation()}] turns to lose population"
                city.getRuleset().units[city.cityConstructions.currentConstructionFromQueue]
                    .let { it != null && it.hasUnique(com.unciv.models.ruleset.unique.UniqueType.ConvertFoodToProductionWhenConstructed) }
                -> "Food converts to production"
                city.isGrowing() -> "[${city.getNumTurnsToNewPopulation()}] turns to new population"
                else -> "Stopped population growth"
            }.tr()
        turnsToPopString += " (${city.population.foodStored}${Fonts.food}/${city.population.getFoodToNextPopulation()}${Fonts.food})"
        return turnsToPopString
    }

    fun setTurnsToPopLabel(label: Label) {
        turnsToPopLabel = label
    }

    fun asExpander(onChange: (() -> Unit)?): ExpanderTab {
        return ExpanderTab(
            title = "{Hero Supply}",
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityStatsTable.HeroSupply",
            startsOutOpened = true,
            onChange = onChange
        ) {
            it.add(this)
            update()
        }
    }
}
