package com.unciv.logic.battle.session

import com.unciv.pure.application.battle.session.BattleClient
import com.unciv.pure.application.battle.session.BattleSession
import com.unciv.pure.application.battle.session.BattleSessionReply
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.application.battle.session.BattleSessionSnapshot
import com.unciv.pure.domain.battle.BattleSide

/**
 * Owns exclusive access to a session. Create one host per battle, then bind both seats.
 * The supplied session and its engine must not be accessed outside this owner.
 * Server adapters bind seats only after authentication; controllers receive only BattleClient.
 */
class LocalBattleHost(private val session: BattleSession) {
    private val lock = Any()

    fun client(seat: BattleSide): BattleClient = object : BattleClient {
        override suspend fun read(): BattleSessionSnapshot = synchronized(lock) {
            session.read(seat)
        }

        override suspend fun submit(request: BattleSessionRequest): BattleSessionReply = synchronized(lock) {
            session.submit(seat, request)
        }
    }
}
