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
        val hero = cityScreen.visitingHero ?: return
        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 36f).coerceAtLeast(1f)
        if (royal) {
            background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
            pad(8f)
        }
        fun addInfo(text: String): Label {
            val label = supplyLabel(text)
            val cell = add(label)
            if (royal) {
                label.setWrap(true)
                cell.width(contentWidth)
            }
            cell.row()
            return label
        }
        foodToVisitingHeroLabel = addInfo(
            "City has ${city.population.foodStored}${Fonts.food}, hero consumes ${
                ceil(
                    hero.army.calculateFoodMaintenance(
                        isInCity = false
                    )
                ).toInt()
            }${Fonts.food}."
        )
        foodOfVisitingHeroLabel = addInfo(
            "Hero has ${hero.hero.currentFood.toInt()}${Fonts.food}, hero max ${hero.basicFoodCapacity.toInt()}${Fonts.food}."
        )
        val maintenance = hero.army.calculateFoodMaintenance(isInCity = false)
        val duration =
                if (maintenance > 0) (hero.hero.currentFood / maintenance).toInt() else Int.MAX_VALUE
        foodDurationLabel = addInfo(
            if (duration == Int.MAX_VALUE) "Hero food will last indefinitely."
            else "Hero food will last for $duration${Fonts.turn}."
        )
        addInfo("Hero has morale ${hero.morale}.")
        calculateFoodValues()
        createFoodExchangeInterface()
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
        val royal = cityScreen.game.settings.skin == "Minimal"
        leftCountLabel = if (royal) supplyLabel("City: ${currFoodCity.toInt()}")
        else Label("City: ${currFoodCity.toInt()}", BaseScreen.skin)
        rightCountLabel = if (royal) supplyLabel("${currFoodHero.toInt()} :Hero")
        else Label("${currFoodHero.toInt()} :Hero", BaseScreen.skin)
        foodSlider = Slider(0f, foodRange, 1f, false, BaseScreen.skin)
        foodSlider.value = minHero
        foodSlider.addListener { _ ->
            updateFoodDistribution()
            false
        }
        val manualSupplyButton = "Max".toTextButton()
        manualSupplyButton.onClick { supplyHeroMaximum() }
        val foodExchangeTable = Table()
        foodExchangeTable.defaults().pad(5f)
        if (royal) {
            val width = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(cityScreen.stage.width - 24f) - 36f).coerceAtLeast(1f)
            leftCountLabel.setWrap(true)
            rightCountLabel.setWrap(true)
            val countWidth = ((width - 20f) / 2f).coerceAtLeast(1f)
            foodExchangeTable.add(leftCountLabel).width(countWidth).fillX()
            foodExchangeTable.add(rightCountLabel).width(countWidth).fillX().row()
            foodExchangeTable.add(foodSlider).colspan(2).width((width - 10f).coerceAtLeast(1f))
                .fillX().row()
            manualSupplyButton.label.setWrap(true)
            foodExchangeTable.add(manualSupplyButton).colspan(2)
                .width((width - 10f).coerceAtLeast(1f)).minHeight(48f).fillX()
        } else {
            foodExchangeTable.add(leftCountLabel).padRight(10f)
            foodExchangeTable.add(foodSlider).growX().pad(5f)
            foodExchangeTable.add(rightCountLabel).padLeft(10f)
            foodExchangeTable.add(manualSupplyButton).padLeft(10f)
        }
        add(foodExchangeTable).growX().colspan(2).row()
    }

    private fun createAutoSupplyToggle() {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val autoSupplyToggle = "".toTextButton()
        fun refresh() {
            autoSupplyToggle.setText(if (city.autoFeedHero) "Auto-Supply: ON" else "Auto-Supply: OFF")
            if (royal) {
                autoSupplyToggle.style = BaseScreen.skin.get(
                    if (city.autoFeedHero) "positive" else "default",
                    TextButton.TextButtonStyle::class.java
                )
                autoSupplyToggle.color = Color.WHITE
            } else {
                autoSupplyToggle.color = if (city.autoFeedHero) Color.GREEN else Color.GRAY
            }
        }
        refresh()
        autoSupplyToggle.onClick {
            city.autoFeedHero = !city.autoFeedHero
            refresh()
        }
        val autoSupplyTable = Table()
        autoSupplyTable.defaults().pad(5f)
        val cell = autoSupplyTable.add(autoSupplyToggle)
        if (royal) {
            val width = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(cityScreen.stage.width - 24f) - 46f).coerceAtLeast(1f)
            autoSupplyToggle.label.setWrap(true)
            cell.width(width).minHeight(48f).fillX()
        }
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
        val expander = ExpanderTab(
            title = "{Hero Supply}",
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityStatsTable.HeroSupply",
            startsOutOpened = true,
            onChange = onChange
        ) {
            it.add(this)
            update()
        }
        if (cityScreen.game.settings.skin == "Minimal") {
            expander.setHeaderAppearance(
                BaseScreen.skinStrings.getUiBackground(
                    "General/ExpanderTab",
                    tintColor = Color.valueOf("D7C89F")
                ),
                Color.valueOf("453925"),
                prefWidth + 20f
            )
        }
        return expander
    }
    private fun supplyLabel(text: String): Label = text.toLabel(
        fontColor = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("453925") else Color.WHITE
    )
}
