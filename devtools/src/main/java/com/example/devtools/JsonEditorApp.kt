package com.example.devtools

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnitData(
    var name: String,
    var unitType: String,
    var movement: String,
    var speed: String,
    var health: String,
    var damage: String,
    val uniques: List<String> = emptyList(),
    val cost: String? = null,
    val strength: String? = null,
    val requiredTech: String? = null,
    val upgradesTo: String? = null,
   // val civilopediaText: List<String> = emptyList()
)

class JsonEditorApp : ApplicationAdapter() {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var table: Table

    private val mapper = ObjectMapper().registerModule(KotlinModule())
    private val unitList = mutableListOf<UnitData>()
    private val jsonFile = "devtools/src/main/resources/Units.json" // Замените на путь к вашему JSON-файлу

    override fun create() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        // Инициализация скина
        skin = initializeSkin()

        table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        loadJson()
        buildUI()
    }

    private fun initializeSkin(): Skin {
        val skin = Skin()

        // Добавляем стандартный шрифт
        val font = BitmapFont() // Используем стандартный BitmapFont
        skin.add("default-font", font)

        // Регистрируем стили для UI-элементов
        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.font = font
        skin.add("default", textButtonStyle)

        val labelStyle = Label.LabelStyle()
        labelStyle.font = font
        skin.add("default", labelStyle)

        val textFieldStyle = TextField.TextFieldStyle()
        textFieldStyle.font = font
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE
        skin.add("default", textFieldStyle)

        // Добавляем стиль для ScrollPane
        val scrollPaneStyle = ScrollPane.ScrollPaneStyle()
        scrollPaneStyle.vScrollKnob = null // Указываем стандартные графические элементы
        scrollPaneStyle.hScrollKnob = null
        skin.add("default", scrollPaneStyle)

        return skin
    }

    private fun loadJson() {
        val file = File(jsonFile)
        println("Looking for JSON file at: ${file.absolutePath}") // Вывод полного пути
        if (file.exists()) {
            println("JSON file found!")
            unitList.addAll(mapper.readValue<List<UnitData>>(file))
        } else {
            println("JSON file not found at: ${file.absolutePath}")
        }
    }

    private fun buildUI() {
        table.clear()

        val scrollPane = ScrollPane(buildDataTable(), skin)
        table.add(scrollPane).fill().expand()

        table.row()
        val saveButton = TextButton("Save", skin)
        saveButton.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                saveJson()
                return true
            }
        })
        table.add(saveButton).center().pad(10f)
    }

    private fun buildDataTable(): Table {
        val dataTable = Table(skin)
        dataTable.defaults().pad(5f)

        dataTable.add("Name").center()
        dataTable.add("Unit Type").center()
        dataTable.add("Movement").center()
        dataTable.add("Speed").center()
        dataTable.add("Health").center()
        dataTable.add("Damage").center()
        dataTable.row()

        for (unit in unitList) {
            val nameField = TextField(unit.name, skin)
            val typeField = TextField(unit.unitType, skin)
            val movementField = TextField(unit.movement, skin)
            val speedField = TextField(unit.speed, skin)
            val healthField = TextField(unit.health, skin)
            val damageField = TextField(unit.damage, skin)

            // Добавляем InputListener для TextField'ов
            nameField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.name = nameField.text
                    return true
                }
            })

            typeField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.unitType = typeField.text
                    return true
                }
            })

            movementField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.movement = movementField.text
                    return true
                }
            })

            speedField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.speed = speedField.text
                    return true
                }
            })

            healthField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.health = healthField.text
                    return true
                }
            })

            damageField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.damage = damageField.text
                    return true
                }
            })

            dataTable.add(nameField).fillX()
            dataTable.add(typeField).fillX()
            dataTable.add(movementField).fillX()
            dataTable.add(speedField).fillX()
            dataTable.add(healthField).fillX()
            dataTable.add(damageField).fillX()
            dataTable.row()
        }

        return dataTable
    }

    private fun saveJson() {
        val file = File(jsonFile)
        mapper.writeValue(file, unitList)
        println("JSON saved!")
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}
