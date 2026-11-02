package com.unciv.ui.heroscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.map.MapUnit
import com.unciv.pure.domain.supply.SupplyMechanic
import com.unciv.ui.army.ArmyView
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.toLabel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HeroOverviewScreen(
    private var viewingHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize, KoinComponent {
    private val supplyMechanic: SupplyMechanic by inject()
    internal val centerAreaHeight = stage.height - 82f
    private val royal = game.settings.skin == "Minimal"
    private val ink = if (royal) Color.valueOf("453925") else Color.WHITE
    private val contentWidth = minOf(740f, stage.width - if (royal) 136f else 52f)
        .coerceAtLeast(160f)
    private val heroArmyManager = ArmyManager(viewingHero.army)
    private val heroArmyView = ArmyView(viewingHero.army, heroArmyManager, this, slotSize = 128f)
    private val heroStatsTable = Table().apply {
        name = "heroOverviewStats"
        align(Align.topLeft)
    }
    private val tabbedPager: TabbedPager

    override fun dispose() {
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {
        setupHeroArmyView()
        val heroOverviewContent = Table().apply {
            add(heroStatsTable).top().padBottom(20f).row()
            add(heroArmyView).grow().align(Align.center)
        }
        if (defaultPage != "") game.settings.lastOverviewPage = defaultPage
        globalShortcuts.add(KeyCharAndCode.BACK) { game.popScreen() }
        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = if (royal) Color.CLEAR else Color.WHITE
        )
        tabbedPager.addClosePage { game.popScreen() }
        val index = tabbedPager.addPage(caption = "Heroes", content = heroOverviewContent)
        tabbedPager.selectPage(index)
        tabbedPager.setFillParent(true)
        stage.addActor(tabbedPager)
    }

    private fun heroLabel(text: String, heading: Boolean = false): Label =
        text.toLabel(fontColor = ink, fontSize = if (heading) 24 else 18).apply {
            if (heading && royal && skin.has("royal-display", BitmapFont::class.java)) {
                style = Label.LabelStyle(style).apply {
                    font = skin.get("royal-display", BitmapFont::class.java)
                }
            }
            setWrap(true)
            setAlignment(if (heading) Align.center else Align.left)
        }

    private fun setupHeroStatsTable() {
        if (royal) {
            heroStatsTable.background = RoyalPanelDrawable(skin)
            heroStatsTable.pad(52f).padTop(58f)
        } else heroStatsTable.pad(10f)

        fun heading(text: String) {
            heroStatsTable.add(heroLabel(text, heading = true))
                .colspan(2).width(contentWidth).padTop(10f).padBottom(10f).row()
        }
        fun stat(text: String, value: String) {
            heroStatsTable.add(heroLabel(text)).width(contentWidth * 0.7f - 10f)
                .left().pad(5f)
            heroStatsTable.add(heroLabel(value).apply { setAlignment(Align.right) })
                .width(contentWidth * 0.3f - 10f).right().pad(5f).row()
        }
        fun paragraph(text: String) {
            heroStatsTable.add(heroLabel(text)).colspan(2)
                .width(contentWidth - 10f).left().pad(5f).row()
        }

        heading("Hero Skills")
        stat("Attack Skill:", viewingHero.heroAttackSkill.toString())
        stat("Defense Skill:", viewingHero.heroDefenseSkill.toString())
        stat("Morale:", viewingHero.morale.toString())
        stat("Luck:", viewingHero.luck.toString())
        if (supplyMechanic.isEnabled) {
            heading("Food supply")
            val currentFood = viewingHero.hero.currentFood
            val maxFood = viewingHero.basicFoodCapacity
            // Plan consumption outside a city, as in the original overview.
            val foodMaintenance = viewingHero.army.calculateFoodMaintenance(false)
            paragraph(
                "Now ${currentFood.toInt()}${Fonts.food} of max ${maxFood.toInt()}${Fonts.food}. Per turn when outside city: %.1f${Fonts.food}."
                    .format(foodMaintenance)
            )
            val foodDuration = if (foodMaintenance > 0) (currentFood / foodMaintenance).toInt()
                else Int.MAX_VALUE
            paragraph(if (foodDuration == Int.MAX_VALUE) "Food will last indefinitely."
                else "Food will last for $foodDuration${Fonts.turn}.")
        }
        heading("City Settlement")
        val cityPopulation = viewingHero.calculateArmyPopulation() + 1
        paragraph("Can settle city with $cityPopulation${Fonts.population}")
    }

    private fun updateHeroStatsTable() {
        heroStatsTable.clear()
        setupHeroStatsTable()
    }

    private fun setupHeroArmyView() {
        heroArmyView.bottom().pad(10f)
        updateHeroArmyView()
    }

    private fun updateHeroArmyView() {
        updateHeroStatsTable()
        heroArmyView.updateView()
        heroArmyView.isVisible = true
    }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen =
        HeroOverviewScreen(viewingHero, game.settings.lastOverviewPage)

    fun resizePage(tab: EmpireOverviewTab) {
    }
}
