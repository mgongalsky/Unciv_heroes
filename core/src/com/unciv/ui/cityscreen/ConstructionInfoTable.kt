package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.IConstruction
import com.unciv.logic.city.PerpetualConstruction
import com.unciv.logic.city.PerpetualStatConversion
import com.unciv.models.UncivSound
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.CityEvent
import com.unciv.models.ruleset.IRulesetObject
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.translations.tr
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.popup.closeAllPopups
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toTextButton

class ConstructionInfoTable(val cityScreen: CityScreen): Table() {
    private val selectedConstructionTable = Table()

    init {
        selectedConstructionTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/ConstructionInfoTable/SelectedConstructionTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.5f)
        )
        add(selectedConstructionTable).pad(2f).fill()
        background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/ConstructionInfoTable/Background",
            tintColor = Color.WHITE
        )
    }

    fun update(selectedConstruction: IConstruction?) {
        selectedConstructionTable.clear()
        if (selectedConstruction == null) {
            isVisible = false
            return
        }
        isVisible = true
        updateSelectedConstructionTable(selectedConstruction)

        if (UncivGame.Current.settings.skin == "Minimal") {
            val scroll = constructionScroll
            selectedConstructionTable.pack()
            val scrollStyle = scroll.style
            val barWidth = maxOf(
                scrollStyle.vScroll?.minWidth ?: 0f,
                scrollStyle.vScrollKnob?.minWidth ?: 0f
            )
            val scrollBackground = scrollStyle.background
            val horizontalInsets = (scrollBackground?.leftWidth ?: 0f) +
                    (scrollBackground?.rightWidth ?: 0f)
            val verticalInsets = (scrollBackground?.topHeight ?: 0f) +
                    (scrollBackground?.bottomHeight ?: 0f)
            val availableHeight = ((cityScreen.stage.height - 10f) / 2f).coerceAtLeast(1f)
            clearChildren()
            add(scroll).width(selectedConstructionTable.prefWidth + barWidth + horizontalInsets)
                .height(
                    minOf(
                        selectedConstructionTable.prefHeight + verticalInsets,
                        availableHeight
                    )
                )
            pack()
            scroll.validate()
            scroll.scrollY = 0f
            scroll.updateVisualScroll()
        } else {
            pack()
        }
    }

    private fun updateSelectedConstructionTable(construction: IConstruction) {
        val city = cityScreen.city
        val cityConstructions = city.cityConstructions
        val royal = UncivGame.Current.settings.skin == "Minimal"
        val contentWidth = if (royal) {
            val panelWidth = (cityScreen.stage.width / 4f).coerceAtLeast(260f)
                .coerceAtMost(cityScreen.stage.width - 10f)
            (panelWidth - 108f).coerceAtLeast(1f)
        } else cityScreen.stage.width / 4f

        fun textLabel(text: String): Label = Label(text, BaseScreen.skin).apply {
            if (royal) {
                style = Label.LabelStyle(style).apply { fontColor = Color.valueOf("453925") }
                setWrap(true)
            }
        }
        if (royal) {
            background = null
            selectedConstructionTable.background =
                com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
        }

        selectedConstructionTable.run {
            if (royal) pad(52f).padTop(58f) else pad(10f)
            val portrait = ImageGetter.getPortraitImage(construction.name, 50f).apply {
                val link = (construction as? IRulesetObject)?.makeLink() ?: return@apply
                if (link.isEmpty()) return@apply
                touchable = Touchable.enabled
                onClick {
                    UncivGame.Current.pushScreen(CivilopediaScreen(city.getRuleset(), link = link))
                }
            }
            val portraitCell = add(portrait).pad(5f)
            if (royal) portraitCell.colspan(2).row()

            var buildingText = construction.name.tr()
            val specialConstruction =
                PerpetualConstruction.perpetualConstructionsMap[construction.name]
            buildingText += specialConstruction?.getProductionTooltip(city)
                ?: cityConstructions.getTurnsToConstructionString(construction.name)
            val title = textLabel(buildingText)
            val titleCell = add(title)
            if (royal) titleCell.colspan(2).width(contentWidth)
            titleCell.row()

            val description = when (construction) {
                is BaseUnit -> construction.getDescription(city)
                is Building -> construction.getDescription(city, true)
                is CityEvent -> getEventDescription(construction, city)
                is PerpetualStatConversion -> construction.description
                    .replace("[rate]", "[${construction.getConversionRate(city)}]").tr()

                else -> ""
            }
            val descriptionLabel = textLabel(description)
            descriptionLabel.wrap = true
            add(descriptionLabel).colspan(2).width(contentWidth)

            if (construction is Building && cityConstructions.isBuilt(construction.name)
                && construction.isSellable()
            ) {
                val sellAmount = cityScreen.city.getGoldForSellingBuilding(construction.name)
                val sellText = "Sell [$sellAmount] " + Fonts.gold
                val sellBuildingButton = sellText.toTextButton()
                row()
                val sellCell = add(sellBuildingButton).padTop(5f).colspan(2).center()
                if (royal) {
                    sellBuildingButton.label.setWrap(true)
                    sellCell.width(contentWidth).minHeight(58f)
                }
                sellBuildingButton.onClick(UncivSound.Coin) {
                    sellBuildingButton.disable()
                    cityScreen.closeAllPopups()
                    ConfirmPopup(
                        cityScreen,
                        "Are you sure you want to sell this [${construction.name}]?",
                        sellText,
                        restoreDefault = { cityScreen.update() }
                    ) {
                        cityScreen.city.sellBuilding(construction.name)
                        cityScreen.clearSelection()
                        cityScreen.update()
                    }.open()
                }
                if (cityScreen.city.hasSoldBuildingThisTurn && !cityScreen.city.civInfo.gameInfo.gameParameters.godMode
                    || cityScreen.city.isPuppet
                    || !UncivGame.Current.worldScreen!!.isPlayersTurn || !cityScreen.canChangeState
                )
                    sellBuildingButton.disable()
            }
        }
    }

    private fun getEventDescription(event: CityEvent, city: CityInfo): String {
        val description = StringBuilder()

        // Add basic description if available
        if (event.description.isNotEmpty()) {
            description.append(event.description.tr())
        }

        // Add duration info
        if (event.duration > 0) {
            description.append("\n\n")
            description.append("Duration: ${event.duration} turns".tr())
        }

        // Add unique effects with icons
        val uniqueEffects = mutableListOf<String>()
        for (unique in event.uniqueObjects) {
            val effectText = formatUniqueWithIcons(unique.text)
            if (effectText.isNotEmpty()) {
                uniqueEffects.add(effectText)
            }
        }

        if (uniqueEffects.isNotEmpty()) {
            description.append("\n\n")
            description.append("Effects:".tr())
            for (effect in uniqueEffects) {
                description.append("\n• ")
                description.append(effect)
            }
        }

        // Add required building info
        if (event.requiredBuilding.isNotEmpty()) {
            description.append("\n\n")
            description.append("Requires: [${event.requiredBuilding}]".tr())
        }

        return description.toString()
    }

    private fun formatUniqueWithIcons(uniqueText: String): String {
        // Replace stat references with game icons using Fonts constants
        return uniqueText
            .replace("Production", "${Fonts.production}Production")
            .replace("Food", "${Fonts.food}Food")
            .replace("Gold", "${Fonts.gold}Gold")
            .replace("Science", "${Fonts.science}Science")
            .replace("Culture", "${Fonts.culture}Culture")
            .replace("Faith", "${Fonts.faith}Faith")
            .replace("Happiness", "${Fonts.happiness}Happiness")
            .replace("Strength", "${Fonts.strength}Strength")
            .replace("Movement", "${Fonts.movement}Movement")
            .tr()
    }

    private val constructionScroll by lazy {
        com.unciv.ui.utils.AutoScrollPane(selectedConstructionTable, BaseScreen.skin).apply {
            name = "cityConstructionScroll"
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
            setFadeScrollBars(false)
        }
    }
}
