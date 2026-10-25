package com.unciv.battle.server

import com.unciv.battle.protocol.WireScenario
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

/** Arguments: assets directory, port, bind address. Defaults bind only to loopback for a TLS proxy. */
object BattleServerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size <= 3) { "Usage: battle-server [assets-directory] [port] [bind-address]" }
        val assets = File(args.getOrNull(0) ?: System.getenv("BATTLE_ASSETS") ?: "assets")
        val portText = args.getOrNull(1) ?: System.getenv("BATTLE_PORT") ?: "8081"
        val port = requireNotNull(portText.toIntOrNull()) { "Invalid battle server port" }
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        val bind = args.getOrNull(2) ?: System.getenv("BATTLE_BIND") ?: "127.0.0.1"
        val catalog = BattleScenarioCatalog(assets)
        val rooms = BattleRooms({ scenario -> catalog.create(scenario) }, recruitment = catalog.recruitment, createPurchasedSession = catalog::createPurchased)
        val server = BattleHttpServer(
            InetSocketAddress(bind, port), rooms,
            listOf(WireScenario(BattleRooms.RECRUITMENT_SCENARIO, "Build army — 500 coins", "Choose up to five stacks. Both players receive 500 coins.")) + catalog.scenarios.map { WireScenario(it.id, it.title, it.description) }
        )
        val stopped = CountDownLatch(1)
        Runtime.getRuntime().addShutdownHook(Thread({
            try {
                server.close()
            } finally {
                stopped.countDown()
            }
        }, "battle-server-shutdown"))
        try {
            server.start()
            println("Battle server listening on $bind:${server.port}; scenarios=${catalog.scenarios.size + 1}")
            stopped.await()
        } catch (ex: Exception) {
            server.close()
            throw ex
        }
    }
}
