package com.unciv.pure.application.battle.session

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.BattleSide

/** One outstanding request per seat. Retries must preserve every field. */
data class BattleSessionRequest(
    val sequence: Long,
    val expectedRevision: Long,
    val command: BattleCommand
)

enum class BattleSessionRejection {
    INVALID_SEQUENCE, REQUEST_CONFLICT, OUT_OF_ORDER, STALE_REVISION,
    BATTLE_FINISHED, NOT_YOUR_TURN, WRONG_TROOP
}

data class BattleSessionReply(
    val sequence: Long,
    val revision: Long,
    val rejection: BattleSessionRejection? = null,
    val result: BattleCommandResult? = null,
    val events: List<BattleEvent> = emptyList()
)

/** The adapter binds a trusted seat; a network payload must never choose its own seat. */
interface BattleSession {
    fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply
    fun read(seat: BattleSide): BattleSessionSnapshot
}

/** Execution includes activation completion on success, but never on a rejected action. */
interface BattleSessionEngine {
    val isFinished: Boolean
    val currentSide: BattleSide?
    val currentTroopId: Int?
    fun execute(command: BattleCommand): BattleSessionExecution
    fun snapshot(): BattleStateSnapshot
}

data class BattleSessionExecution(
    val result: BattleCommandResult,
    val events: List<BattleEvent>
)

/**
 * Transport-independent command ordering. The owner must serialize access to this session
 * and its engine, including reads. No network, graphics or player policy runs here.
 *
 * Each seat retains only its latest receipt. Older requests are rejected, never executed
 * again. A reconnecting client must recover its next sequence from session state.
 */
class OrderedBattleSession(private val engine: BattleSessionEngine) : BattleSession {
    private data class Receipt(val request: BattleSessionRequest, val reply: BattleSessionReply)
    private val receipts = mutableMapOf<BattleSide, Receipt>()
    var revision: Long = 0
        private set

    fun lastSequence(seat: BattleSide): Long = receipts[seat]?.request?.sequence ?: 0L

    override fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply {
        fun reject(reason: BattleSessionRejection) = BattleSessionReply(
            request.sequence, revision, rejection = reason
        )
        if (request.sequence <= 0 || request.sequence == Long.MAX_VALUE) {
            return reject(BattleSessionRejection.INVALID_SEQUENCE)
        }
        val previous = receipts[seat]
        if (previous?.request?.sequence == request.sequence) {
            return if (previous.request == request) previous.reply
            else reject(BattleSessionRejection.REQUEST_CONFLICT)
        }
        if (request.sequence != lastSequence(seat) + 1L) {
            return reject(BattleSessionRejection.OUT_OF_ORDER)
        }
        val boundaryRejection = when {
            request.expectedRevision != revision -> BattleSessionRejection.STALE_REVISION
            engine.isFinished -> BattleSessionRejection.BATTLE_FINISHED
            engine.currentSide != seat -> BattleSessionRejection.NOT_YOUR_TURN
            engine.currentTroopId != request.command.troopId -> BattleSessionRejection.WRONG_TROOP
            else -> null
        }
        val reply = if (boundaryRejection != null) reject(boundaryRejection) else {
            check(revision < Long.MAX_VALUE) { "Battle session revision exhausted" }
            val execution = engine.execute(request.command)
            // A rule rejection may consume randomness, so it is also a new revision.
            revision++
            BattleSessionReply(
                request.sequence, revision, result = execution.result,
                events = execution.events.toList()
            )
        }
        receipts[seat] = Receipt(request, reply)
        return reply
    }
    override fun read(seat: BattleSide): BattleSessionSnapshot = BattleSessionSnapshot(
        revision = revision,
        lastSequence = lastSequence(seat),
        lastReply = receipts[seat]?.reply,
        state = engine.snapshot()
    )
}
