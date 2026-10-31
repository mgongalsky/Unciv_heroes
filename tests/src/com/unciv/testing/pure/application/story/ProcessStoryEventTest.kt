package com.unciv.testing.pure.application.story

import com.unciv.pure.application.story.ProcessStoryEvent
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет постановку сюжетных последствий без игровых объектов и внешних эффектов. */
class ProcessStoryEventTest {
    private val outside = CityPosition(0f, 0f)
    private val gate = CityPosition(1f, 0f)
    private val inside = CityPosition(2f, 0f)
    private fun scenario(once: Boolean = true) = StoryScenario(
        zones = arrayListOf(StoryZone("gate", positions = arrayListOf(gate, inside))),
        triggers = arrayListOf(StoryTrigger(
            id = "enter", zoneId = "gate", once = once,
            effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 5))
        ))
    )
    private fun entry() = StoryEvent(
        actorKey = "unit-1", heroId = "guide", ownerSlotId = "main", ownerName = "Rome",
        from = outside, position = gate
    )

    /** Одноразовость фиксируется до исполнения награды; повторный вход не добавляет вторую награду. */
    @Test
    fun queuesOnceWithoutMutatingInputs() {
        val source = StoryProgress()
        val event = entry()
        val first = ProcessStoryEvent.execute(scenario(), source, event)
        assertTrue(source.pending.isEmpty())
        assertTrue(source.firedTriggers.isEmpty())
        assertEquals(1, first.pending.size)
        assertEquals(1L, first.pending.single().sequence)
        assertEquals(5, first.pending.single().effect.amount)
        assertEquals(first, ProcessStoryEvent.execute(scenario(), first, event))
        first.pending.single().event.ownerName = "Changed"
        assertEquals("Rome", event.ownerName)
    }

    /** Движение внутри зоны и начальная расстановка без предыдущей клетки не считаются входом. */
    @Test
    fun ignoresInternalMovementAndPlacement() {
        assertTrue(ProcessStoryEvent.execute(scenario(), StoryProgress(), entry().copy(from = gate, position = inside)).pending.isEmpty())
        assertTrue(ProcessStoryEvent.execute(scenario(), StoryProgress(), entry().copy(from = null)).pending.isEmpty())
    }

    /** Повторяемый триггер срабатывает при новом пересечении границы и получает новый номер последствия. */
    @Test
    fun repeatableEntryGetsDistinctSequence() {
        val scenario = scenario(false)
        val first = ProcessStoryEvent.execute(scenario, StoryProgress(), entry())
        val second = ProcessStoryEvent.execute(scenario, first, entry())
        assertEquals(listOf(1L, 2L), second.pending.map { it.sequence })
        assertTrue(second.firedTriggers.isEmpty())
    }

    /** Фильтры героя, участника и флагов независимы; отсутствие фильтра допускает обычного героя. */
    @Test
    fun filtersIdentityOwnerAndFlags() {
        val scenario = scenario().apply { triggers.single().apply {
            heroId = "guide"
            ownerSlotId = "main"
            condition.requiredFlags.add("ready")
        } }
        val ready = StoryProgress(flags = linkedSetOf("ready"))
        assertEquals(1, ProcessStoryEvent.execute(scenario, ready, entry()).pending.size)
        assertTrue(ProcessStoryEvent.execute(scenario, ready, entry().copy(heroId = "other")).pending.isEmpty())
        assertTrue(ProcessStoryEvent.execute(scenario, ready, entry().copy(ownerSlotId = "ally")).pending.isEmpty())
        assertTrue(ProcessStoryEvent.execute(scenario, StoryProgress(), entry()).pending.isEmpty())
        assertEquals(1, ProcessStoryEvent.execute(scenario(), StoryProgress(), entry().copy(heroId = "")).pending.size)
    }

    /** Одновременные триггеры сохраняют порядок автора и проверяют условия до исполнения флагов. */
    @Test
    fun simultaneousTriggersUseOneSnapshotAndAuthorOrder() {
        val scenario = scenario().apply {
            triggers.single().effects = arrayListOf(StoryEffect(reference = "met"))
            triggers.add(StoryTrigger(id = "later", zoneId = "gate", condition = StoryCondition(arrayListOf("met")),
                effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 99))))
            triggers.add(StoryTrigger(id = "second", zoneId = "gate",
                effects = arrayListOf(StoryEffect(StoryEffectKind.GrantGold, amount = 2))))
        }
        val result = ProcessStoryEvent.execute(scenario, StoryProgress(), entry())
        assertEquals(listOf(StoryEffectKind.SetFlag, StoryEffectKind.GrantGold), result.pending.map { it.effect.kind })
        assertEquals(setOf("enter", "second"), result.firedTriggers)
        assertTrue(result.flags.isEmpty())
    }
}
