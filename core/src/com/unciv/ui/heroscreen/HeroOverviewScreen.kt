package com.unciv.ui.heroscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.overviewscreen.EmpireOverviewCategories
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.overviewscreen.EmpireOverviewTab.EmpireOverviewTabPersistableData
import com.unciv.ui.overviewscreen.EmpireOverviewTabState
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager

class HeroOverviewScreen(
    private var viewingPlayer: CivilizationInfo,
    private var viewingHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    internal val centerAreaHeight = stage.height - 82f

    private val tabbedPager: TabbedPager
    private val pageObjects = HashMap<EmpireOverviewCategories, EmpireOverviewTab>()

    companion object {
        // This is what keeps per-tab states between overview invocations
        var persistState: Map<EmpireOverviewCategories, EmpireOverviewTabPersistableData>? = null

        private fun updatePersistState(pageObjects: HashMap<EmpireOverviewCategories, EmpireOverviewTab>) {
            persistState = pageObjects.mapValues { it.value.persistableData }.filterNot { it.value.isEmpty() }
        }
    }

    override fun dispose() {
        tabbedPager.selectPage(-1)
        updatePersistState(pageObjects)
        super.dispose()
    }

    init {
        val page =
            if (defaultPage != "") {
                game.settings.lastOverviewPage = defaultPage
                defaultPage
            }
            else game.settings.lastOverviewPage
        val iconSize = Constants.defaultFontSize.toFloat()

        globalShortcuts.add(KeyCharAndCode.BACK) { game.popScreen() }

        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE,
            capacity = EmpireOverviewCategories.values().size)

        tabbedPager.addClosePage { game.popScreen() }

        /*enum class EmpireOverviewCategories(
            val iconName: String,
            val shortcutKey: KeyCharAndCode,
            val scrollAlign: Int,
            val factory: FactoryType,
            val stateTester: StateTesterType
        ) {
        */
        val pageObject = Table(BaseScreen.skin)
        pageObject.pad(10f,0f,10f,0f)
        pageObject.add("Glory to Ukraine!").size(200f)//.padLeft(8f)
        pageObject.add("Hero Type").size(140f)//.padLeft(8f)
        pageObject.add("Attack Skill").size(140f)//.padLeft(8f)
        pageObject.add("Strength").size(140f)//.padLeft(8f)
        pageObject.add("Health").size(140f)//.padLeft(8f)

        pageObject.row()
        pageObject.add("Glory to heroes!").size(200f)//.padLeft(8f)
        pageObject.add(viewingHero.displayName()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.baseUnit.attackSkill.toString()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.baseUnit.strength.toString()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.health.toString()).size(140f)//.padRight(8f)
        //stage.addActor(pageObject)
        val index = tabbedPager.addPage(
            caption = "Heroes",
            content = pageObject
        )
        tabbedPager.selectPage(index)
    //    pageObject.select(selection)

        /*

        for (category in EmpireOverviewCategories.values()) {
            val tabState = category.stateTester(viewingPlayer)
            if (tabState == EmpireOverviewTabState.Hidden) continue
            val icon = if (category.iconName.isEmpty()) null else ImageGetter.getImage(category.iconName)
            val pageObject = category.factory(viewingPlayer, this, persistState?.get(category))
            pageObject.pad(10f, 0f, 10f, 0f)
            pageObjects[category] = pageObject
            val index = tabbedPager.addPage(
                caption = category.name,
                content = pageObject,
                icon, iconSize,
                disabled = tabState != EmpireOverviewTabState.Normal,
                shortcutKey = category.shortcutKey,
                scrollAlign = category.scrollAlign
            )
            if (category.name == page) {
                tabbedPager.selectPage(index)
                pageObject.select(selection)
            }
        }

 */

        tabbedPager.setFillParent(true)
        stage.addActor(tabbedPager)
   }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        updatePersistState(pageObjects)
        return HeroOverviewScreen(viewingPlayer, viewingHero, game.settings.lastOverviewPage)
    }

    fun resizePage(tab: EmpireOverviewTab) {
        val category = (pageObjects.entries.find { it.value == tab } ?: return).key
        tabbedPager.replacePage(category.name, tab)
    }
}
