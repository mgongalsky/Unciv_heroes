package com.unciv.ui.options

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.unciv.MainMenuScreen
import com.unciv.UncivGame
import com.unciv.logic.event.EventBus
import com.unciv.models.UncivSound
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSetting
import com.unciv.models.metadata.GameSettings
import com.unciv.models.metadata.SettingsPropertyChanged
import com.unciv.models.metadata.SettingsPropertyUncivSoundChanged
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.translations.tr
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.center
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.toCheckBox
import com.unciv.ui.utils.extensions.toGdxArray
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.worldscreen.WorldScreen
import com.unciv.utils.concurrency.Concurrency
import com.unciv.utils.concurrency.withGLContext
import kotlin.reflect.KMutableProperty0

/**
 * The Options (Settings) Popup
 * @param screen The caller - note if this is a [WorldScreen] or [MainMenuScreen] they will be rebuilt when major options change.
 */
//region Fields
class OptionsPopup(
    screen: BaseScreen,
    private val selectPage: Int = defaultPage,
    private val onClose: () -> Unit = {}
) : Popup(screen.stage, /** [TabbedPager] handles scrolling */ scrollable = false ) {
    val settings = screen.game.settings
    val tabs: TabbedPager
    val selectBoxMinWidth: Float

    //endregion

    companion object {
        const val defaultPage = 2  // Gameplay
    }

    init {
        if (settings.addCompletedTutorialTask("Open the options table"))
            (screen as? WorldScreen)?.shouldUpdate = true

        val royal = settings.skin == "Minimal"
        if (royal) {
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(58f, 52f, 52f, 52f)
        } else innerTable.pad(0f)
        val tabMaxWidth: Float
        val tabMinWidth: Float
        val tabMaxHeight: Float
        screen.run {
            selectBoxMinWidth = if (stage.width < 600f) 200f else 240f
            tabMaxWidth = if (royal) (stage.width - 128f).coerceAtLeast(1f) else if (isPortrait()) stage.width - 10f else 0.8f * stage.width
            tabMinWidth = minOf(0.6f * stage.width, tabMaxWidth)
            tabMaxHeight = (if (isPortrait()) 0.7f else 0.8f) * stage.height
        }
        tabs = TabbedPager(
            tabMinWidth, tabMaxWidth, 0f, tabMaxHeight,
            headerFontSize = 21, backgroundColor = if (royal) Color.valueOf("EADFBD") else Color.CLEAR, capacity = 8
        )
        add(tabs).pad(0f).grow().row()

        tabs.addPage(
            "About",
            aboutTab(if (royal) tabMaxWidth - 20f else null).apply {
                if (royal) background = BaseScreen.skinStrings.getUiBackground("General/TabbedPager", tintColor = Color.valueOf("eadfbd"))
            },
            ImageGetter.getExternalImage("Icon.png"), 24f
        )
        tabs.addPage(
            "Display",
            displayTab(this, ::reloadWorldAndOptions),
            ImageGetter.getImage("UnitPromotionIcons/Scouting"), 24f
        )
        tabs.addPage(
            "Gameplay",
            gameplayTab(this),
            ImageGetter.getImage("OtherIcons/Options"), 24f
        )
        tabs.addPage(
            "Language",
            languageTab(this, ::reloadWorldAndOptions),
            ImageGetter.getImage("FlagIcons/${settings.language}"), 24f
        )
        tabs.addPage(
            "Sound",
            soundTab(this),
            ImageGetter.getImage("OtherIcons/Speaker"), 24f
        )
        tabs.addPage(
            "Multiplayer",
            multiplayerTab(this),
            ImageGetter.getImage("OtherIcons/Multiplayer"), 24f
        )
        tabs.addPage(
            "Advanced",
            advancedTab(this, ::reloadWorldAndOptions),
            ImageGetter.getImage("OtherIcons/Settings"), 24f
        )

        if (RulesetCache.size > BaseRuleset.values().size) {
            val content = ModCheckTab(screen)
            tabs.addPage("Locate mod errors", content, ImageGetter.getImage("OtherIcons/Mods"), 24f)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
            tabs.addPage("Debug", debugTab(), ImageGetter.getImage("OtherIcons/SecretOptions"), 24f, secret = true)
        }

        val closeCell = addCloseButton {
            screen.game.musicController.onChange(null)
            screen.game.platformSpecificHelper?.allowPortrait(settings.allowAndroidPortrait)
            onClose()
        }.padBottom(10f)

        if (royal) {
            closeCell.actor.label.wrap = true
            closeCell.actor.labelCell.width((tabMaxWidth - closeCell.actor.padLeft - closeCell.actor.padRight - 20f).coerceAtLeast(1f))
            closeCell.width(tabMaxWidth).minHeight(48f).fillX()
            val closeHeight = closeCell.actor.prefHeight.coerceAtLeast(48f) + closeCell.padTop + closeCell.padBottom
            val pagerHeight = (stageToShowOn.height - 24f - innerTable.padTop - innerTable.padBottom - closeHeight).coerceAtLeast(tabs.minHeight)
            getCell(innerTable).maxWidth(stageToShowOn.width - 24f)
            innerTable.getCell(tabs).width(tabMaxWidth).height(pagerHeight)
        }
        pack() // Needed to show the background.
        center(screen.stage)
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (!visible) return
        tabs.askForPassword(secretHashCode = 2747985)
        if (tabs.activePage < 0) tabs.selectPage(selectPage)
    }

    /** Reload this Popup after major changes (resolution, tileset, language, font) */
    private fun reloadWorldAndOptions() {
        Concurrency.run("Reload from options") {
            settings.save()
            withGLContext {
                // We have to run setSkin before the screen is rebuild else changing skins
                // would only load the new SkinConfig after the next rebuild
                BaseScreen.setSkin()
            }
            val screen = UncivGame.Current.screen
            if (screen is WorldScreen) {
                UncivGame.Current.reloadWorldscreen()
            } else if (screen is MainMenuScreen) {
                withGLContext {
                    UncivGame.Current.replaceCurrentScreen(MainMenuScreen())
                }
            }
            withGLContext {
                UncivGame.Current.screen?.openOptionsPopup(tabs.activePage)
            }
        }
    }

    fun addCheckbox(
        table: Table,
        text: String,
        initialState: Boolean,
        updateWorld: Boolean = false,
        action: ((Boolean) -> Unit)
    ) {
        val checkbox = text.toCheckBox(initialState) {
            action(it)
            settings.save()
            val worldScreen = UncivGame.Current.getWorldScreenIfActive()
            if (updateWorld && worldScreen != null) worldScreen.shouldUpdate = true
        }
        val cell = table.add(checkbox).colspan(2).left()
        if (settings.skin == "Minimal") {
            val availableWidth =
                    (tabs.maxWidth - table.padLeft - table.padRight - cell.padLeft - cell.padRight - 20f).coerceAtLeast(
                        1f
                    )
            val indicatorWidth =
                    checkbox.image.prefWidth + checkbox.imageCell.padLeft + checkbox.imageCell.padRight
            val labelWidth =
                    (availableWidth - checkbox.padLeft - checkbox.padRight - indicatorWidth - checkbox.labelCell.padLeft - checkbox.labelCell.padRight).coerceAtLeast(
                        1f
                    )
            checkbox.label.wrap = true
            checkbox.labelCell.width(labelWidth)
            cell.width(availableWidth).fillX()
        }
        cell.row()
    }

    fun addCheckbox(table: Table,
                    text: String,
                    settingsProperty: KMutableProperty0<Boolean>,
                    updateWorld: Boolean = false,
                    action: (Boolean) -> Unit = {}) {
        addCheckbox(table, text, settingsProperty.get(), updateWorld) {
            action(it)
            settingsProperty.set(it)
        }
    }

    /** Добавляет строку настройки; на узкой Royal-панели подпись расположена над контролом. */
    fun addControlRow(
        table: Table,
        text: String,
        control: com.badlogic.gdx.scenes.scene2d.Actor,
        controlMinWidth: Float = 0f
    ) {
        val label = text.toLabel()
        if (settings.skin != "Minimal") {
            table.add(label).left().fillX()
            table.add(control).minWidth(controlMinWidth).pad(10f).row()
            return
        }
        val availableWidth =
                (tabs.maxWidth - table.padLeft - table.padRight - 20f).coerceAtLeast(1f)
        label.wrap = true
        if (availableWidth < 600f) {
            table.add(label).colspan(2).width((availableWidth - 10f).coerceAtLeast(1f)).left().row()
            table.add(control).colspan(2).width((availableWidth - 20f).coerceAtLeast(1f)).pad(10f)
                .fillX().row()
        } else {
            val controlWidth = maxOf(
                controlMinWidth,
                availableWidth * 0.4f
            ).coerceAtMost(availableWidth * 0.5f)
            table.add(label).width((availableWidth - controlWidth - 30f).coerceAtLeast(1f)).left()
            table.add(control).width(controlWidth).pad(10f).fillX().row()
        }
    }

    /** Обновляет контраст динамического содержимого только внутри светлых панелей настроек. */
    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
        if (settings.skin == "Minimal") {
            fun styleContent(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                when (actor) {
                    is com.badlogic.gdx.scenes.scene2d.ui.CheckBox -> {
                        if (actor.style.fontColor == Color.WHITE) {
                            actor.style =
                                    com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle(actor.style)
                                        .apply {
                                            fontColor = Color.valueOf("453925")
                                            disabledFontColor = Color.valueOf("70634c")
                                        }
                        }
                    }

                    is com.badlogic.gdx.scenes.scene2d.ui.Button -> Unit
                    is Label -> {
                        if (actor.style.fontColor == Color.WHITE && actor.color == Color.WHITE) {
                            actor.style = Label.LabelStyle(actor.style)
                                .apply { fontColor = Color.valueOf("453925") }
                        }
                    }

                    is Table -> {
                        // Вложенные карточки со своим фоном сохраняют собственный контракт контраста.
                        if (actor.background == null) for (child in actor.children) styleContent(
                            child
                        )
                    }

                    is com.badlogic.gdx.scenes.scene2d.Group -> {
                        for (child in actor.children) styleContent(child)
                    }
                }
            }
            for (scroll in tabs.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()) {
                scroll.actor?.let { styleContent(it) }
            }
        }
        super.draw(batch, parentAlpha)
    }
}

class SelectItem<T>(val label: String, val value: T) {
    override fun toString(): String = label.tr()
    override fun equals(other: Any?): Boolean = other is SelectItem<*> && value == other.value
    override fun hashCode(): Int = value.hashCode()
}

/**
 * For creating a SelectBox that is automatically backed by a [GameSettings] property.
 *
 * **Warning:** [T] has to be the same type as the [GameSetting.kClass] of the [GameSetting] argument.
 *
 * This will also automatically send [SettingsPropertyChanged] events.
 */
open class SettingsSelect<T : Any>(
    labelText: String,
    items: Iterable<SelectItem<T>>,
    private val setting: GameSetting,
    settings: GameSettings
) {
    private val settingsProperty: KMutableProperty0<T> = setting.getProperty(settings)
    private val label = createLabel(labelText)
    protected val refreshSelectBox = createSelectBox(items.toGdxArray(), settings)
    val items by refreshSelectBox::items

    private fun createLabel(labelText: String): Label {
        val selectLabel = labelText.toLabel()
        selectLabel.wrap = true
        return selectLabel
    }

    private fun createSelectBox(initialItems: Array<SelectItem<T>>, settings: GameSettings): SelectBox<SelectItem<T>> {
        val selectBox = SelectBox<SelectItem<T>>(BaseScreen.skin)
        selectBox.items = initialItems

        selectBox.selected = initialItems.firstOrNull() { it.value == settingsProperty.get() } ?: items.first()
        selectBox.onChange {
            val newValue = selectBox.selected.value
            settingsProperty.set(newValue)
            sendChangeEvent(newValue)
            settings.save()
        }

        return selectBox
    }

    fun onChange(listener: (event: ChangeListener.ChangeEvent?) -> Unit) {
        refreshSelectBox.onChange(listener)
    }

    fun addTo(table: Table) {
        table.add(label).growX().left()
        table.add(refreshSelectBox).row()
    }

    /** Maintains the currently selected item if possible, otherwise resets to the first item */
    fun replaceItems(options: Array<SelectItem<T>>) {
        val prev = refreshSelectBox.selected
        refreshSelectBox.items = options
        refreshSelectBox.selected = prev
    }

    private fun sendChangeEvent(item: T) {
        when (item) {
            is UncivSound -> EventBus.send(object : SettingsPropertyUncivSoundChanged {
                override val gameSetting = setting
                override val value: UncivSound = settingsProperty.get() as UncivSound
            })
            else -> EventBus.send(object : SettingsPropertyChanged {
                override val gameSetting = setting
            })
        }
    }
}
