package com.example.devtools.units

import com.fasterxml.jackson.databind.node.ObjectNode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class UnitEditorWindow(private val projectRoot: Path) : JFrame("Редактор юнитов") {
    private var selectedTabIndex = 0
    private val imagePanels = linkedMapOf<String, List<UnitImagePanel>>()
    private var document: UnitDocument? = null
    private var selected: ObjectNode? = null
    private var form: UnitForm? = null
    private var refreshing = false
    private val unitNames = DefaultListModel<String>()
    private val list = JList(unitNames)
    private val search = JTextField()
    private val details = JPanel(BorderLayout())
    private val status = JLabel("Откройте игровой Units.json")
    private var backupDirectory = projectRoot.resolve("devtools/backups")

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        minimumSize = Dimension(1000, 700)
        setSize(1400, 900)
        setLocationRelativeTo(null)
        layout = BorderLayout(8, 8)
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        fun action(label: String, block: () -> Unit) {
            toolbar.add(JButton(label).apply { addActionListener { safely(block) } })
        }
        action("Открыть") { chooseFile() }
        action("Новый") { createUnit(false) }
        action("Копировать") { createUnit(true) }
        action("Сохранить") { save() }
        action("Экспорт JSON") { export() }
        action("Бэкап JSON с диска") {
            val doc = requireDocument()
            status.text = "Резервная копия: ${doc.backup(backupDirectory)}"
        }
        action("Папка бэкапов") {
            val chooser = JFileChooser(backupDirectory.toFile())
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                backupDirectory = chooser.selectedFile.toPath().toAbsolutePath()
        }
        action("Проверить") {
            commitForm()
            val errors = requireDocument().validate()
            showText(
                if (errors.isEmpty()) "Ошибок параметров и ссылок не найдено" else errors.joinToString(
                    "\n"
                )
            )
        }
        add(toolbar, BorderLayout.NORTH)
        val catalog = JPanel(BorderLayout(4, 4))
        catalog.add(JPanel(BorderLayout()).apply {
            add(JLabel("Поиск по имени / типу"), BorderLayout.NORTH)
            add(search, BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        catalog.add(JScrollPane(list), BorderLayout.CENTER)
        val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalog, details)
        split.dividerLocation = 260
        split.resizeWeight = 0.2
        add(split, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener { event ->
            if (!event.valueIsAdjusting && !refreshing && list.selectedValue != null) safely {
                try {
                    commitForm()
                    val next = requireDocument().units.first {
                        it.path("name").asText() == list.selectedValue
                    }
                    showUnit(next)
                } catch (error: Exception) {
                    refreshList()
                    throw error
                }
            }
        }
        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = refreshList()
            override fun removeUpdate(event: DocumentEvent) = refreshList()
            override fun changedUpdate(event: DocumentEvent) = refreshList()
        })
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                safely { if (canDiscard()) dispose() }
            }
        })
        val defaultFile =
            projectRoot.resolve("android/assets/jsons/Civ V - Gods & Kings/Units.json")
        if (Files.isRegularFile(defaultFile)) safely { open(defaultFile) }
    }

    private fun requireDocument() = document ?: error("Сначала откройте Units.json")

    private fun chooseFile() {
        if (!canDiscard()) return
        val chooser = JFileChooser(projectRoot.resolve("android/assets/jsons").toFile())
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Units.json", "json")
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) open(chooser.selectedFile.toPath())
    }

    private fun open(path: Path) {
        require(path.fileName.toString() == "Units.json") { "Выберите файл Units.json в папке набора правил" }
        val loaded = UnitDocument(path.toAbsolutePath())
        document = loaded
        selected = null
        form = null
        imagePanels.clear()
        details.removeAll()
        refreshList()
        loaded.units.firstOrNull()?.let { showUnit(it); refreshList() }
        title = "Редактор юнитов — ${path.toAbsolutePath()}"
        status.text =
                "${loaded.units.size} юнитов. Изображения общие для встроенных наборов. Комментарии JSON сохраняются в бэкапах."
    }

    private fun refreshList() {
        refreshing = true
        try {
            unitNames.clear()
            val query = search.text.trim()
            document?.units?.filter {
                it.path("name").asText().contains(query, true) || it.path("unitType").asText()
                    .contains(query, true)
            }?.forEach { unitNames.addElement(it.path("name").asText()) }
            list.setSelectedValue(selected?.path("name")?.asText(), true)
        } finally {
            refreshing = false
        }
    }

    private fun showUnit(unit: ObjectNode) {
        selected = unit
        val editor = UnitForm(requireDocument(), unit)
        form = editor
        details.removeAll()
        val tabs = JTabbedPane()
        val name = unit.path("name").asText()
        tabs.addTab(
            "Параметры — $name",
            JScrollPane(editor).apply { verticalScrollBar.unitIncrement = 20 }
        )
        val builtinRoot =
                projectRoot.resolve("android/assets/jsons").toFile().canonicalFile.toPath()
        if (requireDocument().file.toFile().canonicalFile.toPath().startsWith(builtinRoot)) {
            val graphics = JPanel(java.awt.GridLayout(1, 2, 8, 8))
            val panels = imagePanels.getOrPut(name) {
                UnitImages.Kind.values().map { UnitImagePanel(projectRoot, name, it) }
            }
            panels.forEach { graphics.add(it) }
            tabs.addTab("Спрайт и иконка", JScrollPane(graphics))
        } else {
            tabs.addTab(
                "Графика",
                JLabel("Импорт графики доступен для встроенных наборов правил проекта")
            )
        }
        tabs.selectedIndex = selectedTabIndex.coerceIn(0, tabs.tabCount - 1)
        tabs.addChangeListener { selectedTabIndex = tabs.selectedIndex }
        details.add(tabs, BorderLayout.CENTER)
        details.revalidate()
        details.repaint()
    }

    private fun commitForm() {
        val current = selected ?: return
        val value = form?.value() ?: return
        if (value != current) {
            current.removeAll()
            current.setAll<ObjectNode>(value)
        }
    }

    private fun canDiscard(): Boolean {
        val doc = document ?: return true
        val pending = runCatching { form?.value() != selected }.getOrDefault(true)
        if (!doc.isDirty() && !pending && imagePanels.values.flatten()
                    .none { it.dirty }
        ) return true
        val choice = JOptionPane.showOptionDialog(
            this, "Есть несохранённые изменения параметров или изображений", "Редактор юнитов",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            arrayOf("Сохранить", "Отбросить", "Отмена"), "Отмена"
        )
        if (choice == 0) {
            save(); return true
        }
        return choice == 1
    }

    private fun createUnit(copy: Boolean) {
        commitForm()
        val doc = requireDocument()
        val template = if (copy) selected ?: error("Выберите юнита для копирования") else null
        val name =
            JOptionPane.showInputDialog(this, "Имя нового юнита (также имя спрайта и иконки)")
                ?: return
        val created = doc.addUnit(name, template)
        search.text = ""
        showUnit(created)
        refreshList()
        status.text = "Новый юнит добавлен в документ. Настройте параметры и сохраните."
    }

    private fun save() {
        commitForm()
        val doc = requireDocument()
        doc.save(backupDirectory)
        try {
            imagePanels.values.flatten().forEach { it.save(backupDirectory) }
        } catch (error: Exception) {
            throw IllegalStateException(
                "JSON сохранён, но не все изображения записаны. Несохранённые изображения остаются в редакторе. ${error.message}",
                error
            )
        }
        status.text =
                "JSON и исходники изображений сохранены. Для игры пересоберите атласы (см. devtools/README.md)."
    }

    private fun export() {
        commitForm()
        val doc = requireDocument()
        val chooser = JFileChooser(doc.file.parent.toFile())
        chooser.selectedFile = java.io.File("Units-export.json")
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return
        val target = chooser.selectedFile.toPath().toAbsolutePath()
        require(target.toFile().canonicalFile != doc.file.toFile().canonicalFile) {
            "Для записи в исходный файл используйте Сохранить"
        }
        if (Files.exists(target)) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Заменить $target?",
                    "Экспорт",
                    JOptionPane.YES_NO_OPTION
                ) != JOptionPane.YES_OPTION
            ) return
            Files.copy(
                target,
                target.resolveSibling(target.fileName.toString() + "." + UUID.randomUUID() + ".bak")
            )
        }
        UnitDocument.atomicWrite(target, doc.serialized())
        status.text = "Экспортировано: $target. Исходный документ ещё не сохранён."
    }

    private fun showText(text: String) {
        val area = JTextArea(text, 18, 85)
        area.isEditable = false
        area.lineWrap = true
        area.wrapStyleWord = true
        JOptionPane.showMessageDialog(
            this,
            JScrollPane(area),
            "Редактор юнитов",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun safely(block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            status.text = "Ошибка: ${error.message}"
            showText(error.message ?: error.javaClass.simpleName)
        }
    }
}
