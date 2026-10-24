package com.unciv.pure.application.battle.session

/**
 * A seat-bound application port shared by local, AI and remote controllers.
 * Remote implementations may suspend for I/O and throw on transport failure.
 * After an uncertain delivery, read the receipt or retry the unchanged request.
 */
interface BattleClient {
    suspend fun read(): BattleSessionSnapshot
    suspend fun submit(request: BattleSessionRequest): BattleSessionReply
}
