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
import com.unciv.logic.city.IConstruction
import com.unciv.logic.event.hero.Troop
import com.unciv.logic.map.MapUnit
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.darken

class VisitingHeroTable(val cityScreen: CityScreen) : Table() {
    private val heroArmyTable = Table()

    private val stageHeight = cityScreen.stage.height
    private val stageWidth = cityScreen.stage.width


    //private val garrisonWidget = Table()
    private var currHeroArmyTroop : Troop? = null
    private var heroArmyBgImages = HashMap<Troop, Image>()
    private var heroArmyCells = HashMap<Troop, Cell<Group>>()
    private var heroArmyGroup = HashMap<Troop, Group>()

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


        heroArmyTable.right().bottom()
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
        if(visitingHero != null) {
            visitingHero?.troops?.forEach { currTroop ->
                currTroop.initializeImages(cityScreen.city.civInfo)

                var troopGroup = Group()
                troopGroup.height = heroArmyTable.height
                troopGroup.width = heroArmyTable.height
                //garrisonWidget.add(troopGroup).size(iconSize)

                val column = heroArmyTable.columnDefaults(i % 2)
                column.spaceBottom(iconSpacing)
                currTroop.drawInCity(troopGroup, isGarrison = true)
                //troopGroup.addBorder(3f, Color.WHITE, expandCell = false)

                //val bgColor = Color(0f, 0.3f, 0f, 0f)
                val bgTexture = createMonochromaticTexture(
                    64,
                    64,
                    Color.BROWN
                ) // Adjust the dimensions and color as needed
                val bgTextureActive = createMonochromaticTexture(
                    64,
                    64,
                    Color.OLIVE
                ) // Adjust the dimensions and color as needed

                // Create a drawable with a border using the texture
                val drawable = createBorderDrawable(
                    bgTexture,
                    5f,
                    Color.WHITE
                ) // Adjust the border width and color as needed


                //val cell: Cell<*> = garrisonWidget.add()


                val backgroundImage = Image(bgTexture)

// Create a group to hold the troopGroup and the background actor
                val bgGroup = Group()
                bgGroup.addActor(backgroundImage)
                bgGroup.addActor(troopGroup)




                println("Trying to make a clicker.")
                val cell = heroArmyTable.add(bgGroup).size(iconSize)
                cell.actor.touchable = Touchable.enabled
                cell.actor.addListener(object : ClickListener() {

                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        //selectTextureActivate(backgroundActor, bgTextureActive)
                        selectTroopSlot(backgroundImage)
                    }
                })

                //cell.spaceLeft(iconSpacing)

                heroArmyCells[currTroop] = cell
                heroArmyBgImages[currTroop] = backgroundImage
                heroArmyGroup[currTroop] = troopGroup
                //troopGroup.addBorder(3f, Color.WHITE, expandCell = false)

                //cell.background = drawable


                i += 1
            }
        }
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

    private fun selectTroopSlot(bgImage: Image){
        val bgTextureInactive = createMonochromaticTexture(64, 64, Color.BROWN) // Adjust the dimensions and color as needed
        val bgTextureActive = createMonochromaticTexture(64, 64, Color.OLIVE) // Adjust the dimensions and color as needed
        var troop2select = heroArmyBgImages.filterValues { img -> img == bgImage }.keys.first()

        if(currHeroArmyTroop == null)
            bgImage.drawable = TextureRegionDrawable(TextureRegion(bgTextureActive))

        if(currHeroArmyTroop == troop2select)
            bgImage.drawable = TextureRegionDrawable(TextureRegion(bgTextureInactive))


        if(currHeroArmyTroop != null && currHeroArmyTroop != troop2select)
        {
            heroArmyBgImages[currHeroArmyTroop]?.drawable = TextureRegionDrawable(TextureRegion(bgTextureInactive))
            // Swap actors of troop themselves
            val parentOld = heroArmyGroup[currHeroArmyTroop]?.parent
            val parentNew = heroArmyGroup[troop2select]?.parent

            parentOld?.removeActor(heroArmyGroup[currHeroArmyTroop])
            parentNew?.removeActor(heroArmyGroup[troop2select])

            parentOld?.addActor(heroArmyGroup[troop2select])
            parentNew?.addActor(heroArmyGroup[currHeroArmyTroop])


            // Swap troops in the garrison list
            val indexNew = visitingHero?.troops?.indexOf(troop2select)
            val indexOld = visitingHero?.troops?.indexOf(currHeroArmyTroop)

            if (indexNew != null && indexOld != null)
                if (indexNew != -1 && indexOld != -1) {
                    visitingHero?.troops?.set(indexNew, currHeroArmyTroop!!)
                    visitingHero?.troops?.set(indexOld, troop2select)
                }

            // Swap cells in the HashMap
            val cellOld = heroArmyCells[currHeroArmyTroop]
            val cellNew = heroArmyCells[troop2select]

            if (cellOld != null && cellNew != null) {
                // Swap the Cell<Group> values
                heroArmyCells[currHeroArmyTroop!!] = cellNew
                heroArmyCells[troop2select] = cellOld
            }

            // Swap images in the HashMap
            val imageOld = heroArmyBgImages[currHeroArmyTroop]
            val imageNew = heroArmyBgImages[troop2select]

            if (imageOld != null && imageNew != null) {
                // Swap the Cell<Group> values
                heroArmyBgImages[currHeroArmyTroop!!] = imageNew
                heroArmyBgImages[troop2select] = imageOld
            }

            currHeroArmyTroop = null
            return

        }

        // Select new slot, or remove selection if already selected troop is clicked
        currHeroArmyTroop = if(currHeroArmyTroop != troop2select)
            troop2select
        else
            null

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

