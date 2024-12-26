package com.example.devtools

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
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
    val upgradesTo: String? = null
)

class JsonEditorApp : ApplicationAdapter() {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var table: Table

    private val mapper = ObjectMapper().registerModule(KotlinModule())
    private val unitList = mutableListOf<UnitData>()
    private val jsonFile = "devtools/src/main/resources/Units.json"

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

        // Создаем увеличенный шрифт
        val font = BitmapFont().apply { data.setScale(2f) }
        skin.add("default-font", font)

        // Регистрируем стили для UI-элементов
        val textButtonStyle = TextButton.TextButtonStyle().apply {
            this.font = font
        }
        skin.add("default", textButtonStyle)

        val labelStyle = Label.LabelStyle().apply {
            this.font = font
        }
        skin.add("default", labelStyle)

        val textFieldStyle = TextField.TextFieldStyle().apply {
            this.font = font
            this.fontColor = Color.WHITE
            this.background = null
        }
        skin.add("default", textFieldStyle)

        val scrollPaneStyle = ScrollPane.ScrollPaneStyle()
        skin.add("default", scrollPaneStyle)

        return skin
    }

    private fun loadJson() {
        val file = File(jsonFile)
        println("Looking for JSON file at: ${file.absolutePath}")
        if (file.exists()) {
            println("JSON file found!")
            unitList.addAll(mapper.readValue(file))
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
        saveButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                saveJson()
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
            val nameField = TextField(unit.name, skin).apply {
                text = unit.name // Устанавливаем начальный текст
                addListener(object : ClickListener() { // Добавляем слушатель на взаимодействие
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener { // Слушаем изменения текста
                            unit.name = this@apply.text // Сохраняем изменения в объект
                            true
                        }
                    }
                })
            }
            val typeField = TextField(unit.unitType, skin).apply {
                text = unit.unitType
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener {
                            unit.unitType = this@apply.text
                            true
                        }
                    }
                })
            }


            val movementField = TextField(unit.movement, skin).apply {
                text = unit.movement
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener {
                            unit.movement = this@apply.text
                            true
                        }
                    }
                })
            }
            val speedField = TextField(unit.speed, skin).apply {
                text = unit.speed
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener {
                            unit.speed = this@apply.text
                            true
                        }
                    }
                })
            }
            val healthField = TextField(unit.health, skin).apply {
                text = unit.health
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener {
                            unit.health = this@apply.text
                            true
                        }
                    }
                })
            }
            val damageField = TextField(unit.damage, skin).apply {
                text = unit.damage
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        addListener {
                            unit.damage = this@apply.text
                            true
                        }
                    }
                })
            }

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


