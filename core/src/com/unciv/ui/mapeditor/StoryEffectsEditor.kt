package com.unciv.ui.mapeditor

import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.story.StoryEffect
import com.unciv.pure.domain.story.StoryEffectKind
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

/** Общая форма списка последствий для триггеров и ответов; редактирует только черновик. */
object StoryEffectsEditor {
    fun create(
        effects: ArrayList<StoryEffect>, map: TileMap, ruleset: Ruleset, width: Float,
        mutate: (() -> Unit) -> Unit
    ): StoryEditorForm {
        val form = StoryEditorForm(width)
        form.add("Effects".toLabel()).row()
        for ((index, effect) in effects.withIndex()) {
            form.add("${index + 1}. ${effect.kind}".toLabel()).row()
            form.choice(
                "Effect", effect.kind.name,
                StoryEffectKind.values().map { it.name to it.name }) {
                effect.kind = StoryEffectKind.valueOf(it)
            }
            // Вид полей обновляется после сохранения текущего ввода в черновик.
            form.add("Update effect fields".toTextButton().onClick { mutate { } }).row()
            when (effect.kind) {
                StoryEffectKind.SetFlag -> {
                    form.text("Flag", effect.reference) { effect.reference = it.trim() }
                    form.flag("Flag value", effect.flagValue) { effect.flagValue = it }
                }

                StoryEffectKind.Dialogue -> form.text(
                    "Dialogue ID",
                    effect.reference
                ) { effect.reference = it.trim() }

                StoryEffectKind.Cinematic -> form.text(
                    "Cinematic ID",
                    effect.reference
                ) { effect.reference = it.trim() }

                StoryEffectKind.GrantGold, StoryEffectKind.GrantFood,
                StoryEffectKind.SpawnArmy, StoryEffectKind.SetGarrison -> {
                    form.text("Amount", effect.amount.toString()) {
                        effect.amount = requireNotNull(
                            it.trim().toIntOrNull()
                        ) { "Enter a whole number for amount" }
                    }
                    form.choice(
                        "Recipient", effect.recipientSlotId,
                        listOf("" to "Acting participant") + map.scenarioPlayerSlots.map { it.id to it.name.ifBlank { it.id } }) {
                        effect.recipientSlotId = it
                    }
                    if (effect.kind == StoryEffectKind.SpawnArmy || effect.kind == StoryEffectKind.SetGarrison) {
                        form.choice(
                            "Unit type", effect.unitName,
                            ruleset.units.values.filter {
                                ruleset.unitTypes[it.unitType]?.isLandUnit() == true && !it.isCivilian()
                            }.map { it.name to it.name }) {
                            effect.unitName = it
                        }
                        if (effect.kind == StoryEffectKind.SpawnArmy)
                            form.position(
                                "Army position (x,y)",
                                effect.position
                            ) { effect.position = it }
                        else form.choice(
                            "City",
                            effect.reference,
                            map.scenarioCities.map { it.id to it.name }) {
                            effect.reference = it
                        }
                    }
                }

                StoryEffectKind.CancelAttack -> Unit
            }
            form.add("Remove effect".toTextButton().onClick { mutate { effects.remove(effect) } })
                .row()
        }
        form.add("Add effect".toTextButton().onClick { mutate { effects.add(StoryEffect()) } })
            .row()
        return form
    }
}
