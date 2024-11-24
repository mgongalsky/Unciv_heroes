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

        if(cityScreen.currTroop == null) {
            bgImage.drawable = TextureRegionDrawable(TextureRegion(bgTextureActive))
            cityScreen.isGarrisonSelected = false
        }

        if(cityScreen.currTroop == troop2select) {
            bgImage.drawable = TextureRegionDrawable(TextureRegion(bgTextureInactive))
            cityScreen.currTroop = null
            return
        }


        if(cityScreen.currTroop != null && cityScreen.currTroop != troop2select)
        {
            if (!cityScreen.isGarrisonSelected) {

                heroArmyBgImages[cityScreen.currTroop]?.drawable =
                        TextureRegionDrawable(TextureRegion(bgTextureInactive))
                // Swap actors of troop themselves
                val parentOld = heroArmyGroup[cityScreen.currTroop]?.parent
                val parentNew = heroArmyGroup[troop2select]?.parent

                parentOld?.removeActor(heroArmyGroup[cityScreen.currTroop])
                parentNew?.removeActor(heroArmyGroup[troop2select])

                parentOld?.addActor(heroArmyGroup[troop2select])
                parentNew?.addActor(heroArmyGroup[cityScreen.currTroop])


                // Swap troops in the garrison list
                val indexNew = visitingHero?.troops?.indexOf(troop2select)
                val indexOld = visitingHero?.troops?.indexOf(cityScreen.currTroop)

                if (indexNew != null && indexOld != null)
                    if (indexNew != -1 && indexOld != -1) {
                        visitingHero?.troops?.set(indexNew, cityScreen.currTroop!!)
                        visitingHero?.troops?.set(indexOld, troop2select)
                    }

                // Swap cells in the HashMap
                val cellOld = heroArmyCells[cityScreen.currTroop]
                val cellNew = heroArmyCells[troop2select]

                if (cellOld != null && cellNew != null) {
                    // Swap the Cell<Group> values
                    heroArmyCells[cityScreen.currTroop!!] = cellNew
                    heroArmyCells[troop2select] = cellOld
                }

                // Swap images in the HashMap
                val imageOld = heroArmyBgImages[cityScreen.currTroop]
                val imageNew = heroArmyBgImages[troop2select]

                if (imageOld != null && imageNew != null) {
                    // Swap the Cell<Group> values
                    heroArmyBgImages[cityScreen.currTroop!!] = imageNew
                    heroArmyBgImages[troop2select] = imageOld
                }

                cityScreen.currTroop = null
                return
            }
            else
            {
                /** GarrisonWidget short for cityScreen.selectedConstructionTable */
                val GW = cityScreen.constructionsTable
                GW.garrisonBgImages[cityScreen.currTroop]?.drawable =
                        TextureRegionDrawable(TextureRegion(bgTextureInactive))
                // Swap actors of troop themselves
                val parentOld = GW.garrisonGroup[cityScreen.currTroop]?.parent
                val parentNew = heroArmyGroup[troop2select]?.parent

                parentOld?.removeActor(GW.garrisonGroup[cityScreen.currTroop])
                parentNew?.removeActor(heroArmyGroup[troop2select])

                parentOld?.addActor(heroArmyGroup[troop2select])
                parentNew?.addActor(GW.garrisonGroup[cityScreen.currTroop])


                // Swap cells in the HashMap
                val cellOld = GW.garrisonCells[cityScreen.currTroop]
                val cellNew = heroArmyCells[troop2select]

                if (cellOld != null && cellNew != null) {
                    // Swap the Cell<Group> values
                    heroArmyCells.remove(troop2select)
                    GW.garrisonCells.remove(cityScreen.currTroop!!)
                    heroArmyCells[cityScreen.currTroop!!] = cellNew
                    GW.garrisonCells[troop2select] = cellOld
                }

                // Swap images in the HashMap
                val imageOld = GW.garrisonBgImages[cityScreen.currTroop]
                val imageNew = heroArmyBgImages[troop2select]

                if (imageOld != null && imageNew != null) {
                    // Swap the Cell<Group> values
                    heroArmyBgImages.remove(troop2select)
                    GW.garrisonBgImages.remove(cityScreen.currTroop!!)
                    heroArmyBgImages[cityScreen.currTroop!!] = imageNew
                    GW.garrisonBgImages[troop2select] = imageOld
                }

                // Swap groups in the HashMap
                val groupOld = GW.garrisonGroup[cityScreen.currTroop]
                val groupNew = heroArmyGroup[troop2select]

                if (groupOld != null && groupNew != null) {
                    // Swap the Cell<Group> values
                    heroArmyGroup.remove(troop2select)
                    GW.garrisonGroup.remove(cityScreen.currTroop!!)
                    heroArmyGroup[cityScreen.currTroop!!] = groupNew
                    GW.garrisonGroup[troop2select] = groupOld
                }

                // Swap troops in the garrison list
                val indexNew = visitingHero?.troops?.indexOf(troop2select)
                val indexOld = cityScreen.city.garrison.indexOf(cityScreen.currTroop)

                if (indexNew != null && indexOld != null)
                    if (indexNew != -1 && indexOld != -1) {
                        visitingHero?.troops?.set(indexNew, cityScreen.currTroop!!)
                        cityScreen.city.garrison[indexOld] = troop2select
                    }



                cityScreen.currTroop = null
                return

            }


        }

        // Select new slot, or remove selection if already selected troop is clicked
        cityScreen.currTroop = if(cityScreen.currTroop != troop2select)
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

