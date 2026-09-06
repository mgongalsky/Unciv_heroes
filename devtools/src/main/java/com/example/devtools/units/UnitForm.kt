package com.example.devtools.units

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

/** Tree-shaped controls preserve nested fields without requiring the user to edit JSON. */
class UnitForm(private val document: UnitDocument, private val source: ObjectNode) : JPanel() {
    private val readers = linkedMapOf<String, Pair<JCheckBox, NodeControl>>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val fields = linkedSetOf<String>()
        fields.addAll(UnitDocument.stringFields.take(2))
        fields.addAll(UnitDocument.numericFields)
        fields.addAll(UnitDocument.stringFields.drop(2))
        fields.addAll(UnitDocument.listFields)
        fields.add("civilopediaText")
        source.fieldNames().forEachRemaining { fields.add(it) }
        for (field in fields) {
            val value = source.get(field)
            val present = JCheckBox(field, value != null)
            present.toolTipText =
                "Снимите флажок, чтобы убрать поле и использовать значение игры по умолчанию"
            val control = NodeControl(field, value ?: defaultValue(field), document)
            val row = JPanel(BorderLayout(8, 4))
            present.preferredSize = Dimension(190, 28)
            row.add(present, BorderLayout.WEST)
            row.add(control, BorderLayout.CENTER)
            row.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            row.alignmentX = Component.LEFT_ALIGNMENT
            add(row)
            readers[field] = present to control
            fun enabled() {
                enableTree(control, present.isSelected && field != "name")
            }
            present.addActionListener { enabled() }
            if (field == "name") present.isEnabled = false
            enabled()
        }
    }

    fun value(): ObjectNode = UnitDocument.mapper.createObjectNode().also { result ->
        for ((field, pair) in readers) if (pair.first.isSelected)
            result.set<JsonNode>(field, pair.second.value())
    }

    companion object {
        fun defaultValue(field: String): JsonNode = when {
            field in UnitDocument.numericFields -> UnitDocument.mapper.nodeFactory.numberNode(0)
            field in UnitDocument.listFields || field == "civilopediaText" -> UnitDocument.mapper.createArrayNode()
            else -> UnitDocument.mapper.nodeFactory.textNode("")
        }

        private fun enableTree(component: Component, enabled: Boolean) {
            component.isEnabled = enabled
            if (component is java.awt.Container) component.components.forEach {
                enableTree(
                    it,
                    enabled
                )
            }
        }
    }
}

private class NodeControl(
    private val field: String,
    private val original: JsonNode,
    private val document: UnitDocument
) : JPanel(BorderLayout(4, 4)) {
    private var readValue: () -> JsonNode = { original.deepCopy<JsonNode>() }

    init {
        when {
            original.isObject -> objectControls()
            original.isArray -> arrayControls()
            original.isBoolean -> {
                val check = JCheckBox("Да", original.asBoolean())
                add(check)
                readValue = { UnitDocument.mapper.nodeFactory.booleanNode(check.isSelected) }
            }

            else -> scalarControls()
        }
    }

    fun value(): JsonNode = readValue()

    private fun scalarControls() {
        val initial = if (original.isNull) "" else original.asText()
        val choices = document.choices(field)
        val readText: () -> String
        if (choices.isNotEmpty()) {
            val combo =
                JComboBox((listOf("") + choices + listOf(initial)).distinct().toTypedArray())
            combo.isEditable = true
            combo.selectedItem = initial
            add(combo)
            readText = { combo.editor.item?.toString().orEmpty() }
        } else {
            val text = JTextField(initial, 26)
            add(text)
            readText = { text.text }
        }
        readValue = {
            val text = readText()
            when {
                text == initial -> original.deepCopy<JsonNode>()
                field in UnitDocument.numericFields -> {
                    val holder = UnitDocument.mapper.createObjectNode()
                    holder.set<JsonNode>(field, original)
                    require(text.isNotBlank()) { "$field: введите число или снимите флажок поля" }
                    document.setText(holder, field, text)
                    holder.get(field)
                }

                original.isNumber -> {
                    val parsed = UnitDocument.mapper.readTree(text)
                    require(parsed != null && parsed.isNumber) { "$field: требуется число" }
                    parsed
                }

                else -> UnitDocument.mapper.nodeFactory.textNode(text)
            }
        }
    }

    private fun arrayControls() {
        val rows = JPanel()
        rows.layout = BoxLayout(rows, BoxLayout.Y_AXIS)
        val entries = mutableListOf<NodeControl>()
        fun append(value: JsonNode) {
            val control = NodeControl(field, value, document)
            val row = JPanel(BorderLayout(4, 4))
            row.add(control, BorderLayout.CENTER)
            row.add(JButton("−").apply {
                toolTipText = "Удалить элемент списка"
                addActionListener { entries.remove(control); rows.remove(row); rows.revalidate(); rows.repaint() }
            }, BorderLayout.EAST)
            entries.add(control)
            rows.add(row)
            rows.revalidate()
            rows.repaint()
        }
        original.forEach { append(it) }
        add(rows, BorderLayout.CENTER)
        add(JButton("Добавить строку").apply {
            addActionListener {
                append(
                    if (field == "civilopediaText") UnitDocument.mapper.createObjectNode()
                    .put("text", "")
                else original.firstOrNull()?.let {
                    when {
                        it.isObject -> UnitDocument.mapper.createObjectNode()
                        it.isNumber -> UnitDocument.mapper.nodeFactory.numberNode(0)
                        it.isBoolean -> UnitDocument.mapper.nodeFactory.booleanNode(false)
                        else -> UnitDocument.mapper.nodeFactory.textNode("")
                    }
                } ?: UnitDocument.mapper.nodeFactory.textNode(""))
            }
        }, BorderLayout.SOUTH)
        readValue = {
            UnitDocument.mapper.createArrayNode()
                .also { array -> entries.forEach { array.add(it.value()) } }
        }
    }

    private fun objectControls() {
        val rows = JPanel()
        rows.layout = BoxLayout(rows, BoxLayout.Y_AXIS)
        val entries = linkedMapOf<String, NodeControl>()
        fun append(key: String, value: JsonNode) {
            val control = NodeControl(key, value, document)
            val row = JPanel(BorderLayout(4, 4))
            row.add(JLabel(key), BorderLayout.WEST)
            row.add(control, BorderLayout.CENTER)
            row.add(JButton("−").apply {
                addActionListener { entries.remove(key); rows.remove(row); rows.revalidate(); rows.repaint() }
            }, BorderLayout.EAST)
            entries[key] = control
            rows.add(row)
            rows.revalidate()
            rows.repaint()
        }
        original.fields().forEachRemaining { append(it.key, it.value) }
        add(rows, BorderLayout.CENTER)
        add(JButton("Добавить поле").apply {
            addActionListener {
                val key = JOptionPane.showInputDialog(this@NodeControl, "Имя поля")?.trim()
                if (!key.isNullOrBlank() && key !in entries) append(
                    key,
                    UnitDocument.mapper.nodeFactory.textNode("")
                )
            }
        }, BorderLayout.SOUTH)
        readValue = {
            UnitDocument.mapper.createObjectNode().also { obj ->
                entries.forEach { (key, control) -> obj.set<JsonNode>(key, control.value()) }
            }
        }
    }
}
