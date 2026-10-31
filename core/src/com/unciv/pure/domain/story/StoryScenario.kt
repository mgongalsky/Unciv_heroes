package com.unciv.pure.domain.story

import com.unciv.pure.domain.city.CityPosition

/** Факты игрового мира; создание и загрузка юнита не являются движением или обнаружением. */
enum class StoryEventKind { MissionStarted, HeroMoved, ObjectSeen, ArmySeen, ObjectVisited, BeforeAttack }

/** Явные последствия сценария; доступ к миру и ресурсам выполняется игровым адаптером. */
enum class StoryEffectKind { SetFlag, GrantGold, GrantFood, SpawnArmy, SetGarrison, Dialogue, Cinematic, CancelAttack }

/** Проверка сюжетных флагов. Отсутствующий флаг считается false. */
data class StoryCondition(
    var requiredFlags: ArrayList<String> = arrayListOf(),
    var forbiddenFlags: ArrayList<String> = arrayListOf()
) {
    fun matches(flags: Set<String>): Boolean =
        requiredFlags.all { it in flags } && forbiddenFlags.none { it in flags }

    /** Черновик редактора не разделяет списки с действующим сценарием. */
    fun deepCopy() = copy(requiredFlags = ArrayList(requiredFlags), forbiddenFlags = ArrayList(forbiddenFlags))

    fun validate() {
        require((requiredFlags + forbiddenFlags).all { it.isNotBlank() }) { "Story condition has an empty flag" }
        require(requiredFlags.none { it in forbiddenFlags }) { "Story condition requires and forbids the same flag" }
    }
}

/** Зона — явный набор клеток; её имя не используется как идентификатор. */
data class StoryZone(
    var id: String = "",
    var name: String = "",
    var positions: ArrayList<CityPosition> = arrayListOf()
) {
    fun deepCopy() = copy(positions = ArrayList(positions))
}

/**
 * Одно последствие. reference задаёт флаг, диалог, видео, тип войска или ID города по kind.
 * Пустой recipientSlotId означает участника, вызвавшего событие; position нужна для появления армии.
 */
data class StoryEffect(
    var kind: StoryEffectKind = StoryEffectKind.SetFlag,
    var reference: String = "",
    var amount: Int = 0,
    var flagValue: Boolean = true,
    var recipientSlotId: String = "",
    var position: CityPosition? = null,
    var unitName: String = ""
)

/** Ответ игрока; пустой nextNodeId завершает разговор. Переходы назад позволяют задавать дополнительные вопросы. */
data class StoryChoice(
    var id: String = "",
    var text: String = "",
    var nextNodeId: String = "",
    var condition: StoryCondition = StoryCondition(),
    var effects: ArrayList<StoryEffect> = arrayListOf()
) {
    fun deepCopy() = copy(condition = condition.deepCopy(), effects = ArrayList(effects.map { it.copy() }))
}

/** Реплика говорящего. Без choices кнопка продолжения ведёт в nextNodeId либо завершает разговор. */
data class StoryDialogueNode(
    var id: String = "",
    var speaker: String = "",
    var portrait: String = "",
    var text: String = "",
    var nextNodeId: String = "",
    var choices: ArrayList<StoryChoice> = arrayListOf()
) {
    fun deepCopy() = copy(choices = ArrayList(choices.map { it.deepCopy() }))
}

/** Разговор с явной стартовой репликой; AI использует заданные автором ответы, а не случайный выбор. */
data class StoryDialogue(
    var id: String = "",
    var startNodeId: String = "",
    var nodes: ArrayList<StoryDialogueNode> = arrayListOf(),
    var pauseWorld: Boolean = true,
    var focus: CityPosition? = null,
    /** Соответствие ID узла и ID ответа; для узлов без выбора назначение не требуется. */
    var aiChoices: LinkedHashMap<String, String> = linkedMapOf()
) {
    fun deepCopy() = copy(nodes = ArrayList(nodes.map { it.deepCopy() }), aiChoices = LinkedHashMap(aiChoices))
}

/** Описание ролика без зависимости от декодера; fallbackText показывается при недоступном воспроизведении. */
data class StoryCinematic(
    var id: String = "",
    var assetPath: String = "",
    var fallbackText: String = "",
    var skippable: Boolean = true
)

/**
 * Условие запуска. Пустые heroId/ownerSlotId означают любого подходящего героя/участника.
 * Для HeroMoved zoneId означает вход снаружи; targetPosition ограничивает объект, войско или место посещения.
 * once означает однократность для всей миссии, а не отдельно для каждого героя.
 */
data class StoryTrigger(
    var id: String = "",
    var event: StoryEventKind = StoryEventKind.HeroMoved,
    var heroId: String = "",
    var ownerSlotId: String = "",
    var zoneId: String = "",
    var targetPosition: CityPosition? = null,
    var condition: StoryCondition = StoryCondition(),
    var once: Boolean = true,
    var effects: ArrayList<StoryEffect> = arrayListOf()
) {
    fun deepCopy() = copy(condition = condition.deepCopy(), effects = ArrayList(effects.map { it.copy() }))
}

/** Авторская конфигурация карты; прогресс партии не записывается в эти описания. */
data class StoryScenario(
    var zones: ArrayList<StoryZone> = arrayListOf(),
    var triggers: ArrayList<StoryTrigger> = arrayListOf(),
    var dialogues: ArrayList<StoryDialogue> = arrayListOf(),
    var cinematics: ArrayList<StoryCinematic> = arrayListOf()
) {
    fun deepCopy() = copy(
        zones = ArrayList(zones.map { it.deepCopy() }),
        triggers = ArrayList(triggers.map { it.deepCopy() }),
        dialogues = ArrayList(dialogues.map { it.deepCopy() }),
        cinematics = ArrayList(cinematics.map { it.copy() })
    )
}
