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

/**
 * Screen for a battle.
 * This class handles the visual representation of the battle and user interactions.
 * The battle logic itself is managed by the [BattleManager] class.
 * Should not be used for AI duels as it includes player interaction logic.
 */
class BattleScreen(
    private var attackerHero: MapUnit,
    private var defenderHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize {
    // BattleManager handles the battle logic for both armies
    private var manager = BattleManager(attackerHero.army, defenderHero.army)

    // Arrays to store visual representations of troops for attackers and defenders
    private val attackerTroopViewsArray: Array<TroopBattleView?> =
            Array(attackerHero.army.getAllTroops().size ?: 0) { null }
    private val defenderTroopViewsArray: Array<TroopBattleView?> =
            Array(defenderHero.army.getAllTroops().size ?: 0) { null }

    private val verboseTurn = false // Toggle for detailed logging of turns

    // Constants for the center area and battlefield dimensions
    internal val centerAreaHeight = stage.height - 82f
    internal val BFwidth: Int = 14
    internal val BFheight: Int = 8

    // TileMap represents the battlefield layout
    private val battleField: TileMap = TileMap(
        BFwidth, BFheight,
        game.gameInfo!!.ruleSet, attackerHero.currentTile.baseTerrain
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


    /** What to do if the battlefield window is closing */
    override fun dispose() {
        tabbedPager.selectPage(-1) // Deselect the current tab
        super.dispose() // Call the base class's dispose method
    }

    init {
        // Initialize the turn queue for the battle
        manager.initializeTurnQueue()

        // Create visual representations for the attacker's troops
        attackerHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) {
                troop.enterBattle(attackerHero.civInfo, index, attacker = true)
                val troopView = TroopBattleView(troop, this) // Pass BattleScreen for interaction
                attackerTroopViewsArray[index] = troopView // Save to array
            }
        }

        // Create visual representations for the defender's troops
        defenderHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if (troop != null) {
                troop.enterBattle(defenderHero.civInfo, index, attacker = false)
                val troopView = TroopBattleView(troop, this) // Pass BattleScreen for interaction
                defenderTroopViewsArray[index] = troopView // Save to array
            }
        }

        // Load custom cursors for the battle actions
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

        // Initialize tile groups for the battlefield
        val tileSetStrings = TileSetStrings()
        daTileGroups = battleField.values.map { TileGroup(it, tileSetStrings) }

        // Load images for troop pointers
        val pointerString = "TileSets/FantasyHex/Highlight"
        pointerImages = ImageGetter.getLayeredImageColored(pointerString, Color.valueOf("#00AAFF77"))

        // Set up the UI tabbed pager for the battlefield
        tabbedPager = TabbedPager(
            stage.width, stage.width,
            centerAreaHeight, centerAreaHeight,
            separatorColor = Color.WHITE
        )

        // Add a button for exiting the battle
        tabbedPager.addClosePage { shutdownScreen() }

        // Add the battlefield tiles
        addTiles()

        // Add the tabbed pager to the stage
        stage.addActor(tabbedPager)

        // Select the "Battle" tab as the default
        val index = tabbedPager.addPage(
            caption = "Battle",
            content = tileGroupMap
        )
        tabbedPager.selectPage(index)

        // Update the tile shadows for the battlefield
        tabbedPager.setFillParent(true)
        updateTilesShadowing()

        // Start the battle loop in a coroutine
        GlobalScope.launch {
            runBattleLoop()
        }
    }

    /**
     * Loads a custom cursor from a file.
     *
     * @param filename Name of the cursor image file.
     * @param xHotspot X-coordinate of the cursor's hotspot.
     * @param yHotspot Y-coordinate of the cursor's hotspot.
     * @return A [Cursor] object for the specified image.
     */
    fun loadCursor(filename: String, xHotspot: Int, yHotspot: Int): Cursor {
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
            var isMorale = false

            // Check if there are no more troops to process
            if (currentTroop == null) {
                Gdx.app.postRunnable { shutdownScreen() } // Close the battle screen in the UI thread
                return@coroutineScope
            }

            if (verboseTurn) {
                println("Current troop: ${currentTroop.baseUnit.name} at position ${currentTroop.position}")
            }

            // Handle player-controlled troops
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

                    val result = manager.performTurn(action)
                    handleBattleResult(result, currentTroop)
                    if (result.success) break
                }
            } else {
                // Handle AI-controlled troops
                if (verboseTurn) println("AI is performing action for troop: ${currentTroop.baseUnit.name}")
                val aiBattle = AIBattle(manager)
                val result = aiBattle.performTurn(currentTroop)
                handleBattleResult(result, currentTroop)
            }

            // Advance the turn queue if morale doesn't grant an extra turn
            if (!isMorale) {
                manager.advanceTurn()
                if (verboseTurn) println("Turn advanced to next troop")
            } else if (verboseTurn) println("Current troop has morale")

            movePointerToNextTroop()
            updateTilesShadowing()
        }

        // Handle battle end
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

            // Handle specific action types
            when (result.actionType) {
                ActionType.ATTACK -> {
                    if (result.isLuck) {
                        val troopView = getTroopViewFor(currentTroop)
                        troopView?.let { showLuckRainbow(it) }
                    }
                    if (result.movedTo != null) {
                        val attackingTroopView = getTroopViewFor(currentTroop)
                        val attackTileGroup = daTileGroups.firstOrNull { it.tileInfo.position == result.movedTo }
                        attackingTroopView?.updatePosition(attackTileGroup)
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
                    currentTroopView?.updatePosition(
                        daTileGroups.firstOrNull { it.tileInfo.position == result.movedTo }
                    )
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
                Gdx.app.postRunnable { shutdownScreen() }
                val battleResult = manager.getBattleResult()
                battleResult?.let {
                    println("Army of ${it.winningArmy.civInfo.nation.name} won.")

                    // Remove defeated unit from the map
                    if (attackerHero.army == it.winningArmy) {
                        defenderHero.removeFromTile()
                        defenderHero.civInfo.removeUnit(defenderHero)
                        defenderHero.civInfo.updateViewableTiles()
                    } else if (defenderHero.army == it.winningArmy) {
                        attackerHero.removeFromTile()
                        attackerHero.civInfo.removeUnit(attackerHero)
                        attackerHero.civInfo.updateViewableTiles()
                    }
                } ?: println("Error: No battle result available.")
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
            // Ensure that updates occur in the rendering thread
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
                if (i < defenderHero.army.maxSlots) {
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
                continuation.resume(actionAndTileGroup) // Resume coroutine with the action
            }

            continuation.invokeOnCancellation {
                onPlayerActionReceived = null // Clear the listener if coroutine is canceled
            }
        }
    }

    /**
     * Handles player move action to a specified tile group.
     *
     * @param tileGroup The target tile group.
     */
    fun handlePlayerMove(tileGroup: TileGroup) {
        val currentTroopView = getCurrentTroopView()

        if (currentTroopView == null) {
            println("Error: Current troop's view not found!")
            return
        }

        val targetPosition = tileGroup.tileInfo.position

        val actionRequest = BattleActionRequest(
            troop = currentTroopView.getTroopInfo(),
            targetPosition = targetPosition,
            actionType = ActionType.MOVE
        )

        onPlayerActionReceived?.invoke(Pair(actionRequest, tileGroup))
    }

    private var onPlayerActionReceived: ((Pair<BattleActionRequest, TileGroup>) -> Unit)? = null

    /**
     * Moves a troop view to the specified target tile group.
     *
     * @param troopView The TroopBattleView to move.
     * @param targetTileGroup The target TileGroup where the troop should be moved.
     */
    fun moveTroopView(troopView: TroopBattleView, targetTileGroup: TileGroup) {
        troopView.updatePosition(targetTileGroup = targetTileGroup)
    }

    /**
     * Handles an action error by displaying an appropriate message.
     *
     * @param errorId The ID of the error.
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
        println("Error: $message") // Simple debug message
        // TODO: Implement a proper UI for displaying error messages to the player
    }


    /**
     * Draws a pointer on the battlefield at the position of the currently active troop.
     * This function visually indicates the troop that is currently taking its turn.
     */
    fun draw_pointer() {
        // Find the TileGroup corresponding to the pointer's position.
        val pointerTile = daTileGroups.firstOrNull { it.tileInfo.position == pointerPosition }
        if (pointerTile == null) {
            // Log an error if no tile is found at the specified position.
            println("Error: No tile found at position $pointerPosition during drawing a pointer")
            return
        }

        // Iterate over all pointer images (used for highlighting the troop's position).
        for (pointerImage in pointerImages) {
            // Set the scale of the pointer image relative to the tile's dimensions.
            pointerImage.setScale(pointerTile.width / 256f, pointerTile.width / 256f * 0.5f)
            // Position the pointer image slightly above the base of the tile.
            pointerImage.setPosition(0f, pointerTile.height * 0.15f)

            // Set the origin of the pointer image to align it with the tile's origin.
            pointerImage.setOrigin(pointerTile.originX, pointerTile.originY)
            // Disable touch interactions with the pointer image.
            pointerImage.touchable = Touchable.disabled
            // Assign a name to the pointer image for easier debugging and identification.
            pointerImage.name = "pointer"

            // Set the color of the pointer image to white.
            pointerImage.color = Color.WHITE

            // Find the troop group actor on the current tile (if present).
            val troopGroupActor = pointerTile.findActor<Group>("troopGroup")
            if (troopGroupActor != null) {
                // Add the pointer image beneath the troop group actor for proper layering.
                pointerTile.addActorBefore(troopGroupActor, pointerImage)
            } else {
                // Log a warning if no troop group is found and add the pointer directly to the tile.
                println("Warning: troopGroup not found in pointerTile. Adding pointerImage directly.")
                pointerTile.addActor(pointerImage)
            }
        }
    }

    /**
     * Adds tiles to the battlefield and initializes troop views and interactions.
     * This function sets up the visual representation of the battlefield, places troops on their respective tiles,
     * and assigns event listeners for player interactions.
     */
    fun addTiles() {
        // Initialize the tile group map, which manages the battlefield's visual layout.
        tileGroupMap = TileGroupMap(daTileGroups)

        // Place attacking troops on the battlefield and draw their views on the respective tiles.
        attackerTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                val troopTile = daTileGroups.first { it.tileInfo.position == troopView.getBattlefieldPosition() }
                troopView.draw(troopTile, attacker = true)
            }
        }

        // Place defending troops on the battlefield and draw their views on the respective tiles.
        defenderTroopViewsArray.forEach { troopView ->
            if (troopView != null) {
                val troopTile = daTileGroups.first { it.tileInfo.position == troopView.getBattlefieldPosition() }
                troopView.draw(troopTile, attacker = false)
            }
        }

        // Set the pointer to the current troop's position and draw the pointer.
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop == null) {
            println("No troops in both armies at the beginning of the battle")
            shutdownScreen() // Exit if no troops are available.
            return
        }
        pointerPosition = currentTroop.position
        draw_pointer()

        // Add event listeners to each tile for mouse interactions.
        for (tileGroup in daTileGroups) {
            tileGroup.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    // Handle tile click when a battle is active.
                    if (manager.isBattleOn()) {
                        handleTileClick(tileGroup, x, y)
                    }
                }

                override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    // Highlight the tile when the mouse pointer enters.
                    if (manager.isBattleOn()) {
                        tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.5f)
                    }
                    super.enter(event, x, y, pointer, fromActor)
                }

                override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                    // Reset the tile's color when the mouse pointer exits.
                    if (manager.isBattleOn()) {
                        val currentTroopOnTile = getCurrentTroopView() ?: return
                        if (manager.isHexAchievable(currentTroopOnTile.getTroopInfo(), tileGroup.tileInfo.position)) {
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 0.7f)
                        } else {
                            tileGroup.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)
                        }
                    }
                    super.exit(event, x, y, pointer, toActor)
                }
            })

            // Add the tile group to the collection for easy reference and management.
            allTileGroups.add(tileGroup)
            tileGroups[tileGroup.tileInfo] = listOf(tileGroup)
        }

        // Adjust the size of the tile group map to match the stage and add it to the stage.
        tileGroupMap.setSize(stage.width, stage.height)
        stage.addActor(tileGroupMap)
    }

    /**
     * Handles tile click events.
     * Determines the type of action (move, attack, shoot) based on the clicked tile and initiates the appropriate request.
     *
     * @param tileGroup The clicked tile group.
     * @param x The x-coordinate of the click within the tile group.
     * @param y The y-coordinate of the click within the tile group.
     */
    private fun handleTileClick(tileGroup: TileGroup, x: Float, y: Float) {
        // Check if the system is ready to receive a player action.
        if (onPlayerActionReceived == null) {
            println("Player action is not expected at the moment.")
            return
        }

        // Retrieve the currently active troop's view.
        val currentTroopView = getCurrentTroopView()
        if (currentTroopView == null) {
            println("Error: No current troop selected for player action.")
            return
        }

        // Get the troop info and the target tile position.
        val currentTroop = currentTroopView.getTroopInfo()
        val targetPosition = tileGroup.tileInfo.position

        // Handle a shooting action if the troop can shoot and the target is an enemy.
        if (manager.canShoot(currentTroop) && manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
            val actionRequest = BattleActionRequest(
                troop = currentTroop,
                targetPosition = targetPosition,
                actionType = ActionType.SHOOT
            )
            onPlayerActionReceived?.invoke(Pair(actionRequest, tileGroup))
            return
        }

        // Ensure the target tile is reachable; if not, ignore the click.
        if (!manager.isHexAchievable(currentTroop, targetPosition)) return

        // Handle an attack action if the target tile is occupied by an enemy.
        if (manager.isHexOccupiedByEnemy(currentTroop, targetPosition)) {
            // Determine the direction of the attack based on the click position within the tile.
            val direction = pixelToDirection(x, y, tileGroup.baseLayerGroup.width)
            val attackRequest = BattleActionRequest(
                troop = currentTroop,
                targetPosition = targetPosition,
                actionType = ActionType.ATTACK,
                direction = direction
            )
            onPlayerActionReceived?.invoke(Pair(attackRequest, tileGroup))
        } else {
            // If the tile is not occupied by an enemy, handle a move action.
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
    private fun updateTilesShadowing() {
        val currentTroop = getCurrentTroopView() ?: return
        daTileGroups.forEach {
            if (manager.isHexAchievable(currentTroop.getTroopInfo(), it.tileInfo.position))
                it.baseLayerGroup.color = Color(1f, 1f, 1f, 0.7f)
            else
                it.baseLayerGroup.color = Color(1f, 1f, 1f, 1f)
        }
    }

    /**
     * Moves the pointer to the next troop in the turn queue.
     */
    fun movePointerToNextTroop() {
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop != null) {
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
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        moraleImage.apply {
            setScale(0.075f)
            setPosition(
                troopImageActor.width * -0.035f,
                troopImageActor.height * 1.10f
            )
            touchable = Touchable.disabled
            color = Color.WHITE.cpy().apply { a = 0f }
            name = "moraleBirdImage"
        }

        moraleImage.addAction(
            Actions.sequence(
                Actions.alpha(1f, 0.5f),
                Actions.delay(0.3f),
                Actions.alpha(0f, 0.5f),
                Actions.run { moraleImage.remove() }
            )
        )
        troopGroup.addActor(moraleImage)
    }

    /**
     * Displays a luck rainbow animation above the specified troop view.
     *
     * @param troopView The view of the troop to display the luck rainbow for.
     */
    fun showLuckRainbow(troopView: TroopBattleView) {
        val troopGroup = troopView.getCurrentGroup()
        val troopImageActor = troopGroup.findActor<Image>("troopImage")
        if (troopImageActor == null) {
            println("Error: No actor named 'troopImage' found in troop group.")
            return
        }

        luckRainbowImage.apply {
            val scaleRainbowImage = 0.130f
            setScale(scaleRainbowImage)
            setPosition(-width * scaleRainbowImage * 0.2f, height * scaleRainbowImage * 0.4f)
            touchable = Touchable.disabled
            color = Color.WHITE.cpy().apply { a = 0f }
            name = "luckRainbowImage"
        }

        luckRainbowImage.addAction(
            Actions.sequence(
                Actions.alpha(1f, 0.5f),
                Actions.delay(0.3f),
                Actions.alpha(0f, 0.5f),
                Actions.run { luckRainbowImage.remove() }
            )
        )
        troopGroup.addActor(luckRainbowImage)
    }

    /**
     * Determines the direction of the attack based on the mouse pointer position.
     *
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     * @return The direction of the attack.
     */
    fun pixelToDirection(x: Float, y: Float, width: Float): Direction {
        val x0 = width / 2
        val height = width * 1.1547f
        val y0 = x0 * 0.577f

        return when {
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 && x <= x0 -> Direction.TopLeft
            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 && y - y0 - x * y0 / x0 >= 0 -> Direction.CenterLeft
            y - y0 - x * y0 / x0 < 0 && x <= x0 -> Direction.BottomLeft
            y - (height - y0) + x * (height - y0) / (3f * x0) < 0 && x > x0 -> Direction.BottomRight
            y - (height - y0) + x * (height - y0) / (3f * x0) >= 0 && y - y0 - x * y0 / x0 < 0 -> Direction.CenterRight
            y - y0 - x * y0 / x0 >= 0 && x > x0 -> Direction.TopRight
            else -> Direction.DirError
        }
    }

    /**
     * Selects the appropriate crosshair cursor based on the tile state.
     *
     * @param tileGroup The tile group the pointer is hovering over.
     * @param x The x-coordinate of the pointer relative to the tile.
     * @param y The y-coordinate of the pointer relative to the tile.
     * @param width The width of the hexagon tile.
     */
    fun chooseCrosshair(tileGroup: TileGroup, x: Float, y: Float, width: Float) {
        val targetHex = tileGroup.tileInfo.position
        val currentTroop = getCurrentTroopView() ?: return

        if (manager.canShoot(currentTroop.getTroopInfo()) &&
                manager.isHexOccupiedByEnemy(currentTroop.getTroopInfo(), targetHex)) {
            Gdx.graphics.setCursor(cursorShoot)
            return
        }

        if (!manager.isHexAchievable(currentTroop.getTroopInfo(), targetHex)) {
            Gdx.graphics.setCursor(cursorCancel)
        } else {
            if (manager.isHexOccupiedByAlly(currentTroop.getTroopInfo(), targetHex)) {
                Gdx.graphics.setCursor(cursorCancel)
            } else if (manager.isHexOccupiedByEnemy(currentTroop.getTroopInfo(), targetHex)) {
                val direction = pixelToDirection(x, y, width)
                val hexToMove = HexMath.oneStepTowards(targetHex, direction)
                if (!manager.isHexOnBattleField(hexToMove)) {
                    Gdx.graphics.setCursor(cursorCancel)
                } else if (manager.isHexAchievable(currentTroop.getTroopInfo(), hexToMove)) {
                    Gdx.graphics.setCursor(cursorAttack[direction.num])
                } else {
                    Gdx.graphics.setCursor(cursorCancel)
                }
            } else {
                Gdx.graphics.setCursor(cursorMove)
            }
        }
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
        return BattleScreen(attackerHero, defenderHero)
    }

    /**
     * Resizes the page. Currently a placeholder.
     *
     * @param tab The tab to resize.
     */
    fun resizePage(tab: EmpireOverviewTab) {
        // No implementation yet
    }

    /**
     * Shuts down the battle screen and performs cleanup.
     * Ensures the default system cursor is restored and the game screen stack is updated.
     */
    private fun shutdownScreen() {
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        game.popScreen()
    }
}

