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

// Now it's just copied from HeroOverviewScreen
// All coordinates are hex, not offset

/** Screen for a battle. Visual routines are here, logic of the battle is in [BattleManager] class. Must not be used for AI duels. */
class BattleScreen(
    private var attackerHero: MapUnit,
    private var defenderHero: MapUnit,
    defaultPage: String = "",
    selection: String = ""
) : BaseScreen(), RecreateOnResize{
    private var manager = BattleManager(attackerHero.army, defenderHero.army)

    // Array to store TroopArmyView or null for empty slots
    private val attackerTroopViewsArray: Array<TroopBattleView?> =
            Array(attackerHero.army.getAllTroops().size ?: 0) { null }
    private val defenderTroopViewsArray: Array<TroopBattleView?> =
            Array(defenderHero.army.getAllTroops().size ?: 0) { null }

    private val verboseTurn = false // Флаг для включения/выключения вербозинга хода


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

    // Создаем изображение радуги
    val luckRainbowImage = ImageGetter.getExternalImage("LuckRainbow.png")
    val moraleImage = ImageGetter.getExternalImage("MoraleBird.png")


    /** Handle for a table with a battlefield. Could be obsolete in future */
    private val tabbedPager: TabbedPager

    /** What to do if battlefield window is closing */
    override fun dispose() {
        tabbedPager.selectPage(-1)
        super.dispose()
    }

    init {

        manager.initializeTurnQueue()

        // Iterate through slots and create/update views
        attackerHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if(troop != null) {
                troop.enterBattle(attackerHero.civInfo, index, attacker = true)
                val troopView = TroopBattleView(troop, this) // Pass ArmyView for interaction
                attackerTroopViewsArray[index] = troopView // Save to array
                //add(troopView).size(64f).pad(5f)
            }
        }

        // Iterate through slots and create/update views
        defenderHero.army.getAllTroops()?.forEachIndexed { index, troop ->
            if(troop != null) {
                troop.enterBattle(defenderHero.civInfo, index, attacker = false)
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
        updateTilesShadowing()

        //coroutineScope{launch {
        GlobalScope.launch {
            runBattleLoop()
        }
        //shutdownScreen()
    //}
        //}

    }

    fun loadCursor(filename: String, xHotspot: Int, yHotspot: Int) : Cursor{

        val texture = Texture("ExtraImages/" + filename)
        texture.textureData.prepare()
        val pixmap = texture.textureData.consumePixmap()
        return Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)

    }


    suspend fun runBattleLoop() = coroutineScope {
        while (manager.isBattleOn()) {
            val currentTroop = manager.getCurrentTroop()
            var isMorale = false

            if (currentTroop == null) {
                Gdx.app.postRunnable {
                    shutdownScreen() // Закрываем экран в UI-потоке
                }
                return@coroutineScope // Немедленно выходим из корутины
            }

            if (verboseTurn) {
                println("Current troop: ${currentTroop.baseUnit.name} at position ${currentTroop.position}")
            }

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
                if (verboseTurn) println("AI is performing action for troop: ${currentTroop.baseUnit.name}")

                val aiBattle = AIBattle(manager)
                val result = aiBattle.performTurn(currentTroop)

                handleBattleResult(result, currentTroop)
            }

            if (!isMorale) {
                manager.advanceTurn()
                if (verboseTurn) println("Turn advanced to next troop")
            } else if (verboseTurn) println("Current troop has morale")

            movePointerToNextTroop()
            updateTilesShadowing()
        }

        println("Battle has ended!")
        // shutdownScreen()
    }

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


            when (result.actionType) {
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

            if (result.battleEnded) {
                if (verboseTurn) println("Battle finished after action ${result.actionType}. Closing screen.")
                Gdx.app.postRunnable {
                    shutdownScreen() // Закрываем экран в UI-потоке
                }
                val battleResult = manager.getBattleResult()
                if (battleResult == null){
                    println("Bug with battle result.")
                } else {
                    if (verboseTurn) println("Army of ${battleResult.winningArmy.civInfo.nation.name} won.")

                    // Remove defeated unit from map
                    if (attackerHero.army == battleResult.winningArmy) {
                        defenderHero.removeFromTile()
                        defenderHero.civInfo.removeUnit(defenderHero)
                        defenderHero.civInfo.updateViewableTiles()
                    }
                    if (defenderHero.army == battleResult.winningArmy) {
                        attackerHero.removeFromTile()
                        attackerHero.civInfo.removeUnit(attackerHero)
                        attackerHero.civInfo.updateViewableTiles()
                    }

                }
            }
        } else {
            if (verboseTurn) println("Action ${result.actionType} failed with error: ${result.errorId}")
            handleActionError(result.errorId)
        }
    }


    /**
     * Обновляет массивы attackerTroopViewsArray и defenderTroopViewsArray
     * на основе текущего состояния армии в менеджере.
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

        /*
        // Обновляем массивы для защищающейся армии
        manager.getDefenderArmy().getAllTroops().forEachIndexed { index, troop ->
            if (troop == null) {
                // Удаляем вью для уничтоженного отряда
                defenderTroopViewsArray[index]?.perish()
                defenderTroopViewsArray[index] = null
            } else {
                // Если вью уже существует, обновляем его параметры
                val existingView = defenderTroopViewsArray[index]
                if (existingView != null) {
                    existingView.updateStats()
                } else {
                    println("View for defender troop does not exist")
                    // Если вью отсутствует, создаем новое
                    //defenderTroopViewsArray[index] = TroopBattleView(troop, this)
                    //val troopTileGroup = daTileGroups.firstOrNull { it.tileInfo.position == troop.position }
                    //troopTileGroup?.let { defenderTroopViewsArray[index]?.draw(it, attacker = false) }
                }
            }
        }

         */
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


    fun handlePlayerMove(tileGroup: TileGroup) {
        // Получаем текущий TroopBattleView
        val currentTroopView = getCurrentTroopView()

        if (currentTroopView == null) {
            println("Error: Current troop's view not found!")
            return
        }

        // Определяем целевую позицию
        val targetPosition = tileGroup.tileInfo.position

        // Создаем запрос на действие
        val actionRequest = BattleActionRequest(
            troop = currentTroopView.getTroopInfo(),
            targetPosition = targetPosition,
            actionType = ActionType.MOVE
        )

        // Передаем действие в callback (корутину, которая ждёт действия)
        onPlayerActionReceived?.invoke(Pair(actionRequest, tileGroup))
    }


    private var onPlayerActionReceived: ((Pair<BattleActionRequest, TileGroup>) -> Unit)? = null


    fun moveTroopView(troopView: TroopBattleView, targetTileGroup: TileGroup) {
        troopView.updatePosition(targetTileGroup = targetTileGroup) // Обновить внутреннюю позицию и представление
    }


    private fun handleActionError(errorId: ErrorId?) {
        when (errorId) {
            ErrorId.TOO_FAR -> showError("Target is too far away!")
            ErrorId.OCCUPIED_BY_ALLY -> showError("Target tile is occupied by an ally!")
            ErrorId.NOT_IMPLEMENTED -> showError("This action is not implemented yet!")
            ErrorId.INVALID_TARGET -> showError("Invalid target!")
            else -> showError("An unknown error occurred!")
        }
    }

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



    /** Draw a pointer to a currently active troop. */
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

                        val currentTroopOnTile = getCurrentTroopView() ?: return
                        if (manager.isHexAchievable(
                                    currentTroopOnTile.getTroopInfo(),
                                    tileGroup.tileInfo.position
                                )
                        )
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


    fun getCurrentTroopView(): TroopBattleView? {
        val currentTroop = manager.getCurrentTroop()
        return attackerTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
            ?: defenderTroopViewsArray.find { it?.getTroopInfo() == currentTroop }
    }


    private fun updateTilesShadowing(){
        val currentTroop = getCurrentTroopView() ?: return
        // TODO: Principally it works, but we need to fix coordinates conversions and distances. UPD maybe fixed
        daTileGroups.forEach {
            if (manager.isHexAchievable(currentTroop.getTroopInfo(), it.tileInfo.position))
                it.baseLayerGroup.color = Color(1f,1f,1f,0.7f)
            else
                it.baseLayerGroup.color = Color(1f,1f,1f,1f)


        }
    }

    fun movePointerToNextTroop() {
        val currentTroop = manager.getCurrentTroop()
        if (currentTroop != null){
            pointerPosition = currentTroop.position
            draw_pointer()
        } else
            println("Queue is empty, nowhere to put pointer")
    }

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

        // The code is similar to onClick routines. See details comments there.
        val targetHex = tileGroup.tileInfo.position
        val currentTroop = getCurrentTroopView() ?: return

        if (    manager.canShoot(currentTroop.getTroopInfo()) &&
                manager.isHexOccupiedByEnemy(currentTroop.getTroopInfo(), targetHex)){
            Gdx.graphics.setCursor(cursorShoot)
            return
        }

        // for non-shooting troops:
        if (!manager.isHexAchievable(currentTroop.getTroopInfo(), targetHex))
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
                if (manager.isHexAchievable(currentTroop.getTroopInfo(), hexToMove))
                    Gdx.graphics.setCursor(cursorAttack[direction.num])
                else
                    Gdx.graphics.setCursor(cursorCancel)
                return
            }

            Gdx.graphics.setCursor(cursorMove)
        }



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
        return BattleScreen(attackerHero, defenderHero)
    }

    fun resizePage(tab: EmpireOverviewTab) {
    }

    private fun shutdownScreen()
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

