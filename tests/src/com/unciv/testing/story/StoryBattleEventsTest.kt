package com.unciv.testing.story

import com.badlogic.gdx.math.Vector2
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.CityInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.story.StoryBattleEvents
import com.unciv.logic.story.StoryRuntime
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.story.*
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет предбоевой диалог на производственном пути без создания боевого экрана. */
class StoryBattleEventsTest {
    private fun prepare(fixture: CityTestFixture): Pair<MapUnit, CityInfo> {
        fixture.foundCity()
        val city = CityInfo().apply {
            state.ownership = CityOwnership.Neutral
            location = Vector2(4f, 0f)
            tiles.add(location)
            garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", 1) }
            bindNeutralWorld(fixture.game)
            fixture.game.cityRegistry.register(this)
            setTransients()
        }
        val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
        hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 50) }
        hero.updateVisibleTiles()
        fixture.civilization.updateSightAndResources()
        fixture.game.tileMap.story = StoryScenario(
            triggers = arrayListOf(StoryTrigger(id = "parley", event = StoryEventKind.BeforeAttack,
                effects = arrayListOf(StoryEffect(StoryEffectKind.Dialogue, "parley")))),
            dialogues = arrayListOf(StoryDialogue(id = "parley", startNodeId = "offer",
                nodes = arrayListOf(StoryDialogueNode(id = "offer", text = "Отступишь?", choices = arrayListOf(
                    StoryChoice("leave", "Уйти", effects = arrayListOf(StoryEffect(StoryEffectKind.CancelAttack))),
                    StoryChoice("fight", "Сражаться")
                ))), aiChoices = linkedMapOf("offer" to "leave")))
        )
        return hero to city
    }

    private fun answer(game: GameInfo, choice: String) {
        val progress = game.storyProgress
        StoryRuntime.respond(game, "Rome", progress.pending.first().sequence, progress.revision, choice)
    }

    /** Отмена и повторное продолжение не расходуют атаку и не повреждают ни одну армию. */
    @Test
    fun cancellationLeavesWorldUnchanged() {
        CityTestFixture().use { fixture ->
            val (hero, city) = prepare(fixture)
            val movement = hero.currentMovement
            val attacks = hero.attacksThisTurn
            val guard = city.garrisonInfo.getTroopAt(0)!!
            val health = guard.currentHealth
            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))
            assertTrue(StoryRuntime.isBlocked(fixture.game))
            assertNotNull(fixture.game.storyPendingAttack)
            assertEquals(attacks, hero.attacksThisTurn)
            answer(fixture.game, "leave")
            StoryBattleEvents.resume(fixture.game, "Rome")
            StoryBattleEvents.resume(fixture.game, "Rome")
            assertNull(fixture.game.storyPendingAttack)
            assertFalse(fixture.game.storyAttackCancelled)
            assertEquals(movement, hero.currentMovement, 0f)
            assertEquals(attacks, hero.attacksThisTurn)
            assertNull(city.ownerOrNull)
            assertSame(guard, city.garrisonInfo.getTroopAt(0))
            assertEquals(health, guard.currentHealth)
            assertSame(hero, hero.currentTile.militaryUnit)
        }
    }

    /** Изменившаяся допустимость цели проверяется после ответа, до запуска тактической сессии. */
    @Test
    fun removedGarrisonInvalidatesApprovedAttack() {
        CityTestFixture().use { fixture ->
            val (hero, city) = prepare(fixture)
            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))
            assertTrue(StoryRuntime.isBlocked(fixture.game))
            answer(fixture.game, "fight")
            city.garrisonInfo = ArmyInfo(null, 2)
            val attacks = hero.attacksThisTurn
            StoryBattleEvents.resume(fixture.game, "Rome")
            assertNull(fixture.game.storyPendingAttack)
            assertEquals(attacks, hero.attacksThisTurn)
            assertNull(city.ownerOrNull)
            assertEquals(Vector2(3f, 0f), hero.currentTile.position)
        }
    }

    /** JSON сохраняет конкретную цель и очередь; чтение само по себе не начинает бой. */
    @Test
    fun pendingAttackSurvivesSerialization() {
        CityTestFixture().use { fixture ->
            val (hero, city) = prepare(fixture)
            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))
            assertTrue(StoryRuntime.isBlocked(fixture.game))
            val loaded = json().fromJson(GameInfo::class.java, json().toJson(fixture.game))
            assertEquals(fixture.game.storyPendingAttack, loaded.storyPendingAttack)
            assertEquals(fixture.game.storyProgress, loaded.storyProgress)
            assertFalse(loaded.storyAttackCancelled)
            assertEquals("city:${city.id}", loaded.storyPendingAttack!!.targetKey)
            val copy = fixture.game.clone()
            copy.storyPendingAttack!!.targetKey = "changed"
            assertEquals("city:${city.id}", fixture.game.storyPendingAttack!!.targetKey)
        }
    }

    /** Разрешённая атака продолжается один раз; повторный вызов не запускает ещё один бой. */
    @Test
    fun approvedAttackResumesExactlyOnce() {
        CityTestFixture().use { fixture ->
            val (hero, city) = prepare(fixture)
            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))
            assertTrue(StoryRuntime.isBlocked(fixture.game))
            answer(fixture.game, "fight")
            // Тактическая сессия AI позволяет проверить результат продолжения без графического экрана.
            fixture.civilization.playerType = com.unciv.logic.civilization.PlayerType.AI
            val attacks = hero.attacksThisTurn
            StoryBattleEvents.resume(fixture.game, "Rome")
            assertNull(fixture.game.storyPendingAttack)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(hero, city.getCenterTile().militaryUnit)
            assertEquals(attacks + 1, hero.attacksThisTurn)
            StoryBattleEvents.resume(fixture.game, "Rome")
            assertEquals(attacks + 1, hero.attacksThisTurn)
        }
    }

    /** Авторский ответ AI отменяет бой синхронно и не оставляет ожидания человеческого ввода. */
    @Test
    fun aiDeclinesAttackUsingAuthoredChoice() {
        CityTestFixture().use { fixture ->
            val (hero, city) = prepare(fixture)
            fixture.civilization.playerType = com.unciv.logic.civilization.PlayerType.AI
            val movement = hero.currentMovement
            val attacks = hero.attacksThisTurn
            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))
            assertTrue(fixture.game.storyProgress.firedTriggers.contains("parley"))
            assertFalse(StoryRuntime.isBlocked(fixture.game))
            assertNull(fixture.game.storyPendingAttack)
            assertNull(city.ownerOrNull)
            assertEquals(attacks, hero.attacksThisTurn)
            assertEquals(movement, hero.currentMovement, 0f)
            assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.currentAmount)
        }
    }
}
