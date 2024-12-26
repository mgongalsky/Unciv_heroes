package com.example.devtools

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.fasterxml.jackson.annotation.JsonAnySetter

import java.io.File

data class CivilopediaEntry(
    var text: String?
)


//@JsonIgnoreProperties(ignoreUnknown = true)
data class UnitData(
    var name: String,
    var unitType: String,
    var movement: String?,
    var speed: String?,
    var health: String?,
    var damage: String?,
    var uniques: List<String>?,
    var cost: String?,
    var strength: String?,
    var requiredTech: String?,
    var upgradesTo: String?,
    var rangedStrength: String?,
    var replaces: String?,
    var uniqueTo: String?,
    var attackSound: String?,
    var attackSkill: String?,
    var promotions: List<String>?,
    var obsoleteTech: String?,
    var requiredResource: String?,
    var hurryCostModifier: String?,
    var range: String?,
    var religiousStrength: String?,
    var interceptRange: String?,
    var civilopediaText: List<CivilopediaEntry>? // Здесь используется список объектов
)

class JsonEditorApp : ApplicationAdapter() {
    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private lateinit var table: Table

    private val mapper = ObjectMapper().registerModule(KotlinModule())
    private val unitList = mutableListOf<UnitData>()
    private val jsonFile = "devtools/src/main/resources/jsons/Units.json"

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

        // Создаём шрифт
        val font = BitmapFont()
        font.data.setScale(2f) // Увеличиваем размер шрифта для лучшей читаемости
        skin.add("default-font", font)

        // Создаём стандартный стиль для TextField
        val textFieldStyle = TextField.TextFieldStyle()
        textFieldStyle.font = font
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.BLACK
        textFieldStyle.background = createBackground(com.badlogic.gdx.graphics.Color.GRAY)
        textFieldStyle.focusedBackground = createBackground(com.badlogic.gdx.graphics.Color.LIGHT_GRAY)
        textFieldStyle.cursor = createBackground(com.badlogic.gdx.graphics.Color.WHITE)
        skin.add("default", textFieldStyle)

        // Создаём стандартный стиль для Label
        val labelStyle = Label.LabelStyle()
        labelStyle.font = font
        labelStyle.fontColor = com.badlogic.gdx.graphics.Color.BLACK
        skin.add("default", labelStyle)

        // Добавляем стиль для ScrollPane
        val scrollPaneStyle = ScrollPane.ScrollPaneStyle()
        skin.add("default", scrollPaneStyle)

        // Создаём стиль для TextButton
        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.font = font
        textButtonStyle.fontColor = com.badlogic.gdx.graphics.Color.BLACK
        textButtonStyle.up = createBackground(com.badlogic.gdx.graphics.Color.DARK_GRAY)
        textButtonStyle.down = createBackground(com.badlogic.gdx.graphics.Color.LIGHT_GRAY)
        textButtonStyle.checked = createBackground(com.badlogic.gdx.graphics.Color.GRAY)
        skin.add("default", textButtonStyle)

        // Создаём стиль для SelectBox
        val selectBoxStyle = SelectBox.SelectBoxStyle().apply {
            this.font = font
            fontColor = com.badlogic.gdx.graphics.Color.BLACK
            background = createBackground(com.badlogic.gdx.graphics.Color.WHITE) // Фон SelectBox
            scrollStyle = ScrollPane.ScrollPaneStyle() // Стиль для скролл-бара
            listStyle = ListStyle().apply {
                this.font = font
                this.fontColorSelected = com.badlogic.gdx.graphics.Color.WHITE // Цвет текста для выделенного элемента
                this.fontColorUnselected = com.badlogic.gdx.graphics.Color.BLACK // Цвет текста для невыбранных элементов
                this.selection = createBackground(com.badlogic.gdx.graphics.Color.BLUE) // Цвет выделения
                this.background = createBackground(com.badlogic.gdx.graphics.Color.YELLOW) // Непрозрачный фон для списка
            }
        }
        skin.add("default", selectBoxStyle)

        return skin
    }

    private fun createBackground(color: com.badlogic.gdx.graphics.Color): Drawable {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val texture = com.badlogic.gdx.graphics.Texture(pixmap)
        pixmap.dispose() // Освобождаем ресурсы Pixmap после создания Texture
        return com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(texture))
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

    private fun loadUnitTypes(): com.badlogic.gdx.utils.Array<String> {
        val uniqueTypes = unitList.map { it.unitType }.distinct()
        return com.badlogic.gdx.utils.Array(uniqueTypes.toTypedArray())
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

        // Добавляем заголовки таблицы
        dataTable.add("Name").center()
        dataTable.add("Unit Type").center()
        dataTable.add("Movement").center()
        dataTable.add("Speed").center()
        dataTable.add("Health").center()
        dataTable.add("Damage").center()
        dataTable.add("Cost").center()
        dataTable.add("Required Tech").center()
        dataTable.row()

        // Загружаем уникальные unitType
        val unitTypes = loadUnitTypes()

        for (unit in unitList) {
            val nameField = TextField(unit.name, skin)

            // Создаем выпадающий список для unitType
            val unitTypeSelectBox = SelectBox<String>(skin)
            unitTypeSelectBox.setItems(unitTypes)
            unitTypeSelectBox.selected = unit.unitType // Устанавливаем текущее значение
            unitTypeSelectBox.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    unit.unitType = unitTypeSelectBox.selected // Обновляем значение unitType
                }
            })

            val movementField = TextField(unit.movement, skin)
            val speedField = TextField(unit.speed, skin)
            val healthField = TextField(unit.health, skin)
            val damageField = TextField(unit.damage, skin)

            // Поле для Cost
            val costField = TextField(unit.cost ?: "", skin)
            costField.setTextFieldFilter(TextField.TextFieldFilter { _, c -> c.isDigit() || c == '\b' }) // Только числа
            costField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.cost = if (costField.text.isBlank()) null else costField.text
                    return true
                }
            })

            // Поле для Required Tech
            val requiredTechField = TextField(unit.requiredTech ?: "", skin)
            requiredTechField.addListener(object : InputListener() {
                override fun keyTyped(event: InputEvent?, character: Char): Boolean {
                    unit.requiredTech = if (requiredTechField.text.isBlank()) null else requiredTechField.text
                    return true
                }
            })

            // Установка фильтра для числовых полей
            val numericFilter = TextField.TextFieldFilter { _, c ->
                c.isDigit() || c == '\b'
            }

            movementField.setTextFieldFilter(numericFilter)
            speedField.setTextFieldFilter(numericFilter)
            healthField.setTextFieldFilter(numericFilter)
            damageField.setTextFieldFilter(numericFilter)

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

            // Добавляем строки в таблицу
            dataTable.add(nameField).fillX()
            dataTable.add(unitTypeSelectBox).fillX()
            dataTable.add(movementField).fillX()
            dataTable.add(speedField).fillX()
            dataTable.add(healthField).fillX()
            dataTable.add(damageField).fillX()
            dataTable.add(costField).fillX()
            dataTable.add(requiredTechField).fillX()
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
        // Устанавливаем белый цвет для очистки экрана
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f) // Белый цвет (RGBA)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}


