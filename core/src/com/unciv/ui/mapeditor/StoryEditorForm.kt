package com.unciv.ui.mapeditor

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.setFontSize

/** Поля независимого черновика; capture читает ввод, но никогда не записывает карту. */
class StoryEditorForm(private val fieldWidth: Float) : Table(BaseScreen.skin) {
    private val readers = arrayListOf<() -> Unit>()
    init { defaults().pad(4f).growX(); top() }

    fun text(caption: String, value: String, multiline: Boolean = false, save: (String) -> Unit) {
        add(caption.toLabel().apply { wrap = true }).width(fieldWidth).row()
        val input: TextField = if (multiline) TextArea(value, skin) else TextField(value, skin)
        input.name = "story-field:$caption"
        val cell = add(input).width(fieldWidth)
        if (multiline) cell.height(110f)
        cell.row()
        readers.add { save(input.text) }
    }

    fun flag(caption: String, value: Boolean, save: (Boolean) -> Unit) {
        val input = CheckBox(caption.tr(), skin).apply { isChecked = value }
        add(input).row()
        readers.add { save(input.isChecked) }
    }

    /** Подписи отделены от стабильных значений ссылок. Неизвестное значение сохраняется для исправления. */
    fun choice(caption: String, value: String, options: List<Pair<String, String>>, save: (String) -> Unit) {
        data class Option(val id: String, val label: String) { override fun toString() = label }
        val values = options.map { Option(it.first, it.second) }.toMutableList()
        if (values.none { it.id == value }) values.add(Option(value, value))
        val input = SelectBox<Option>(skin).apply {
            setItems(*values.toTypedArray())
            selected = values.first { it.id == value }
        }
        add(caption.toLabel()).row()
        add(input).width(fieldWidth).row()
        readers.add { save(input.selected.id) }
    }

    fun position(caption: String, value: CityPosition?, save: (CityPosition?) -> Unit) {
        text(caption, value?.let { "${it.x},${it.y}" }.orEmpty()) { raw ->
            if (raw.isBlank()) save(null)
            else {
                val parts = raw.split(',').map { it.trim().toFloatOrNull() }
                require(parts.size == 2 && parts.all { it != null && it.isFinite() }) {
                    "Enter coordinates as x,y or leave empty"
                }
                save(CityPosition(parts[0]!!, parts[1]!!))
            }
        }
    }

    fun include(form: StoryEditorForm) {
        add(form).growX().row()
        readers.add { form.capture() }
    }

    fun capture() { readers.forEach { it() } }

    /** Наблюдает ввод во вложенных формах; нажатия командных кнопок обрабатываются отдельно. */
    fun observeChanges(changed: (com.badlogic.gdx.scenes.scene2d.Actor) -> Unit) {
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                if (actor is TextField || actor is SelectBox<*> || actor is CheckBox) changed(actor)
            }
        })
    }

    /** Уплотняет экземпляр формы; явный размер не зависит от исходного масштаба шрифта skin. */
    fun compact(): StoryEditorForm {
        fun visit(table: Table) {
            for (cell in table.cells) {
                cell.pad(1f)
                when (val actor = cell.actor) {
                    is StoryEditorForm -> visit(actor)
                    is CheckBox -> {
                        actor.label.setFontSize(16)
                        // Без переноса подпись сохраняет естественную ширину во внутренней таблице CheckBox.
                        actor.label.setWrap(false)
                        actor.left()
                        actor.imageCell.padRight(4f)
                        cell.width(fieldWidth).minHeight(30f)
                    }

                    is com.badlogic.gdx.scenes.scene2d.ui.TextButton -> {
                        actor.label.setFontSize(16)
                        cell.height(30f)
                    }

                    is com.badlogic.gdx.scenes.scene2d.ui.Label -> {
                        actor.setFontSize(16)
                        actor.setWrap(true)
                        cell.width(fieldWidth)
                    }

                    is TextArea -> cell.height(68f)
                    is TextField -> cell.height(30f)
                }
            }
            table.invalidateHierarchy()
        }
        visit(this)
        return this
    }

}
