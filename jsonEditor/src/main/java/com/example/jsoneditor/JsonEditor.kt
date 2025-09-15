package com.example.jsoneditor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

data class UnitData(
    var name: String = "",
    var unitType: String = "",
    var movement: String = "",
    var speed: String = "",
    var health: String = "",
    var damage: String = "",
    var cost: String? = null,
    var strength: String? = null,
    var uniques: List<String>? = null,
    var promotions: List<String>? = null,
    var requiredTech: String? = null,
    var civilopediaText: List<String>? = null
)

class JsonEditor : Application() {

    private val mapper = jacksonObjectMapper()
    private var unitList = mutableListOf<UnitData>()
    private val tableView = TableView<UnitData>()

    override fun start(primaryStage: Stage) {
        val root = BorderPane()

        // Top Menu
        val menuBar = MenuBar()
        val fileMenu = Menu("File")
        val openItem = MenuItem("Open")
        val saveItem = MenuItem("Save")
        fileMenu.items.addAll(openItem, saveItem)
        menuBar.menus.add(fileMenu)
        root.top = menuBar

        // Table View
        setupTableView()
        root.center = tableView

        // Add/Remove Buttons
        val addButton = Button("Add Unit")
        val removeButton = Button("Remove Selected")
        val buttonBox = HBox(10.0, addButton, removeButton)
        root.bottom = buttonBox

        // Open JSON File
        openItem.setOnAction {
            val fileChooser = FileChooser().apply { title = "Open JSON File" }
            val file = fileChooser.showOpenDialog(primaryStage)
            file?.let { loadJson(it) }
        }

        // Save JSON File
        saveItem.setOnAction {
            val fileChooser = FileChooser().apply { title = "Save JSON File" }
            val file = fileChooser.showSaveDialog(primaryStage)
            file?.let { saveJson(it) }
        }

        // Add Unit
        addButton.setOnAction {
            val newUnit = UnitData(name = "New Unit")
            unitList.add(newUnit)
            tableView.items.setAll(unitList)
        }

        // Remove Selected Unit
        removeButton.setOnAction {
            val selectedUnit = tableView.selectionModel.selectedItem
            unitList.remove(selectedUnit)
            tableView.items.setAll(unitList)
        }

        // Show Scene
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.title = "JSON Editor"
        primaryStage.show()
    }

    private fun setupTableView() {
        val nameColumn = TableColumn<UnitData, String>("Name").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("name")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.name = it.newValue }
        }

        val unitTypeColumn = TableColumn<UnitData, String>("Unit Type").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("unitType")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.unitType = it.newValue }
        }

        val movementColumn = TableColumn<UnitData, String>("Movement").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("movement")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.movement = it.newValue }
        }

        val speedColumn = TableColumn<UnitData, String>("Speed").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("speed")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.speed = it.newValue }
        }

        val healthColumn = TableColumn<UnitData, String>("Health").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("health")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.health = it.newValue }
        }

        val damageColumn = TableColumn<UnitData, String>("Damage").apply {
            cellValueFactory = javafx.scene.control.cell.PropertyValueFactory("damage")
            cellFactory = TextFieldTableCell.forTableColumn()
            setOnEditCommit { it.rowValue.damage = it.newValue }
        }

        tableView.columns.addAll(nameColumn, unitTypeColumn, movementColumn, speedColumn, healthColumn, damageColumn)
        tableView.isEditable = true
    }

    private fun loadJson(file: File) {
        unitList = mapper.readValue(file)
        tableView.items.setAll(unitList)
    }

    private fun saveJson(file: File) {
        mapper.writeValue(file, unitList)
    }
}

fun main() {
    Application.launch(JsonEditorApp::class.java)
}
