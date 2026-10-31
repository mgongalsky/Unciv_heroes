package com.unciv.pure.domain.story

import com.unciv.pure.domain.city.CityPosition

/** Снимок факта мира. actorKey идентифицирует и обычного героя; heroId задан только для сценарной личности. */
data class StoryEvent(
    var kind: StoryEventKind = StoryEventKind.HeroMoved,
    var actorKey: String = "",
    var heroId: String = "",
    var ownerSlotId: String = "",
    var ownerName: String = "",
    var from: CityPosition? = null,
    var position: CityPosition? = null,
    /** Для отложенной атаки нужен ID конкретного защитника, а не только его прежняя клетка. */
    var targetKey: String = ""
)

/** Последствие с сохранённым контекстом; nodeId хранит текущую реплику незавершённого диалога. */
data class PendingStoryEffect(
    var sequence: Long = 0,
    var effect: StoryEffect = StoryEffect(),
    var event: StoryEvent = StoryEvent(),
    var nodeId: String = ""
) {
    fun deepCopy() = copy(effect = effect.copy(), event = event.copy())
}

/** Прогресс одной партии. Не содержит объектов мира, callback, UI или ссылок на авторский сценарий. */
data class StoryProgress(
    var flags: LinkedHashSet<String> = linkedSetOf(),
    var firedTriggers: LinkedHashSet<String> = linkedSetOf(),
    var pending: ArrayList<PendingStoryEffect> = arrayListOf(),
    var nextSequence: Long = 1,
    /** Отличает повторно доставленную команду выбора даже при возврате к той же реплике. */
    var revision: Long = 0
) {
    fun deepCopy() = copy(
        flags = LinkedHashSet(flags), firedTriggers = LinkedHashSet(firedTriggers),
        pending = ArrayList(pending.map { it.deepCopy() })
    )
}
