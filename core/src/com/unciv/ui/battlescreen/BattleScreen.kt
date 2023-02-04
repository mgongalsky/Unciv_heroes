package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.Constants
import com.unciv.logic.HexMath
import com.unciv.logic.HexMath.hex2EvenQCoords
import com.unciv.logic.HexMath.hexTranspose
import com.unciv.logic.hero.Troop
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.tilegroups.WorldTileGroup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.onClick

// Now it's just copied from HeroOverviewScreen

class BattleScreen(
    private var viewingHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    internal val centerAreaHeight = stage.height - 82f
    internal val BFwidth : Int = 14
    internal val BFheight : Int = 8
    private val battleField : TileMap = TileMap(BFwidth, BFheight,  game.gameInfo!!.ruleSet, viewingHero.currentTile.baseTerrain)
//    private lateinit var tileGroupMap: TileGroupMap<TileGroup>
    val tileGroups = HashMap<TileInfo, List<TileGroup>>()
    private lateinit var tileGroupMap: TileGroupMap<TileGroup>
    private val allTileGroups = ArrayList<TileGroup>()

    // Have TileInfo
    // Need TileGroup

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
        //battleField[1,1].
        val terraLayer = ArrayList<Group>()
        //for (group in tileGroups.sortedByDescending { it.tileInfo.position.x + it.tileInfo.position.y }) {
        //    unitLayers.add(group.unitLayerGroup.apply { setPosition(group.x,group.y) })
/*
        //battleField.tileMatrix  // ArrayList<TileInfo>
        for (tileArray in battleField.tileMatrix)
        {
            for (tile in tileArray)
            {
                //val tileGroup : TileGroup()
                //tileGroup = Tile
                //tileGroup.tileInfo = tile
            }
            tileGroup.tileInfo = tile
            val newGroup : Group
            newGroup = Group()
            newGroup.x = 1f
            newGroup.y = 1f
           // newGroup.
            terraLayer.add(newGroup)
          //  tile.

        }

 */
        //for (group in tileGroups) addActor(group) // The above layers are for the visual layers, this is for the clickability of the tile

        //
        //    tileGroupMap = TileGroupMap()
        globalShortcuts.add(KeyCharAndCode.BACK) { game.popScreen() }

        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE)


        tabbedPager.addClosePage { game.popScreen() }
/*
        val pageObject = Table(BaseScreen.skin)
        pageObject.pad(10f,0f,10f,0f)
        pageObject.add("Battle!").size(200f)//.padLeft(8f)
        pageObject.add("Hero Type").size(140f)//.padLeft(8f)
        pageObject.add("Attack Skill").size(140f)//.padLeft(8f)
        pageObject.add("Strength").size(140f)//.padLeft(8f)
        pageObject.add("Health").size(140f)//.padLeft(8f)
        pageObject.add("We are on:").size(140f)//.padLeft(8f)

        pageObject.row()
        pageObject.add("Glory to heroes!").size(200f)//.padLeft(8f)
        pageObject.add(viewingHero.displayName()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.baseUnit.attackSkill.toString()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.baseUnit.strength.toString()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.health.toString()).size(140f)//.padRight(8f)
        pageObject.add(viewingHero.currentTile.baseTerrain).size(140f)


 */
        //stage.addActor(pageObject)

        addTiles()
       // val img = ImageGetter.getImage("Warrior")
        val pixelUnitImages = ImageGetter.getLayeredImageColored("TileSets/FantasyHex/Units/Warrior-1")
        //     stage.addActor(img)
        for (pixelUnitImage in pixelUnitImages) {
            pixelUnitImage.setPosition(0f,0f)
            stage.addActor(pixelUnitImage)
            //     setHexagonImageSize(pixelUnitImage)// Treat this as A TILE, which gets overlayed on the base tile.
        }


        //   val acTroop: Actor = Actor()
     //   acTroop.
     //   TileSets/AbsoluteUnits/Units/Warrior
 //       val pixelUnitImages = ImageGetter.getLayeredImageColored(newImageLocation, null, nation.getInnerColor(), nation.getOuterColor())
 //       for (pixelUnitImage in pixelUnitImages) {
  //          pixelMilitaryUnitGroup.addActor(pixelUnitImage)

        stage.addActor(tabbedPager)

        val index = tabbedPager.addPage(
            caption = "Battle",
            content = tileGroupMap
        )
        tabbedPager.selectPage(index)






        tabbedPager.setFillParent(true)


   }
    // Copied from EditorMapHolder
    internal fun addTiles(){

        val tileSetStrings = TileSetStrings()
        val daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }

        tileGroupMap = TileGroupMap(
            daTileGroups)

        var exTile = daTileGroups.first { HexMath.hexTranspose(HexMath.hex2EvenQCoords(it.tileInfo.position)) == viewingHero.exampleTroop.position }
        viewingHero.exampleTroop.enterBattle(viewingHero.civInfo)
        viewingHero.exampleTroop.drawOnBattle(exTile)

        for (tileGroup in daTileGroups)
        {
                allTileGroups.add(tileGroup)

                tileGroups[tileGroup.tileInfo] = listOf(tileGroup)
        }

        for (tileGroup in allTileGroups) {

/* revisit when Unit editing is re-implemented
            // This is a hack to make the unit icons render correctly on the game, even though the map isn't part of a game
            // and the units aren't assigned to any "real" CivInfo
            //to do make safe the !!
            //to do worse - don't create a whole Civ instance per unit
            tileGroup.tileInfo.getUnits().forEach {
                it.civInfo = CivilizationInfo().apply {
                    nation = ruleset.nations[it.owner]!!
                }
            }
*/
            tileGroup.showEntireMap = true
            tileGroup.update()
          //  if (touchable != Touchable.disabled)
            //    tileGroup.onClick { onTileClick(tileGroup.tileInfo) }
        }

        tileGroupMap.setSize(stage.width, stage.height)
        stage.addActor(tileGroupMap)

//        tileGroupMap.layout()

 //       scrollPercentX = .5f
   //     scrollPercentY = .5f
     //   updateVisualScroll()
    }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        return BattleScreen(viewingHero, game.settings.lastOverviewPage)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }
}
