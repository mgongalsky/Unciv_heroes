package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.MapUnit
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.darken

class VisitingHeroTable(val cityScreen: CityScreen) : Table() {
    private val heroArmyTable = Table()

    private val stageHeight = cityScreen.stage.height
    private val stageWidth = cityScreen.stage.width


    //private val garrisonWidget = Table()
    //private var currHeroArmyTroop : Troop? = null
    internal var heroArmyBgImages = HashMap<TroopInfo, Image>()
    internal var heroArmyCells = HashMap<TroopInfo, Cell<Group>>()
    internal var heroArmyGroup = HashMap<TroopInfo, Group>()

    var visitingHero: MapUnit?


    init {
        right().bottom()
        heroArmyTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/ConstructionInfoTable/heroArmyTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.5f)
        )
        add(heroArmyTable).pad(2f).fill()
        background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/ConstructionInfoTable/Background",
            tintColor = Color.WHITE
        )


        //heroArmyTable.right().bottom()
        // Here is code dubbing from CityConstructionTable.kt init
        // garrisonWidget.height(stageHeight / 8)
        val tableHeight = stageHeight / 8f
        heroArmyTable.height = tableHeight


        val iconSize = heroArmyTable.height - 2 * heroArmyTable.padTop
        val iconSpacing = 0f
        val iconAmount = 4 // Change this value to set the number of icons

        //val exampleTroop = Troop(15,"Archer")
        //var iterTroop = cityScreen.city.garrison.iterator()
        var i = 0
        //  var currTroop = cityScreen.city.garrison.first()
        cityScreen.city.run {visitingHero = tileMap[location].militaryUnit}
        isVisible = true
        //heroArmyTable.bottom().right()

    }

    fun update() {
        //heroArmyTable.clear()  // clears content and listeners

        isVisible = true

        //updateHeroArmyTable()

       // pack()
    }

    private fun updateHeroArmyTable() {
        val descriptionLabel = Label("description", BaseScreen.skin)  // already translated
        heroArmyTable.add(descriptionLabel).colspan(2).width(stageWidth/3).row()

    }



    fun createMonochromaticTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()

        return texture
    }

    fun createBorderDrawable(texture: Texture, borderWidth: Float, borderColor: Color): Drawable {
        val sprite = Sprite(texture)
        val borderSprite = Sprite(texture)

        borderSprite.setSize(sprite.width + borderWidth * 2f, sprite.height + borderWidth * 2f)
        borderSprite.setColor(borderColor)

        val borderBatch = SpriteBatch()
        borderBatch.begin()
        borderSprite.draw(borderBatch)
        borderBatch.end()

        val drawable = SpriteDrawable(sprite)
        drawable.leftWidth = borderWidth
        drawable.rightWidth = borderWidth
        drawable.topHeight = borderWidth
        drawable.bottomHeight = borderWidth

        return drawable
    }



}

