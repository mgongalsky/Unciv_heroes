package com.unciv.battle.server

import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleLoss
import java.security.SecureRandom
import java.util.Base64

class RoomAccess(val roomCode: String, val token: String, val side: BattleSide) {
    override fun toString() = "RoomAccess(roomCode=$roomCode, side=$side)"
}

enum class RoomFailure { NOT_FOUND, UNAUTHORIZED, FULL, NOT_READY, CAPACITY, INVALID_NONCE }
class RoomException(val reason: RoomFailure) : RuntimeException(reason.name)

data class RoomView(
    val scenarioId: String,
    val side: BattleSide,
    val opponentJoined: Boolean,
    val attackerReady: Boolean,
    val defenderReady: Boolean,
    val battle: BattleSessionSnapshot,
    val battleNumber: Int = 1,
    val attackerRematch: Boolean = false,
    val defenderRematch: Boolean = false,
    val report: BattleReport? = null
)

/** Owns sessions and serializes commands, rematch votes, snapshots and expiry. */
class BattleRooms(
    private val createSession: (String) -> BattleSession,
    private val maxRooms: Int = 32,
    private val idleMillis: Long = 30 * 60 * 1000L,
    private val now: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private class Room(
        val scenario: String,
        var session: BattleSession,
        val attacker: RoomAccess,
        var touched: Long
    ) {
        var initialTroops = session.read(BattleSide.ATTACKER).state.troops.toList()
        var battleNumber = 1
        var defender: RoomAccess? = null
        var joinNonce: String? = null
        var attackerReady = false
        var defenderReady = false
        var attackerRematch = false
        var defenderRematch = false
    }
    private val random = SecureRandom()
    private val rooms = mutableMapOf<String, Room>()

    init { require(maxRooms > 0 && idleMillis > 0) }

    private fun secret(bytes: Int): String = ByteArray(bytes).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    @Synchronized
    fun create(scenarioId: String): RoomAccess {
        val time = now()
        rooms.entries.removeAll { time - it.value.touched >= idleMillis }
        if (rooms.size >= maxRooms) throw RoomException(RoomFailure.CAPACITY)
        val session = createSession(scenarioId)
        var code = secret(6)
        while (code in rooms) code = secret(6)
        val access = RoomAccess(code, secret(32), BattleSide.ATTACKER)
        rooms[code] = Room(scenarioId, session, access, now())
        return access
    }

    @Synchronized
    fun join(code: String, nonce: String): RoomAccess {
        if (!nonce.matches(Regex("[A-Za-z0-9_-]{32,128}"))) throw RoomException(RoomFailure.INVALID_NONCE)
        val room = room(code)
        val existing = room.defender
        if (existing != null) {
            if (room.joinNonce != nonce) throw RoomException(RoomFailure.FULL)
            room.touched = now()
            return existing
        }
        return RoomAccess(code, secret(32), BattleSide.DEFENDER).also {
            room.defender = it
            room.joinNonce = nonce
            room.touched = now()
        }
    }

    @Synchronized
    fun read(code: String, token: String): RoomView {
        val room = room(code)
        val side = authenticate(room, token)
        room.touched = now()
        return view(room, side)
    }

    @Synchronized
    fun ready(code: String, token: String, battleNumber: Int = 1): RoomView {
        val room = room(code)
        val side = authenticate(room, token)
        require(battleNumber == room.battleNumber) { "Battle number mismatch" }
        if (side == BattleSide.ATTACKER) room.attackerReady = true else room.defenderReady = true
        room.touched = now()
        return view(room, side)
    }

    @Synchronized
    fun submit(code: String, token: String, request: BattleSessionRequest,
               battleNumber: Int = 1): BattleSessionReply {
        val room = room(code)
        val side = authenticate(room, token)
        require(battleNumber == room.battleNumber) { "Battle number mismatch" }
        if (!room.attackerReady || !room.defenderReady) throw RoomException(RoomFailure.NOT_READY)
        room.touched = now()
        return room.session.submit(side, request)
    }

    /** A retry for the previous battle reads the new state without voting in it. */
    @Synchronized
    fun rematch(code: String, token: String, battleNumber: Int): RoomView {
        val room = room(code)
        val side = authenticate(room, token)
        require(battleNumber > 0)
        if (battleNumber == room.battleNumber - 1) {
            room.touched = now()
            return view(room, side)
        }
        require(battleNumber == room.battleNumber) { "Battle number mismatch" }
        if (room.defender == null || !room.session.read(side).state.finished)
            throw RoomException(RoomFailure.NOT_READY)
        if (side == BattleSide.ATTACKER) room.attackerRematch = true else room.defenderRematch = true
        if (room.attackerRematch && room.defenderRematch) {
            check(room.battleNumber < Int.MAX_VALUE) { "Battle number exhausted" }
            val next = createSession(room.scenario)
            val initial = next.read(BattleSide.ATTACKER).state.troops.toList()
            room.session = next
            room.initialTroops = initial
            room.battleNumber++
            room.attackerRematch = false
            room.defenderRematch = false
            room.attackerReady = true
            room.defenderReady = true
        }
        room.touched = now()
        return view(room, side)
    }

    private fun view(room: Room, side: BattleSide): RoomView {
        val snapshot = room.session.read(side)
        val report = if (!snapshot.state.finished) null else {
            val remaining = snapshot.state.troops.associateBy { it.id }
            fun losses(seat: BattleSide) = room.initialTroops.filter { it.side == seat }.mapNotNull {
                val lost = (it.currentAmount - (remaining[it.id]?.currentAmount ?: 0)).coerceAtLeast(0)
                if (lost == 0) null else BattleLoss(it.id, it.unitName, lost)
            }
            BattleReport(snapshot.state.winner, losses(BattleSide.ATTACKER), losses(BattleSide.DEFENDER))
        }
        return RoomView(room.scenario, side, room.defender != null,
            room.attackerReady, room.defenderReady, snapshot, room.battleNumber,
            room.attackerRematch, room.defenderRematch, report)
    }

    private fun room(code: String): Room {
        val room = rooms[code] ?: throw RoomException(RoomFailure.NOT_FOUND)
        if (now() - room.touched >= idleMillis) {
            rooms.remove(code)
            throw RoomException(RoomFailure.NOT_FOUND)
        }
        return room
    }

    private fun authenticate(room: Room, token: String): BattleSide = when (token) {
        room.attacker.token -> BattleSide.ATTACKER
        room.defender?.token -> BattleSide.DEFENDER
        else -> throw RoomException(RoomFailure.UNAUTHORIZED)
    }
}
