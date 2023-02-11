package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.Constants
import com.unciv.logic.HexMath
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.onClick
import com.unciv.utils.concurrency.Concurrency

// Now it's just copied from HeroOverviewScreen

class BattleScreen(
    private var manager: BattleManager,
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
    lateinit var pointerPosition : Vector2
    lateinit var pointerImages : ArrayList<Image>
    lateinit var daTileGroups : List<TileGroup>

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
        globalShortcuts.add(KeyCharAndCode.BACK) { game.popScreen() }

        val tileSetStrings = TileSetStrings()
        daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }

        val pointerString = "TileSets/FantasyHex/Highlight"
        pointerImages =
                ImageGetter.getLayeredImageColored(pointerString, Color.valueOf("#00AAFF77"))
   //     pointerPosition = Vector2(0f, 0f)


        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE)


        tabbedPager.addClosePage {shutdownScreen()}

        addTiles()

        stage.addActor(tabbedPager)

        val index = tabbedPager.addPage(
            caption = "Battle",
            content = tileGroupMap
        )
        tabbedPager.selectPage(index)

        tabbedPager.setFillParent(true)
   }
    fun draw_pointer()
    {
        var pointerTile = daTileGroups.first { HexMath.hex2EvenQCoords(it.tileInfo.position) == pointerPosition }
        for (pointerImage in pointerImages) {
            pointerImage.setScale(pointerTile.width/256f, pointerTile.width/256f*0.5f)
          //  pointerImage.moveBy(0f, pointerTile.height*0.15f)
            pointerImage.setPosition(0f, pointerTile.height*0.15f)

            pointerImage.setOrigin(pointerTile.originX, pointerTile.originY)
            pointerImage.touchable = Touchable.disabled
            pointerImage.name = "pointer"
           // if(pointerTile.)
            pointerTile.addActorBefore(pointerTile.findActor("troopGroup"), pointerImage)
       //     pointerTile.addActor(pointerImage)
        }
        for (tileGroup in daTileGroups)
            tileGroup.baseLayerGroup.color = Color(1f,1f,1f,1f)
        // TODO: Principally it works, but we need to fix coordinates conversions and distances
        var achievableHexes = daTileGroups.filter { HexMath.getDistance(it.tileInfo.position,HexMath.evenQ2HexCoords(pointerPosition)) < manager.currentTroop.baseUnit.speed }
        for (achievableHex in achievableHexes)
            achievableHex.baseLayerGroup.color = Color(1f,1f,1f,0.7f)

       // pointerTile.baseLayerGroup.color
    }
    // Copied from EditorMapHolder
    internal fun addTiles(){


        tileGroupMap = TileGroupMap(
            daTileGroups)



        //   var monster = Monster(40, "Crossbowman")
     //   monster.troops.forEachIndexed { index, troop -> troop.enterBattle(viewingHero.civInfo.gameInfo.civilizations.first(), index, attacker = false)}
        manager.defendingTroops.forEach { troop ->
            //        var troopTile = daTileGroups.first { HexMath.hexTranspose(HexMath.hex2EvenQCoords(it.tileInfo.position)) == troop.position }
            var troopTile = daTileGroups.first { HexMath.hex2EvenQCoords(it.tileInfo.position) == troop.position }
            //         var troopTile = daTileGroups.first { it.tileInfo.position == troop.position }
            troop.drawOnBattle(troopTile, attacker = false)
        }


        manager.attackingTroops.forEach { troop ->
            var troopTile = daTileGroups.first { HexMath.hex2EvenQCoords(it.tileInfo.position) == troop.position }
            troop.drawOnBattle(troopTile, attacker = true)
        }
   //     pointerImages =
  //              ImageGetter.getLayeredImageColored(poString, Color.valueOf("#00AAFF77"))
        pointerPosition = manager.sequence.first().position
        draw_pointer()

        for (tileGroup in daTileGroups)
        {
            tileGroup.onClick {
                tileGroupOnClick(tileGroup)
            }

            // Right mouse click listener
            tileGroup.addListener(object : ClickListener() {
                /*
                init {
                    button = Input.Buttons.RIGHT
                }

                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val unit = worldScreen.bottomUnitTable.selectedUnit
                        ?: return
                    Concurrency.run("WorldScreenClick") {
                        onTileRightClicked(unit, tileGroup.tileInfo)
                    }
                }

                 */

                override fun enter(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    fromActor: Actor?
                ) {
                    tileGroup.baseLayerGroup.color = Color(1f,1f,1f,0.5f)
                    super.enter(event, x, y, pointer, fromActor)
                    //var cursor = Cursor()
                    if(HexMath.getDistance(tileGroup.tileInfo.position, HexMath.evenQ2HexCoords(manager.currentTroop.position)) >= manager.currentTroop.baseUnit.speed)
                        Gdx.graphics.setSystemCursor(SystemCursor.NotAllowed)
                    else
                        Gdx.graphics.setSystemCursor(SystemCursor.Hand)




                    // Graphics.   .setCursor()
                }

                override fun exit(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    toActor: Actor?
                ) {
                    // TODO: This must be rewritten to avoid code doubling
                    if(HexMath.getDistance(tileGroup.tileInfo.position, HexMath.evenQ2HexCoords(manager.currentTroop.position)) >= manager.currentTroop.baseUnit.speed)
                        tileGroup.baseLayerGroup.color = Color(1f,1f,1f,1f)
                    else
                        tileGroup.baseLayerGroup.color = Color(1f,1f,1f,0.7f)

                    super.exit(event, x, y, pointer, toActor)
                }
            })

            //   tileGroup.name = "terrainHex"
         //   tileGroup.color = Color(1.0f, 1.0f, 1.0f, 0.3f)
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
        }

        tileGroupMap.setSize(stage.width, stage.height)
        stage.addActor(tileGroupMap)

    }

    private fun tileGroupOnClick(tileGroup: TileGroup)
    {
        if(tileGroup.findActor<Image>("troopImage") != null)
            return
        val position = HexMath.hex2EvenQCoords(tileGroup.tileInfo.position)
        // Here the value of 5 must be substituted to unit speed
        if(HexMath.getDistance(tileGroup.tileInfo.position, HexMath.evenQ2HexCoords(manager.currentTroop.position)) >= manager.currentTroop.baseUnit.speed)
            return
        manager.currentTroop.apply {
            this.troopGroup.findActor<Label>("hexCoordsLabel")?.setText(position.x.toString() + ", " + position.y.toString())
        }
        //tileGroup.showHighlight(Color.BLUE, 0.7f)

        tileGroup.addActor(manager.currentTroop.troopGroup)
       // tileGroup.showHighlight(Color.BLUE, 0.7f)

        tileGroup.update()
        manager.moveCurrentTroop(position)
        pointerPosition = manager.currentTroop.position
        draw_pointer()


    }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        return BattleScreen(manager, viewingHero, game.settings.lastOverviewPage)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }

    fun shutdownScreen()
    {
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        manager.finishBattle()
        game.popScreen()

    }
}
