package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.battle.protocol.*
import com.unciv.infrastructure.battle.network.BattleReplyJson.int
import com.unciv.infrastructure.battle.network.BattleReplyJson.required
import com.unciv.infrastructure.battle.network.BattleReplyJson.text
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point

internal fun battleRequestJson(value: Any): String = Json().apply {
    setTypeName(null)
    setOutputType(JsonWriter.OutputType.json)
}.toJson(value)

data class RemoteScenario(val id: String, val title: String, val description: String)

class RemoteBattleLobby(private val transport: BattleHttpTransport) {
    suspend fun scenarios(): List<RemoteScenario> {
        val root = JsonReader().parse(transport.request("GET", "/v1/scenarios"))
        BattleProtocol.requireVersion(root.int("protocolVersion"))
        val entries = root.required("scenarios").also { require(it.isArray) }
        return entries.map {
            RemoteScenario(it.required("id").text(), it.required("title").text(), it.required("description").text())
        }
    }

    suspend fun create(scenarioId: String): RemoteBattleClient {
        val request = CreateBattleRoomRequest(BattleProtocol.VERSION, scenarioId).also { it.validate() }
        return ticket(transport.request("POST", "/v1/rooms", battleRequestJson(request)))
    }

    /** Retain the same random nonce if the join response is lost. */
    suspend fun join(roomCode: String, clientNonce: String): RemoteBattleClient {
        require(roomCode.matches(Regex("[A-Za-z0-9_-]{8}")))
        val request = JoinBattleRoomRequest(BattleProtocol.VERSION, clientNonce).also { it.validate() }
        return ticket(transport.request("POST", "/v1/rooms/$roomCode/join", battleRequestJson(request)))
    }

    private fun ticket(text: String): RemoteBattleClient {
        val root = JsonReader().parse(text)
        BattleProtocol.requireVersion(root.int("protocolVersion"))
        return RemoteBattleClient(
            transport, root.required("roomCode").text(), root.required("playerToken").text(),
            BattleSide.valueOf(root.required("side").text())
        )
    }
}

/** Credentials are retained across battles; each client instance is bound to one battle number. */
class RemoteBattleClient internal constructor(
    private val transport: BattleHttpTransport,
    val roomCode: String,
    private val token: String,
    val side: BattleSide,
    val battleNumber: Int = 1
) : BattleClient {
    init {
        require(roomCode.matches(Regex("[A-Za-z0-9_-]{8}")))
        require(token.matches(Regex("[A-Za-z0-9_-]{43}")))
        require(battleNumber > 0)
    }

    private val path = "/v1/rooms/$roomCode"
    var latestRoom: RemoteRoomState? = null
        private set

    fun forBattle(number: Int): RemoteBattleClient =
            RemoteBattleClient(transport, roomCode, token, side, number)

    suspend fun room(): RemoteRoomState = checkedRoom(transport.request("GET", path, token = token))
    suspend fun ready(): RemoteRoomState = checkedRoom(
        transport.request(
            "POST", "$path/ready",
            "{\"protocolVersion\":${BattleProtocol.VERSION},\"battleNumber\":$battleNumber}", token
        )
    )

    /** Retrying this call always votes for the same completed battle. */
    suspend fun rematch(): RemoteRoomState = checkedRoom(
        transport.request(
            "POST", "$path/rematch",
            "{\"protocolVersion\":${BattleProtocol.VERSION},\"battleNumber\":$battleNumber}", token
        )
    )

    override suspend fun read(): BattleSessionSnapshot = room().also {
        check(it.battleNumber == battleNumber) { "The room has moved to another battle" }
    }.battle

    override suspend fun submit(request: BattleSessionRequest): BattleSessionReply {
        fun Point.wire() = WirePoint(x, y)
        val command = request.command
        val wire = when (command) {
            is BattleCommand.Move -> WireBattleCommand(
                BattleProtocol.VERSION, request.sequence, request.expectedRevision,
                WireAction.MOVE, command.troopId, command.target.wire()
            )

            is BattleCommand.Attack -> WireBattleCommand(
                BattleProtocol.VERSION,
                request.sequence,
                request.expectedRevision,
                WireAction.ATTACK,
                command.troopId,
                command.target.wire(),
                command.attackFrom?.wire()
            )

            is BattleCommand.Shoot -> WireBattleCommand(
                BattleProtocol.VERSION, request.sequence, request.expectedRevision,
                WireAction.SHOOT, command.troopId, command.target.wire()
            )

            is BattleCommand.Skip -> WireBattleCommand(
                BattleProtocol.VERSION, request.sequence, request.expectedRevision,
                WireAction.SKIP, command.troopId
            )
        }.also { it.validate() }
        val commandPath =
                if (battleNumber == 1) "$path/commands" else "$path/battles/$battleNumber/commands"
        val reply = BattleReplyJson.decode(
            transport.request(
                "POST",
                commandPath,
                battleRequestJson(wire),
                token
            )
        )
        require(reply.sequence == request.sequence) { "Battle response sequence mismatch" }
        return reply
    }

    private fun checkedRoom(text: String): RemoteRoomState = BattleRoomJson.decode(text).also {
        require(it.side == side) { "Battle response seat mismatch" }
        latestRoom = it
    }
}
