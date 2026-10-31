package com.unciv.pure.application.story

import com.unciv.pure.domain.story.*

/** Выбирает триггеры по факту мира и ставит последствия в очередь, не выполняя внешних эффектов. */
object ProcessStoryEvent {
    fun execute(scenario: StoryScenario, progress: StoryProgress, event: StoryEvent): StoryProgress {
        val result = progress.deepCopy()
        val matching = scenario.triggers.filter { trigger ->
            trigger.event == event.kind &&
                (!trigger.once || trigger.id !in progress.firedTriggers) &&
                (trigger.heroId.isEmpty() || trigger.heroId == event.heroId) &&
                (trigger.ownerSlotId.isEmpty() || trigger.ownerSlotId == event.ownerSlotId) &&
                (trigger.targetPosition == null || trigger.targetPosition == event.position) &&
                trigger.condition.matches(progress.flags) && entersZone(scenario, trigger, event)
        }
        // Условия всех триггеров оцениваются на одном входном снимке, до последствий этого события.
        for (trigger in matching) {
            if (trigger.once) result.firedTriggers.add(trigger.id)
            for (effect in trigger.effects) {
                require(result.nextSequence > 0 && result.nextSequence < Long.MAX_VALUE) { "Story sequence exhausted" }
                result.pending.add(PendingStoryEffect(result.nextSequence++, effect.copy(), event.copy()))
            }
        }
        if (matching.isNotEmpty()) {
            require(result.revision < Long.MAX_VALUE) { "Story revision exhausted" }
            result.revision++
        }
        return result
    }

    /** Координата предыдущей клетки исключает повторный вход при движении внутри зоны. */
    private fun entersZone(scenario: StoryScenario, trigger: StoryTrigger, event: StoryEvent): Boolean {
        if (trigger.zoneId.isEmpty()) return true
        val zone = requireNotNull(scenario.zones.firstOrNull { it.id == trigger.zoneId }) {
            "Unknown story zone ${trigger.zoneId}"
        }
        return event.kind == StoryEventKind.HeroMoved && event.from != null &&
            event.position in zone.positions && event.from !in zone.positions
    }
}
