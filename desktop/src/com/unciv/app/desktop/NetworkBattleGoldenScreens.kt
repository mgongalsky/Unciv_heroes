package com.unciv.app.desktop

import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.logic.battle.session.BattleFieldPreparation
import com.unciv.logic.battle.session.BattleSessionSetup
import com.unciv.logic.map.ClimateParameters
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.ui.battlescreen.BattleRoomScreen
import com.unciv.ui.battlescreen.BattleSessionScreen
import com.unciv.ui.utils.BaseScreen

/** Production screens with no network and no changing combat state. */
internal object NetworkBattleGoldenScreens {
    fun room(): BaseScreen = BattleRoomScreen(onBattleReady = { _, _ ->
        error("Golden room must not connect")
    })

    fun battle(): BaseScreen {
        val ruleset = ArenaBattleSetup.ruleset()
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        val prepared = BattleFieldPreparation.create(climate, climate) {
            requireNotNull(ruleset.terrains[it]).impassable
        }
        val session = BattleSessionSetup.create(
            BattleSessionSetup.Side(List(4) { BattleSessionSetup.Stack("Spearman", 12) }),
            BattleSessionSetup.Side(List(4) { BattleSessionSetup.Stack("Archer", 10) }),
            RulesetTroopDefinitionSource(ruleset), prepared.field,
            object : IBattleRandom {
                override fun nextDouble(): Double = error("Rendering must not draw battle randomness")
            }, 0.0, 0.0
        )
        val snapshot = BattleFieldSession(session, prepared.description).read(BattleSide.ATTACKER)
        val client = object : BattleClient {
            override suspend fun read(): BattleSessionSnapshot = snapshot
            override suspend fun submit(request: BattleSessionRequest): BattleSessionReply =
                error("Golden battle must not submit commands")
        }
        return BattleSessionScreen(client, BattleSide.ATTACKER, snapshot, ruleset)
    }
}
