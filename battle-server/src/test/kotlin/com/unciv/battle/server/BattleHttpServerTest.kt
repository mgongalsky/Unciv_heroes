package com.unciv.battle.server

import com.badlogic.gdx.utils.JsonReader
import com.unciv.battle.protocol.WireScenario
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import org.junit.Assert.*
import org.junit.Test

class BattleHttpServerTest {
    private class Session : BattleSession {
        override fun read(seat: BattleSide) = BattleSessionSnapshot(
            0, 0, null, BattleStateSnapshot(emptyList(), listOf(1), 1, 1, BattleSide.ATTACKER, false, null)
        )
        override fun submit(seat: BattleSide, request: BattleSessionRequest) =
            BattleSessionReply(request.sequence, 0, BattleSessionRejection.WRONG_TROOP)
    }

    private fun request(server: BattleHttpServer, method: String, path: String, body: String? = null, token: String? = null): Pair<Int, String> {
        val connection = URL("http://127.0.0.1:${server.port}$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            return status to stream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun servesRoomLifecycleAndRejectsUnauthenticatedCommands() {
        val server = BattleHttpServer(InetSocketAddress("127.0.0.1", 0), BattleRooms({ Session() }),
            listOf(WireScenario("scenario", "Scenario", "Description")))
        try {
            server.start()
            assertEquals(200, request(server, "GET", "/v1/scenarios").first)
            val created = request(server, "POST", "/v1/rooms", """{"protocolVersion":1,"scenarioId":"scenario"}""")
            assertEquals(created.second, 200, created.first)
            val ticket = JsonReader().parse(created.second)
            val code = ticket.getString("roomCode")
            val token = ticket.getString("playerToken")
            val path = "/v1/rooms/$code"
            assertEquals(401, request(server, "GET", path).first)
            assertEquals(200, request(server, "GET", path, token = token).first)
            val joined = request(server, "POST", "$path/join", """{"protocolVersion":1,"clientNonce":"${"b".repeat(32)}"}""")
            assertEquals(joined.second, 200, joined.first)
            val otherToken = JsonReader().parse(joined.second).getString("playerToken")
            assertEquals(200, request(server, "POST", "$path/ready", """{"protocolVersion":1}""", token).first)
            assertEquals(200, request(server, "POST", "$path/ready", """{"protocolVersion":1}""", otherToken).first)
            val command = """{"protocolVersion":1,"sequence":1,"expectedRevision":0,"action":"SKIP","troopId":1}"""
            assertEquals(401, request(server, "POST", "$path/commands", command).first)
            val reply = request(server, "POST", "$path/commands", command, token)
            assertEquals(reply.second, 200, reply.first)
            assertEquals("WRONG_TROOP", JsonReader().parse(reply.second).getString("rejection"))
            assertEquals(400, request(server, "POST", "$path/commands", command.replace("SKIP", "UNKNOWN"), token).first)
            assertEquals(413, request(server, "POST", "$path/commands", "x".repeat(4097), token).first)
        } finally {
            server.close()
        }
    }

    @Test
    fun realBattleSurvivesDuplicateHttpDeliveryAndReturnsTheSameStateToBothPlayers() {
        val configured = System.getProperty("battle.assets")
        val assets = if (configured != null) java.io.File(configured) else
            generateSequence(java.io.File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                .flatMap { sequenceOf(it, java.io.File(it, "android/assets")) }
                .first { java.io.File(it, "jsons/BattleScenarios.json").isFile }
        val catalog = BattleScenarioCatalog(assets)
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), BattleRooms({ catalog.create(it, 42) }),
            catalog.scenarios.map { WireScenario(it.id, it.title, it.description) }
        )
        try {
            server.start()
            fun parsed(response: Pair<Int, String>): com.badlogic.gdx.utils.JsonValue {
                assertEquals(response.second, 200, response.first)
                return JsonReader().parse(response.second)
            }

            fun content(value: com.badlogic.gdx.utils.JsonValue): String =
                    value.toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json)

            val attacker = parsed(
                request(
                    server, "POST", "/v1/rooms",
                    """{"protocolVersion":1,"scenarioId":"spearmen-vs-archers"}"""
                )
            )
            val path = "/v1/rooms/${attacker.getString("roomCode")}"
            val attackerToken = attacker.getString("playerToken")
            val defender = parsed(
                request(
                    server, "POST", "$path/join",
                    """{"protocolVersion":1,"clientNonce":"${"d".repeat(32)}"}"""
                )
            )
            val defenderToken = defender.getString("playerToken")
            parsed(
                request(
                    server,
                    "POST",
                    "$path/ready",
                    """{"protocolVersion":1}""",
                    attackerToken
                )
            )
            val ready = parsed(
                request(
                    server,
                    "POST",
                    "$path/ready",
                    """{"protocolVersion":1}""",
                    defenderToken
                )
            )
            assertEquals("ACTIVE", ready.getString("phase"))
            val initial = ready.get("battle").get("state")
            val troopId = initial.getInt("currentTroopId")
            val activeToken =
                    if (initial.getString("currentSide") == "ATTACKER") attackerToken else defenderToken
            val command =
                    """{"protocolVersion":1,"sequence":1,"expectedRevision":0,"action":"SKIP","troopId":$troopId}"""
            val first = request(server, "POST", "$path/commands", command, activeToken)
            val firstJson = parsed(first)
            assertTrue(firstJson.get("result").getBoolean("success"))
            assertEquals(1L, firstJson.getLong("revision"))
            val duplicate = request(server, "POST", "$path/commands", command, activeToken)
            assertEquals(first, duplicate)
            val a = parsed(request(server, "GET", path, token = attackerToken)).get("battle")
            val b = parsed(request(server, "GET", path, token = defenderToken)).get("battle")
            assertEquals(1L, a.getLong("revision"))
            assertEquals(1L, b.getLong("revision"))
            assertEquals(content(a.get("state")), content(b.get("state")))
            val recovered = parsed(request(server, "GET", path, token = activeToken)).get("battle")
            assertEquals(1L, recovered.getLong("lastSequence"))
            assertEquals(content(firstJson), content(recovered.get("lastReply")))
        } finally {
            server.close()
        }
    }

    @Test
    fun productionClientTransportReadsCatalogAndPreservesHttpFailures() =
            kotlinx.coroutines.runBlocking {
                val server = BattleHttpServer(
                    InetSocketAddress("127.0.0.1", 0), BattleRooms({ Session() }),
                    listOf(WireScenario("scenario", "Scenario", "Description"))
                )
                try {
                    server.start()
                    val client =
                            com.unciv.infrastructure.battle.network.UrlBattleHttpTransport("http://127.0.0.1:${server.port}")
                    val catalog = JsonReader().parse(client.request("GET", "/v1/scenarios"))
                    assertEquals("scenario", catalog.get("scenarios").get(0).getString("id"))
                    val ticket = JsonReader().parse(
                        client.request(
                            "POST", "/v1/rooms",
                            """{"protocolVersion":1,"scenarioId":"scenario"}"""
                        )
                    )
                    val path = "/v1/rooms/${ticket.getString("roomCode")}"
                    try {
                        client.request("GET", path)
                        fail("Missing token must fail")
                    } catch (ex: com.unciv.infrastructure.battle.network.BattleHttpException) {
                        assertEquals(401, ex.status)
                        assertEquals(
                            "UNAUTHORIZED",
                            JsonReader().parse(ex.responseBody).getString("code")
                        )
                    }
                    val view = JsonReader().parse(
                        client.request(
                            "GET",
                            path,
                            token = ticket.getString("playerToken")
                        )
                    )
                    assertEquals("WAITING", view.getString("phase"))
                    val bounded = com.unciv.infrastructure.battle.network.UrlBattleHttpTransport(
                        "http://127.0.0.1:${server.port}", maximumResponseBytes = 8
                    )
                    try {
                        bounded.request("GET", "/v1/scenarios")
                        fail("Oversized response must fail")
                    } catch (ex: java.io.IOException) {
                        assertEquals("Battle server response is too large", ex.message)
                    }
                } finally {
                    server.close()
                }
            }

    @Test
    fun remoteBattleClientUsesTheApplicationPortOverHttp() = kotlinx.coroutines.runBlocking {
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), BattleRooms({ Session() }),
            listOf(WireScenario("scenario", "Scenario", "Description"))
        )
        try {
            server.start()
            val lobby = com.unciv.infrastructure.battle.network.RemoteBattleLobby(
                com.unciv.infrastructure.battle.network.UrlBattleHttpTransport("http://127.0.0.1:${server.port}")
            )
            assertEquals("scenario", lobby.scenarios().single().id)
            val attacker = lobby.create("scenario")
            val defender = lobby.join(attacker.roomCode, "z".repeat(32))
            assertEquals(BattleSide.ATTACKER, attacker.side)
            assertEquals(BattleSide.DEFENDER, defender.side)
            assertEquals(
                com.unciv.infrastructure.battle.network.RemoteRoomPhase.WAITING,
                attacker.room().phase
            )
            attacker.ready()
            assertEquals(
                com.unciv.infrastructure.battle.network.RemoteRoomPhase.ACTIVE,
                defender.ready().phase
            )
            val port: BattleClient = attacker
            val before = port.read()
            assertEquals(1, before.state.currentTroopId)
            val reply = port.submit(
                BattleSessionRequest(
                    1, before.revision,
                    com.unciv.pure.application.battle.BattleCommand.Skip(1)
                )
            )
            assertEquals(BattleSessionRejection.WRONG_TROOP, reply.rejection)
            assertEquals(attacker.read().state, defender.read().state)
        } finally {
            server.close()
        }
    }
}
