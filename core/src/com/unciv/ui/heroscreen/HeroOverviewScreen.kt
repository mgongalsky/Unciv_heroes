package com.unciv.ui.heroscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.unciv.Constants
import com.unciv.logic.map.MapUnit
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager

class HeroOverviewScreen(
    private var viewingHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    internal val centerAreaHeight = stage.height - 82f

    private val tabbedPager: TabbedPager

    override fun dispose() {
        tabbedPager.selectPage(-1)
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
            separatorColor = Color.WHITE)

        tabbedPager.addClosePage { game.popScreen() }

        val pageObject = Table(BaseScreen.skin)
        pageObject.background = BaseScreen.skin.get("fantasy_background", NinePatchDrawable::class.java)

        val font = BaseScreen.skin.get("smallOldLondon", BitmapFont::class.java)
        font.data.scaleX = 0.3f // scale font width by 50%
        font.data.scaleY = 0.3f // scale font height by 50%
        //val font = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        //val font =

     //   val labelStyle = Label.LabelStyle().apply {
     //       this.font = BaseScreen.skin.get("oldLondon", BitmapFont::class.java)
     //   }

        //pageObject.add
        pageObject.pad(10f,0f,10f,0f)
        pageObject.add(Label("Hero Type", BaseScreen.skin, "fantasyLabel")).size(140f)//.padLeft(8f)
        pageObject.add(Label("Attack Skill", BaseScreen.skin, "fantasyLabel")).size(140f)//.padLeft(8f)
        pageObject.add(Label("Defense Skill", BaseScreen.skin, "fantasyLabel")).size(140f)//.padLeft(8f)
       // pageObject.add("Strength").size(140f)//.padLeft(8f)
       // pageObject.add("Health").size(140f)//.padLeft(8f)

        pageObject.row()
        pageObject.add(Label(viewingHero.displayName(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)
        pageObject.add(Label(viewingHero.heroAttackSkill.toString(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)
        pageObject.add(Label(viewingHero.heroDefenseSkill.toString(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)
    //    pageObject.add(viewingHero.baseUnit.strength.toString()).size(140f)//.padRight(8f)
    //    pageObject.add(viewingHero.health.toString()).size(140f)//.padRight(8f)
        //stage.addActor(pageObject)
        val index = tabbedPager.addPage(
            caption = "Heroes",
            content = pageObject
        )
        tabbedPager.selectPage(index)


        tabbedPager.setFillParent(true)
        stage.addActor(tabbedPager)
   }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        return HeroOverviewScreen(viewingHero, game.settings.lastOverviewPage)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }
}
