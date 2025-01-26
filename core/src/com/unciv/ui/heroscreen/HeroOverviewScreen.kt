package com.unciv.ui.heroscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.map.MapUnit
import com.unciv.ui.army.ArmyView
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.rendering.TilableFrame
import com.unciv.ui.rendering.Tiled2DDrawable
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager
import kotlin.math.ceil

class HeroOverviewScreen(
    private var viewingHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    internal val centerAreaHeight = stage.height - 82f

    // Добавляем ArmyManager и ArmyView
    private val heroArmyManager = ArmyManager(viewingHero.army)
    private val heroArmyView = ArmyView(viewingHero.army, heroArmyManager, this, slotSize = 128f)

    // Таблица для отображения навыков героя
    private val heroStatsTable = Table().apply {
        // Добавляем стиль и отступы
        pad(10f).align(Align.topLeft)
    }


    private val tabbedPager: TabbedPager

    override fun dispose() {
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {

        // Настраиваем таблицу с навыками героя
        setupHeroStatsTable()
        // Настраиваем основной интерфейс
        setupHeroArmyView()

        // Создаём общий контейнер для вкладки
        val heroOverviewContent = Table().apply {
            add(heroStatsTable).growX().align(Align.top).padBottom(20f).row() // Таблица навыков
            add(heroArmyView).grow().align(Align.center) // Армия героя
        }


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

     //   val pageObject = Table(BaseScreen.skin)
        //pageObject.background = BaseScreen.skin.get("fantasy_background", NinePatchDrawable::class.java)
        //pageObject.background = BaseScreen.skin.get("tiled_fantasy_background", Tiled2DDrawable::class.java)


        /*
        fun createTroopSlot(style: String): Table {
            return Table().apply {
                background = BaseScreen.skin.get(style, TilableFrame::class.java)
                //add(Label(labelText, BaseScreen.skin, "fantasyLabel"))
            }
        }

        val pageObject = createTroopSlot("fantasy_frame")


        val font = BaseScreen.skin.get("smallOldLondon", BitmapFont::class.java)
        font.data.scaleX = 0.3f // scale font width by 50%
        font.data.scaleY = 0.3f // scale font height by 50%
        //val font = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        //val font =

     //   val labelStyle = Label.LabelStyle().apply {
     //       this.font = BaseScreen.skin.get("oldLondon", BitmapFont::class.java)
     //   }

        //pageObject.add
        pageObject.pad(10f,0f,60f,0f).align(Align.bottom)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).padLeft(50f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(10f).align(
            Align.bottom)//.padLeft(8f)
        pageObject.add(createTroopSlot("troop_slot")).width(100f).height(150f).padRight(50f).align(
            Align.bottom)//.padLeft(8f)
        //pageObject.add(Label("Attack Skill", BaseScreen.skin, "fantasyLabel")).size(140f)//.padLeft(8f)
        //pageObject.add(Label("Defense Skill", BaseScreen.skin, "fantasyLabel")).size(140f)//.padLeft(8f)
        //pageObject.add("Strength").size(140f)//.padLeft(8f)
        //pageObject.add("Health").size(140f)//.padLeft(8f)


         */
  //      pageObject.row()
  //      pageObject.add(Label(viewingHero.displayName(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)
   //     pageObject.add(Label(viewingHero.heroAttackSkill.toString(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)



    //    pageObject.add(Label(viewingHero.heroDefenseSkill.toString(), BaseScreen.skin, "fantasyLabel")).size(140f)//.padRight(8f)
    //    pageObject.add(viewingHero.baseUnit.strength.toString()).size(140f)//.padRight(8f)
    //    pageObject.add(viewingHero.health.toString()).size(140f)//.padRight(8f)
        //stage.addActor(pageObject)
        val index = tabbedPager.addPage(
            caption = "Heroes",
            content = heroOverviewContent
        )
        tabbedPager.selectPage(index)


        tabbedPager.setFillParent(true)
        stage.addActor(tabbedPager)


   }

    private fun setupHeroStatsTable() {
        //val font = BaseScreen.skin.get("default", BitmapFont::class.java)

        // Заголовок
        heroStatsTable.add(Label("Hero Skills", skin))
            .colspan(2).align(Align.center).padBottom(10f)
        heroStatsTable.row()

        // Навыки героя
        heroStatsTable.add(Label("Attack Skill:", skin)).align(Align.left).pad(5f)
        heroStatsTable.add(Label(viewingHero.heroAttackSkill.toString(), skin)).align(Align.right).pad(5f)
        heroStatsTable.row()

        heroStatsTable.add(Label("Defense Skill:", skin)).align(Align.left).pad(5f)
        heroStatsTable.add(Label(viewingHero.heroDefenseSkill.toString(), skin)).align(Align.right).pad(5f)
        heroStatsTable.row()

        heroStatsTable.add(Label("Morale:", skin)).align(Align.left).pad(5f)
        heroStatsTable.add(Label(viewingHero.morale.toString(), skin)).align(Align.right).pad(5f)
        heroStatsTable.row()

        heroStatsTable.add(Label("Luck:", skin)).align(Align.left).pad(5f)
        heroStatsTable.add(Label(viewingHero.luck.toString(), skin)).align(Align.right).pad(5f)
        heroStatsTable.row()

        // Заголовок
        heroStatsTable.add(Label("Food supply", skin))
            .colspan(2).align(Align.center).padBottom(10f)
        heroStatsTable.row()

        // Here it shows always for out of city food consumption
        val currentFood = viewingHero.getCurrentFood()
        val maxFood = viewingHero.basicFoodCapacity
        val foodMaintenance = viewingHero.army.calculateFoodMaintenance(isInCity = false)

        val supplyString = "Now ${currentFood.toInt()}${Fonts.food} of max ${maxFood.toInt()}${Fonts.food}. Per turn: %.1f${Fonts.food}.".format(foodMaintenance)

        heroStatsTable.add(Label(supplyString, skin)).align(Align.left).pad(5f)
      //  heroStatsTable.add(Label(viewingHero.luck.toString(), skin)).align(Align.right).pad(5f)
        heroStatsTable.row()


        // Позиционирование таблицы
        heroStatsTable.setPosition(20f, stage.height - 20f, Align.topLeft)
    }

    private fun updateHeroStatsTable() {
        // Очищаем таблицу и создаём её заново
        heroStatsTable.clear()
        setupHeroStatsTable()
    }


    /**
     * Настраиваем отображение армии героя
     */
    private fun setupHeroArmyView() {
        // Позиционируем ArmyView в центре экрана или в нужном месте
        heroArmyView.bottom().pad(10f) // Отступы и выравнивание
        stage.addActor(heroArmyView) // Добавляем в сцену

        // Обновляем данные вида
        updateHeroArmyView()
    }

    /**
     * Обновляет вид ArmyView, если данные изменились
     */
    private fun updateHeroArmyView() {
        updateHeroStatsTable()
        heroArmyView.updateView() // Обновляем вид
        heroArmyView.isVisible = true // Отображаем
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
