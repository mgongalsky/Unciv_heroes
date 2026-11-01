package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Полотно графа: расположение и стрелки являются представлением, не частью сохраняемого сюжета. */
class StoryDialogueGraphCanvas(
    dialogue: StoryDialogue,
    selectedId: String,
    select: (String) -> Unit
) : Group() {
    private val cardWidth = 240f
    private val cardHeight = maxOf(150f, (dialogue.nodes.maxOfOrNull { it.choices.size } ?: 0) * 60f + 70f)

    init {
        name = "story-dialogue-graph-canvas"
        // Кратчайшее расстояние от старта определяет колонку; циклы не запускают повторный обход.
        val depths = linkedMapOf(dialogue.startNodeId to 0)
        val pending = java.util.ArrayDeque<String>()
        pending.add(dialogue.startNodeId)
        val byId = dialogue.nodes.associateBy { it.id }
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            val node = byId[id] ?: continue
            val targets = if (node.choices.isEmpty()) listOf(node.nextNodeId) else node.choices.map { it.nextNodeId }
            for (target in targets) {
                if (target.isEmpty() || target !in byId || target in depths) continue
                depths[target] = depths.getValue(id) + 1
                pending.add(target)
            }
        }
        val unreachableDepth = (depths.values.maxOrNull() ?: 0) + 1
        val columns = dialogue.nodes.groupBy { depths[it.id] ?: unreachableDepth }
        val rows = columns.values.maxOfOrNull { it.size } ?: 1
        val endColumn = (columns.keys.maxOrNull() ?: 0) + 1
        val rowHeight = cardHeight + 150f
        setSize((endColumn + 1) * 460f + 120f, rows * rowHeight + 180f)
        val positions = linkedMapOf<String, Vector2>()
        for ((column, nodes) in columns) {
            for ((row, node) in nodes.withIndex()) {
                positions[node.id] = Vector2(60f + column * 460f, height - cardHeight - 60f - row * rowHeight)
            }
        }
        val endPosition = Vector2(60f + endColumn * 460f, height - cardHeight - 60f)

        // Каждому ответу принадлежит отдельный выход: подписи соседних ответов не накладываются.
        var edgeIndex = 0
        for (node in dialogue.nodes) {
            val source = positions.getValue(node.id)
            val transitions = if (node.choices.isEmpty()) listOf("Continue" to node.nextNodeId)
                else node.choices.map { it.text.ifBlank { it.id } to it.nextNodeId }
            for ((answerIndex, transition) in transitions.withIndex()) {
                val (label, targetId) = transition
                val target = if (targetId.isEmpty()) endPosition else positions[targetId] ?: continue
                val start = Vector2(source.x + cardWidth, source.y + cardHeight - 45f - answerIndex * 60f)
                val finish = Vector2(target.x, target.y + cardHeight / 2f)
                val tint = if (node.id == selectedId) Color.GOLD else Color.LIGHT_GRAY
                val lane = source.y - 35f - (edgeIndex % 4) * 22f
                val points = if (finish.x > start.x) {
                    val middle = start.x + 180f + (answerIndex % 3) * 10f
                    listOf(start, Vector2(middle, start.y), Vector2(middle, finish.y), finish)
                } else {
                    val outer = start.x + 180f + (answerIndex % 3) * 10f
                    listOf(start, Vector2(outer, start.y), Vector2(outer, lane),
                        Vector2(finish.x - 30f, lane), Vector2(finish.x - 30f, finish.y), finish)
                }
                for (index in 0 until points.lastIndex) line(points[index], points[index + 1], tint)
                val before = points[points.lastIndex - 1]
                val angle = atan2(finish.y - before.y, finish.x - before.x)
                for (offset in listOf(-0.5f, 0.5f)) {
                    line(finish, Vector2(finish.x - cos(angle + offset) * 12f,
                        finish.y - sin(angle + offset) * 12f), tint)
                }
                addActor(label.take(72).toLabel(tint).apply {
                    name = "story-graph-edge:${node.id}:$edgeIndex"
                    wrap = true
                    setFontScale(0.65f)
                    setBounds(start.x + 6f, start.y + 3f, 166f, 48f)
                    touchable = Touchable.disabled
                })
                edgeIndex++
            }
        }
        for (node in dialogue.nodes) {
            val position = positions.getValue(node.id)
            val tint = if (node.id == selectedId) Color(0.18f, 0.32f, 0.48f, 1f)
                else Color(0.14f, 0.17f, 0.22f, 1f)
            addActor(Table().apply {
                name = "story-graph-node:${node.id}"
                background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(tint)
                defaults().pad(5f).growX()
                val heading = (if (node.id == dialogue.startNodeId) "▶ " else "") + node.id
                add(heading.toLabel(Color.GOLD).apply { wrap = true }).width(cardWidth - 20f).row()
                add(node.speaker.toLabel(Color.SKY).apply { wrap = true }).width(cardWidth - 20f).row()
                add(node.text.take(100).toLabel().apply { wrap = true; setFontScale(0.75f) })
                    .width(cardWidth - 20f).growY().row()
                setBounds(position.x, position.y, cardWidth, cardHeight)
                touchable = Touchable.enabled
                onClick { select(node.id) }
            })
        }
        addActor(Table().apply {
            name = "story-graph-end"
            background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(Color(0.22f, 0.18f, 0.18f, 1f))
            add("End dialogue".toLabel(Color.SALMON)).pad(12f)
            setBounds(endPosition.x, endPosition.y, cardWidth, cardHeight)
            touchable = Touchable.disabled
        })
    }

    /** Линии рисуются обычным Batch вместе с виджетами, без отдельного GL-прохода. */
    private fun line(from: Vector2, to: Vector2, tint: Color) {
        if (from.epsilonEquals(to, 0.01f)) return
        addActor(ImageGetter.getLine(from.x, from.y, to.x, to.y, 2f).apply {
            color = tint.cpy()
            touchable = Touchable.disabled
        })
    }
}
