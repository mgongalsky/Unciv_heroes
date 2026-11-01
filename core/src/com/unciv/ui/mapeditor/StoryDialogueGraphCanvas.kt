package com.unciv.ui.mapeditor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Полотно отвечает за геометрию и жесты; публикацию сюжета и историю выполняет экран. */
class StoryDialogueGraphCanvas(
    private val state: StoryDialogueGraphState,
    private val callbacks: Callbacks
) : Group() {
    data class Output(val nodeId: String, val choiceId: String?)
    class Callbacks(
        val select: (String) -> Unit = {},
        val move: (String, Float, Float) -> Unit = { _, _, _ -> },
        val connect: (Output, String) -> Unit = { _, _ -> },
        val selectOutput: (Output) -> Unit = {}
    )
    /** Совместимость отдельных предпросмотров, которым достаточно выбора карточки. */
    constructor(dialogue: StoryDialogue, selectedId: String, select: (String) -> Unit) :
        this(StoryDialogueGraphState(dialogue).apply { select(selectedId) }, Callbacks(select = select))

    private val dialogue = state.dialogue.deepCopy()
    private val cardWidth = 300f
    private val cards = linkedMapOf<String, Group>()
    private val inputs = linkedMapOf<String, Actor>()
    private val outputs = linkedMapOf<Output, Actor>()
    private val targets = linkedMapOf<Output, String>()
    private val wires = Group().apply { touchable = Touchable.disabled }
    private val temporary = Group().apply { touchable = Touchable.disabled }
    private val end = Table()
    private var moving: String? = null
    private var originalPosition: Vector2? = null
    private var connecting: Output? = null
    private var hoverTarget: String? = null
    val gestureActive get() = moving != null || connecting != null

    init {
        name = "story-dialogue-graph-canvas"
        addActor(wires)
        val depths = linkedMapOf(dialogue.startNodeId to 0)
        val queue = java.util.ArrayDeque<String>().apply { add(dialogue.startNodeId) }
        val nodes = dialogue.nodes.associateBy { it.id }
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            val node = nodes[id] ?: continue
            val destinations = if (node.choices.isEmpty()) listOf(node.nextNodeId) else node.choices.map { it.nextNodeId }
            for (destination in destinations) if (destination in nodes && destination !in depths) {
                depths[destination] = depths.getValue(id) + 1
                queue.add(destination)
            }
        }
        val unreachable = (depths.values.maxOrNull() ?: 0) + 1
        val rows = hashMapOf<Int, Int>()
        val rowHeight = 210f + maxOf(1, dialogue.nodes.maxOfOrNull { it.choices.size } ?: 1) * 48f
        var edgeIndex = 0
        for (node in dialogue.nodes) {
            val column = depths[node.id] ?: unreachable
            val row = rows[column] ?: 0
            rows[column] = row + 1
            val position = state.ensurePosition(node.id,
                StoryDialogueGraphState.Position(60f + column * 440f, 60f + row * rowHeight))
            val transitions = if (node.choices.isEmpty()) listOf(Triple<String?, String, String>(null, "Continue", node.nextNodeId))
                else node.choices.map { Triple(it.id, it.text.ifBlank { it.id }, it.nextNodeId) }
            val cardHeight = 156f + transitions.size * 48f
            val card = Group().apply {
                name = "story-graph-node:${node.id}"
                setBounds(position.x, position.y, cardWidth, cardHeight)
            }
            cards[node.id] = card
            card.addActor(Image(ImageGetter.getDrawable("OtherIcons/whiteDot")).apply {
                color = if (node.id == state.selectedId) Color(0.18f, 0.32f, 0.48f, 1f) else Color(0.12f, 0.16f, 0.22f, 1f)
                setSize(cardWidth, cardHeight); touchable = Touchable.disabled
            })
            val header = Table().apply {
                name = "story-graph-drag:${node.id}"
                setBounds(22f, cardHeight - 40f, cardWidth - 44f, 36f)
                touchable = Touchable.enabled
                setClip(true)
            }
            header.addActor(fitted((if (node.id == dialogue.startNodeId) "▶ " else "") + node.id,
                Color.GOLD, header.width, header.height))
            header.addListener(moveListener(node.id, card))
            card.addActor(header)
            card.addActor(fitted(node.speaker, Color.SKY, cardWidth - 44f, 24f).apply {
                setPosition(22f, cardHeight - 68f)
            })
            card.addActor(fitted(node.text, Color.WHITE, cardWidth - 44f, 68f).apply {
                name = "story-graph-text:${node.id}"; setPosition(22f, cardHeight - 140f)
            })
            card.onClick { if (!gestureActive) callbacks.select(node.id) }
            val input = port("story-graph-input:${node.id}", Color.SKY).apply {
                setPosition(0f, cardHeight - 32f)
            }
            inputs[node.id] = input
            card.addActor(input)
            for ((index, transition) in transitions.withIndex()) {
                val (choiceId, caption, target) = transition
                val output = Output(node.id, choiceId)
                val y = cardHeight - 164f - index * 48f
                card.addActor(fitted(caption, Color.LIGHT_GRAY, cardWidth - 48f, 38f).apply {
                    name = "story-graph-edge:${node.id}:${edgeIndex++}"
                    setPosition(22f, y - 16f)
                })
                val handle = port("story-graph-output:${node.id}:${choiceId ?: "continue"}", Color.GOLD).apply {
                    setPosition(cardWidth - 24f, y - 10f)
                    addListener(connectionListener(output))
                }
                outputs[output] = handle
                targets[output] = target
                card.addActor(handle)
            }
            addActor(card)
        }
        end.apply {
            name = "story-graph-end"
            background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(Color(0.22f, 0.18f, 0.18f, 1f))
            add(fitted("End dialogue", Color.SALMON, 170f, 50f)).pad(12f)
            setSize(200f, 80f)
            touchable = Touchable.disabled
        }
        addActor(end)
        addActor(temporary)
        updateExtent()
        redrawWires()
    }

    /** Измеряет текст при фиксированной ширине; родительская Table не сужает wrap-label до нуля. */
    private fun fitted(
        text: String,
        tint: Color,
        availableWidth: Float,
        availableHeight: Float
    ): Label {
        val label = object : Label(text, com.unciv.ui.utils.BaseScreen.skin) {
            override fun getPrefWidth(): Float = availableWidth
        }.apply {
            name = "story-graph-label"
            color = tint.cpy()
            wrap = true
            setFontScale(state.textScale)
            setSize(availableWidth, availableHeight)
            touchable = Touchable.disabled
        }
        if (label.prefHeight > availableHeight) {
            var low = 0
            var high = text.length
            while (low < high) {
                val middle = (low + high + 1) / 2
                label.setText(text.take(middle).trimEnd() + "…")
                if (label.prefHeight <= availableHeight) low = middle else high = middle - 1
            }
            label.setText(text.take(low).trimEnd() + "…")
            // Короткие подписи не должны выходить за область даже при большой высоте строки выбранного шрифта.
            var scale = state.textScale
            while (label.prefHeight > availableHeight && scale > 0.1f) {
                scale *= 0.9f
                label.setFontScale(scale)
            }
        }
        return label
    }

    private fun port(id: String, tint: Color) = Image(ImageGetter.getDrawable("OtherIcons/whiteDot")).apply {
        name = id; color = tint.cpy(); setSize(24f, 24f); touchable = Touchable.enabled
    }

    private fun point(actor: Actor): Vector2 = actor.localToActorCoordinates(this, Vector2(actor.width / 2f, actor.height / 2f))
    private fun mouse(event: InputEvent) = stageToLocalCoordinates(Vector2(event.stageX, event.stageY))

    /** Заголовок забирает жест у ScrollPane; координаты drag учитывают масштаб полотна. */
    private fun moveListener(id: String, card: Group) = object : InputListener() {
        private var down = Vector2()
        private var dragged = false
        override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
            if (button != Input.Buttons.LEFT || gestureActive) return false
            event.stop()
            stage.keyboardFocus = null
            down = mouse(event)
            originalPosition = Vector2(card.x, card.y)
            moving = id
            dragged = false
            return true
        }
        override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
            if (moving != id) return
            event.stop()
            val delta = mouse(event).sub(down)
            if (delta.len2() > 16f) dragged = true
            if (!dragged) return
            val origin = originalPosition ?: return
            card.setPosition((origin.x + delta.x).coerceAtLeast(20f), (origin.y + delta.y).coerceAtLeast(20f))
            redrawWires()
        }
        override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
            event.stop()
            if (moving != id) return
            if (event.isTouchFocusCancel) { cancelGesture(); return }
            moving = null
            originalPosition = null
            if (dragged) callbacks.move(id, card.x, card.y) else callbacks.select(id)
        }
    }

    /** Drop вне входа отменяется; отключение связи является отдельной явной командой. */
    private fun connectionListener(output: Output) = object : InputListener() {
        override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
            if (button != Input.Buttons.LEFT || gestureActive) return false
            event.stop()
            stage.keyboardFocus = null
            connecting = output
            callbacks.selectOutput(output)
            return true
        }
        override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
            if (connecting != output) return
            event.stop()
            val cursor = mouse(event)
            hoverTarget = inputs.entries.firstOrNull { point(it.value).dst2(cursor) <= 32f * 32f }?.key
            inputs.forEach { (id, actor) -> actor.color = if (id == hoverTarget) Color.LIME else Color.SKY }
            temporary.clearChildren()
            line(temporary, point(outputs.getValue(output)), cursor, Color.GOLD)
        }
        override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
            event.stop()
            if (connecting != output) return
            val target = if (event.isTouchFocusCancel) null else inputs.entries
                .firstOrNull { point(it.value).dst2(mouse(event)) <= 32f * 32f }?.key
            cancelGesture()
            if (target != null) callbacks.connect(output, target)
        }
    }

    fun cancelGesture(): Boolean {
        if (!gestureActive) return false
        moving?.let { id -> originalPosition?.let { cards[id]?.setPosition(it.x, it.y) } }
        moving = null; originalPosition = null; connecting = null; hoverTarget = null
        inputs.values.forEach { it.color = Color.SKY }
        temporary.clearChildren()
        redrawWires()
        return true
    }

    private fun updateExtent() {
        val right = cards.values.maxOfOrNull { it.x + it.width } ?: 300f
        val top = cards.values.maxOfOrNull { it.y + it.height } ?: 300f
        end.setPosition(right + 140f, 60f)
        setSize(right + 400f, top + 100f)
    }

    private fun redrawWires() {
        wires.clearChildren()
        for ((output, targetId) in targets) {
            val from = point(outputs.getValue(output))
            val to = if (targetId.isEmpty()) Vector2(end.x, end.y + end.height / 2f)
                else inputs[targetId]?.let(::point) ?: continue
            val middle = maxOf(from.x + 50f, (from.x + to.x) / 2f)
            val route = if (to.x > from.x) listOf(from, Vector2(middle, from.y), Vector2(middle, to.y), to)
                else listOf(from, Vector2(from.x + 45f, from.y), Vector2(from.x + 45f, cards.getValue(output.nodeId).y - 20f),
                    Vector2(to.x - 35f, cards.getValue(output.nodeId).y - 20f), Vector2(to.x - 35f, to.y), to)
            val tint = if (output.nodeId == state.selectedId) Color.GOLD else Color.LIGHT_GRAY
            for (i in 0 until route.lastIndex) line(wires, route[i], route[i + 1], tint)
            val before = route[route.lastIndex - 1]
            val angle = atan2(to.y - before.y, to.x - before.x)
            for (offset in listOf(-0.5f, 0.5f)) line(wires, to,
                Vector2(to.x - cos(angle + offset) * 12f, to.y - sin(angle + offset) * 12f), tint)
        }
    }

    private fun line(group: Group, from: Vector2, to: Vector2, tint: Color) {
        if (from.epsilonEquals(to, 0.01f)) return
        group.addActor(ImageGetter.getLine(from.x, from.y, to.x, to.y, 2f).apply {
            color = tint.cpy(); touchable = Touchable.disabled
        })
    }
    private var openText: ((String, String?) -> Unit)? = null
    private var inlineInput: com.badlogic.gdx.scenes.scene2d.ui.TextArea? = null
    private var inlineLabelName: String? = null

    /** Подключает редактирование только текста; заголовки и порты сохраняют собственные жесты. */
    fun installTextEditing(open: (String, String?) -> Unit) {
        openText = open
        for (node in dialogue.nodes) {
            val card = cards.getValue(node.id)
            card.findActor<Label>("story-graph-text:${node.id}")
                ?.let { editableLabel(it, node.id, null) }
            val answers = card.children.filterIsInstance<Label>()
                .filter { it.name?.startsWith("story-graph-edge:") == true }
            node.choices.forEachIndexed { index, choice ->
                answers.getOrNull(index)?.let { editableLabel(it, node.id, choice.id) }
            }
        }
    }

    /** Останавливает всплытие клика, иначе выбор карточки пересоздаст только что открытое поле. */
    private fun editableLabel(label: Label, nodeId: String, choiceId: String?) {
        label.touchable = Touchable.enabled
        label.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                if (button != Input.Buttons.LEFT) return false
                event.stop()
                return true
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                event.stop()
                if (!event.isTouchFocusCancel && x in 0f..label.width && y in 0f..label.height)
                    openText?.invoke(nodeId, choiceId)
            }
        })
    }

    /** Полный текст редактируется поверх подписи; поле и курсор не пересоздаются при вводе. */
    fun beginInline(nodeId: String, choiceId: String?, changed: (String, Any) -> Unit) {
        finishInline()
        val node = state.dialogue.nodes.first { it.id == nodeId }
        val card = cards.getValue(nodeId)
        val label = if (choiceId == null) card.findActor<Label>("story-graph-text:$nodeId") else {
            val index = node.choices.indexOfFirst { it.id == choiceId }
            require(index >= 0) { "Unknown answer: $choiceId" }
            card.children.filterIsInstance<Label>()
                .filter { it.name?.startsWith("story-graph-edge:") == true }[index]
        }
        requireNotNull(label)
        val value =
                if (choiceId == null) node.text else node.choices.first { it.id == choiceId }.text
        val input = com.badlogic.gdx.scenes.scene2d.ui.TextArea(
            value,
            com.unciv.ui.utils.BaseScreen.skin
        )
        input.name = "story-inline:$nodeId:${choiceId ?: "text"}"
        input.setBounds(label.x, label.y, label.width, maxOf(38f, label.height))
        inlineInput = input
        inlineLabelName = label.name
        label.isVisible = false
        input.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                changed(input.text, input)
            }
        })
        input.addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                event.stop()
                return true
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                event.stop()
            }

            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                if (keycode != Input.Keys.ESCAPE && keycode != Input.Keys.BACK) return false
                event.stop()
                finishInline()
                return true
            }
        })
        input.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.FocusListener() {
            override fun keyboardFocusChanged(event: FocusEvent, actor: Actor, focused: Boolean) {
                if (!focused && inlineInput === input) finishInline()
            }
        })
        card.addActor(input)
        stage.keyboardFocus = input
        input.cursorPosition = input.text.length
    }

    /** Завершение поля оставляет уже введённый текст в локальном черновике; Cancel экрана отменяет весь черновик. */
    fun finishInline(): Boolean {
        val input = inlineInput ?: return false
        inlineInput = null
        val labelName = inlineLabelName
        inlineLabelName = null
        input.parent?.findActor<Label>(labelName)?.isVisible = true
        if (input.stage?.keyboardFocus === input) input.stage.keyboardFocus = null
        input.remove()
        return true
    }

    /** Обновляет подписи на месте, сохраняя расположение карточек и активное встроенное поле. */
    fun refreshText(node: com.unciv.pure.domain.story.StoryDialogueNode) {
        val card = cards[node.id] ?: return
        fun replace(old: Label, text: String, choiceId: String?, editable: Boolean) {
            val updated = fitted(text, old.color, old.width, old.height).apply {
                name = old.name
                setPosition(old.x, old.y)
                isVisible = name != inlineLabelName
            }
            old.remove()
            card.addActor(updated)
            if (editable && openText != null) editableLabel(updated, node.id, choiceId)
        }
        card.findActor<Label>("story-graph-text:${node.id}")
            ?.let { replace(it, node.text, null, true) }
        card.children.filterIsInstance<Label>().firstOrNull { it.name == "story-graph-label" }
            ?.let { replace(it, node.speaker, null, false) }
        val labels = card.children.filterIsInstance<Label>()
            .filter { it.name?.startsWith("story-graph-edge:") == true }
        node.choices.forEachIndexed { index, choice ->
            labels.getOrNull(index)
                ?.let { replace(it, choice.text.ifBlank { choice.id }, choice.id, true) }
        }
        inlineInput?.toFront()
    }
}
