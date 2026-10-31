package com.unciv.logic.story

import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.ScenarioStoryEditorCommands
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.pure.domain.story.*

/** Небольшая независимая карта для редактора и приёмки полного сюжетного пути. */
object StoryDemoScenario {
    /** Создаёт новый пример; существующая карта и игровые объекты не изменяются. */
    fun create(ruleset: Ruleset): TileMap {
        val map = TileMap(6, ruleset, false)
        map.mapParameters.name = "The guarded gate"
        map.mapParameters.mapSize = MapSizeNew(6)
        map.scenarioPlayerSlots.add(ScenarioPlayerSlot("first", "Traveller"))
        map.scenarioCities.add(ScenarioCity(
            id = "gate", name = "Guarded gate", position = CityPosition(4f, 0f),
            typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle",
            garrison = arrayListOf(ScenarioCityTroop("Spearman", 1))
        ))
        require("Spearman" in ruleset.units) { "The story example requires Spearman" }
        ScenarioStoryEditorCommands.apply(map, ruleset,
            listOf(ScenarioHero("guide", "Проводник", "Warrior", "first", CityPosition(1f, 0f))),
            story())
        return map
    }

    /** Стабильные ID позволяют редактировать пример и проверять оба исхода после независимого запуска. */
    private fun story() = StoryScenario(
        zones = arrayListOf(StoryZone("approach", "Перед воротами", arrayListOf(CityPosition(2f, 0f)))),
        cinematics = arrayListOf(StoryCinematic(
            id = "opening-film", assetPath = "Videos/story-gate-intro.webm",
            fallbackText = "Проводник вышел к старым воротам. За стенами ждёт стража."
        )),
        dialogues = arrayListOf(
            StoryDialogue(id = "intro", startNodeId = "start", nodes = arrayListOf(
                StoryDialogueNode(id = "start", speaker = "Проводник",
                    text = "Я могу договориться со стражей или попытаться взять ворота силой.")
            )),
            StoryDialogue(id = "approach", startNodeId = "warning", nodes = arrayListOf(
                StoryDialogueNode(id = "warning", speaker = "Стражник",
                    text = "Стой! Подойди к воротам. Прежде чем начнётся бой, мы поговорим.")
            )),
            StoryDialogue(id = "parley", startNodeId = "offer", nodes = arrayListOf(
                StoryDialogueNode(id = "offer", speaker = "Стражник", text = "Ты пришёл с миром?",
                    choices = arrayListOf(
                        StoryChoice("ask", "Почему ворота закрыты?", nextNodeId = "answer"),
                        StoryChoice("peace", "Я не буду нападать", effects = arrayListOf(
                            StoryEffect(StoryEffectKind.CancelAttack),
                            StoryEffect(StoryEffectKind.SetFlag, "peace"),
                            StoryEffect(StoryEffectKind.GrantGold, amount = 17)
                        )),
                        StoryChoice("fight", "Готовься к бою", effects = arrayListOf(
                            StoryEffect(StoryEffectKind.SetFlag, "fight")
                        ))
                    )),
                StoryDialogueNode(id = "answer", speaker = "Стражник",
                    text = "Мы охраняем жителей. Откажись от нападения — и получишь припасы на дорогу.",
                    nextNodeId = "offer")
            ), aiChoices = linkedMapOf("offer" to "peace")),
            StoryDialogue(id = "peace-reaction", startNodeId = "line", pauseWorld = false,
                nodes = arrayListOf(StoryDialogueNode(id = "line", speaker = "Проводник",
                    text = "Мы договорились. Стража осталась невредимой, а дорога продолжается."))),
            StoryDialogue(id = "fight-reaction", startNodeId = "line", pauseWorld = false,
                nodes = arrayListOf(StoryDialogueNode(id = "line", speaker = "Проводник",
                    text = "Я выбрал бой. Теперь придётся жить с последствиями.")))
        ),
        triggers = arrayListOf(
            StoryTrigger(id = "opening", event = StoryEventKind.MissionStarted, ownerSlotId = "first",
                effects = arrayListOf(
                    StoryEffect(StoryEffectKind.Cinematic, "opening-film"),
                    StoryEffect(StoryEffectKind.Dialogue, "intro")
                )),
            StoryTrigger(id = "approach", heroId = "guide", zoneId = "approach",
                effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "approach"))),
            StoryTrigger(id = "parley", event = StoryEventKind.BeforeAttack, heroId = "guide",
                targetPosition = CityPosition(4f, 0f),
                effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "parley"))),
            StoryTrigger(id = "peace-reaction", heroId = "guide",
                condition = StoryCondition(requiredFlags = arrayListOf("peace")),
                effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "peace-reaction"))),
            StoryTrigger(id = "fight-reaction", heroId = "guide",
                condition = StoryCondition(requiredFlags = arrayListOf("fight")),
                effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "fight-reaction")))
        )
    )
}
