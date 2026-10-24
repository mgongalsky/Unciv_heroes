package com.unciv.battle.server

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.battle.protocol.*
import com.unciv.infrastructure.battle.network.BattleHttpTransport
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BattlePresentationRoundTripTest {
    @Test
    fun clientRestoresAllCommandShapesFormationAndTerrain() = runBlocking {
        val state = BattleStateSnapshot(
            listOf(BattleTroopSnapshot(1, BattleSide.ATTACKER, 0, "Archer", 10, 8, 50, 100,
                5, 10, 12, 0, 160, Point(-1, 0), 20, 40, false, true, 15, 2, 20, 50)),
            listOf(1), 1, 1, BattleSide.ATTACKER, false, null,
            BattleFieldDescription(2, 1, listOf(
                BattleFieldCell(Point(-1, 0), "Grassland", listOf("Hill"), 0.25, 0.6, false),
                BattleFieldCell(Point(0, 0), "Mountain", emptyList(), -0.5, 0.4, true)
            )),
            listOf(BattleCommand.Skip(1), BattleCommand.Move(1, Point(0, 0)),
                BattleCommand.Shoot(1, Point(1, 1)), BattleCommand.Attack(1, Point(1, 1), Point(0, 1)))
        )
        val snapshot = BattleSessionSnapshot(7, 3, BattleSessionReply(3, 7,
            rejection = BattleSessionRejection.STALE_REVISION), state)
        val wire = BattleSnapshotWireMapper.encode(RoomView(
            "scenario", BattleSide.ATTACKER, true, true, true, snapshot
        ))
        fun json(value: Any) = Json().apply {
            setTypeName(null)
            setOutputType(JsonWriter.OutputType.json)
        }.toJson(value)
        val transport = object : BattleHttpTransport {
            override suspend fun request(method: String, path: String, body: String?, token: String?): String =
                if (method == "POST" && path == "/v1/rooms") {
                    json(BattleRoomTicket(BattleProtocol.VERSION, "abcdefgh", "a".repeat(43), WireSide.ATTACKER))
                } else {
                    assertEquals("GET", method)
                    assertEquals("/v1/rooms/abcdefgh", path)
                    assertEquals("a".repeat(43), token)
                    json(wire)
                }
        }
        val client = RemoteBattleLobby(transport).create("scenario")
        assertEquals(snapshot, client.read())
    }
}
