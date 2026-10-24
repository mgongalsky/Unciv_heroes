package com.unciv.pure.application.battle.session

import com.unciv.pure.domain.battle.BattleSide

/** Adds static presentation terrain without changing command ordering or execution. */
class BattleFieldSession(
    private val session: BattleSession,
    description: BattleFieldDescription
) : BattleSession {
    private val field = description.detached()

    override fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply =
        session.submit(seat, request)

    override fun read(seat: BattleSide): BattleSessionSnapshot {
        val snapshot = session.read(seat)
        return snapshot.copy(state = snapshot.state.copy(field = field.detached()))
    }

    private fun BattleFieldDescription.detached() = copy(
        cells = cells.map { it.copy(features = it.features.toList()) }
    )
}
