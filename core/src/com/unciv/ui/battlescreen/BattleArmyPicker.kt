package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.unciv.pure.application.battle.recruitment.BattleArmyDraft
import com.unciv.pure.application.battle.recruitment.BattleArmySelection
import com.unciv.pure.application.battle.recruitment.BattleArmyStack
import com.unciv.pure.application.battle.recruitment.BattleArmyTemplate
import com.unciv.pure.application.battle.recruitment.BattleArmyTemplates
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.random.Random
import com.unciv.ui.utils.extensions.setFontSize

/** Shared editor for local and server-authoritative recruitment. */
class BattleArmyPicker(
    private val selection: BattleArmySelection,
    private val onChanged: (Boolean) -> Unit = {}
) : Table() {
    private val draft = BattleArmyDraft(selection)
    private val summary = label("").apply { setWrap(true) }
    private val feedback = label("Templates are editable.").apply { setWrap(true) }
    private val grid = Table()
    private val templates = SelectBox<String>(BaseScreen.skin).apply {
        setItems(*BattleArmyTemplate.values().map { it.title }.toTypedArray())
    }
    private val applyTemplate = "Replace army with preset".toTextButton()
    private val randomArmy = "Replace with random army".toTextButton()
    private var updating = false
    private var layoutWidth = -1
    private data class Card(
        val panel: Table, val type: SelectBox<String>, val typeCell: Cell<SelectBox<String>>,
        val portrait: Group, val amount: Label, val price: Label,
        val minusTen: TextButton, val minus: TextButton, val plus: TextButton,
        val plusTen: TextButton, val maximum: TextButton, val clear: TextButton,
        var shownUnit: String? = null
    )
    private val cards = mutableListOf<Card>()

    private fun label(text: String) = text.toLabel(fontSize = 18)

    init {
        defaults().pad(3f)
        add(summary).width(420f).minHeight(26f).row()
        applyTemplate.label.setFontSize(18)
        randomArmy.label.setFontSize(18)
        applyTemplate.pad(4f, 10f, 4f, 10f)
        randomArmy.pad(4f, 10f, 4f, 10f)
        val toolbar = Table().apply {
            defaults().pad(4f).minHeight(38f)
            add(templates).width(180f)
            add(applyTemplate).minWidth(72f)
            add(randomArmy).minWidth(92f)
        }
        add(toolbar).row()
        add(feedback).width(420f).minHeight(22f).row()
        add(grid).row()
        repeat(selection.maxStacks) { slot ->
            val type = SelectBox<String>(BaseScreen.skin).apply {
                setItems(*this@BattleArmyPicker.selection.units.map { it.unitName }.toTypedArray())
            }
            val portrait = Group().apply { setSize(88f, 88f) }
            val amount = label("0").apply {
                setAlignment(Align.center)
                setFontSize(22)
            }
            val price = label("").apply {
                setFontSize(16)
                setWrap(true)
            }

            fun button(text: String, action: () -> Unit): TextButton = text.toTextButton().apply {
                label.setFontSize(16)
                pad(3f, 4f, 3f, 4f)
                onClick {
                    if (!isDisabled && !draft.locked) {
                        action()
                        feedback.setText("Templates are editable.")
                        refresh()
                    }
                }
            }

            val minusTen = button("−10") { draft.adjust(slot, -10) }
            val minus = button("−") { draft.adjust(slot, -1) }
            val plus = button("+") { draft.adjust(slot, 1) }
            val plusTen = button("+10") { draft.adjust(slot, 10) }
            val maximum = button("Max") { draft.fill(slot) }
            val clear = button("Clear") { draft.clear(slot) }
            val details = Table()
            details.defaults().pad(2f)
            details.add(label("Stack ${slot + 1}")).left().row()
            val typeCell = details.add(type).width(292f).minHeight(34f).left()
            details.row()
            details.add(price).growX().minHeight(22f).left().row()
            val controls = Table().apply {
                defaults().pad(2f).minHeight(38f)
                add(minusTen).width(44f)
                add(minus).width(36f)
                add(amount).width(44f)
                add(plus).width(36f)
                add(plusTen).width(44f)
                add(maximum).width(52f)
                add(clear).width(52f)
            }
            val panel = Table().apply {
                background = BaseScreen.skin.newDrawable(
                    "RoundedEdgeRectangle",
                    Color(0.10f, 0.15f, 0.25f, 1f)
                )
                pad(6f)
                add(portrait).size(88f).padRight(6f)
                add(details).growX().row()
                add(controls).colspan(2).center().padTop(4f).row()
            }
            cards += Card(
                panel, type, typeCell, portrait, amount, price,
                minusTen, minus, plus, plusTen, maximum, clear
            )
            type.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (updating) return
                    if (!draft.locked) draft.choose(
                        slot,
                        selection.units[type.selectedIndex].unitName
                    )
                    refresh()
                }
            })
        }
        applyTemplate.onClick {
            if (!applyTemplate.isDisabled && !draft.locked) {
                useTemplate(BattleArmyTemplate.values()[templates.selectedIndex])
            }
        }
        randomArmy.onClick {
            if (!randomArmy.isDisabled && !draft.locked) generate(random = Random.Default)
        }
        arrange(420)
        refresh()
    }

    /** Uses stage coordinates so the layout also follows the game's UI scale. */
    override fun act(delta: Float) {
        super.act(delta)
        val available = stage?.width?.minus(64f)?.toInt() ?: return
        arrange(available.coerceIn(360, 1000))
    }

    private fun arrange(available: Int) {
        if (layoutWidth == available) return
        layoutWidth = available
        val rowWidth = available.toFloat() - 8f
        val wide = available >= 900
        grid.clearChildren()
        cards.forEachIndexed { index, card ->
            card.panel.clearChildren()
            card.panel.background = BaseScreen.skin.newDrawable(
                "RoundedEdgeRectangle", Color(0.10f, 0.15f, 0.25f, 1f)
            ).apply {
                minHeight = 0f
                minWidth = 0f
            }
            card.panel.pad(2f)
            card.portrait.setScale(32f / 88f)
            val controls = Table().apply {
                defaults().pad(1f).height(30f)
                add(card.minusTen).width(44f)
                add(card.minus).width(32f)
                add(card.amount).width(44f)
                add(card.plus).width(32f)
                add(card.plusTen).width(44f)
                add(card.maximum).width(48f)
                add(card.clear).width(48f)
            }
            val identity = Table().apply {
                defaults().pad(1f)
                add(label("${index + 1}")).width(24f)
                add(card.portrait).size(32f)
                add(card.type).width(if (wide) rowWidth - 582f else rowWidth - 80f).height(30f)
            }
            if (wide) {
                card.panel.add(identity).left()
                card.panel.add(card.price).width(180f).padLeft(6f).padRight(6f)
                card.panel.add(controls).right()
            } else {
                card.panel.add(identity).growX().row()
                card.panel.add(card.price).growX().minHeight(22f).row()
                card.panel.add(controls).center()
            }
            card.panel.name = "armyStack${index + 1}"
            grid.add(card.panel).width(rowWidth).pad(2f).row()
        }
        getCell(summary).width(rowWidth)
        getCell(feedback).width(rowWidth)
        invalidateHierarchy()
    }

    fun useTemplate(template: BattleArmyTemplate = BattleArmyTemplate.BALANCED) {
        if (!draft.locked) generate(template)
    }

    private fun generate(
        template: BattleArmyTemplate = BattleArmyTemplate.BALANCED,
        random: Random? = null
    ) {
        if (draft.locked) return
        val replacement = try {
            BattleArmyTemplates.create(selection, template, random)
        } catch (failure: IllegalArgumentException) {
            feedback.setText(failure.message ?: "Template unavailable")
            return
        }
        val replace: () -> Unit = {
            if (!draft.locked) {
                setArmy(replacement)
                feedback.setText(
                    if (random == null) "${template.title} — edit any stack below."
                    else "Random army — edit any stack below."
                )
            }
        }
        if (draft.stacks().isEmpty()) {
            replace()
        } else {
            val owner = stage ?: return
            com.unciv.ui.popup.ConfirmPopup(
                owner,
                "Replace your entire army with ${if (random == null) "the selected preset" else "a random army"}?\nYour current selection will be lost.",
                "Replace army",
                action = replace
            ).open()
        }
    }

    fun selectedArmy(): List<BattleArmyStack> = selection.validate(draft.stacks())

    fun trySelectedArmy(): List<BattleArmyStack>? = try {
        selectedArmy()
    } catch (failure: IllegalArgumentException) {
        feedback.setText(failure.message ?: "Choose at least one soldier")
        onChanged(false)
        null
    }

    fun setArmy(stacks: List<BattleArmyStack>) {
        draft.restore(stacks)
        feedback.setText("Templates are editable.")
        refresh()
    }

    fun setLocked(locked: Boolean) {
        draft.locked = locked
        refresh()
    }

    private fun refresh() {
        updating = true
        try {
            templates.isDisabled = draft.locked
            applyTemplate.isDisabled = draft.locked
            randomArmy.isDisabled = draft.locked
            cards.forEachIndexed { slot, card ->
                val name = draft.unitName(slot)
                card.type.selectedIndex = selection.units.indexOfFirst { it.unitName == name }
                card.type.isDisabled = draft.locked
                val count = draft.count(slot)
                card.amount.setText(count.toString())
                card.price.setText(
                    "${draft.unitCost(slot)} × $count = ${
                        count.toLong() * draft.unitCost(
                            slot
                        )
                    }"
                )
                val canAdd = count < draft.maximum(slot)
                card.minusTen.isDisabled = draft.locked || count == 0
                card.minus.isDisabled = draft.locked || count == 0
                card.clear.isDisabled = draft.locked || count == 0
                card.plus.isDisabled = draft.locked || !canAdd
                card.plusTen.isDisabled = draft.locked || !canAdd
                card.maximum.isDisabled = draft.locked || !canAdd
                if (card.shownUnit != name) {
                    card.portrait.clearChildren()
                    ImageGetter.getLayeredImageColored(
                        "TileSets/AbsoluteUnits/Units/$name",
                        null,
                        null,
                        null
                    )
                        .forEach { layer ->
                            layer.setSize(88f, 88f)
                            card.portrait.addActor(layer)
                        }
                    card.shownUnit = name
                }
                card.amount.color = if (count == 0) Color.LIGHT_GRAY else Color.GOLD
            }
            val stacks = draft.stacks()
            summary.setText("Coins: ${draft.remaining()} / ${selection.budget}     Stacks: ${stacks.size}/${selection.maxStacks}")
            onChanged(stacks.isNotEmpty() && draft.cost() <= selection.budget)
        } finally {
            updating = false
        }
    }
}
