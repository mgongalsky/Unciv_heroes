package com.unciv.pure.application.story

import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.story.*

/** Проверяет связи авторского сценария на значениях до записи карты или запуска миссии. */
object ValidateStoryScenario {
    fun execute(
        story: StoryScenario,
        heroIds: Set<String>,
        slotIds: Set<String>,
        cityIds: Set<String>,
        mapPositions: Set<CityPosition>
    ) {
        fun ids(values: List<String>, label: String): Set<String> {
            require(values.all { it.isNotBlank() } && values.distinct().size == values.size) {
                "$label: empty or duplicate ID"
            }
            return values.toSet()
        }
        val zones = ids(story.zones.map { it.id }, "Story zones")
        val dialogues = ids(story.dialogues.map { it.id }, "Story dialogues")
        val cinematics = ids(story.cinematics.map { it.id }, "Story cinematics")
        ids(story.triggers.map { it.id }, "Story triggers")
        fun position(value: CityPosition?) {
            require(value == null || value in mapPositions) { "Story position is outside the map: $value" }
        }
        fun effects(values: List<StoryEffect>, allowCancelAttack: Boolean) {
            for (effect in values) {
                require(effect.recipientSlotId.isEmpty() || effect.recipientSlotId in slotIds) {
                    "Unknown story recipient slot ${effect.recipientSlotId}"
                }
                position(effect.position)
                when (effect.kind) {
                    StoryEffectKind.SetFlag -> require(effect.reference.isNotBlank()) { "Empty story flag" }
                    StoryEffectKind.GrantGold, StoryEffectKind.GrantFood ->
                        require(effect.amount > 0) { "Story reward must be positive" }
                    StoryEffectKind.SpawnArmy -> {
                        require(effect.unitName.isNotBlank() && effect.amount > 0 && effect.position != null) {
                            "SpawnArmy requires a unit, positive amount and position"
                        }
                    }
                    StoryEffectKind.SetGarrison -> {
                        require(effect.reference in cityIds && effect.unitName.isNotBlank() && effect.amount > 0) {
                            "SetGarrison requires a known city, unit and positive amount"
                        }
                    }
                    StoryEffectKind.Dialogue -> require(effect.reference in dialogues) { "Unknown story dialogue ${effect.reference}" }
                    StoryEffectKind.Cinematic -> require(effect.reference in cinematics) { "Unknown story cinematic ${effect.reference}" }
                    StoryEffectKind.CancelAttack -> require(allowCancelAttack) { "CancelAttack requires an attack or dialogue context" }
                }
            }
        }
        for (zone in story.zones) {
            require(zone.positions.isNotEmpty() && zone.positions.distinct().size == zone.positions.size) {
                "Story zone ${zone.id}: empty or repeated cells"
            }
            zone.positions.forEach { position(it) }
        }
        for (dialogue in story.dialogues) {
            val nodes = ids(dialogue.nodes.map { it.id }, "Dialogue ${dialogue.id} nodes")
            require(dialogue.startNodeId in nodes) { "Dialogue ${dialogue.id}: unknown start node" }
            position(dialogue.focus)
            require(dialogue.aiChoices.keys.all { it in nodes }) { "Dialogue ${dialogue.id}: unknown AI node" }
            for (node in dialogue.nodes) {
                require(node.text.isNotBlank()) { "Dialogue ${dialogue.id}/${node.id}: empty text" }
                require(node.nextNodeId.isEmpty() || node.nextNodeId in nodes) { "Unknown next dialogue node ${node.nextNodeId}" }
                ids(node.choices.map { it.id }, "Dialogue ${dialogue.id}/${node.id} choices")
                require(node.choices.isEmpty() || node.nextNodeId.isEmpty()) { "Dialogue node has both choices and an automatic next node" }
                for (choice in node.choices) {
                    require(choice.text.isNotBlank()) { "Empty dialogue choice text" }
                    require(choice.nextNodeId.isEmpty() || choice.nextNodeId in nodes) { "Unknown choice destination ${choice.nextNodeId}" }
                    choice.condition.validate()
                    effects(choice.effects, true)
                }
                if (node.choices.isNotEmpty()) {
                    // Ответы могут продолжать разговор: немедленный выход из каждого узла не обязателен.
                    val aiChoice = node.choices.firstOrNull { it.id == dialogue.aiChoices[node.id] }
                    require(aiChoice != null && aiChoice.condition.requiredFlags.isEmpty() && aiChoice.condition.forbiddenFlags.isEmpty()) {
                        "Dialogue ${dialogue.id}/${node.id}: an unconditional AI choice is required"
                    }
                } else require(node.id !in dialogue.aiChoices) { "AI choice assigned to a node without choices" }
            }
            // AI не должен бесконечно ходить по вопросам; путь автора проверяется без запуска мира.
            val visited = HashSet<String>()
            var current = dialogue.startNodeId
            while (current.isNotEmpty()) {
                require(visited.add(current)) { "Dialogue ${dialogue.id}: AI path contains a cycle" }
                val node = dialogue.nodes.first { it.id == current }
                current = if (node.choices.isEmpty()) node.nextNodeId
                    else node.choices.first { it.id == dialogue.aiChoices[node.id] }.nextNodeId
            }
        }
        for (cinematic in story.cinematics) {
            require(cinematic.fallbackText.isNotBlank()) { "Cinematic ${cinematic.id}: fallback text is required" }
            val path = cinematic.assetPath.replace('\\', '/')
            require(!path.startsWith('/') && ':' !in path && path.split('/').none { it == ".." }) {
                "Cinematic ${cinematic.id}: use an asset-relative path"
            }
        }
        for (trigger in story.triggers) {
            trigger.condition.validate()
            require(trigger.heroId.isEmpty() || trigger.heroId in heroIds) { "Unknown trigger hero ${trigger.heroId}" }
            require(trigger.ownerSlotId.isEmpty() || trigger.ownerSlotId in slotIds) { "Unknown trigger owner slot ${trigger.ownerSlotId}" }
            require(trigger.zoneId.isEmpty() || trigger.zoneId in zones) { "Unknown trigger zone ${trigger.zoneId}" }
            require(trigger.zoneId.isEmpty() || trigger.event == StoryEventKind.HeroMoved) { "Zones require a movement event" }
            require(trigger.event != StoryEventKind.MissionStarted || trigger.heroId.isEmpty()) { "Mission start has no acting hero" }
            position(trigger.targetPosition)
            require(trigger.effects.isNotEmpty()) { "Story trigger ${trigger.id} has no effects" }
            effects(trigger.effects, trigger.event == StoryEventKind.BeforeAttack)
        }
    }
}
