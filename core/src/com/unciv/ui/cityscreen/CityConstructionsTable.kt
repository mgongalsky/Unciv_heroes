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
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.logic.city.CityConstructions
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.IConstruction
import com.unciv.logic.city.INonPerpetualConstruction
import com.unciv.logic.city.PerpetualConstruction
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileInfo
import com.unciv.models.UncivSound
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.CityEvent
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.stats.Stat
import com.unciv.models.translations.tr
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.popup.closeAllPopups
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.UncivTooltip.Companion.addTooltip
import com.unciv.ui.utils.extensions.addBorder
import com.unciv.ui.utils.extensions.addCell
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.brighten
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.getConsumesAmountString
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.keyShortcuts
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.packIfNeeded
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.utils.concurrency.Concurrency
import com.unciv.utils.concurrency.launchOnGLThread
import kotlin.math.max
import kotlin.math.min
import com.unciv.ui.utils.AutoScrollPane as ScrollPane

/**
 * Manager to hold and coordinate two widgets for the city screen left side:
 * - Construction queue with switch to [ConstructionInfoTable] button and the enqueue / buy buttons.
 *   The queue is scrollable, limited to one third of the stage height.
 * - Available constructions display, scrolling, grouped with expanders and therefore of dynamic height.
 */
class CityConstructionsTable(private val cityScreen: CityScreen) {
    private var compactContent: Table? = null

    /* -1 = Nothing, >= 0 queue entry (0 = current construction) */
    private var selectedQueueEntry = -1 // None
    private var preferredBuyStat = Stat.Gold  // Used for keyboard buy

    private val upperTable = Table(BaseScreen.skin)
    private val constructionsQueueScrollPane: ScrollPane
    private val constructionsQueueTable = Table()
    private val buyButtonsTable = Table()

    private val lowerTable = Table()
    private val availableConstructionsScrollPane: ScrollPane
    private val availableConstructionsTable = Table()
    private val lowerTableScrollCell: Cell<ScrollPane>


    private val pad = 10f
    private val posFromEdge = CityScreen.posFromEdge
    private val stageHeight = cityScreen.stage.height

    /** Gets or sets visibility of [both widgets][CityConstructionsTable] */
    var isVisible: Boolean
        get() = upperTable.isVisible
        set(value) {
            upperTable.isVisible = value
            lowerTable.isVisible = value
        }

    init {
        constructionsQueueScrollPane = ScrollPane(constructionsQueueTable.addBorder(2f, Color.WHITE))
        constructionsQueueScrollPane.setOverscroll(false, false)
        constructionsQueueTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityConstructionTable/ConstructionsQueueTable",
            tintColor = Color.BLACK
        )

        upperTable.defaults().left().top()
        upperTable.add(constructionsQueueScrollPane)
            .maxHeight(stageHeight / 3 - 10f)
            .padBottom(pad).row()
        upperTable.add(buyButtonsTable).padBottom(pad).row()

        availableConstructionsScrollPane = ScrollPane(availableConstructionsTable.addBorder(2f, Color.WHITE))
        availableConstructionsScrollPane.setOverscroll(false, false)
        availableConstructionsTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityConstructionTable/AvailableConstructionsTable",
            tintColor = Color.BLACK
        )
        // Lets keep some space (1/8) for garrison table
        lowerTableScrollCell = lowerTable.add(availableConstructionsScrollPane).bottom().padBottom(stageHeight / 8)

        lowerTable.row()



    }

// TB Deleted
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

    /** Forces layout calculation and returns the upper Table's (construction queue) width */
    fun getUpperWidth() = upperTable.packIfNeeded().width
    /** Forces layout calculation and returns the lower Table's (available constructions) width
     *  - or - the upper Table's width, whichever is greater (in case the former only contains "Loading...")
     */
    fun getLowerWidth() = max(lowerTable.packIfNeeded().width, getUpperWidth())

    fun addActorsToStage() {
        if (cityScreen.game.settings.skin == "Minimal") {
            val paper = Color.valueOf("EADFBD")
            val border = Color.valueOf("A18B5A")
            constructionsQueueTable.background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityConstructionTable/ConstructionsQueueTable", tintColor = paper
            )
            availableConstructionsTable.background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityConstructionTable/AvailableConstructionsTable", tintColor = paper
            )
            constructionsQueueScrollPane.actor = constructionsQueueTable.addBorder(2f, border)
            availableConstructionsScrollPane.actor =
                    availableConstructionsTable.addBorder(2f, border)
            if (!availableConstructionsTable.hasChildren()) {
                availableConstructionsTable.add(
                    Constants.loading.toLabel(
                        fontColor = Color.valueOf("453925")
                    )
                ).pad(10f)
            }
        }
        upperTable.name = "cityConstructionUpperPanel"
        lowerTable.name = "cityConstructionLowerPanel"
        cityScreen.stage.addActor(upperTable)
        cityScreen.stage.addActor(lowerTable)
        lowerTable.setPosition(posFromEdge, posFromEdge, Align.bottomLeft)
    }

    fun update(selectedConstruction: IConstruction?) {
        updateButtons(selectedConstruction)
        updateConstructionQueue()
        if (compactContent != null) {
            // Размеры областей задаёт страница; pack legacy-контейнеров здесь не нужен.
            compactContent?.invalidateHierarchy()
            updateAvailableConstructions()
            return
        }
        val royal = cityScreen.game.settings.skin == "Minimal"
        val currentHeight = if (royal) cityScreen.stage.height else stageHeight
        if (royal) upperTable.getCell(constructionsQueueScrollPane)
            .maxHeight((currentHeight / 3f - 10f).coerceAtLeast(0f))
        upperTable.pack()
        upperTable.setPosition(posFromEdge, currentHeight - posFromEdge, Align.topLeft)
        val bottomReserve = if (royal)
            max(currentHeight / 8f, cityScreen.garrisonView.packIfNeeded().height + pad)
        else stageHeight / 8f
        lowerTableScrollCell.padBottom(bottomReserve)
            .maxHeight(
                (currentHeight - bottomReserve - upperTable.height - 2 * posFromEdge).coerceAtLeast(
                    0f
                )
            )
        lowerTable.pack()
        lowerTable.setPosition(posFromEdge, posFromEdge, Align.bottomLeft)
        updateAvailableConstructions()
    }

    private fun updateButtons(construction: IConstruction?) {
        buyButtonsTable.clear()
        val buttons = ArrayList<TextButton>()
        buttons.add(getQueueButton(construction))
        if (construction != null && construction !is PerpetualConstruction)
            buttons.addAll(getBuyButtons(construction as INonPerpetualConstruction))
        if (compactContent != null) {
            // В выделенной полосе одна строка: дополнительные способы покупки доступны прокруткой.
            buyButtonsTable.left()
            for (button in buttons) {
                button.label.setWrap(false)
                HeroicCityStyle.applyActionStyle(button)
                buyButtonsTable.add(button).minWidth(140f).height(48f).padRight(5f)
            }
            return
        }
        if (cityScreen.game.settings.skin != "Minimal") {
            for (button in buttons) buyButtonsTable.add(button).padRight(5f)
            return
        }
        val width = contentWidth()
        val columns = if (width < 400f) 1 else minOf(2, buttons.size)
        val buttonWidth = (width - 5f * (columns - 1)) / columns
        buttons.forEachIndexed { index, button ->
            button.label.setWrap(true)
            button.label.setAlignment(Align.center)
            val cell = buyButtonsTable.add(button).width(buttonWidth).minHeight(48f).fillX()
                .padBottom(5f)
            if (index % columns != columns - 1) cell.padRight(5f)
            if (index % columns == columns - 1) buyButtonsTable.row()
        }
    }

    private fun updateConstructionQueue() {
        if (compactContent != null) {
            updateCompactQueue()
            return
        }
        val queueScrollY = constructionsQueueScrollPane.scrollY
        constructionsQueueTable.clear()
        val royal = cityScreen.game.settings.skin == "Minimal"
        val separatorColor = if (royal) Color.valueOf("A18B5A") else Color.WHITE
        val cityConstructions = cityScreen.city.cityConstructions
        val currentConstruction = cityConstructions.currentConstructionFromQueue
        val queue = cityConstructions.constructionQueue

        constructionsQueueTable.defaults().pad(0f)
        constructionsQueueTable.add(getHeader("Current construction")).fillX()
        constructionsQueueTable.addSeparator(color = separatorColor)
        if (currentConstruction != "")
            constructionsQueueTable.add(getQueueEntry(0, currentConstruction)).expandX().fillX()
                .row()
        else {
            val label = "Pick a construction".toLabel(
                fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
            )
            val cell = constructionsQueueTable.add(label).pad(2f)
            if (royal) {
                label.setWrap(true)
                cell.width(
                    (contentWidth() - 4f).coerceAtLeast(1f)
                )
            }
            cell.row()
        }
        constructionsQueueTable.addSeparator(color = separatorColor)
        if (queue.size > 1) {
            constructionsQueueTable.add(getHeader("Construction queue")).fillX()
            constructionsQueueTable.addSeparator(color = separatorColor)
            queue.forEachIndexed { index, constructionName ->
                if (index != 0) {
                    constructionsQueueTable.add(getQueueEntry(index, constructionName))
                        .expandX().fillX().row()
                    if (index != queue.size - 1)
                        constructionsQueueTable.addSeparator(color = separatorColor)
                }
            }
        }
        constructionsQueueScrollPane.layout()
        constructionsQueueScrollPane.scrollY = queueScrollY
        constructionsQueueScrollPane.updateVisualScroll()
    }

    private fun getConstructionButtonDTOs(): ArrayList<ConstructionButtonDTO> {
        val constructionButtonDTOList = ArrayList<ConstructionButtonDTO>()

        val city = cityScreen.city
        val cityConstructions = city.cityConstructions

        val constructionsSequence = city.getRuleset().units.values.asSequence() +
                city.getRuleset().buildings.values.asSequence() +
                city.getRuleset().cityEvents.values.asSequence()


        city.cityStats.updateTileStats() // only once
        for (entry in constructionsSequence.filter { it.shouldBeDisplayed(cityConstructions) }) {
            val useStoredProduction = entry is Building || entry is CityEvent || !cityConstructions.isBeingConstructedOrEnqueued(entry.name)
            var buttonText = entry.name.tr() + cityConstructions.getTurnsToConstructionString(entry.name, useStoredProduction)
            for ((resource, amount) in entry.getResourceRequirements()) {
                buttonText += "\n" + resource.getConsumesAmountString(amount).tr()
            }

            constructionButtonDTOList.add(
                ConstructionButtonDTO(
                    entry,
                    buttonText,
                    entry.getRejectionReasons(cityConstructions).getMostImportantRejectionReason()
                )
            )
        }

        for (specialConstruction in PerpetualConstruction.perpetualConstructionsMap.values
                .filter { it.shouldBeDisplayed(cityConstructions) }
        ) {
            constructionButtonDTOList.add(
                ConstructionButtonDTO(
                    specialConstruction,
                    "Produce [${specialConstruction.name}]".tr() + specialConstruction.getProductionTooltip(city)
                )
            )
        }

        return constructionButtonDTOList
    }

    private fun updateAvailableConstructions() {
        val constructionsScrollY = availableConstructionsScrollPane.scrollY

        if (!availableConstructionsTable.hasChildren()) {
            availableConstructionsTable.add(Constants.loading.toLabel()).pad(10f)
        }

        Concurrency.run("Construction info gathering - ${cityScreen.city.name}") {
            // Сбор информации о доступных строительных объектах и событиях
            val constructionButtonDTOList = getConstructionButtonDTOs()
            val city = cityScreen.city
            val cityEvents = city.cityConstructions.getConstructableEvents()
                .map { event ->
                    ConstructionButtonDTO(
                        event,
                        "[${event.name}]".tr()
                    )
                }

            launchOnGLThread {
                val units = ArrayList<Table>()
                val buildableWonders = ArrayList<Table>()
                val buildableNationalWonders = ArrayList<Table>()
                val buildableBuildings = ArrayList<Table>()
                val cityEventsTables = ArrayList<Table>()
                val specialConstructions = ArrayList<Table>()

                var maxButtonWidth = constructionsQueueTable.width
                for (dto in constructionButtonDTOList) {
                    val constructionButton = getConstructionButton(dto)
                    when (dto.construction) {
                        is BaseUnit -> {
                            units.add(constructionButton)
                        }
                        is Building -> {
                            when {
                                dto.construction.isWonder -> buildableWonders += constructionButton
                                dto.construction.isNationalWonder -> buildableNationalWonders += constructionButton
                                else -> buildableBuildings += constructionButton
                            }
                        }
                        is CityEvent -> {
                            cityEventsTables.add(constructionButton)
                        }
                        is PerpetualConstruction -> {
                            specialConstructions.add(constructionButton)
                        }
                    }
                    maxButtonWidth = max(maxButtonWidth, constructionButton.packIfNeeded().width)
                }

                availableConstructionsTable.apply {
                    clear()
                    defaults().left().bottom()
                    addCategory("Units", units, maxButtonWidth)
                    addCategory("Buildings", buildableBuildings, maxButtonWidth)
                    addCategory("Wonders", buildableWonders, maxButtonWidth)
                    addCategory("National Wonders", buildableNationalWonders, maxButtonWidth)
                    addCategory("City Events", cityEventsTables, maxButtonWidth)
                    addCategory("Other", specialConstructions, maxButtonWidth)
                    pack()
                }

                if (compactContent != null) {
                    // Асинхронная загрузка меняет содержимое, но не размеры выделенных областей.
                    compactContent?.invalidateHierarchy()
                    compactContent?.validate()
                } else {
                    availableConstructionsScrollPane.setSize(maxButtonWidth,
                        min(availableConstructionsTable.prefHeight, lowerTableScrollCell.maxHeight))
                    lowerTable.pack()
                }
                availableConstructionsScrollPane.apply {
                    layout()
                    scrollY = constructionsScrollY
                    updateVisualScroll()
                }
            }
        }
    }

    private fun getQueueEntry(constructionQueueIndex: Int, constructionName: String): Table {
        val city = cityScreen.city
        val cityConstructions = city.cityConstructions
        val royal = cityScreen.game.settings.skin == "Minimal"
        val selected = constructionQueueIndex == selectedQueueEntry
        val rowWidth = contentWidth()
        val table = Table()
        table.name = "cityQueueEntry:$constructionQueueIndex"
        table.align(Align.left).pad(5f)
        table.background = BaseScreen.skinStrings.getUiBackground(
            if (selected) "CityScreen/CityConstructionTable/QueueEntrySelected"
            else "CityScreen/CityConstructionTable/QueueEntry",
            tintColor = if (royal) Color.valueOf(if (selected) "C6C394" else "EADFBD")
            else if (selected) Color.GREEN.darken(0.5f) else Color.BLACK
        )
        val isFirstConstructionOfItsKind = cityConstructions.isFirstConstructionOfItsKind(
            constructionQueueIndex, constructionName
        )
        var text = constructionName.tr() +
                if (constructionName in PerpetualConstruction.perpetualConstructionsMap) "\n∞"
                else cityConstructions.getTurnsToConstructionString(
                    constructionName,
                    isFirstConstructionOfItsKind
                )
        for ((resource, amount) in cityConstructions.getConstruction(constructionName)
            .getResourceRequirements())
            text += "\n" + resource.getConsumesAmountString(amount).tr()

        table.defaults().pad(2f).minWidth(40f)
        if (isFirstConstructionOfItsKind) table.add(getProgressBar(constructionName)).minWidth(5f)
        else table.add().minWidth(5f)
        table.add(ImageGetter.getPortraitImage(constructionName, 40f)).padRight(10f)
        val label = text.toLabel(fontColor = if (royal) Color.valueOf("453925") else Color.WHITE)
        val labelCell = table.add(label).expandX().fillX().left()
        if (royal) {
            label.setWrap(true)
            labelCell.width((rowWidth - 80f).coerceAtLeast(1f))
            table.row()
            val actions = Table()
            if (constructionQueueIndex > 0)
                actions.add(getRaisePriorityButton(constructionQueueIndex, constructionName, city))
                    .pad(2f)
            if (constructionQueueIndex != cityConstructions.constructionQueue.lastIndex)
                actions.add(getLowerPriorityButton(constructionQueueIndex, constructionName, city))
                    .pad(2f)
            actions.add(getRemoveFromQueueButton(constructionQueueIndex, city)).pad(2f)
            table.add(actions).colspan(3).right().row()
        } else {
            if (constructionQueueIndex > 0)
                table.add(getRaisePriorityButton(constructionQueueIndex, constructionName, city))
                    .right()
            else table.add().right()
            if (constructionQueueIndex != cityConstructions.constructionQueue.lastIndex)
                table.add(getLowerPriorityButton(constructionQueueIndex, constructionName, city))
                    .right()
            else table.add().right()
            table.add(getRemoveFromQueueButton(constructionQueueIndex, city)).right()
        }
        table.touchable = Touchable.enabled
        table.onClick {
            if (selectedQueueEntry == constructionQueueIndex) {
                city.cityConstructions.removeFromQueue(constructionQueueIndex, false)
                selectedQueueEntry = -1
                cityScreen.clearSelection()
            } else {
                cityScreen.selectConstruction(constructionName)
                selectedQueueEntry = constructionQueueIndex
            }
            cityScreen.update()
        }
        return table
    }

    private fun getProgressBar(constructionName: String): Group {
        val cityConstructions = cityScreen.city.cityConstructions
        val construction = cityConstructions.getConstruction(constructionName)
        if (construction is PerpetualConstruction) return Table()
        if (cityConstructions.getWorkDone(constructionName) == 0) return Table()
        val constructionPercentage = cityConstructions.getWorkDone(constructionName) /
                (construction as INonPerpetualConstruction).getProductionCost(cityConstructions.cityInfo.civInfo)
                    .toFloat()
        val royal = cityScreen.game.settings.skin == "Minimal"
        return ImageGetter.getProgressBarVertical(
            width = 2f,
            height = 30f,
            percentComplete = constructionPercentage,
            progressColor = if (royal) Color.valueOf("D6AD48") else Color.BROWN.brighten(0.5f),
            backgroundColor = if (royal) Color.valueOf("453925") else Color.WHITE
        )
    }

    private class ConstructionButtonDTO(val construction: IConstruction, val buttonText: String, val rejectionReason: String? = null)

    private fun getConstructionButton(constructionButtonDTO: ConstructionButtonDTO): Table {
        val construction = constructionButtonDTO.construction
        val royal = cityScreen.game.settings.skin == "Minimal"
        val selected = !isSelectedQueueEntry() && cityScreen.selectedConstruction == construction
        val rowWidth = contentWidth()
        val pickConstructionButton = Table()
        pickConstructionButton.name = "cityConstruction:${construction.name}"
        pickConstructionButton.align(Align.left).pad(5f)
        pickConstructionButton.background = BaseScreen.skinStrings.getUiBackground(
            if (selected) "CityScreen/CityConstructionTable/PickConstructionButtonSelected"
            else "CityScreen/CityConstructionTable/PickConstructionButton",
            tintColor = if (royal) Color.valueOf(if (selected) "C6C394" else "EADFBD")
            else if (selected) Color.GREEN.darken(0.5f) else Color.BLACK
        )
        pickConstructionButton.touchable = Touchable.enabled
        pickConstructionButton.add(getProgressBar(construction.name)).padRight(5f)
        pickConstructionButton.add(ImageGetter.getPortraitImage(construction.name, 40f))
            .padRight(10f)
        val label = constructionButtonDTO.buttonText.toLabel(
            fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
        )
        val labelCell = pickConstructionButton.add(label).expandX().fillX()
        if (royal) {
            label.setWrap(true)
            labelCell.width((rowWidth - 115f).coerceAtLeast(1f))
        }
        if (!cannotAddConstructionToQueue(
                construction,
                cityScreen.city,
                cityScreen.city.cityConstructions
            )
        ) {
            if (compactContent != null) {
                val addButton = "+".toTextButton()
                HeroicCityStyle.applyCompactStyle(addButton)
                addButton.addTooltip("Add to queue")
                addButton.onClick(UncivSound.Silent) {
                    addConstructionToQueue(construction, cityScreen.city.cityConstructions)
                }
                pickConstructionButton.add(addButton).size(32f)
            } else {
                val addToQueueButton = ImageGetter.getImage("OtherIcons/New")
                    .apply { color = Color.BLACK }.surroundWithCircle(40f)
                addToQueueButton.onClick(UncivSound.Silent) {
                    addConstructionToQueue(construction, cityScreen.city.cityConstructions)
                }
                pickConstructionButton.add(addToQueueButton).size(40f)
            }
        } else if (royal) {
            pickConstructionButton.add().width(40f)
        }
        pickConstructionButton.row()

        if (constructionButtonDTO.rejectionReason != null) {
            if (!royal) pickConstructionButton.color = Color.GRAY
            val reasonLabel = constructionButtonDTO.rejectionReason.toLabel(
                if (royal) Color.valueOf("8C2923") else Color.RED
            ).apply { wrap = true }
            val reasonCell = pickConstructionButton.add(reasonLabel)
                .colspan(pickConstructionButton.columns).fillX().left().padTop(2f)
            if (royal) reasonCell.width((rowWidth - 10f).coerceAtLeast(1f))
        }
        pickConstructionButton.onClick {
            if (cityScreen.selectedConstruction == construction)
                cityScreen.clearSelection()
            else
                cityScreen.selectConstruction(construction)
            selectedQueueEntry = -1
            cityScreen.update()
        }
        return pickConstructionButton
    }

    private fun isSelectedQueueEntry(): Boolean = selectedQueueEntry >= 0

    private fun cannotAddConstructionToQueue(construction: IConstruction, city: CityInfo, cityConstructions: CityConstructions): Boolean {
        return cityConstructions.isQueueFull()
                || !cityConstructions.getConstruction(construction.name).isBuildable(cityConstructions)
                || !cityScreen.canChangeState
                || construction is PerpetualConstruction && cityConstructions.isBeingConstructedOrEnqueued(construction.name)
                || city.isPuppet
    }

    private fun getQueueButton(construction: IConstruction?): TextButton {
        val city = cityScreen.city
        val cityConstructions = city.cityConstructions
        val button: TextButton

        if (isSelectedQueueEntry()) {
            button = "Remove from queue".toTextButton()
            if (!cityScreen.canCityBeChanged())
                button.disable()
            else {
                button.onClick {
                    cityConstructions.removeFromQueue(selectedQueueEntry, false)
                    cityScreen.clearSelection()
                    selectedQueueEntry = -1
                    cityScreen.update()
                }
            }
        } else {
            button = "Add to queue".toTextButton()
            if (construction == null
                    || cannotAddConstructionToQueue(construction, city, cityConstructions)) {
                button.disable()
            } else {
                button.onClick(UncivSound.Silent) {
                    addConstructionToQueue(construction, cityConstructions)
                }
            }
        }

        button.labelCell.pad(5f)
        return button
    }

    private fun addConstructionToQueue(construction: IConstruction, cityConstructions: CityConstructions) {
        // Some evil person decided to double tap real fast - #4977
        if (cannotAddConstructionToQueue(construction, cityScreen.city, cityConstructions))
            return

        // UniqueType.CreatesOneImprovement support - don't add yet, postpone until target tile for the improvement is selected
        if (construction is Building && construction.hasCreateOneImprovementUnique()) {
            cityScreen.startPickTileForCreatesOneImprovement(construction, Stat.Gold, false)
            return
        }
        cityScreen.stopPickTileForCreatesOneImprovement()

        SoundPlayer.play(getConstructionSound(construction))

        cityConstructions.addToQueue(construction.name)
        if (!construction.shouldBeDisplayed(cityConstructions)) // For buildings - unlike units which can be queued multiple times
            cityScreen.clearSelection()
        cityScreen.update()
        cityScreen.game.settings.addCompletedTutorialTask("Pick construction")
    }

    private fun getConstructionSound(construction: IConstruction): UncivSound {
        return when(construction) {
            is Building -> UncivSound.Construction
            is BaseUnit -> UncivSound.Promote
            PerpetualConstruction.gold -> UncivSound.Coin
            PerpetualConstruction.science -> UncivSound.Paper
            PerpetualConstruction.culture -> UncivSound.Policy
            PerpetualConstruction.faith -> UncivSound.Choir
            else -> UncivSound.Click
        }
    }

    private fun getBuyButtons(construction: INonPerpetualConstruction?): List<TextButton> {
        return Stat.statsUsableToBuy.mapNotNull { getBuyButton(construction, it) }
    }

    private fun getBuyButton(construction: INonPerpetualConstruction?, stat: Stat = Stat.Gold): TextButton? {
        if (stat !in Stat.statsUsableToBuy || construction == null)
            return null

        val city = cityScreen.city
        val button = "".toTextButton()

        if (!isConstructionPurchaseShown(construction, stat)) {
            // This can't ever be bought with the given currency.
            // We want one disabled "buy" button without a price for "priceless" buildings such as wonders
            // We don't want such a button when the construction can be bought using a different currency
            if (stat != Stat.Gold || construction.canBePurchasedWithAnyStat(city))
                return null
            button.setText("Buy".tr())
            button.disable()
        } else {
            val constructionBuyCost = construction.getStatBuyCost(city, stat)!!
            button.setText("Buy".tr() + " " + constructionBuyCost + stat.character)

            button.onActivation {
                button.disable()
                buyButtonOnClick(construction, stat)
            }
            button.isEnabled = isConstructionPurchaseAllowed(construction, stat, constructionBuyCost)
            button.keyShortcuts.add('B')
            button.addTooltip('B')  // The key binding is done in CityScreen constructor
            preferredBuyStat = stat  // Not very intelligent, but the least common currency "wins"
        }

        button.labelCell.pad(5f)

        return button
    }

    private fun buyButtonOnClick(construction: INonPerpetualConstruction, stat: Stat = preferredBuyStat) {
        if (construction !is Building || !construction.hasCreateOneImprovementUnique())
            return askToBuyConstruction(construction, stat)
        if (selectedQueueEntry < 0)
            return cityScreen.startPickTileForCreatesOneImprovement(construction, stat, true)
        // Buying a UniqueType.CreatesOneImprovement building from queue must pass down
        // the already selected tile, otherwise a new one is chosen from Automation code.
        val improvement = construction.getImprovementToCreate(cityScreen.city.getRuleset())!!
        val tileForImprovement = cityScreen.city.cityConstructions.getTileForImprovement(improvement.name)
        askToBuyConstruction(construction, stat, tileForImprovement)
    }

    /** Ask whether user wants to buy [construction] for [stat].
     *
     * Used from onClick and keyboard dispatch, thus only minimal parameters are passed,
     * and it needs to do all checks and the sound as appropriate.
     */
    fun askToBuyConstruction(
        construction: INonPerpetualConstruction,
        stat: Stat = preferredBuyStat,
        tile: TileInfo? = null
    ) {
        if (!isConstructionPurchaseShown(construction, stat)) return
        val city = cityScreen.city
        val constructionBuyCost = construction.getStatBuyCost(city, stat)!!
        if (!isConstructionPurchaseAllowed(construction, stat, constructionBuyCost)) return

        cityScreen.closeAllPopups()

        val purchasePrompt = "Currently you have [${city.getStatReserve(stat)}] [${stat.name}].".tr() + "\n\n" +
                "Would you like to purchase [${construction.name}] for [$constructionBuyCost] [${stat.character}]?".tr()
        ConfirmPopup(
            cityScreen,
            purchasePrompt,
            "Purchase",
            true,
            restoreDefault = { cityScreen.update() }
        ) { purchaseConstruction(construction, stat, tile) }.open()
    }

    /** This tests whether the buy button should be _shown_ */
    private fun isConstructionPurchaseShown(construction: INonPerpetualConstruction, stat: Stat): Boolean {
        val city = cityScreen.city
        return construction.canBePurchasedWithStat(city, stat)
    }

    /** This tests whether the buy button should be _enabled_ */
    private fun isConstructionPurchaseAllowed(construction: INonPerpetualConstruction, stat: Stat, constructionBuyCost: Int): Boolean {
        val city = cityScreen.city
        return when {
            city.isPuppet -> false
            !cityScreen.canChangeState -> false
            city.isInResistance() -> false
            !construction.isPurchasable(city.cityConstructions) -> false    // checks via 'rejection reason'
            construction is BaseUnit && !city.canPlaceNewUnit(construction) -> false
            city.civInfo.gameInfo.gameParameters.godMode -> true
            constructionBuyCost == 0 -> true
            else -> city.getStatReserve(stat) >= constructionBuyCost
        }
}

    /** Called only by askToBuyConstruction's Yes answer - not to be confused with [CityConstructions.purchaseConstruction]
     * @param tile supports [UniqueType.CreatesOneImprovement]
     */
    private fun purchaseConstruction(
        construction: INonPerpetualConstruction,
        stat: Stat = Stat.Gold,
        tile: TileInfo? = null
    ) {
        SoundPlayer.play(stat.purchaseSound)
        val city = cityScreen.city
        if (!city.cityConstructions.purchaseConstruction(
                    construction.name,
                    selectedQueueEntry,
                    false,
                    stat,
                    tile
                )
        ) {
            ConstructionPurchaseFailurePopup(cityScreen.stage, construction.name, city.name).open()
            return
        }
        if (isSelectedQueueEntry() || cityScreen.selectedConstruction?.isBuildable(city.cityConstructions) != true) {
            selectedQueueEntry = -1
            cityScreen.clearSelection()

            // Allow buying next queued or auto-assigned construction right away
            city.cityConstructions.chooseNextConstruction()
            if (city.cityConstructions.currentConstructionFromQueue.isNotEmpty()) {
                val newConstruction = city.cityConstructions.getCurrentConstruction()
                if (newConstruction is INonPerpetualConstruction)
                    cityScreen.selectConstruction(newConstruction)
            }
        }
        cityScreen.update()
    }

    private fun getRaisePriorityButton(constructionQueueIndex: Int, name: String, city: CityInfo): Table {
        if (compactContent != null) return compactQueueAction("↑", "Move up") {
            city.cityConstructions.raisePriority(constructionQueueIndex)
            cityScreen.selectConstruction(name)
            selectedQueueEntry = constructionQueueIndex - 1
            cityScreen.update()
        }
        val tab = Table()
        tab.add(ImageGetter.getArrowImage(Align.top).apply { color = Color.BLACK }.surroundWithCircle(40f))
        if (cityScreen.canCityBeChanged()) {
            tab.touchable = Touchable.enabled
            tab.onClick {
                tab.touchable = Touchable.disabled
                city.cityConstructions.raisePriority(constructionQueueIndex)
                cityScreen.selectConstruction(name)
                selectedQueueEntry = constructionQueueIndex - 1
                cityScreen.update()
            }
        }
        return tab
    }

    private fun getLowerPriorityButton(constructionQueueIndex: Int, name: String, city: CityInfo): Table {
        if (compactContent != null) return compactQueueAction("↓", "Move down") {
            city.cityConstructions.lowerPriority(constructionQueueIndex)
            cityScreen.selectConstruction(name)
            selectedQueueEntry = constructionQueueIndex + 1
            cityScreen.update()
        }
        val tab = Table()
        tab.add(ImageGetter.getArrowImage(Align.bottom).apply { color = Color.BLACK }.surroundWithCircle(40f))
        if (cityScreen.canCityBeChanged()) {
            tab.touchable = Touchable.enabled
            tab.onClick {
                tab.touchable = Touchable.disabled
                city.cityConstructions.lowerPriority(constructionQueueIndex)
                cityScreen.selectConstruction(name)
                selectedQueueEntry = constructionQueueIndex + 1
                cityScreen.update()
            }
        }
        return tab
    }

    private fun getRemoveFromQueueButton(constructionQueueIndex: Int, city: CityInfo): Table {
        if (compactContent != null) return compactQueueAction("×", "Remove from queue") {
            city.cityConstructions.removeFromQueue(constructionQueueIndex, false)
            selectedQueueEntry = -1
            cityScreen.clearSelection()
            cityScreen.update()
        }
        val tab = Table()
        tab.add(ImageGetter.getImage("OtherIcons/Stop").surroundWithCircle(40f))
        if (cityScreen.canCityBeChanged()) {
            tab.touchable = Touchable.enabled
            tab.onClick {
                tab.touchable = Touchable.disabled
                city.cityConstructions.removeFromQueue(constructionQueueIndex, false)
                cityScreen.clearSelection()
                cityScreen.update()
            }
        }
        return tab
    }

    private fun getHeader(title: String): Table {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val label = title.toLabel(
            fontColor = if (royal) Color.valueOf("453925") else Color.WHITE,
            fontSize = Constants.headingFontSize
        )
        val table = Table().background(
            BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityConstructionTable/Header",
                tintColor = if (royal) Color.valueOf("D7C89F") else BaseScreen.skinStrings.skinConfig.baseColor
            )
        )
        val cell = table.add(label).pad(4f)
        if (royal) {
            label.setWrap(true)
            cell.width(
                (contentWidth() - 8f).coerceAtLeast(1f)
            )
        }
        return table
    }

    private fun resizeAvailableConstructionsScrollPane() {
        if (compactContent != null) {
            compactContent?.invalidateHierarchy()
            return
        }
        availableConstructionsScrollPane.height =
                min(availableConstructionsTable.prefHeight, lowerTableScrollCell.maxHeight)
        lowerTable.pack()
    }

    private fun Table.addCategory(title: String, list: ArrayList<Table>, prefWidth: Float) {
        if (list.isEmpty()) return
        val royal = cityScreen.game.settings.skin == "Minimal"
        val separatorColor = if (royal) Color.valueOf("A18B5A") else Color.WHITE
        if (rows > 0) addSeparator(color = separatorColor)
        val expander = ExpanderTab(
            title,
            defaultPad = 0f,
            expanderWidth = prefWidth,
            persistenceID = "CityConstruction.$title",
            onChange = { resizeAvailableConstructionsScrollPane() }
        ) {
            for (table in list) {
                it.addSeparator(color = separatorColor, colSpan = 1)
                it.add(table).left().row()
            }
        }
        if (royal) {
            expander.setHeaderAppearance(
                BaseScreen.skinStrings.getUiBackground(
                    "General/ExpanderTab", tintColor = Color.valueOf("D7C89F")
                ),
                Color.valueOf("453925"),
                prefWidth
            )
        }
        add(expander).prefWidth(prefWidth).growX().row()
    }

    /** Каталог и очередь прокручиваются независимо внутри выделенной области страницы. */
    internal fun asCompactContent(width: Float, height: Float): Table {
        compactContent?.let { return it }
        compactPanelWidth = (width - 12f).coerceAtLeast(1f)
        upperTable.remove()
        lowerTable.remove()
        constructionsQueueScrollPane.actor = constructionsQueueTable
        availableConstructionsScrollPane.actor = availableConstructionsTable
        constructionsQueueScrollPane.setScrollingDisabled(true, false)
        availableConstructionsScrollPane.setScrollingDisabled(true, false)
        val actions = ScrollPane(buyButtonsTable).apply {
            setScrollingDisabled(false, true)
            setOverscroll(false, false)
        }
        val queueHeight = minOf(160f, height * 0.30f).coerceAtLeast(44f)
        return Table().apply {
            name = "cityCompactConstructions"
            top().left()
            add(availableConstructionsScrollPane).width(width).minHeight(0f).growY().row()
            add(actions).width(width).height(54f).row()
            add(constructionsQueueScrollPane).width(width).height(queueHeight).row()
            compactContent = this
        }
    }
    private var compactPanelWidth: Float? = null

    /** Ширина строк определяется контейнером; legacy сохраняет прежний расчёт. */
    private fun contentWidth(): Float =
            compactPanelWidth ?: (cityScreen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(cityScreen.stage.width - 24f)

    /** Действия текущего проекта остаются доступны после удаления его дубля из нижней очереди. */
    internal fun currentProjectActions(): Table {
        val city = cityScreen.city
        val queue = city.cityConstructions.constructionQueue
        val actions = Table()
        if (queue.isEmpty()) return actions
        val select = "…".toTextButton()
        HeroicCityStyle.applyCompactStyle(select)
        select.addTooltip("Current construction")
        select.onClick {
            selectedQueueEntry = 0
            cityScreen.selectConstruction(queue[0])
            cityScreen.update()
        }
        actions.add(select).size(32f).padRight(3f)
        if (queue.size > 1) actions.add(getLowerPriorityButton(0, queue[0], city)).size(32f)
            .padRight(3f)
        actions.add(getRemoveFromQueueButton(0, city)).size(32f)
        return actions
    }

    /** Нижняя область содержит только ожидающие проекты, с исходными индексами игровой очереди. */
    private fun updateCompactQueue() {
        val savedY = constructionsQueueScrollPane.scrollY
        val queue = cityScreen.city.cityConstructions.constructionQueue
        constructionsQueueTable.clear()
        constructionsQueueTable.top().left()
        constructionsQueueTable.add(
            "Construction queue".toLabel(
                fontSize = 17,
                fontColor = Color.valueOf("453925")
            )
        )
            .left().pad(3f).row()
        if (queue.size <= 1) {
            constructionsQueueTable.add(
                "—".toLabel(
                    fontSize = 16,
                    fontColor = Color.valueOf("70634C")
                )
            )
                .left().pad(3f).row()
        }
        for (index in 1 until queue.size) {
            val name = queue[index]
            val row = Table()
            row.add(ImageGetter.getPortraitImage(name, 26f)).size(26f).padRight(5f)
            val label = name.toLabel(fontSize = 16, fontColor = Color.valueOf("453925"))
                .apply { setEllipsis(true) }
            label.onClick {
                selectedQueueEntry = index
                cityScreen.selectConstruction(name)
                cityScreen.update()
            }
            row.add(label).width((contentWidth() - 150f).coerceAtLeast(1f)).left()
            row.add(getRaisePriorityButton(index, name, cityScreen.city)).size(32f).padRight(3f)
            if (index < queue.lastIndex) row.add(
                getLowerPriorityButton(
                    index,
                    name,
                    cityScreen.city
                )
            ).size(32f).padRight(3f)
            else row.add().size(32f).padRight(3f)
            row.add(getRemoveFromQueueButton(index, cityScreen.city)).size(32f)
            constructionsQueueTable.add(row).growX().padBottom(3f).row()
        }
        constructionsQueueScrollPane.layout()
        constructionsQueueScrollPane.scrollY = savedY
        constructionsQueueScrollPane.updateVisualScroll()
    }

    /** Небольшое действие очереди; блокировка проверяется и при создании, и при нажатии. */
    private fun compactQueueAction(text: String, tooltip: String, action: () -> Unit): Table {
        val button = text.toTextButton()
        if (!cityScreen.canCityBeChanged()) button.disable()
        HeroicCityStyle.applyCompactStyle(button)
        button.addTooltip(tooltip)
        button.onClick {
            if (cityScreen.canCityBeChanged()) action()
        }
        return Table().apply { add(button).size(32f) }
    }
}
