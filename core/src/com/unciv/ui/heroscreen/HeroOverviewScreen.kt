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

    /** Отдельный обзор использует те же сведения и подписи, что и городская вкладка. */
    private fun setupHeroStatsTable() {
        if (royal) {
            heroStatsTable.background = RoyalPanelDrawable(skin)
            heroStatsTable.pad(52f).padTop(58f)
        } else heroStatsTable.pad(10f)
        val details = HeroDetailsTable(royal)
        details.update(
            HeroOverviewSnapshotAdapter.read(viewingHero, supplyMechanic.isEnabled),
            contentWidth
        )
        heroStatsTable.add(details).width(contentWidth).top().left()
    }

    private fun updateHeroStatsTable() {
        heroStatsTable.clear()
        setupHeroStatsTable()
    }

    /** Изменение состава армии обновляет расход еды и сведения об основании города. */
    private fun setupHeroArmyView() {
        heroArmyView.onArmyUpdated = { updateHeroStatsTable() }
        heroArmyView.bottom().pad(10f)
        updateHeroArmyView()
    }

    private fun updateHeroArmyView() {
        updateHeroStatsTable()
        heroArmyView.updateView(initialized = false)
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
