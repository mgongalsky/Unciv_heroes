package com.unciv.ui.mapeditor

import com.unciv.pure.domain.story.StoryDialogue

/** Состояние открытого редактора: геометрия и история не сериализуются в игровой сценарий. */
class StoryDialogueGraphState(source: StoryDialogue) {
    data class Position(val x: Float, val y: Float)
    private data class Snapshot(
        val dialogue: StoryDialogue,
        val positions: Map<String, Position>,
        val selectedId: String,
        val textScale: Float
    )

    var dialogue = source.deepCopy()
        private set
    var selectedId = source.startNodeId
        private set
    var textScale = 0.65f
        private set
    private val coordinates = linkedMapOf<String, Position>()
    val positions: Map<String, Position> get() = coordinates.toMap()
    private val past = arrayListOf<Snapshot>()
    private val future = arrayListOf<Snapshot>()
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    /** Автоматическая начальная раскладка заполняет только отсутствующие позиции без создания команды. */
    fun ensurePosition(id: String, position: Position): Position = coordinates.getOrPut(id) { position }

    fun select(id: String) {
        require(dialogue.nodes.any { it.id == id }) { "Unknown node: $id" }
        selectedId = id
    }

    /** Публикует проверенную команду и переносит позицию переименованного узла. */
    fun publish(updated: StoryDialogue, selection: String, renamedFrom: String? = null) {
        require(updated.nodes.any { it.id == selection }) { "Unknown selected node: $selection" }
        val before = snapshot()
        val nextPositions = coordinates.toMutableMap()
        if (renamedFrom != null && renamedFrom != selection)
            nextPositions.remove(renamedFrom)?.let { nextPositions[selection] = it }
        val ids = updated.nodes.map { it.id }.toSet()
        nextPositions.keys.retainAll(ids)
        dialogue = updated.deepCopy()
        selectedId = selection
        coordinates.clear()
        coordinates.putAll(nextPositions)
        remember(before)
    }

    /** Один законченный drag создаёт одну запись истории; промежуточные кадры остаются в Canvas. */
    fun move(id: String, x: Float, y: Float) {
        require(dialogue.nodes.any { it.id == id }) { "Unknown node: $id" }
        require(x.isFinite() && y.isFinite()) { "Invalid node position" }
        val before = snapshot()
        coordinates[id] = Position(x.coerceAtLeast(20f), y.coerceAtLeast(20f))
        remember(before)
    }

    fun changeTextScale(value: Float) {
        require(value.isFinite()) { "Invalid text size" }
        val before = snapshot()
        textScale = value.coerceIn(0.45f, 1f)
        remember(before)
    }

    /** Ручная раскладка сбрасывается только явной командой автора. */
    fun autoLayout() {
        val before = snapshot()
        coordinates.clear()
        remember(before)
    }

    fun undo(): Boolean {
        if (past.isEmpty()) return false
        future.add(snapshot())
        restore(past.removeAt(past.lastIndex))
        return true
    }

    fun redo(): Boolean {
        if (future.isEmpty()) return false
        past.add(snapshot())
        restore(future.removeAt(future.lastIndex))
        return true
    }

    private fun snapshot() = Snapshot(dialogue.deepCopy(), coordinates.toMap(), selectedId, textScale)

    private fun remember(before: Snapshot) {
        if (before == snapshot()) return
        past.add(before)
        if (past.size > 100) past.removeAt(0)
        future.clear()
    }

    private fun restore(snapshot: Snapshot) {
        dialogue = snapshot.dialogue.deepCopy()
        coordinates.clear()
        coordinates.putAll(snapshot.positions)
        selectedId = snapshot.selectedId
        textScale = snapshot.textScale
    }
}
