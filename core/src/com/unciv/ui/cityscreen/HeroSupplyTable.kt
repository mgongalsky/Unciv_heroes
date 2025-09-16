package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.unciv.Constants
import com.unciv.logic.civilization.HeroAction
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.models.translations.tr
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.math.ceil
import kotlin.math.min

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
        
        val hero = cityScreen.visitingHero!!
        
        // Display basic hero food information
        val heroFoodConsumption = "City has ${city.population.foodStored}${Fonts.food}, hero consumes ${ceil(hero.army.calculateFoodMaintenance(isInCity = false)).toInt()}${Fonts.food}."
        foodToVisitingHeroLabel = add(heroFoodConsumption.toLabel()).actor as Label
        row()
        
        val heroCurrentFood = "Hero has ${hero.getCurrentFood().toInt()}${Fonts.food}, hero max ${hero.basicFoodCapacity.toInt()}${Fonts.food}."
        foodOfVisitingHeroLabel = add(heroCurrentFood.toLabel()).actor as Label
        row()
        
        // Calculate and display food duration for hero
        val heroFoodMaintenance = hero.army.calculateFoodMaintenance(isInCity = false)
        val heroCurrentFoodValue = hero.getCurrentFood()
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
        currFoodHero = hero.getCurrentFood()
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
    
    private fun updateFoodDistribution() {
        val hero = cityScreen.visitingHero!!
        
        // Update food distribution based on slider
        hero.setCurrentFood(foodSlider.value + currFoodHero - minHero)
        city.population.foodStored = (foodRange - foodSlider.value + currFoodCity - minCity).toInt()
        
        currFoodHero = foodSlider.value + currFoodHero - minHero
        currFoodCity = foodRange - foodSlider.value + currFoodCity - minCity
        
        freeFoodHero = maxFoodHero - currFoodHero
        freeFoodCity = maxFoodCity - currFoodCity
        
        minHero = min(currFoodHero, freeFoodCity)
        minCity = min(currFoodCity, freeFoodHero)
        
        // Update labels
        leftCountLabel.setText("City: " + currFoodCity.toInt().toString())
        rightCountLabel.setText(currFoodHero.toInt().toString() + " :Hero")
        
        // Update hero info labels
        val heroFoodConsumption = "City has ${city.population.foodStored}${Fonts.food}, hero consumes ${ceil(hero.army.calculateFoodMaintenance(isInCity = false)).toInt()}${Fonts.food}."
        foodToVisitingHeroLabel.setText(heroFoodConsumption)
        
        val heroCurrentFood = "Hero has ${hero.getCurrentFood().toInt()}${Fonts.food}, hero max ${hero.basicFoodCapacity.toInt()}${Fonts.food}."
        foodOfVisitingHeroLabel.setText(heroCurrentFood)
        
        // Update food duration info
        val heroFoodMaintenance = hero.army.calculateFoodMaintenance(isInCity = false)
        val updatedFoodDuration = if (heroFoodMaintenance > 0) {
            (hero.getCurrentFood() / heroFoodMaintenance).toInt()
        } else {
            Int.MAX_VALUE
        }
        
        val updatedDurationString = if (updatedFoodDuration == Int.MAX_VALUE) {
            "Hero food will last indefinitely."
        } else {
            "Hero food will last for $updatedFoodDuration${Fonts.turn}."
        }
        
        foodDurationLabel.setText(updatedDurationString)
        
        // Update city population info if turnsToPopLabel exists
        if (::turnsToPopLabel.isInitialized) {
            turnsToPopLabel.setText(updateTurnsToPopString())
        }
    }
    
    private fun supplyHeroMaximum() {
        // Calculate how much food can be transferred
        val heroSpaceLeft = maxFoodHero - currFoodHero
        val cityFoodAvailable = currFoodCity
        val foodToTransfer = min(heroSpaceLeft, cityFoodAvailable)
        
        if (foodToTransfer > 0) {
            // Transfer maximum possible food to hero
            val hero = cityScreen.visitingHero!!
            val previousFood = currFoodHero
            hero.setCurrentFood(currFoodHero + foodToTransfer)
            city.population.foodStored = (currFoodCity - foodToTransfer).toInt()
            
            // Update the slider position to reflect the transfer
            val newSliderValue = minHero + foodToTransfer
            foodSlider.value = newSliderValue
            
            // Trigger slider's update logic to refresh all labels
            foodSlider.fire(ChangeListener.ChangeEvent())
            
            // Check if hero reached maximum food capacity
            val newHeroFood = hero.getCurrentFood()
            if (newHeroFood >= maxFoodHero && previousFood < maxFoodHero) {
                // Hero just reached maximum food - send notification
                city.civInfo.addNotification(
                    "Hero in [${city.name}] is fully supplied and ready!",
                    HeroAction(city.location),
                    hero.displayName(),
                    NotificationIcon.Food
                )
            }
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