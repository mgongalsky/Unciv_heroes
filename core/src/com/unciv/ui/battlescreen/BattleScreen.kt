package com.unciv.ui.battlescreen

import BattleActionResult
import ErrorId
import com.unciv.logic.battle.BattleManager

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.badlogic.gdx.graphics.Texture
import com.unciv.logic.Direction
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.unciv.ai.AIBattle
import com.unciv.logic.HexMath
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.ICombatant
import com.unciv.logic.battle.MapUnitCombatant
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Now it's just copied from HeroOverviewScreen
// All coordinates are hex, not offset

/**
 * Screen for a battle.
 * This class handles the visual representation of the battle and user interactions.
 * The battle logic itself is managed by the [BattleManager] class.
 * Should not be used for AI duels as it includes player interaction logic.
 */
class BattleScreen(
    private var attacker: ICombatant,
    private var defender: ICombatant,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize{

    private var attackerArmy = when (val a = attacker) {
        is MapUnitCombatant -> a.unit.army // Явно создаем локальную переменную `a` для проверки типа
        is CityCombatant -> a.city.garrisonInfo
        else -> throw IllegalArgumentException("Unsupported attacker type")
    }

    private var defenderArmy = when (val d = defender) {
        is MapUnitCombatant -> d.unit.army // Если защитник - MapUnitCombatant, берем армию юнита
        is CityCombatant -> d.city.garrisonInfo // Если защитник - CityCombatant, берем информацию о гарнизоне
        else -> throw IllegalArgumentException("Unsupported defender type")
    }

    private var attackerCiv = when (val a = attacker) {
        is MapUnitCombatant -> a.unit.civInfo // Явно создаем локальную переменную `a` для проверки типа
        is CityCombatant -> a.city.civInfo
        else -> throw IllegalArgumentException("Unsupported attacker type")
    }

    private var defenderCiv = when (val d = defender) {
        is MapUnitCombatant -> d.unit.civInfo // Если защитник - MapUnitCombatant, берем армию юнита
        is CityCombatant -> d.city.civInfo // Если защитник - CityCombatant, берем информацию о гарнизоне
        else -> throw IllegalArgumentException("Unsupported defender type")
    }


    private var manager = BattleManager(attackerArmy, defenderArmy)

    // Arrays to store visual representations of troops for attackers and defenders
    private val attackerTroopViewsArray: Array<TroopBattleView?> =
            Array(attackerArmy.getAllTroops().size ?: 0) { null }
    private val defenderTroopViewsArray: Array<TroopBattleView?> =
            Array(defenderArmy.getAllTroops().size ?: 0) { null }

    private val verboseTurn = false // Toggle for detailed logging of turns

    // Constants for the center area and battlefield dimensions
    internal val centerAreaHeight = stage.height - 82f
    internal val BFwidth: Int = 14
    internal val BFheight: Int = 8

    private val defenderTile = when (val d = defender) {
        is MapUnitCombatant -> d.unit.currentTile // Если защитник - MapUnitCombatant, берем армию юнита
        is CityCombatant -> d.city.getCenterTile() // Если защитник - CityCombatant, берем информацию о гарнизоне
        else -> throw IllegalArgumentException("Unsupported defender type")
    }

    // TileMap represents the battlefield layout
    private val battleField: TileMap = TileMap(
        BFwidth, BFheight,
        game.gameInfo!!.ruleSet, defenderTile.baseTerrain
    )

    // Holds the mapping of tiles to their groups for visual representation
    val tileGroups = HashMap<TileInfo, List<TileGroup>>()

    private lateinit var tileGroupMap: TileGroupMap<TileGroup>
    private val allTileGroups = ArrayList<TileGroup>()

    /** Position of a pointer to the currently active troop */
    lateinit var pointerPosition: Vector2

    // List of images for the pointer representation
    var pointerImages: ArrayList<Image>
    var daTileGroups: List<TileGroup>

    // Different cursors for various actions
    val cursorMove: Cursor
    val cursorCancel: Cursor
    val cursorShoot: Cursor
    var cursorAttack: ArrayList<Cursor> = ArrayList()

    // External images for visual effects (e.g., morale and luck indicators)
    val luckRainbowImage = ImageGetter.getExternalImage("LuckRainbow.png")
    val moraleImage = ImageGetter.getExternalImage("MoraleBird.png")

    /** Container for the battlefield UI elements. Could be reworked in the future */
    private val tabbedPager: TabbedPager

    /** What to do if battlefield window is closing */
    override fun dispose() {
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {
        manager.initializeTurnQueue()

        attackerArmy.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) {
                troop.enterBattle(attackerCiv, index, attacker = true, battleField)
                val troopView = TroopBattleView(troop, this)
                attackerTroopViewsArray[index] = troopView
            }
        }

        defenderArmy.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) {
                troop.enterBattle(defenderCiv, index, attacker = false, battleField)
                val troopView = TroopBattleView(troop, this)
                defenderTroopViewsArray[index] = troopView
            }
        }

        cursorMove = loadCursor("BattleMoveCursor128.png", 32, 64)
        cursorShoot = loadCursor("BattleArrowCursor128.png", 64, 64)
        cursorCancel = loadCursor("BattleCancelCursor64.png", 32, 32)
        cursorAttack.add(loadCursor("BattleAttackCursor0.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor1.png", 64, 16))
        cursorAttack.add(loadCursor("BattleAttackCursor2.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor3.png", 64, 64))
        cursorAttack.add(loadCursor("BattleAttackCursor4.png", 64, 16))
        cursorAttack.add(loadCursor("BattleAttackCursor5.png", 64, 64))

        // Shortcut for exiting the battle screen
        globalShortcuts.add(KeyCharAndCode.BACK) { shutdownScreen() }
        // Instead of directly calling skipTurn(), send a SKIP action request to the manager
        globalShortcuts.add(KeyCharAndCode.SPACE) { sendSkipTurnRequest() }

        val tileSetStrings = TileSetStrings()
        daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }

        val pointerString = "TileSets/FantasyHex/Highlight"
        pointerImages = ImageGetter.getLayeredImageColored(pointerString, Color.valueOf("#00AAFF77"))

        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE
        )

        tabbedPager.addClosePage { shutdownScreen() }

        addTiles()

        stage.addActor(tabbedPager)

        val index = tabbedPager.addPage(
            caption = "Battle",
            content = tileGroupMap
        )
        tabbedPager.selectPage(index)

        tabbedPager.setFillParent(true)
        updateTilesShadowing()

        GlobalScope.launch {
            runBattleLoop()
        }
    }

    /**
     * Sends a skip-turn request to the BattleManager.
     *
     * This function creates a BattleActionRequest with action type SKIP using the current troop
     * and its current position as the target. It then invokes onPlayerActionReceived to send the request.
     */
    private fun sendSkipTurnRequest() {
        val currentTroop = manager.getCurrentTroop() ?: return
        // Create a SKIP action request using current troop and its current position
        val skipRequest = BattleActionRequest(
            troop = currentTroop,
            targetPosition = currentTroop.position,
            actionType = ActionType.SKIP
        )
        // Find the TileGroup corresponding to the current troop's position
        val targetTileGroup = daTileGroups.firstOrNull { it.tileInfo.position == currentTroop.position }
        if (targetTileGroup == null) {
            println("Error: No tile group found for current troop's position.")
            return
        }
        // Send the skip request via the player action callback
        onPlayerActionReceived?.invoke(Pair(skipRequest, targetTileGroup))
    }

    /**
     * Loads a custom cursor from a file.
     *
     * @param filename Name of the cursor image file.
     * @param xHotspot X-coordinate of the cursor's hotspot.
     * @param yHotspot Y-coordinate of the cursor's hotspot.
     * @return A [Cursor] object for the specified image.
     */
    fun loadCursor(filename: String, xHotspot: Int, yHotspot: Int) : Cursor{

        val texture = Texture("ExtraImages/" + filename)
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()
        return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)

    }

    /**
     * The main battle loop. Runs until the battle ends.
     */
    suspend fun runBattleLoop() = coroutineScope {
        while (manager.isBattleOn()) {
            val currentTroop = manager.getCurrentTroop()
            if (currentTroop == null) {
                Gdx.app.postRunnable { shutdownScreen() }
                return@coroutineScope
            }
            if (verboseTurn) println("Current troop: ${currentTroop.baseUnit.name} at position ${currentTroop.position}")

            var result: BattleActionResult? = null

            if (currentTroop.isPlayerControlled()) {
                while (true) {
                    if (verboseTurn) println("Waiting for player action...")
                    val (action, targetTileGroup) = waitForPlayerAction()
                    if (verboseTurn) {
                        println("Received action: ${action.actionType} targeting ${action.targetPosition}")
                        if (action.actionType == ActionType.ATTACK) {
                            println("Direction for attack: ${action.direction}")
                        }
                    }
                    result = manager.performTurn(action)
                    handleBattleResult(result, currentTroop)
                    if (result.success) break
                }
            } else {
                if (verboseTurn) println("AI is performing action for troop: ${currentTroop.baseUnit.name}")
                val aiBattle = AIBattle(manager)
                result = aiBattle.performTurn(currentTroop)
                handleBattleResult(result, currentTroop)
            }

            if (result != null && !result.isMorale) {
                manager.advanceTurn()
                if (verboseTurn) println("Turn advanced to next troop")
            } else if (verboseTurn) {
                println("Current troop has morale and gets an extra turn")
            }

            movePointerToNextTroop()
            updateTilesShadowing()
        }
        manager.finishBattle()
        println("Battle has ended!")
    }

    /**
     * Processes the result of a battle action.
     *
     * @param result The result of the action.
     * @param currentTroop The troop that performed the action.
     */
    private fun handleBattleResult(
        result: BattleActionResult,
        currentTroop: TroopInfo
    ) {
        if (result.success) {
            if (verboseTurn) {
                println("Action ${result.actionType} succeeded")
                println("Moved from: ${result.movedFrom}, Moved to: ${result.movedTo}")
            }

            val targetTileGroup = daTileGroups.firstOrNull(){it.tileInfo.position == currentTroop.position}

            // Handle specific action types
            when (result.actionType) {
                ActionType.SKIP -> {
                    // Add action here if necessary
                }
                ActionType.ATTACK -> {
                    if (result.isLuck) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showLuckRainbow(it) }
                    }


                    if (result.movedTo != null) {
                        val attackingTroopView = getTroopViewFor(currentTroop)
                        val attackPosition = result.movedTo
                        val attackTileGroup = daTileGroups.firstOrNull { it.tileInfo.position == attackPosition }

                        if (attackTileGroup != null && attackingTroopView != null) {
                            attackingTroopView.updatePosition(attackTileGroup)
                            if (verboseTurn) println("Updated attacking troop view to position $attackPosition")
                        }
                    }

                    refreshTroopViews()


                    if (result.isMorale && manager.isBattleOn()) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showMoraleBird(it) }
                    }
                }

                ActionType.SHOOT -> {
                    if (result.isLuck) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showLuckRainbow(it) }
                    }
                    refreshTroopViews()

                    if (result.isMorale && manager.isBattleOn()) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showMoraleBird(it) }
                    }
                }

                ActionType.MOVE -> {
                    val currentTroopView = getTroopViewFor(currentTroop)
                    currentTroopView?.updatePosition(targetTileGroup)
                    if (verboseTurn) println("Moved troop view to ${targetTileGroup?.tileInfo?.position}")

                    refreshTroopViews()


                    if (result.isMorale && manager.isBattleOn()) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showMoraleBird(it) }
                    }
                }
            }

            // Handle end of the battle
            if (result.battleEnded) {
                if (verboseTurn) println("Battle finished after action ${result.actionType}. Closing screen.")
                Gdx.app.postRunnable {
                    shutdownScreen() // Закрываем экран в UI-потоке
                }
                manager.finishBattle()
                val battleResult = manager.getBattleResult()
                if (battleResult == null){
                    println("Bug with battle result.")
                } else {
                    if (verboseTurn) println("Army of ${battleResult.winningArmy.civInfo.nation.name} won.")

                    // Remove defeated unit from map
                    if (attackerArmy == battleResult.winningArmy) {
                        val d = defender
                        if (d is MapUnitCombatant) {
                            val defenderHero = d.unit
                            defenderHero.removeFromTile()
                            defenderHero.civInfo.removeUnit(defenderHero)
                            defenderHero.civInfo.updateViewableTiles()
                        }
                        if (d is CityCombatant) {
                            val defenderCity = d.city
                            defenderCity.moveToCiv(attackerCiv)
                        }

                    }
                    if (defenderArmy == battleResult.winningArmy) {
                        val a = attacker
                        if (a is MapUnitCombatant) {
                            val attackerHero = a.unit
                            attackerHero.removeFromTile()
                            attackerHero.civInfo.removeUnit(attackerHero)
                            attackerHero.civInfo.updateViewableTiles()
                        }
                    }

                }
            }
        } else {
            if (verboseTurn) println("Action ${result.actionType} failed with error: ${result.errorId}")
            handleActionError(result.errorId)
        }
    }


    /**
     * Refreshes the arrays of troop views for both attackers and defenders
     * based on the current state of the armies in the manager.
     */
    fun refreshTroopViews() {
        Gdx.app.postRunnable {
            // Убедитесь, что операции обновления происходят в графическом потоке
            for (i in attackerTroopViewsArray.indices) {
                val troop = manager.getAttackerArmy().getTroopAt(i)
                if (troop == null) {
                    attackerTroopViewsArray[i]?.perish()
                    attackerTroopViewsArray[i] = null
                } else {
                    attackerTroopViewsArray[i]?.updateStats()
                }
            }

            for (i in defenderTroopViewsArray.indices) {
                if (i < defenderArmy.maxSlots) {
                    val troop = manager.getDefenderArmy().getTroopAt(i)
                    if (troop == null) {
                        defenderTroopViewsArray[i]?.perish()
                        defenderTroopViewsArray[i] = null
                    } else {
                        defenderTroopViewsArray[i]?.updateStats()
                    }
                }
            }
        }

    }

    /**
     * Finds the TroopBattleView corresponding to the given TroopInfo.
     *
     * @param troop The TroopInfo for which the view is needed.
     * @return The corresponding TroopBattleView, or null if not found.
     */
    fun getTroopViewFor(troop: TroopInfo): TroopBattleView? {
        // Search in the attacker's troop views
        attackerTroopViewsArray.forEach { troopView ->
            if (troopView?.getTroopInfo() == troop) {
                return troopView
            }
        }

        // Search in the defender's troop views
        defenderTroopViewsArray.forEach { troopView ->
            if (troopView?.getTroopInfo() == troop) {
                return troopView
            }
        }

        return null // Not found
    }

    /**
     * Waits for the player's action input.
     *
     * @return A pair of BattleActionRequest and TileGroup representing the action and its target.
     */
    suspend fun waitForPlayerAction(): Pair<BattleActionRequest, TileGroup> {
        return suspendCancellableCoroutine { continuation ->
            onPlayerActionReceived = { actionAndTileGroup ->
                continuation.resume(actionAndTileGroup) // Возвращаем кортеж
            }

            continuation.invokeOnCancellation {
                onPlayerActionReceived = null
            }
        }
    }

    private var onPlayerActionReceived: ((Pair<BattleActionRequest, TileGroup>) -> Unit)? = null

    /**
     * Handles player move action to a specified tile group.
     *
     * @param tileGroup The target tile group.
     */
    private fun handleActionError(errorId: ErrorId?) {
        when (errorId) {
            ErrorId.TOO_FAR -> showError("Target is too far away!")
            ErrorId.OCCUPIED_BY_ALLY -> showError("Target tile is occupied by an ally!")
            ErrorId.NOT_IMPLEMENTED -> showError("This action is not implemented yet!")
            ErrorId.INVALID_TARGET -> showError("Invalid target!")
            else -> showError("An unknown error occurred!")
        }
    }

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    private fun showError(message: String) {
        // Простое сообщение в консоль для отладки
        println("Error: $message")

        // Not tested below:
        /*
        // Отобразим сообщение игроку, например, через всплывающее окно
        val errorLabel = Label(message, BaseScreen.skin).apply {
            color = Color.RED
            setFontScale(1.2f)
            setPosition(stage.width / 2 - width / 2, stage.height - 100f)
        }

        stage.addActor(errorLabel)

        // Удалим сообщение через 3 секунды
        errorLabel.addAction(
            Actions.sequence(
            Actions.delay(3f),
            Actions.fadeOut(0.5f),
            Actions.removeActor()
        ))

         */
    }



    /**
     * Draws a pointer to the currently active troop.
     */
    fun draw_pointer() {
        // Find a tileGroup with specified pointer position
        val pointerTile = daTileGroups.firstOrNull { it.tileInfo.position == pointerPosition }
        if (pointerTile == null) {
            println("Error: No tile found at position $pointerPosition during drawing a pointer")
            return // Или другая логика для обработки отсутствия тайла
        }
        for (pointerImage in pointerImages) {
            // Note that here fixed sizes are used. Must be improved.
            pointerImage.setScale(pointerTile.width / 256f, pointerTile.width / 256f * 0.5f)
            pointerImage.setPosition(0f, pointerTile.height * 0.15f)

            pointerImage.setOrigin(pointerTile.originX, pointerTile.originY)
            pointerImage.touchable = Touchable.disabled
            pointerImage.name = "pointer"

            // Set the pointer image color to white (but it is black in fact)
            pointerImage.color = Color.WHITE


            // Here we find an actor devoted to a troop and put a pointer underneath
            //val act = pointerTile.findActor("troopGroup")
            //if(pointerTile.findActor("troopGroup") == null)
            val troopGroupActor = pointerTile.findActor<Group>("troopGroup")
            if (troopGroupActor != null) {
                pointerTile.addActorBefore(troopGroupActor, pointerImage)
            } else {
                println("Warning: troopGroup not found in pointerTile. Adding pointerImage directly.")
                pointerTile.addActor(pointerImage)
            }

            //pointerTile.addActorBefore(pointerTile.findActor("troopGroup"), pointerImage)
        }

        // Now we highlight achievable hexes by transparency. First of all we make all hexes non-transparent.
        //for (tileGroup in daTileGroups)
        //    tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)
        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        // var achievableHexes = daTileGroups.filter { manager.isHexAchievable(it.tileInfo.position) }
        // for (achievableHex in achievableHexes)
        //       achievableHex.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
        //  }
    }

    /**
     * Adds tiles to create a rectangular array of battlefield tiles.
     */
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



        // Set pointer to first troop
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop == null) {
            println("No troops in both armies at the beggining of the battle")
            shutdownScreen()
            return
        }
        pointerPosition = currentTroop.position
        draw_pointer()

        // Add various mouse listeners to each tile
        for (tileGroup in daTileGroups)
        {
            // Right mouse click listener
            tileGroup.addListener(object : ClickListener() {
                override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                    if(manager.isBattleOn()) {
                        // TODO: it is better to use width directly from Hexagon actor rather than baseLayerGroup actors
                        chooseCrosshair(tileGroup, x, y, tileGroup.baseLayerGroup.width)
                    }
                        return super.mouseMoved(event, x, y)
                }

                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if(manager.isBattleOn()) {
                        // Выводим координаты тайла в консоль
                        println("Tile clicked: position=${tileGroup.tileInfo.position}")

                        // Или через логгирование GDX
                        Gdx.app.log(
                            "TileClick",
                            "Tile clicked at position=${tileGroup.tileInfo.position}"
                        )

                        handleTileClick(tileGroup, x, y) // Обрабатываем клик на тайл
                    }
                }

                override fun enter(
                    event: InputEvent?,
                    x: Float,
                    y: Float,
                    pointer: Int,
                    fromActor: Actor?
                ) {
                    if(manager.isBattleOn()) {
                        // Highlight a tile as currently targeted by mouse pointer
                        tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.5f)

                        // Choose apropriate crosshair
                        if (fromActor != null) {
                            val width = fromActor.width
                            chooseCrosshair(tileGroup, x, y, width)
                        }

                        super.enter(event, x, y, pointer, fromActor)
                    }
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
                    if(manager.isBattleOn()) {
                        val currentTroopNew = manager.getCurrentTroop()

                        val currentTroopOnTile = getCurrentTroopView() ?: return
                        if (currentTroopNew != null && currentTroopNew.movement.getReachableTilesInCurrentTurn().contains(tileGroup.tileInfo))
                        /*
                        if (manager.isHexAchievable(
                                    currentTroopOnTile.getTroopInfo(),
                                    tileGroup.tileInfo.position
                                )
                        )

                         */
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.7f)
                        else
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)

                        super.exit(event, x, y, pointer, toActor)
                    }


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

    /**
     * Handles tile click events.
     *
     * @param tileGroup The clicked tile group.
     * @param x The x-coordinate of the click within the tile group.
     * @param y The y-coordinate of the click within the tile group.
     */
    private fun handleTileClick(tileGroup: TileGroup, x: Float, y: Float) {
        if (onPlayerActionReceived == null) {
            println("Player action is not expected at the moment.")
            return
        }

        val currentTroopView = getCurrentTroopView()
        if (currentTroopView == null) {
            println("Error: No current troop selected for player action.")
            return
        }

        val currentTroop = currentTroopView.getTroopInfo()
        val targetPosition = tileGroup.tileInfo.position

        // Проверяем, можно ли стрелять
        if (manager.canShoot(currentTroop) && manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
            val actionRequest = BattleActionRequest(
                troop = currentTroop,
                targetPosition = targetPosition,
                actionType = ActionType.SHOOT
            )
            onPlayerActionReceived?.invoke(Pair(actionRequest, tileGroup))
            return
        }

        if (!manager.isHexAchievable(currentTroop, targetPosition))
            return

        // Проверяем, можем ли атаковать
        if (manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
            // Вычисляем направление атаки
            val direction = pixelToDirection(x, y, tileGroup.baseLayerGroup.width)

            // Создаём запрос атаки
            val attackRequest = BattleActionRequest(
                troop = currentTroop,
                targetPosition = targetPosition,
                actionType = ActionType.ATTACK,
                direction = direction
            )

            // Передаём запрос
            onPlayerActionReceived?.invoke(Pair(attackRequest, tileGroup))
        } else {
            // Иначе создаём запрос перемещения
            val moveRequest = BattleActionRequest(
                troop = currentTroop,
                targetPosition = targetPosition,
                actionType = ActionType.MOVE
            )

            onPlayerActionReceived?.invoke(Pair(moveRequest, tileGroup))
        }
    }

    /**
     * Returns the current troop view based on the troop in the manager's queue.
     *
     * @return The view for the current troop, or null if not found.
     */
    fun getCurrentTroopView(): TroopBattleView? {
        val currentTroop = manager.getCurrentTroop()
        return attackerTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
            ?: defenderTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
    }

    /**
     * Updates tile shadowing based on the achievable positions for the current troop.
     */
    private fun updateTilesShadowing(){
        //val currentTroop = getCurrentTroopView() ?: return
        val currentTroop = manager.getCurrentTroop()


        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        daTileGroups.forEach {
            if (currentTroop != null && currentTroop.movement.getReachableTilesInCurrentTurn().contains(it.tileInfo))

            //if (manager.isHexAchievable(currentTroop.getTroopInfo(), it.tileInfo.position))
                it.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
            else
                it.baseLayerGroup.color = Color(1f,1f,1f,1f)


        }
    }

    /**
     * Moves the pointer to the next troop in the turn queue.
     */
    fun movePointerToNextTroop() {
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop != null){
            pointerPosition = currentTroop.position
            draw_pointer()
        } else
            println("Queue is empty, nowhere to put pointer")
    }

    /**
     * Displays a morale bird animation above the specified troop view.
     *
     * @param troopView The view of the troop to display the morale bird for.
     */
    fun showMoraleBird(troopView: TroopBattleView) {
        val troopGroup = troopView.getCurrentGroup()

        // Ищем потомка с именем "troopImage"
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        // Учитываем размеры и масштаб "troopImage"
        val troopWidth = troopImageActor.width * troopImageActor.scaleX
        val troopHeight = troopImageActor.height * troopImageActor.scaleY

        // Создаем изображение птицы морали
        moraleImage.apply {
            setScale(0.075f) // Настраиваем масштаб
            // TODO: those numbers are magical, we need to apply normal object hierarchy

            setPosition(
                troopWidth * -0.035f,  // Сдвиг влево
                troopHeight * 1.10f    // Сдвиг вверх
            )
            touchable = Touchable.disabled // Птица не кликабельна
            color = Color.WHITE.cpy().apply { a = 0f } // Начальная прозрачность
            name = "moraleBirdImage" // Уникальное имя для отладки
        }

        // Анимация: появление -> задержка -> исчезновение
        val fadeIn = Actions.alpha(1f, 0.5f)  // Плавное появление (0.5 секунды)
        val delay = Actions.delay(0.3f)       // Задержка (0.3 секунды)
        val fadeOut = Actions.alpha(0f, 0.5f) // Плавное исчезновение (0.5 секунды)
        val removeActor = Actions.run {
            moraleImage.remove() // Удаляем изображение после завершения анимации
        }

        // Привязываем последовательность действий к изображению
        moraleImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, removeActor))

        // Добавляем изображение в группу отряда
        troopGroup.addActor(moraleImage)
    }

    /**
     * Displays a luck rainbow animation above the specified troop view.
     *
     * @param troopView The view of the troop to display the luck rainbow for.
     */
    fun showLuckRainbow(troopView: TroopBattleView) {
        val troopGroup = troopView.getCurrentGroup()

        // Ищем потомка с именем "troopImage"
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        // Учитываем размеры и масштаб "troopImage"
        val troopWidth = troopImageActor.width * troopImageActor.scaleX
        val troopHeight = troopImageActor.height * troopImageActor.scaleY

        // Создаем изображение радуги удачи
        luckRainbowImage.apply {
            val scaleRainbowImage = 0.130f
            setScale(scaleRainbowImage) // Настраиваем масштаб
            //setOrigin(Align.center)
            align = Align.topRight
            // TODO: those numbers are magical, we need to apply normal object hierarchy
            setPosition(-width*scaleRainbowImage*0.2f,height*scaleRainbowImage*0.4f)
            //setPosition(
            //    troopWidth * -0.25f,  // Сдвиг влево
            //    troopHeight * 0.30f    // Сдвиг вверх
            //)

            //setPosition(0f, 0f, Align.center)
            //setPosition(
             //   troopWidth * 0f - width/2/0.13f,  // Сдвиг влево
            //    troopHeight * 0.55f  // Сдвиг вверх
            //)
            touchable = Touchable.disabled // Радуга не кликабельна
            color = Color.WHITE.cpy().apply { a = 0f } // Начальная прозрачность
            name = "luckRainbowImage" // Уникальное имя для отладки
        }

        // Анимация: появление -> задержка -> исчезновение
        val fadeIn = Actions.alpha(1f, 0.5f)  // Плавное появление (0.5 секунды)
        val delay = Actions.delay(0.3f)       // Задержка (0.3 секунды)
        val fadeOut = Actions.alpha(0f, 0.5f) // Плавное исчезновение (0.5 секунды)
        val removeActor = Actions.run {
            luckRainbowImage.remove() // Удаляем изображение после завершения анимации
        }

        // Привязываем последовательность действий к изображению
        luckRainbowImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, removeActor))

        // Добавляем изображение в группу отряда
        troopGroup.addActor(luckRainbowImage)
    }

    fun refreshTargetTroop(targetHex: Vector2) {

    }

    /**
     * Selects the appropriate crosshair cursor based on the tile state.
     *
     * @param tileGroup The tile group the pointer is hovering over.
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     */
    fun chooseCrosshair(tileGroup:TileGroup, x: Float, y: Float, width: Float)
    {

        // The code is similar to onClick routines. See details comments there.
        val targetHex = tileGroup.tileInfo.position
        val currentTroop = getCurrentTroopView() ?: return

        if (    manager.canShoot(currentTroop.getTroopInfo()) &&
                manager.isHexOccupiedByEnemy(currentTroop.getTroopInfo(), targetHex)){
            Gdx.graphics.setCursor(cursorShoot)
            return
        }

        if (!currentTroop.getTroopInfo().isCurrentTileInitialized()){
            Gdx.graphics.setCursor(cursorCancel)
            return
        }

        // for non-shooting troops:
        if (!currentTroop.getTroopInfo().movement.getReachableTilesInCurrentTurn().contains(tileGroup.tileInfo)
                && manager.isHexFree(targetHex))
        //if (!manager.isHexAchievable(currentTroop.getTroopInfo(), targetHex))
            Gdx.graphics.setCursor(cursorCancel)
        else {
            if (manager.isHexOccupiedByAlly(currentTroop.getTroopInfo(), targetHex)) {
                Gdx.graphics.setCursor(cursorCancel) /// TODO: change to question
                return
            }

            if (manager.isHexOccupiedByEnemy(currentTroop.getTroopInfo(), targetHex)) {
                val direction = pixelToDirection(x, y, width)
                val hexToMove = HexMath.oneStepTowards(targetHex, direction)
                if(!manager.isHexOnBattleField(hexToMove)){
                    Gdx.graphics.setCursor(cursorCancel)
                    return
                }

                if (currentTroop.getTroopInfo().movement.getReachableTilesInCurrentTurn().contains(battleField[hexToMove])
                        && (manager.isHexFree(hexToMove) || hexToMove == currentTroop.getTroopInfo().position))
                        //if (manager.isHexAchievable(currentTroop.getTroopInfo(), hexToMove))
                    Gdx.graphics.setCursor(cursorAttack[direction.num])
                else
                    Gdx.graphics.setCursor(cursorCancel)
                return
            }

            Gdx.graphics.setCursor(cursorMove)
        }



    }

    /**
     * Determines the direction of the attack based on the mouse pointer position.
     *
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     * @return The direction of the attack.
     */
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

     /**
     * Resumes the screen. Used to replace the current screen after recreation.
     */
    override fun resume() {
        game.replaceCurrentScreen(recreate())
    }

    /**
     * Recreates the screen instance.
     *
     * @return A new instance of [BattleScreen].
     */
    override fun recreate(): BaseScreen {
        return BattleScreen(attacker, defender)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }

    /**
     * Shuts down the battle screen and performs cleanup.
     * Ensures the default system cursor is restored and the game screen stack is updated.
     */
    private fun shutdownScreen()
    {
        // Change cursor to arrow, default for map view.
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        game.popScreen()

    }
}

