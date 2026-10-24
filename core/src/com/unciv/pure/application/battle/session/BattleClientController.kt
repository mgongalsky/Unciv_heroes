package com.unciv.pure.application.battle.session

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.BattleSide
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes client operations and retains uncertain deliveries across transport failures. */
class BattleClientController(
    private val client: BattleClient,
    private val side: BattleSide
) {
    private val mutex = Mutex()
    var snapshot: BattleSessionSnapshot? = null
        private set
    var pendingRequest: BattleSessionRequest? = null
        private set
    var lastReply: BattleSessionReply? = null
        private set

    suspend fun refresh(): BattleSessionSnapshot = mutex.withLock { readState() }

    suspend fun submit(command: BattleCommand): BattleSessionSnapshot = mutex.withLock {
        check(pendingRequest == null) { "Resolve the previous command before submitting another" }
        val current = requireNotNull(snapshot) { "Read battle state before submitting a command" }
        check(!current.state.finished) { "Battle has finished" }
        check(current.state.currentSide == side) { "Not your turn" }
        check(current.state.currentTroopId == command.troopId) { "Wrong active troop" }
        check(current.lastSequence < Long.MAX_VALUE - 1) { "Request sequence exhausted" }
        val request = BattleSessionRequest(current.lastSequence + 1, current.revision, command)
        pendingRequest = request
        deliver(request)
    }

    /** Retry the exact request, including its original expected revision. */
    suspend fun retry(): BattleSessionSnapshot = mutex.withLock {
        val request = pendingRequest ?: return@withLock readState()
        deliver(request)
    }

    private suspend fun deliver(request: BattleSessionRequest): BattleSessionSnapshot {
        val reply = client.submit(request)
        check(reply.sequence == request.sequence) { "Response sequence mismatch" }
        check(reply.rejection !in setOf(
            BattleSessionRejection.INVALID_SEQUENCE,
            BattleSessionRejection.REQUEST_CONFLICT,
            BattleSessionRejection.OUT_OF_ORDER
        )) { "Request ordering conflict; reconnect before continuing" }
        lastReply = reply
        // Retain pending until a fresh read confirms the receipt and supplies the resulting state.
        return readState()
    }

    private suspend fun readState(): BattleSessionSnapshot {
        val received = client.read()
        val previous = snapshot
        check(previous == null || received.revision >= previous.revision) { "Battle revision moved backwards" }
        check(previous == null || received.lastSequence >= previous.lastSequence) { "Request sequence moved backwards" }
        val pending = pendingRequest
        if (pending != null) {
            check(received.lastSequence <= pending.sequence) { "Another controller used this player seat" }
            if (received.lastSequence == pending.sequence) {
                val receipt = requireNotNull(received.lastReply) { "Missing command receipt" }
                check(receipt.sequence == pending.sequence && receipt.revision <= received.revision) {
                    "Invalid command receipt"
                }
                lastReply = receipt
                pendingRequest = null
            }
        }
        snapshot = received
        return received
    }
}
