package com.unciv.battle.protocol

/** Wire values only: no dependency on the engine, UI or a transport library. */
object BattleProtocol {
    const val VERSION = 1
    const val MAX_REQUEST_BYTES = 4096
    const val MAX_COORDINATE = 1024

    fun requireVersion(version: Int) {
        require(version == VERSION) { "Unsupported battle protocol version: $version" }
    }
}

enum class WireAction { MOVE, ATTACK, SHOOT, SKIP }
enum class WireSide { ATTACKER, DEFENDER }
enum class WireRoomPhase { WAITING, ACTIVE, FINISHED, CLOSED }

data class WirePoint(val x: Int, val y: Int) {
    fun validate() {
        require(x in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
        require(y in -BattleProtocol.MAX_COORDINATE..BattleProtocol.MAX_COORDINATE)
    }
}

/** Authentication is carried separately; clients cannot submit a side or damage result. */
data class WireBattleCommand(
    val protocolVersion: Int,
    val sequence: Long,
    val expectedRevision: Long,
    val action: WireAction,
    val troopId: Int,
    val target: WirePoint? = null,
    val attackFrom: WirePoint? = null
) {
    /** Call after decoding, before mapping to an application command. */
    fun validate() {
        BattleProtocol.requireVersion(protocolVersion)
        require(sequence in 1 until Long.MAX_VALUE) { "Invalid request sequence" }
        require(expectedRevision >= 0) { "Invalid state revision" }
        require(troopId > 0) { "Invalid troop ID" }
        target?.validate()
        attackFrom?.validate()
        when (action) {
            WireAction.SKIP -> require(target == null && attackFrom == null) { "Skip has no coordinates" }
            WireAction.MOVE, WireAction.SHOOT -> {
                require(target != null) { "Target is required" }
                require(attackFrom == null) { "Only melee attacks have an approach tile" }
            }
            WireAction.ATTACK -> require(target != null && attackFrom != null) {
                "Melee attacks require a target and an approach tile"
            }
        }
    }
}

data class CreateBattleRoomRequest(val protocolVersion: Int, val scenarioId: String) {
    fun validate() {
        BattleProtocol.requireVersion(protocolVersion)
        require(scenarioId.matches(Regex("[a-z0-9][a-z0-9-]{0,63}"))) { "Invalid scenario ID" }
    }
}

/** clientNonce is generated once per join attempt and retained for retries. */
data class JoinBattleRoomRequest(val protocolVersion: Int, val clientNonce: String) {
    fun validate() {
        BattleProtocol.requireVersion(protocolVersion)
        require(clientNonce.matches(Regex("[A-Za-z0-9_-]{32,128}"))) { "Invalid join nonce" }
    }
}

/** The token authenticates a seat and must not appear in logs or invitation codes. */
class BattleRoomTicket(
    val protocolVersion: Int,
    val roomCode: String,
    val playerToken: String,
    val side: WireSide
) {
    override fun toString(): String = "BattleRoomTicket(roomCode=$roomCode, side=$side)"
}
