package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.Constants
import com.unciv.logic.HexMath
import com.unciv.logic.hero.Monster
import com.unciv.logic.hero.Troop
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

        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE)


        tabbedPager.addClosePage { manager.finishBattle(); game.popScreen() }

        addTiles()

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

     //   var monster = Monster(40, "Crossbowman")
     //   monster.troops.forEachIndexed { index, troop -> troop.enterBattle(viewingHero.civInfo.gameInfo.civilizations.first(), index, attacker = false)}
        manager.defendingTroops!!.forEach { troop ->
            //        var troopTile = daTileGroups.first { HexMath.hexTranspose(HexMath.hex2EvenQCoords(it.tileInfo.position)) == troop.position }
            var troopTile = daTileGroups.first { HexMath.hex2EvenQCoords(it.tileInfo.position) == troop.position }
            //         var troopTile = daTileGroups.first { it.tileInfo.position == troop.position }
            troop.drawOnBattle(troopTile, attacker = false)
        }


        manager.attackingTroops!!.forEach { troop ->
            var troopTile = daTileGroups.first { HexMath.hex2EvenQCoords(it.tileInfo.position) == troop.position }
            troop.drawOnBattle(troopTile, attacker = true)
        }

        for (tileGroup in daTileGroups)
        {
            tileGroup.onClick {
                tileGroupOnClick(tileGroup)
            }

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
        manager.moveCurrentTroop(position)
        manager.currentTroop?.apply {
            this.troopGroup.findActor<Label>("hexCoordsLabel")?.setText(position.x.toString() + ", " + position.y.toString())
        }

        tileGroup.addActor(manager.currentTroop?.troopGroup)

        tileGroup.update()

    }

    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        return BattleScreen(manager, viewingHero, game.settings.lastOverviewPage)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }
}
