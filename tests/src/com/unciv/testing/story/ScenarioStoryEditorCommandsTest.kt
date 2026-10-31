package com.unciv.testing.story

import com.unciv.logic.map.ScenarioStoryEditorCommands
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет публикацию общего черновика без частичных изменений карты. */
class ScenarioStoryEditorCommandsTest {
    private fun hero() = ScenarioHero("scout", "Разведчик", "Warrior", "one", CityPosition(0f, 0f))
    private fun story() = StoryScenario(triggers = arrayListOf(
        StoryTrigger(id = "arrival", heroId = "scout",
            effects = arrayListOf(StoryEffect(StoryEffectKind.SetFlag, "arrived")))
    ))

    @Test fun applyCopiesDraftAndDetectsNoChange() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            val heroes = arrayListOf(hero())
            val draft = story()
            assertTrue(ScenarioStoryEditorCommands.apply(map, fixture.ruleset, heroes, draft))
            assertFalse(ScenarioStoryEditorCommands.apply(map, fixture.ruleset, heroes, draft))
            heroes.single().name = "Изменённый черновик"
            draft.triggers.single().effects.single().reference = "changed"
            draft.triggers.single().condition.requiredFlags.add("new")
            assertEquals("Разведчик", map.scenarioHeroes.single().name)
            assertEquals("arrived", map.story!!.triggers.single().effects.single().reference)
            assertTrue(map.story!!.triggers.single().condition.requiredFlags.isEmpty())
        }
    }

    @Test fun referencedHeroCannotBeDeletedWithoutUpdatingStory() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            ScenarioStoryEditorCommands.apply(map, fixture.ruleset, listOf(hero()), story())
            val before = map.story!!.deepCopy()
            try {
                ScenarioStoryEditorCommands.apply(map, fixture.ruleset, emptyList(), before)
                fail("Ссылка на удаляемого героя должна быть отклонена")
            } catch (_: IllegalArgumentException) { }
            assertEquals(listOf(hero()), map.scenarioHeroes)
            assertEquals(before, map.story)
            assertTrue(ScenarioStoryEditorCommands.apply(map, fixture.ruleset, emptyList(), null))
            assertTrue(map.scenarioHeroes.isEmpty())
            assertNull(map.story)
        }
    }

    @Test fun invalidEffectDoesNotPublishHeroChanges() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            ScenarioStoryEditorCommands.apply(map, fixture.ruleset, listOf(hero()), story())
            val before = map.story!!.deepCopy()
            val draft = story().apply {
                triggers.single().effects.add(StoryEffect(StoryEffectKind.SpawnArmy,
                    amount = 1, unitName = "Missing story unit", position = CityPosition(1f, 0f)))
            }
            try {
                ScenarioStoryEditorCommands.apply(map, fixture.ruleset,
                    listOf(hero().copy(name = "Новое имя")), draft)
                fail("Неизвестное войско должно быть отклонено")
            } catch (_: IllegalArgumentException) { }
            assertEquals(listOf(hero()), map.scenarioHeroes)
            assertEquals(before, map.story)
        }
    }

    /** Соседние инструменты редактора учитывают ссылки из ветвей диалога. */
    @Test
    fun dialogueEffectsProtectCityAndRecipientSlot() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            map.scenarioCities.add(
                com.unciv.pure.domain.city.ScenarioCity(
                    id = "fort",
                    name = "Fort"
                )
            )
            map.story = StoryScenario(
                dialogues = arrayListOf(
                    StoryDialogue(
                        id = "talk", startNodeId = "start", nodes = arrayListOf(
                            StoryDialogueNode(
                                id = "start", text = "Help?", choices = arrayListOf(
                                    StoryChoice(
                                        "yes", "Yes", effects = arrayListOf(
                                            StoryEffect(
                                                StoryEffectKind.SetGarrison,
                                                "fort",
                                                amount = 1,
                                                unitName = "Warrior"
                                            ),
                                            StoryEffect(
                                                StoryEffectKind.GrantGold,
                                                amount = 5,
                                                recipientSlotId = "one"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
            try {
                com.unciv.logic.map.ScenarioCityEditorCommands.removeCity(map, "fort")
                fail("Используемый город должен остаться")
            } catch (_: IllegalArgumentException) {
            }
            try {
                com.unciv.logic.map.ScenarioCityEditorCommands.removeSlot(map, "one")
                fail("Используемый слот должен остаться")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals("fort", map.scenarioCities.single().id)
            assertEquals("one", map.scenarioPlayerSlots.single().id)
            map.story = null
            assertTrue(com.unciv.logic.map.ScenarioCityEditorCommands.removeCity(map, "fort"))
            assertTrue(com.unciv.logic.map.ScenarioCityEditorCommands.removeSlot(map, "one"))
        }
    }

    /** Фильтр участника защищает слот даже без героя и награды этому участнику. */
    @Test
    fun triggerOwnerProtectsSlot() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            map.story = StoryScenario(
                triggers = arrayListOf(
                    StoryTrigger(
                        id = "entry",
                        ownerSlotId = "one"
                    )
                )
            )
            try {
                com.unciv.logic.map.ScenarioCityEditorCommands.removeSlot(map, "one")
                fail("Слот фильтра события должен остаться")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals("one", map.scenarioPlayerSlots.single().id)
        }
    }

    /** Применённая карта сохраняет граф диалога и ссылки; повторно открытые данные независимы. */
    @Test
    fun appliedScenarioSurvivesMapSerialization() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
            val draft = StoryScenario(
                zones = arrayListOf(StoryZone("gate", "Gate", arrayListOf(CityPosition(1f, 0f)))),
                triggers = arrayListOf(
                    StoryTrigger(
                        id = "arrival", heroId = "scout", ownerSlotId = "one", zoneId = "gate",
                        effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "meeting"))
                    )
                ),
                dialogues = arrayListOf(
                    StoryDialogue(
                        id = "meeting", startNodeId = "question",
                        nodes = arrayListOf(
                            StoryDialogueNode(
                                id = "question", speaker = "Guard", text = "Will you help?",
                                choices = arrayListOf(
                                    StoryChoice(
                                        "accept", "Yes", effects = arrayListOf(
                                            StoryEffect(StoryEffectKind.SetFlag, "accepted"),
                                            StoryEffect(
                                                StoryEffectKind.GrantGold,
                                                amount = 17,
                                                recipientSlotId = "one"
                                            )
                                        )
                                    ),
                                    StoryChoice(
                                        "leave", "No", effects = arrayListOf(
                                            StoryEffect(StoryEffectKind.SetFlag, "declined")
                                        )
                                    )
                                )
                            )
                        ), aiChoices = linkedMapOf("question" to "leave")
                    )
                )
            )
            assertTrue(
                ScenarioStoryEditorCommands.apply(
                    map,
                    fixture.ruleset,
                    listOf(hero()),
                    draft
                )
            )
            val serializer = com.unciv.json.json()
            val restored = serializer.fromJson(
                com.unciv.logic.map.TileMap::class.java, serializer.toJson(map)
            )
            assertEquals(map.scenarioHeroes, restored.scenarioHeroes)
            assertEquals(map.scenarioPlayerSlots, restored.scenarioPlayerSlots)
            assertEquals(map.story, restored.story)
            com.unciv.logic.map.ScenarioMapValidation.validateStructure(restored)
            restored.scenarioHeroes.single().name = "Changed after loading"
            restored.story!!.zones.single().positions.clear()
            restored.story!!.dialogues.single().nodes.single().choices.first().effects.clear()
            restored.story!!.dialogues.single().aiChoices.clear()
            assertEquals("Разведчик", map.scenarioHeroes.single().name)
            assertEquals(1, map.story!!.zones.single().positions.size)
            assertEquals(
                2,
                map.story!!.dialogues.single().nodes.single().choices.first().effects.size
            )
            assertEquals("leave", map.story!!.dialogues.single().aiChoices["question"])
        }
    }

    /** Проверка карты работает с авторскими определениями без BaseUnit.ruleset и видит последствия ответов. */
    @Test
    fun definitionValidationUsesExplicitRulesetAndChecksStoryArmies() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            val ruleset = com.unciv.models.ruleset.Ruleset()
            ruleset.unitTypes["Infantry"] =
                    com.unciv.models.ruleset.unit.UnitType("Infantry", "Land")
            ruleset.units["Guard"] = com.unciv.models.ruleset.unit.BaseUnit().apply {
                name = "Guard"
                unitType = "Infantry"
                strength = 10
            }
            map.scenarioHeroes.add(hero().copy(unitName = "Guard"))
            map.story = StoryScenario(
                triggers = arrayListOf(
                    StoryTrigger(
                        id = "spawn", effects = arrayListOf(
                            StoryEffect(
                                StoryEffectKind.SpawnArmy, amount = 1, unitName = "Missing",
                                position = CityPosition(1f, 0f)
                            )
                        )
                    )
                ),
                dialogues = arrayListOf(
                    StoryDialogue(
                        id = "talk", nodes = arrayListOf(
                            StoryDialogueNode(
                                id = "offer", choices = arrayListOf(
                                    StoryChoice(
                                        id = "reward", effects = arrayListOf(
                                            StoryEffect(
                                                StoryEffectKind.SetGarrison,
                                                "fort",
                                                amount = 1,
                                                unitName = "Missing"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
            val before = map.story!!.deepCopy()
            val errors = com.unciv.logic.map.ScenarioMapValidation.definitionErrors(map, ruleset)
            assertEquals(2, errors.size)
            assertTrue(errors.any { "Story trigger spawn" in it && "Missing" in it })
            assertTrue(errors.any { "Story choice talk/offer/reward" in it && "Missing" in it })
            assertEquals(before, map.story)
            ruleset.units["Guard"]!!.unitType = "Unknown type"
            assertTrue(
                com.unciv.logic.map.ScenarioMapValidation.definitionErrors(map, ruleset)
                    .any { "Scenario hero scout" in it })
        }
    }
}
