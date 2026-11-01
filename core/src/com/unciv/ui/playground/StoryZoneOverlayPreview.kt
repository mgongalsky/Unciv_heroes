package com.unciv.ui.playground

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.StoryScenario
import com.unciv.pure.domain.story.StoryZone
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.mapeditor.StoryZoneOverlay

/** Проверяет реальные контуры и переключатели без утверждения визуальных эталонов. */
internal fun checkStoryZoneContours(click: (Actor, String) -> Unit) {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val origin = CityPosition(0f, 0f)
    val map = TileMap(3, ruleset, false).apply {
        story = StoryScenario(zones = arrayListOf(
            StoryZone("a", "Area A", arrayListOf(origin)),
            StoryZone("b", "Area B", arrayListOf(origin))
        ))
    }
    val saved = map.story!!.deepCopy()
    val editor = MapEditorScreen(map)
    var recreated: MapEditorScreen? = null
    fun overlays(screen: MapEditorScreen) = screen.mapHolder.tileGroups.values.flatMap { groups ->
        groups.flatMap { it.children.toList().filterIsInstance<StoryZoneOverlay>() }
    }
    fun edges(id: String) = overlays(editor).filter { it.zoneId == id }.sumOf { it.boundaryEdges.size }
    try {
        check(edges("a") == 6 && edges("b") == 6) { "Overlapping zones lost their separate boundaries" }
        check(overlays(editor).all { it.touchable == Touchable.disabled })
        check(overlays(editor).flatMap { it.children.toList() }.all {
            it.x.isFinite() && it.y.isFinite() && it.width.isFinite() && it.height.isFinite()
        })
        editor.tabs.selectPage("Edit")
        editor.tabs.edit.selectPage("Story")
        click(editor.stage.root, "Show all story zones")
        check(!editor.storyZonesVisible && overlays(editor).isEmpty() && !editor.isDirty)
        click(editor.stage.root, "Show all story zones")
        check(edges("a") == 6 && edges("b") == 6)
        click(editor.stage.root, "Area A (a)")
        editor.tileClickHandler!!.invoke(map[Vector2(1f, 0f)])
        check(edges("a") == 10 && edges("b") == 6) { "Shared edge inside a zone was not hidden" }
        check(map.story == saved && !editor.isDirty)
        click(editor.stage.root, "Discard changes")
        check(edges("a") == 6 && map.story == saved)
        click(editor.stage.root, "Area A (a)")
        editor.tileClickHandler!!.invoke(map[Vector2(1f, 0f)])
        editor.tabs.selectPage("View")
        check(edges("a") == 6 && map.story == saved) { "Leaving Story retained draft contours" }
        click(editor.stage.root, "Show all story zones")
        check(overlays(editor).isEmpty() && !editor.isDirty)
        recreated = editor.recreate() as MapEditorScreen
        check(!recreated!!.storyZonesVisible && overlays(recreated!!).isEmpty())
    } finally {
        recreated?.dispose()
        editor.dispose()
    }

    // Соседство через шов разрешает штатная карта; каждый зеркальный TileGroup получает те же рёбра.
    val wrapped = TileMap(3, ruleset, true).apply {
        mapParameters.worldWrap = true
        mapParameters.mapSize.radius = 3
    }
    val offsets = listOf(-1 to 1, -1 to 0, 0 to -1, 1 to -1, 1 to 0, 0 to 1)
    val seam = wrapped.values.asSequence().flatMap { tile ->
        offsets.withIndex().asSequence().mapNotNull { (edge, offset) ->
            val x = tile.position.x.toInt() + offset.first
            val y = tile.position.y.toInt() + offset.second
            val neighbor = if (wrapped.contains(x, y)) null else wrapped.getIfTileExistsOrNull(x, y)
            if (neighbor == null || neighbor === tile) null else Triple(tile, neighbor, edge)
        }
    }.firstOrNull() ?: error("Wrapped fixture has no seam neighbor")
    wrapped.story = StoryScenario(zones = arrayListOf(StoryZone("seam", "Seam", arrayListOf(
        CityPosition(seam.first.position.x, seam.first.position.y),
        CityPosition(seam.second.position.x, seam.second.position.y)
    ))))
    val wrappedEditor = MapEditorScreen(wrapped)
    try {
        val groups = wrappedEditor.mapHolder.tileGroups.getValue(seam.first)
        check(groups.size == 3) { "Wrapped editor did not create mirror tiles" }
        val contours = groups.map { group -> group.children.toList().filterIsInstance<StoryZoneOverlay>().single() }
        check(contours.all { seam.third !in it.boundaryEdges }) { "Zone has an internal border at the map seam" }
        check(contours.map { it.boundaryEdges }.distinct().size == 1)
    } finally {
        wrappedEditor.dispose()
    }
}
