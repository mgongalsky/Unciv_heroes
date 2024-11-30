package com.unciv.ui.battlescreen

import com.unciv.logic.battle.NewBattleManager

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector
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
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.map.TileGroupMap
import com.unciv.ui.overviewscreen.EmpireOverviewTab
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick

// Now it's just copied from HeroOverviewScreen
// All coordinates are hex, not offset

/** Screen for a battle. Visual routines are here, logic of the battle is in [BattleManager] class. Must not be used for AI duels. */
class NewBattleScreen(
    private var attackerHero: MapUnit,
    private var defenderHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    private var manager = NewBattleManager(attackerHero.army, defenderHero.army)

    // Array to store TroopArmyView or null for empty slots
    private val attackerTroopViewsArray: Array<TroopBattleView?> =
            Array(attackerHero.army.getAllTroops().size ?: 0) { null }
    private val defenderTroopViewsArray: Array<TroopBattleView?> =
            Array(defenderHero.army.getAllTroops().size ?: 0) { null }


    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    internal val centerAreaHeight = stage.height - 82f
    internal val BFwidth : Int = 14
    internal val BFheight : Int = 8
    private val battleField : TileMap = TileMap(BFwidth, BFheight,  game.gameInfo!!.ruleSet, attackerHero.currentTile.baseTerrain)
    val tileGroups = HashMap<TileInfo, List<TileGroup>>()
    private lateinit var tileGroupMap: TileGroupMap<TileGroup>
    private val allTileGroups = ArrayList<TileGroup>()
    /** Position of a pointer to a currently active troop. */
    lateinit var pointerPosition : Vector2
    var pointerImages : ArrayList<Image>
    var daTileGroups : List<TileGroup>
    val cursorMove : Cursor
    val cursorCancel : Cursor
    val cursorShoot : Cursor
    // val cursorQuestion : Cursor
    var cursorAttack : ArrayList<Cursor> = ArrayList()

    /** Handle for a table with a battlefield. Could be obsolete in future */
    private val tabbedPager: TabbedPager

    /** What to do if battlefield window is closing */
    override fun dispose() {
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {

        // Iterate through slots and create/update views
        attackerHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if(troop != null) {
                troop.enterBattle(attackerHero.civInfo, index, attacker = true, oldVersion = false)
                val troopView = TroopBattleView(troop, this) // Pass ArmyView for interaction
                attackerTroopViewsArray[index] = troopView // Save to array
                //add(troopView).size(64f).pad(5f)
            }
        }

        // Iterate through slots and create/update views
        defenderHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if(troop != null) {
                troop.enterBattle(defenderHero.civInfo, index, attacker = false, oldVersion = false)
                val troopView = TroopBattleView(troop, this) // Pass ArmyView for interaction
                defenderTroopViewsArray[index] = troopView // Save to array
                //add(troopView).size(64f).pad(5f)
            }
        }


        // Load cursor pixmaps
        cursorMove = loadCursor("BattleMoveCursor128.png", 32,64)
        cursorShoot = loadCursor("BattleArrowCursor128.png", 64,64)
        cursorCancel = loadCursor("BattleCancelCursor64.png", 32,32)
        //  cursorQuestion = loadCursor("BattleQuestionCursor128.png", 32,64)
        cursorAttack.add(loadCursor("BattleAttackCursor0.png", 64,64))
        cursorAttack.add(loadCursor("BattleAttackCursor1.png", 64,16))
        cursorAttack.add(loadCursor("BattleAttackCursor2.png", 64,64))
        cursorAttack.add(loadCursor("BattleAttackCursor3.png", 64,64))
        cursorAttack.add(loadCursor("BattleAttackCursor4.png", 64,16))
        cursorAttack.add(loadCursor("BattleAttackCursor5.png", 64,64))

        // TODO: Cursors are fixed-sized, what is not really good

        /*
        val page =
            if (defaultPage != "") {
                game.settings.lastOverviewPage = defaultPage
                defaultPage
            }
            else game.settings.lastOverviewPage
        val iconSize = Constants.defaultFontSize.toFloat()
        //battleField[1,1].
        val terraLayer = ArrayList<Group>()
        */

        // shotcut for exiting the battle
        globalShortcuts.add(KeyCharAndCode.BACK) { shutdownScreen() }

        // Loading all the assets into daTileGroups
        val tileSetStrings = TileSetStrings()
        daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }

        // Loading pixmap for the current troop pointer
        val pointerString = "TileSets/FantasyHex/Highlight"
        pointerImages =
                ImageGetter.getLayeredImageColored(pointerString, Color.valueOf("#00AAFF77"))

        // Routines with table containing battlefield. Copied.
        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE)

        // Button for exiting the battle
        tabbedPager.addClosePage {shutdownScreen()}

        // Add net of tiles of battlefield
        addTiles()

        stage.addActor(tabbedPager)

        val index = tabbedPager.addPage(
            caption = "Battle",
            content = tileGroupMap
        )
        tabbedPager.selectPage(index)

        tabbedPager.setFillParent(true)

    }

    fun loadCursor(filename: String, xHotspot: Int, yHotspot: Int) : Cursor{

        val texture = Texture("ExtraImages/" + filename)
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()
        return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)

    }

    /** Draw a pointer to a currently active troop. */
    fun draw_pointer() {
        // Find a tileGroup with specified pointer position
        var pointerTile = daTileGroups.first { it.tileInfo.position == pointerPosition }
        for (pointerImage in pointerImages) {
            // Note that here fixed sizes are used. Must be improved.
            pointerImage.setScale(pointerTile.width / 256f, pointerTile.width / 256f * 0.5f)
            pointerImage.setPosition(0f, pointerTile.height * 0.15f)

            pointerImage.setOrigin(pointerTile.originX, pointerTile.originY)
            pointerImage.touchable = Touchable.disabled
            pointerImage.name = "pointer"
            // Here we find an actor devoted to a troop and put a pointer underneath
            //val act = pointerTile.findActor("troopGroup")
            //if(pointerTile.findActor("troopGroup") == null)
            pointerTile.addActorBefore(pointerTile.findActor("troopGroup"), pointerImage)
        }

        // Now we highlight achievable hexes by transparency. First of all we make all hexes non-transparent.
        for (tileGroup in daTileGroups)
            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)
        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        // var achievableHexes = daTileGroups.filter { manager.isHexAchievable(it.tileInfo.position) }
        // for (achievableHex in achievableHexes)
        //       achievableHex.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
        //  }
    }
    /** creating a rectangular array of battlefield tiles */
    fun addTiles(){

        tileGroupMap = TileGroupMap(daTileGroups)

        // Draw attacking troops
        attackerTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                var troopTile =
                        daTileGroups.first { it.tileInfo.position == troopView.getBattlefieldPosition() }
                troopView.draw(troopTile, attacker = true)
            }
        }

        // Draw defending troops
        defenderTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                var troopTile =
                        daTileGroups.first { it.tileInfo.position == troopView.getBattlefieldPosition() }
                troopView.draw(troopTile, attacker = false)
            }
        }


        //  manager.attackingTroops.forEach { troop ->
      //      var troopTile = daTileGroups.first { it.tileInfo.position == troop.position }
     //       troop.drawOnBattle(troopTile, attacker = true)
     //   }


        /*
        // Draw defending troops
        manager.defendingTroops.forEach { troop ->
            var troopTile = daTileGroups.first { it.tileInfo.position == troop.position }
            troop.drawOnBattle(troopTile, attacker = false)
        }

        // Draw attacking troops
        manager.attackingTroops.forEach { troop ->
            var troopTile = daTileGroups.first { it.tileInfo.position == troop.position }
            troop.drawOnBattle(troopTile, attacker = true)
        }

        // Draw a pointer to currently active troop
        pointerPosition = manager.sequence.first().position
        draw_pointer

         */

        // Add various mouse listeners to each tile
        for (tileGroup in daTileGroups)
        {
            // Right mouse click listener
            tileGroup.addListener(object : ClickListener() {
                override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                    // TODO: it is better to use width directly from Hexagon actor rather than baseLayerGroup actors
                    //chooseCrosshair(tileGroup, x, y, tileGroup.baseLayerGroup.width)
                    return super.mouseMoved(event, x, y)
                }

                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    //tileGroupOnClick(tileGroup, x, y)
                }

                override fun enter(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    fromActor: Actor?
                ) {
                    // Highlight a tile as currently targeted by mouse pointer
                    tileGroup.baseLayerGroup.color = Color(1f,1f,1f,0.5f)

                    // Choose apropriate crosshair
                    if(fromActor != null) {
                        val width = fromActor.width
                        //chooseCrosshair(tileGroup, x, y, width)
                    }

                    super.enter(event, x, y, pointer, fromActor)
                }

                // Restore the tile after mouse pointer exited it
                override fun exit(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    toActor: Actor?
                ) {
                    // TODO: This must be rewritten to avoid code doubling
                    /*
                    if(manager.isHexAchievable(tileGroup.tileInfo.position))
                        tileGroup.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
                    else
                        tileGroup.baseLayerGroup.color = Color(1f,1f,1f,1f)

                    super.exit(event, x, y, pointer, toActor)

                     */
                }
            })

            allTileGroups.add(tileGroup)

            tileGroups[tileGroup.tileInfo] = listOf(tileGroup)
        }

        for (tileGroup in allTileGroups) {

            tileGroup.showEntireMap = true
            tileGroup.update()
        }

        tileGroupMap.setSize(stage.width, stage.height)
        stage.addActor(tileGroupMap)

    }

    fun movePointerToNextTroop() {
        //pointerPosition = manager.currentTroop.position
        //draw_pointer()
    }

    /** Routing for mouse clicking a certian tile. */
    private fun tileGroupOnClick(tileGroup: TileGroup, x: Float, y: Float)
    {
        val targetHex = tileGroup.tileInfo.position
        /*
        // If current troop can shoot:
        if(manager.currentTroop.baseUnit.rangedStrength != 0 &&
                manager.isTroopOnHex(targetHex) &&
                targetHex != manager.currentTroop.position &&
                manager.getTroopOnHex(targetHex).civInfo != manager.currentTroop.civInfo
        ){
            manager.attack(manager.getTroopOnHex(targetHex))
            // We need to check that we didn't kill the troop and it still exists
            refreshTargetTroop(targetHex)

            // After a shoot we turn to next troop and redraw the pointer
            tileGroup.update()
            manager.nextTurn()

            //movePointerToNextTroop()
            return
        }

        // for non-shooting troops:
        var hexToMove = Vector2(0f,0f)
        if(manager.isTroopOnHex(targetHex)) {
            if (manager.getTroopOnHex(targetHex).civInfo != manager.currentTroop.civInfo) {

                if (manager.isHexAchievable(targetHex)) {
                    // Determine the direction depending in which part of target hex mouse cursor is located
                    val direction = pixelToDirection(x, y, tileGroup.baseLayerGroup.width)
                    // TODO: remove code dubbing
                    // Specify the hex, where we have intention to go
                    hexToMove = HexMath.oneStepTowards(targetHex, direction)
                    if(!manager.isHexOnBattleField(hexToMove))
                        return

                    // We need to check several things:
                    if ((!manager.isTroopOnHex(hexToMove) || // Hex to move is free
                                    hexToMove == manager.currentTroop.position) && // that is not the same hex where we are
                            manager.isHexAchievable(hexToMove))  // hex is achievable
                    {
                        // Attack target
                        manager.attack(targetHex)
                        // Redraw the label with amount of units in the targeted troop
                        redrawMovedTroop(targetHex, hexToMove)

                        // Move current troop and redraw the pointer
                        manager.moveCurrentTroop(hexToMove)

                        //movePointerToNextTroop()
                    }
                }
            }
            return

        }

        if(!manager.isHexAchievable(targetHex))
            return
        // hexCoordsLabel is used for debug only and shows various coordinates and parameters
        manager.currentTroop.apply {
            this.troopGroup.findActor<Label>("hexCoordsLabel")?.setText(
                targetHex.x.toString() + ", " + targetHex.y.toString() + "\r\n" +
                        targetHex.x.toString() + ", " + targetHex.y.toString() + "\r\n" +
                        HexMath.getDistance(targetHex, Vector2(0f,0f)).toString()
            )

        }

        tileGroup.addActor(manager.currentTroop.troopGroup)
        tileGroup.update()

        // Move to next troop and redraw the pointer
        manager.moveCurrentTroop(targetHex)
        //movePointerToNextTroop()

         */
    }

    fun refreshTargetTroop(targetHex: Vector2) {

/*
        if (manager.isTroopOnHex(targetHex))
            manager.getTroopOnHex(targetHex).apply {
                this.troopGroup.findActor<Label>("amountLabel")
                    ?.setText(currentAmount.toString())
            }

        tileGroups[battleField[targetHex]]?.first { it.isTouchable }?.apply {
            this.update()
        }

 */
    }

    //fun
    fun redrawMovedTroop(targetHex: Vector2, hexToMove: Vector2) {

        /*
        val tileGroup = tileGroups[battleField[targetHex]]?.first { it.isTouchable }!!
        manager.currentTroop.apply {
            this.troopGroup.findActor<Label>("amountLabel")
                ?.setText(currentAmount.toString())
        }
        if (manager.isTroopOnHex(targetHex))
            manager.getTroopOnHex(targetHex).apply {
                this.troopGroup.findActor<Label>("amountLabel")
                    ?.setText(currentAmount.toString())
            }

        tileGroups[battleField[hexToMove]]?.first { it.isTouchable }?.apply {
            this.addActor(manager.currentTroop.troopGroup)
            this.update()
        }
        tileGroup.update()

         */
    }

    /** Routing for choose appropriate mouse cursor: for movement, attack, shooting and info. In other cases "cancel" cursor is shown */
    fun chooseCrosshair(tileGroup:TileGroup, x: Float, y: Float, width: Float)
    {
        /*
        // The code is similar to onClick routines. See details comments there.
        val targetHex = tileGroup.tileInfo.position

        // if current troop can shoot:
        if(manager.currentTroop.baseUnit.rangedStrength != 0 &&
                manager.isTroopOnHex(targetHex) &&
                targetHex != manager.currentTroop.position &&
                manager.getTroopOnHex(targetHex).civInfo != manager.currentTroop.civInfo
        ){
            Gdx.graphics.setCursor(cursorShoot)
            return
        }

        // for non-shooting troops:
        if(!manager.isHexAchievable(targetHex))
            Gdx.graphics.setCursor(cursorCancel)

        else {
            if(manager.isTroopOnHex(targetHex)){
                if (manager.getTroopOnHex(targetHex).civInfo != manager.currentTroop.civInfo) {

                    val direction = pixelToDirection(x, y, width)
                    val hexToMove = HexMath.oneStepTowards(targetHex, direction)
                    if(!manager.isHexOnBattleField(hexToMove)){
                        Gdx.graphics.setCursor(cursorCancel)
                        return
                    }
                    if ((!manager.isTroopOnHex(hexToMove) || hexToMove == manager.currentTroop.position) &&
                            manager.isHexAchievable(hexToMove))
                        Gdx.graphics.setCursor(cursorAttack[direction.num])
                    else
                        Gdx.graphics.setCursor(cursorCancel)
                }
                else
                    Gdx.graphics.setCursor(cursorCancel)  /// TODO: change to question

                return
            }

            Gdx.graphics.setCursor(cursorMove)
        }


         */
    }

    /** Determine direction of the supposed attack by position of the mouse pointer */
    fun pixelToDirection(x: Float, y: Float, width: Float): Direction{
        // width of the hex

        val x0 = width/2
        val height = width * 1.1547f
        val y0 = x0 * 0.577f // tangents of 30 degrees

        // Here we divide the hex with defender into 6 triangles in order to show from which adjacent hex attack will be mad
        // We have three diagonal lines intersecting at the center of the hex:

        when{
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 &&
                    x <= x0
            -> return Direction.TopLeft

            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 &&
                    y - y0 - x * y0 / x0 >= 0
            -> return Direction.CenterLeft

            y - y0 - x * y0 / x0 < 0 &&
                    x <= x0
            -> return Direction.BottomLeft

            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 &&
                    x > x0
            -> return Direction.BottomRight

            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 &&
                    y - y0 - x * y0 / x0 < 0
            -> return Direction.CenterRight

            y - y0 - x * y0 / x0 >= 0 &&
                    x > x0
            -> return Direction.TopRight
        }
        return Direction.DirError

    }
    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    override fun recreate(): BaseScreen {
        return NewBattleScreen(attackerHero, defenderHero)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }

    internal fun shutdownScreen(calledFromManager: Boolean = false)
    {
        // Change cursor to arrow, default for map view.
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        // Take into account that this function can be called from BattleManager. In that case we need just to close screen.
        /*
        if(!calledFromManager)
            manager.finishBattle()
        else
            game.popScreen()

         */
        game.popScreen()

    }
}

