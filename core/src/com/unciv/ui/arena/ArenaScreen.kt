package com.unciv.ui.arena

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.pure.domain.arena.ArenaRun
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.battlescreen.BattleScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RecreateOnResize
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

class ArenaScreen(
    private var run: ArenaRun = ArenaBattleSetup.newRun(System.currentTimeMillis()),
    private val onRunChanged: (ArenaRun) -> Unit = {}
) : BaseScreen(), RecreateOnResize {
    init {
        globalShortcuts.add(KeyCharAndCode.BACK) { game.popScreen() }
        rebuild()
    }

    private fun rebuild() {
        stage.clear()
        val content = createArenaContent(run, ::startBattle, {
            if (run.advanceTier()) {
                onRunChanged(run)
                rebuild()
            }
        }, { game.popScreen() })
        stage.addActor(AutoScrollPane(content).apply { setFillParent(true) })
    }

    private fun startBattle() {
        val battleRun = run
        val attempt = battleRun.beginBattle() ?: return
        try {
            val ruleset = ArenaBattleSetup.ruleset()
            val encounter = attempt.encounter
            val player = ArenaBattleSetup.createArmyFromStacks(
                encounter.playerArmy, ruleset, encounter.troopSlots
            )
            val opponent = ArenaBattleSetup.createArmyFromStacks(
                encounter.opponentArmy, ruleset, encounter.troopSlots
            )
            game.pushScreen(BattleScreen.forArena(player, opponent) { report ->
                val outcome = when {
                    report == null -> ArenaRun.Outcome.ABANDONED
                    report.winner == BattleSide.ATTACKER -> ArenaRun.Outcome.VICTORY
                    report.winner == BattleSide.DEFENDER -> ArenaRun.Outcome.DEFEAT
                    else -> ArenaRun.Outcome.DRAW
                }
                battleRun.finishBattle(attempt, outcome)
            })
        } catch (exception: Exception) {
            battleRun.finishBattle(attempt, ArenaRun.Outcome.ABANDONED)
            rebuild()
            ToastPopup("Could not start arena battle: ${exception.message}", this)
        }
    }

    override fun resume() {
        rebuild()
    }

    override fun recreate(): BaseScreen = ArenaScreen(run, onRunChanged)
}

fun createArenaContent(
    run: ArenaRun,
    onBattle: () -> Unit,
    onNewRun: () -> Unit,
    onBack: () -> Unit
): Table = Table().apply {
    fun armyCell(stacks: List<com.unciv.pure.domain.arena.ArenaStack>) = Table().apply {
        stacks.groupBy { it.unitName }.forEach { (unit, troops) ->
            add(ImageGetter.getImage("UnitIcons/$unit")).size(32f).padRight(8f)
            add(Table().apply {
                add("$unit: ${troops.sumOf { it.count.toLong() }}".toLabel()).left().row()
                add(
                    "${troops.size} squads: ${troops.joinToString(" + ") { it.count.toString() }}"
                        .toLabel(fontSize = 16)
                ).left()
            }).left().padBottom(4f).row()
        }
    }
    defaults().pad(8f)
    add("Arena — Tier ${run.tier}".toLabel(fontSize = 32)).colspan(3).padBottom(12f).row()
    add("Win three battles. Troop bonus: +${run.playerBonusPercent}%.".toLabel()).colspan(3).row()
    add("Up to five squads per army. Retries start fresh.".toLabel()).colspan(3).row()
    add("Progress: ${run.completedBattles} / ${run.encounters.size}".toLabel(fontSize = 24))
        .colspan(3).padBottom(16f).row()
    run.encounters.forEachIndexed { index, encounter ->
        val status = when {
            index < run.completedBattles -> "Completed"
            index == run.completedBattles -> "Next battle"
            else -> "Locked"
        }
        val tint = if (index < run.completedBattles) Color.GREEN
        else if (index == run.completedBattles) Color.GOLD else Color.LIGHT_GRAY
        val format = if (encounter.isMixed) "Mixed" else "Single unit type"
        add("${index + 1}. $status\n$format".toLabel().apply { color = tint }).left()
        add(armyCell(encounter.playerArmy)).left()
        add(armyCell(encounter.opponentArmy)).left().row()
    }
    val feedback = when {
        run.isComplete -> "Tier complete! All three battles won."
        run.lastOutcome == ArenaRun.Outcome.VICTORY -> "Victory! The next battle is ready."
        run.lastOutcome == ArenaRun.Outcome.DEFEAT -> "Defeat. Try again — your progress is safe."
        run.lastOutcome == ArenaRun.Outcome.DRAW -> "Draw. Try this battle again."
        run.lastOutcome == ArenaRun.Outcome.ABANDONED -> "Battle left. You can retry the same encounter."
        else -> "Your troops are on the left. Defeat the opposing army!"
    }
    add(feedback.toLabel()).colspan(3).padTop(16f).row()
    if (run.isComplete) {
        val nextBonus = (run.playerBonusPercent - 10).coerceAtLeast(0)
        add("Next tier (+$nextBonus% troops)".toTextButton().apply { onActivation(onNewRun) })
            .colspan(3).height(55f).row()
    } else {
        val retry = run.lastOutcome != null && run.lastOutcome != ArenaRun.Outcome.VICTORY
        val caption = if (retry) "Retry battle" else "Start battle ${run.completedBattles + 1}"
        add(caption.toTextButton().apply { onActivation(onBattle) }).colspan(3).height(55f).row()
    }
    add("Back".toTextButton().apply { onActivation(onBack) }).colspan(3).height(45f).row()
}
