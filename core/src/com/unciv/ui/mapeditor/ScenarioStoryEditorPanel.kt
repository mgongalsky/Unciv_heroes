package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.ScenarioStoryEditorCommands
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.util.UUID

/** Общий независимый черновик сюжета: переключение разделов не применяет изменения к карте. */
class ScenarioStoryEditorPanel(
    private val map: TileMap, private val ruleset: Ruleset, private val panelWidth: Float,
    private val changed: () -> Unit,
    private val highlight: (List<CityPosition>) -> Unit
) : Table(BaseScreen.skin) {
    private var draft = map.story?.deepCopy() ?: StoryScenario()
    private var section = "Zones"
    private var selectedZone: StoryZone? = null
    private var form: StoryEditorForm? = null
    private val error = "".toLabel(Color.SALMON).apply { wrap = true }
    private val widthForFields get() = (panelWidth - 40f).coerceAtLeast(100f)

    init { refresh() }

    /** Клик добавляет или убирает клетку только в выбранной зоне черновика. */
    fun selectTile(position: CityPosition) {
        val zone = selectedZone ?: return
        if (section != "Zones") return
        mutate {
            if (!zone.positions.remove(position)) zone.positions.add(position)
        }
    }

    private fun attempt(action: () -> Unit) {
        try {
            action()
            error.setText("")
        } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty().tr())
        }
    }

    /** Сначала читает поля, затем меняет структуру черновика и перестраивает виджеты. */
    private fun mutate(action: () -> Unit) = attempt {
        form?.capture()
        action()
        refresh()
    }

    private fun refresh() {
        clear()
        top()
        defaults().pad(4f).growX()
        val navigation = Table(skin).apply { defaults().pad(2f).growX() }
        for ((index, page) in listOf("Zones", "Triggers", "Dialogues", "Cinematics").withIndex()) {
            val button = page.toTextButton().apply {
                name = "scenario-story-section:$page"
                color = if (section == page) Color.SKY else Color.WHITE
            }.onClick {
                mutate { section = page }
                // Новый раздел начинается сверху, даже если предыдущий содержал длинный диалог.
                generateSequence(parent) { it.parent }
                    .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()
                    .firstOrNull()?.let { scroll ->
                        scroll.validate()
                        scroll.scrollY = 0f
                        scroll.updateVisualScroll()
                    }
            }
            navigation.add(button).width((widthForFields - 8f) / 2f)
            if (index % 2 == 1) navigation.row()
        }
        add(navigation).width(widthForFields).row()
        add("Apply story".toTextButton().apply { name = "scenario-story-apply" }.onClick {
            attempt {
                form?.capture()
                if (ScenarioStoryEditorCommands.apply(
                            map,
                            ruleset,
                            map.scenarioHeroes,
                            draft
                        )
                ) changed()
            }
        }).row()
        add("Discard changes".toTextButton().onClick {
            draft = map.story?.deepCopy() ?: StoryScenario()
            selectedZone = null
            error.setText("")
            refresh()
        }).row()
        add(error).width(widthForFields).row()
        val count = when (section) {
            "Zones" -> draft.zones.size
            "Triggers" -> draft.triggers.size
            "Dialogues" -> draft.dialogues.size
            else -> draft.cinematics.size
        }
        add("${section.tr()} ($count)".toLabel(Color.SKY).apply {
            name = "scenario-story-section-title"
            wrap = true
        }).width(widthForFields).row()
        if (count == 0) {
            val explanation = when (section) {
                "Zones" -> "No zones yet. Add a zone, then select its tiles on the map."
                "Triggers" -> "No triggers yet. Add a trigger to connect a game event to story effects."
                "Dialogues" -> "No dialogues yet. Add a dialogue to write lines and choices."
                else -> "No cinematics yet. Add a cinematic with a video path and fallback text."
            }
            add(explanation.toLabel().apply {
                name = "scenario-story-empty-section"
                wrap = true
            }).width(widthForFields).row()
        }
        val fields = StoryEditorForm(widthForFields)
        form = fields
        when (section) {
            "Zones" -> zones(fields)
            "Triggers" -> triggers(fields)
            "Dialogues" -> dialogues(fields)
            "Cinematics" -> cinematics(fields)
        }
        add(fields).growX().row()
        highlight(if (section == "Zones") selectedZone?.positions.orEmpty() else emptyList())
    }

    private fun zones(fields: StoryEditorForm) {
        fields.add("Select a zone, then click map tiles to toggle membership".toLabel().apply { wrap = true })
            .width(widthForFields).row()
        for (zone in draft.zones) {
            fields.add("${zone.name} (${zone.id})".toTextButton().onClick {
                mutate { selectedZone = zone }
            }).row()
        }
        fields.add("Add zone".toTextButton().onClick {
            mutate {
                val zone = StoryZone(UUID.randomUUID().toString(), "New zone")
                draft.zones.add(zone)
                selectedZone = zone
            }
        }).row()
        selectedZone?.let { zone ->
            fields.text("Zone ID", zone.id) { zone.id = it.trim() }
            fields.text("Name", zone.name) { zone.name = it }
            fields.add("${zone.positions.size} tiles".toLabel()).row()
            fields.add("Delete zone".toTextButton().onClick {
                mutate { draft.zones.remove(zone); selectedZone = null }
            }).row()
        }
    }

    private fun triggers(fields: StoryEditorForm) {
        for (trigger in draft.triggers) {
            fields.text("Trigger ID", trigger.id) { trigger.id = it.trim() }
            fields.choice("Event", trigger.event.name, StoryEventKind.values().map { it.name to it.name }) {
                trigger.event = StoryEventKind.valueOf(it)
            }
            fields.choice("Hero", trigger.heroId,
                listOf("" to "Any eligible hero") + map.scenarioHeroes.map { it.id to "${it.name} (${it.id})" }) {
                trigger.heroId = it
            }
            fields.choice("Player slot", trigger.ownerSlotId,
                listOf("" to "Any participant") + map.scenarioPlayerSlots.map { it.id to it.name.ifBlank { it.id } }) {
                trigger.ownerSlotId = it
            }
            fields.choice("Entry zone (movement only)", trigger.zoneId,
                listOf("" to "Any tile") + draft.zones.map { it.id to it.name.ifBlank { it.id } }) {
                trigger.zoneId = it
            }
            fields.position("Target position (x,y; optional)", trigger.targetPosition) { trigger.targetPosition = it }
            fields.flag("Once per mission", trigger.once) { trigger.once = it }
            StoryDialogueEditor.condition(fields, trigger.condition)
            fields.include(StoryEffectsEditor.create(trigger.effects, map, ruleset, widthForFields, ::mutate))
            fields.add("Delete trigger".toTextButton().onClick { mutate { draft.triggers.remove(trigger) } }).row()
        }
        fields.add("Add trigger".toTextButton().onClick {
            mutate { draft.triggers.add(StoryTrigger(id = UUID.randomUUID().toString())) }
        }).row()
    }

    private fun dialogues(fields: StoryEditorForm) {
        for (dialogue in draft.dialogues) {
            fields.add("${dialogue.id} (${dialogue.nodes.size})".toLabel().apply { wrap = true })
                .width(widthForFields).row()
            fields.add("Edit dialogue: ${dialogue.id}".toTextButton().apply {
                name = "scenario-edit-dialogue:${dialogue.id}"
            }.onClick {
                attempt {
                    form?.capture()
                    val screen =
                            requireNotNull(previewScreen) { "Dialogue editing requires an editor screen" }
                    screen.game.pushScreen(
                        StoryDialogueGraphScreen(
                            dialogue,
                            map,
                            ruleset
                        ) { edited ->
                            // Проверяем весь кандидат до замены общего черновика, включая внешние ссылки на диалог.
                            val candidate = draft.deepCopy()
                            val index = draft.dialogues.indexOf(dialogue)
                            require(index >= 0) { "Dialogue is no longer present in the draft" }
                            candidate.dialogues[index] = edited.deepCopy()
                            com.unciv.pure.application.story.ValidateStoryScenario.execute(
                                candidate,
                                map.scenarioHeroes.map { it.id }.toSet(),
                                map.scenarioPlayerSlots.map { it.id }.toSet(),
                                map.scenarioCities.map { it.id }.toSet(),
                                map.values.map { CityPosition(it.position.x, it.position.y) }
                                    .toSet()
                            )
                            draft = candidate
                            refresh()
                        })
                }
            }).row()
            fields.add("Preview dialogue: ${dialogue.id}".toTextButton().apply {
                name = "scenario-preview-dialogue:${dialogue.id}"
            }.onClick { preview(StoryEffect(StoryEffectKind.Dialogue, dialogue.id)) }).row()
            fields.add("Delete dialogue: ${dialogue.id}".toTextButton().onClick {
                mutate { draft.dialogues.remove(dialogue) }
            }).row()
        }
        fields.add("Add dialogue".toTextButton().onClick {
            mutate {
                draft.dialogues.add(
                    StoryDialogue(
                        id = UUID.randomUUID().toString(), startNodeId = "start",
                        nodes = arrayListOf(
                            StoryDialogueNode(
                                id = "start",
                                text = "New dialogue line"
                            )
                        )
                    )
                )
            }
        }).row()
    }

    private fun cinematics(fields: StoryEditorForm) {
        for (cinematic in draft.cinematics) {
            fields.add("Preview cinematic: ${cinematic.id}".toTextButton().apply {
                name = "scenario-preview-cinematic:${cinematic.id}"
            }.onClick {
                attempt {
                    form?.capture()
                    preview(StoryEffect(StoryEffectKind.Cinematic, cinematic.id))
                }
            }).row()
            fields.text("Cinematic ID", cinematic.id) { cinematic.id = it.trim() }
            fields.text("Video asset path", cinematic.assetPath) { cinematic.assetPath = it.trim() }
            fields.text(
                "Fallback text",
                cinematic.fallbackText,
                multiline = true
            ) { cinematic.fallbackText = it }
            fields.flag("Allow skipping", cinematic.skippable) { cinematic.skippable = it }
            fields.add(
                "Delete cinematic".toTextButton()
                    .onClick { mutate { draft.cinematics.remove(cinematic) } }).row()
        }
        fields.add("Add cinematic".toTextButton().onClick {
            mutate {
                draft.cinematics.add(
                    StoryCinematic(
                        id = UUID.randomUUID().toString(),
                        fallbackText = "New cinematic scene"
                    )
                )
            }
        }).row()
    }

    /** Экран-владелец задаётся адаптером редактора; отдельные проверки формы могут работать без него. */
    var previewScreen: BaseScreen? = null

    /** Проверяет снимок черновика перед просмотром, не выполняя Apply. */
    private fun preview(effect: StoryEffect) = attempt {
        form?.capture()
        val screen = requireNotNull(previewScreen) { "Preview requires an editor screen" }
        com.unciv.pure.application.story.ValidateStoryScenario.execute(
            draft,
            map.scenarioHeroes.map { it.id }.toSet(),
            map.scenarioPlayerSlots.map { it.id }.toSet(),
            map.scenarioCities.map { it.id }.toSet(),
            map.values.map { CityPosition(it.position.x, it.position.y) }.toSet()
        )
        StoryScenePreview(screen, draft, effect).open()
    }

    /** Отдаёт независимый снимок зон для слоя просмотра; карта до Apply не изменяется. */
    fun zonePreviewSnapshot(): List<StoryZone> = draft.zones.map { it.deepCopy() }
}
