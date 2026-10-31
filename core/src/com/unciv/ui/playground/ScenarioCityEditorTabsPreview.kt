package com.unciv.ui.playground

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.ScenarioCityOwner
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.keyShortcuts
import com.unciv.ui.utils.extensions.OnClickListener

/** Выполняет сценарий настоящего редактора: слоты, ввод текста, нейтральный город, кисть и пересоздание экрана. */
fun createScenarioCityEditorTabsPreview(): BaseScreen {
    val editor = MapEditorScreen(TileMap(3, RulesetCache.getVanillaRuleset()))
    /** Вызывает обработчик именованной кнопки, сохраняя настоящий путь изменения формы и карты. */
    fun click(name: String) {
        editor.stage.root.findActor<Actor>(name).listeners.filterIsInstance<OnClickListener>()
            .single().function(null, 0f, 0f)
    }
    check(editor.tabs.getPageIndex("Cities") == -1)
    editor.tabs.selectPage("Edit")
    check(editor.tabs.edit.selectPage("Players"))
    check(!editor.allowDragPainting && editor.tileClickHandler == null)
    val slotName = editor.stage.root.findActor<TextField>("scenario-player-slot-name")
    // Два уровня горячих клавиш не должны перехватывать буквы, пока фокус находится в текстовом поле.
    var globalShortcutCalls = 0
    var actorShortcutCalls = 0
    val shortcutActor = Actor()
    editor.stage.addActor(shortcutActor)
    editor.globalShortcuts.add('q') { globalShortcutCalls++ }
    shortcutActor.keyShortcuts.add('q') { actorShortcutCalls++ }
    editor.stage.keyboardFocus = slotName
    for (character in "player q1") {
        val keycode = if (character == ' ') Input.Keys.SPACE
            else Input.Keys.valueOf(character.uppercaseChar().toString())
        editor.stage.keyDown(keycode)
        editor.stage.keyTyped(character)
        editor.stage.keyUp(keycode)
    }
    check(slotName.text == "player q1")
    check(globalShortcutCalls == 0 && actorShortcutCalls == 0)
    check(editor.tabs.activePage == 2 && editor.tabs.edit.currentSubTab() == "Players")
    editor.stage.keyboardFocus = null
    editor.stage.keyDown(Input.Keys.Q)
    editor.stage.keyUp(Input.Keys.Q)
    check(globalShortcutCalls == 1 && actorShortcutCalls == 1)
    editor.globalShortcuts.remove(KeyCharAndCode('q'))
    shortcutActor.remove()
    // Переименование меняет подпись слота, но сохраняет его ID; неиспользуемый слот можно удалить.
    click("scenario-player-add")
    val playerId = editor.tileMap.scenarioPlayerSlots.single().id
    check(editor.tileMap.scenarioPlayerSlots.single().name == "player q1")
    editor.stage.root.findActor<TextField>("scenario-player-name-0").text = "Renamed player"
    click("scenario-player-rename-0")
    check(editor.tileMap.scenarioPlayerSlots.single().name == "Renamed player")
    check(editor.tileMap.scenarioPlayerSlots.single().id == playerId)
    click("scenario-player-delete-0")
    check(editor.tileMap.scenarioPlayerSlots.isEmpty())
    editor.tabs.selectPage("View")
    editor.tabs.selectPage("Edit")
    check(editor.tabs.edit.currentSubTab() == "Players" && !editor.allowDragPainting)
    check(editor.stage.root.findActor<TextField>("scenario-player-slot-name") != null)
    check(editor.tabs.edit.selectPage("Cities"))
    editor.tileClickHandler!!.invoke(editor.tileMap[Vector2.Zero])
    val form = editor.stage.root
    check(form.findActor<CheckBox>("scenario-city-capital") != null)
    check(form.findActor<Actor>("scenario-player-add") == null)
    form.findActor<TextField>("scenario-city-name").text = "Saved town"
    click("scenario-city-apply")
    check(editor.tileMap.scenarioCities.single().name == "Saved town")
    check(editor.tileMap.scenarioCities.single().owner == ScenarioCityOwner.Neutral)
    check(editor.tileMap.scenarioPlayerSlots.isEmpty())
    /** Проверяет спрайт превью без скрытого создания CityInfo или записи улучшения в клетку. */
    fun checkCastle(screen: MapEditorScreen, expected: Boolean) {
        val center = screen.tileMap[Vector2.Zero]
        for (group in screen.mapHolder.tileGroups.getValue(center))
            check(group.tileBaseImages.any { it.name?.contains("City center") == true } == expected)
        check(center.getCity() == null && center.improvement == null)
    }
    checkCastle(editor, true)
    val savedCity = editor.tileMap.scenarioCities.single()
    editor.tileMap.scenarioCities.clear()
    editor.mapHolder.refreshScenarioCityPreviews()
    checkCastle(editor, false)
    editor.tileMap.scenarioCities.add(savedCity)
    editor.mapHolder.refreshScenarioCityPreviews()
    checkCastle(editor, true)
    // Возврат к рельефу включает кисть, а возврат к Cities снова отключает её.
    editor.tabs.edit.selectPage("Terrain")
    check(editor.allowDragPainting && editor.tileClickHandler != null)
    editor.tabs.edit.selectPage("Cities")
    check(!editor.allowDragPainting)
    editor.tabs.selectPage("View")
    check(editor.allowDragPainting)
    editor.tabs.selectPage("Edit")
    check(editor.tabs.edit.currentSubTab() == "Cities" && !editor.allowDragPainting)
    // Пересозданный экран должен сохранить применённое описание, выбранную вкладку и признак изменений.
    val recreated = editor.recreate() as MapEditorScreen
    editor.dispose()
    check(recreated.tabs.edit.currentSubTab() == "Cities" && !recreated.allowDragPainting)
    check(recreated.isDirty && recreated.tileMap.scenarioCities.single().name == "Saved town")
    checkCastle(recreated, true)
    check(recreated.stage.root.findActor<TextField>("scenario-city-name").text == "Saved town")
    return recreated
}
