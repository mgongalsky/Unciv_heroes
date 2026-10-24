package com.unciv.battle.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.unciv.battle.protocol.*
import java.net.InetSocketAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** HTTP adapter only. Use a TLS reverse proxy when exposing the service on the Internet. */
class BattleHttpServer(
    address: InetSocketAddress,
    private val rooms: BattleRooms,
    private val scenarios: List<WireScenario>
) : AutoCloseable {
    private val executor = ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue<Runnable>(64))
    private val server = HttpServer.create(address, 32).apply {
        executor = this@BattleHttpServer.executor
        createContext("/") { exchange -> handle(exchange) }
    }
    val port: Int get() = server.address.port
    fun start() = server.start()

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private class HttpFailure(val status: Int, val code: String) : RuntimeException()

    private fun body(exchange: HttpExchange): String {
        val type = exchange.requestHeaders.getFirst("Content-Type")?.substringBefore(';')?.trim()
        if (type != "application/json") throw HttpFailure(415, "JSON_REQUIRED")
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val count = exchange.requestBody.read(buffer)
            if (count < 0) break
            if (output.size() + count > BattleProtocol.MAX_REQUEST_BYTES) throw HttpFailure(413, "REQUEST_TOO_LARGE")
            output.write(buffer, 0, count)
        }
        return output.toString("UTF-8")
    }

    private fun token(exchange: HttpExchange): String {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: throw HttpFailure(401, "UNAUTHORIZED")
        if (!header.startsWith("Bearer ")) throw HttpFailure(401, "UNAUTHORIZED")
        return header.removePrefix("Bearer ").also {
            if (!it.matches(Regex("[A-Za-z0-9_-]{43}"))) throw HttpFailure(401, "UNAUTHORIZED")
        }
    }

    private fun ticket(access: RoomAccess) = BattleRoomTicket(
        BattleProtocol.VERSION, access.roomCode, access.token, WireSide.valueOf(access.side.name)
    )

    private fun route(exchange: HttpExchange): Any {
        val path = exchange.requestURI.path
        val method = exchange.requestMethod
        if (path == "/v1/scenarios" && method == "GET")
            return WireScenarioCatalog(BattleProtocol.VERSION, scenarios)
        if (path == "/v1/rooms" && method == "POST") {
            val root = BattleHttpJson.objectValue(body(exchange))
            BattleHttpJson.fields(root, setOf("protocolVersion", "scenarioId"))
            val request = CreateBattleRoomRequest(
                BattleHttpJson.integer(root, "protocolVersion"),
                BattleHttpJson.string(root, "scenarioId")
            ).also { it.validate() }
            if (scenarios.none { it.id == request.scenarioId }) throw HttpFailure(
                400,
                "UNKNOWN_SCENARIO"
            )
            return ticket(rooms.create(request.scenarioId))
        }
        val battleMatch =
                Regex("/v1/rooms/([A-Za-z0-9_-]{8})/battles/([1-9][0-9]*)/commands").matchEntire(
                    path
                )
        if (battleMatch != null) {
            if (method != "POST") throw HttpFailure(405, "METHOD_NOT_ALLOWED")
            val number = battleMatch.groupValues[2].toIntOrNull() ?: throw HttpFailure(
                400,
                "INVALID_REQUEST"
            )
            val credential = token(exchange)
            val request = BattleCommandWireMapper.decode(BattleHttpJson.command(body(exchange)))
            return BattleReplyWireMapper.encode(
                rooms.submit(
                    battleMatch.groupValues[1],
                    credential,
                    request,
                    number
                )
            )
        }
        val match =
                Regex("/v1/rooms/([A-Za-z0-9_-]{8})(?:/(join|ready|commands|rematch))?").matchEntire(
                    path
                )
                    ?: throw HttpFailure(404, "NOT_FOUND")
        val code = match.groupValues[1]
        val action = match.groupValues[2]
        if (action == "join" && method == "POST") {
            val root = BattleHttpJson.objectValue(body(exchange))
            BattleHttpJson.fields(root, setOf("protocolVersion", "clientNonce"))
            val request = JoinBattleRoomRequest(
                BattleHttpJson.integer(root, "protocolVersion"),
                BattleHttpJson.string(root, "clientNonce")
            ).also { it.validate() }
            return ticket(rooms.join(code, request.clientNonce))
        }
        if (action.isEmpty() && method == "GET")
            return BattleSnapshotWireMapper.encode(rooms.read(code, token(exchange)))
        if (action == "rematch" && method == "POST") {
            val credential = token(exchange)
            val root = BattleHttpJson.objectValue(body(exchange))
            BattleHttpJson.fields(root, setOf("protocolVersion", "battleNumber"))
            BattleProtocol.requireVersion(BattleHttpJson.integer(root, "protocolVersion"))
            return BattleSnapshotWireMapper.encode(
                rooms.rematch(
                    code,
                    credential,
                    BattleHttpJson.integer(root, "battleNumber")
                )
            )
        }
        if (action == "ready" && method == "POST") {
            val credential = token(exchange)
            val root = BattleHttpJson.objectValue(body(exchange))
            if (root.has("stacks")) {
                val request = BattleHttpJson.readyArmy(root)
                val stacks = request.stacks.map {
                    com.unciv.pure.application.battle.recruitment.BattleArmyStack(
                        it.unitName,
                        it.count
                    )
                }
                return BattleSnapshotWireMapper.encode(
                    rooms.ready(
                        code,
                        credential,
                        request.battleNumber,
                        stacks
                    )
                )
            }
            BattleHttpJson.fields(root, setOf("protocolVersion", "battleNumber"))
            BattleProtocol.requireVersion(BattleHttpJson.integer(root, "protocolVersion"))
            val number = if (root.has("battleNumber")) BattleHttpJson.integer(
                root,
                "battleNumber"
            ) else 1
            return BattleSnapshotWireMapper.encode(rooms.ready(code, credential, number))
        }
        if (action == "commands" && method == "POST") {
            val credential = token(exchange)
            val request = BattleCommandWireMapper.decode(BattleHttpJson.command(body(exchange)))
            return BattleReplyWireMapper.encode(rooms.submit(code, credential, request, 1))
        }
        throw HttpFailure(405, "METHOD_NOT_ALLOWED")
    }

    private fun handle(exchange: HttpExchange) {
        try {
            val (status, value) = try {
                200 to route(exchange)
            } catch (ex: HttpFailure) {
                ex.status to WireError(BattleProtocol.VERSION, ex.code)
            } catch (ex: RoomException) {
                val status = when (ex.reason) {
                    RoomFailure.UNAUTHORIZED -> 401
                    RoomFailure.NOT_FOUND -> 404
                    RoomFailure.CAPACITY -> 503
                    RoomFailure.INVALID_NONCE -> 400
                    RoomFailure.FULL, RoomFailure.NOT_READY -> 409
                }
                status to WireError(BattleProtocol.VERSION, ex.reason.name)
            } catch (_: IllegalArgumentException) {
                400 to WireError(BattleProtocol.VERSION, "INVALID_REQUEST")
            } catch (_: Exception) {
                500 to WireError(BattleProtocol.VERSION, "INTERNAL_ERROR")
            }
            val bytes = BattleHttpJson.encode(value).toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.responseHeaders.set("Cache-Control", "no-store")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        } finally {
            exchange.close()
        }
    }
}
