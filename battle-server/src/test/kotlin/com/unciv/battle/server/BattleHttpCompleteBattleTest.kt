package com.unciv.battle.server

import com.badlogic.gdx.math.Vector2
import com.unciv.battle.protocol.WireScenario
import com.unciv.infrastructure.battle.network.RemoteBattleLobby
import com.unciv.infrastructure.battle.network.UrlBattleHttpTransport
import com.unciv.logic.HexMath
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleClientController
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.application.battle.session.BattleSessionRejection
import com.unciv.pure.application.battle.session.BattleStateSnapshot
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress

class BattleHttpCompleteBattleTest {
    private fun choose(state: BattleStateSnapshot): BattleCommand {
        val commands = state.availableCommands
        commands.filterIsInstance<BattleCommand.Shoot>().firstOrNull()?.let { return it }
        commands.filterIsInstance<BattleCommand.Attack>().firstOrNull()?.let { return it }
        val enemies = state.troops.filter { it.side != state.currentSide }.mapNotNull { it.position }
        fun distance(point: Point): Int = enemies.minOfOrNull {
            HexMath.getDistance(Vector2(point.x.toFloat(), point.y.toFloat()), Vector2(it.x.toFloat(), it.y.toFloat()))
        } ?: Int.MAX_VALUE
        val active = state.troops.single { it.id == state.currentTroopId }
        val currentDistance = active.position?.let(::distance) ?: Int.MAX_VALUE
        commands.filterIsInstance<BattleCommand.Move>()
            .minByOrNull { distance(it.target) }?.takeIf { distance(it.target) < currentDistance }?.let { return it }
        return commands.filterIsInstance<BattleCommand.Skip>().single()
    }

    @Test
    fun bothClientsAgreeThroughoutCombatAndRejectActionsAfterCompletion() = runBlocking {
        val catalog = BattleScenarioCatalog(File(requireNotNull(System.getProperty("battle.assets"))))
        val server = BattleHttpServer(
            InetSocketAddress("127.0.0.1", 0), BattleRooms({ catalog.create(it, 42L) }),
            catalog.scenarios.map { WireScenario(it.id, it.title, it.description) }
        )
        try {
            server.start()
            val url = "http://127.0.0.1:${server.port}"
            val attacker = RemoteBattleLobby(UrlBattleHttpTransport(url)).create("archers-vs-horsemen")
            val defender = RemoteBattleLobby(UrlBattleHttpTransport(url)).join(attacker.roomCode, "c".repeat(32))
            attacker.ready()
            defender.ready()
            val ports = mapOf(BattleSide.ATTACKER to attacker, BattleSide.DEFENDER to defender)
            val controllers = ports.mapValues { (side, client) -> BattleClientController(client, side) }
            val acted = mutableSetOf<BattleSide>()
            var latest = attacker.read()
            val field = latest.state.field
            var actions = 0
            while (!latest.state.finished && actions < 300) {
                val side = requireNotNull(latest.state.currentSide)
                val controller = controllers.getValue(side)
                val before = controller.refresh()
                assertEquals(latest.state, before.state)
                val command = choose(before.state)
                val after = controller.submit(command)
                val reply = requireNotNull(controller.lastReply)
                assertNull(reply.rejection)
                assertTrue("Offered command failed: $command", requireNotNull(reply.result).success)
                assertEquals(before.revision + 1, after.revision)
                assertEquals(field, after.state.field)
                val other = ports.getValue(if (side == BattleSide.ATTACKER) BattleSide.DEFENDER else BattleSide.ATTACKER).read()
                assertEquals(after.revision, other.revision)
                assertEquals(after.state, other.state)
                acted.add(side)
                latest = after
                actions++
            }
            assertTrue("Battle did not finish after $actions actions", latest.state.finished)
            assertEquals(setOf(BattleSide.ATTACKER, BattleSide.DEFENDER), acted)
            assertNotNull(latest.state.winner)
            assertTrue(latest.state.availableCommands.isEmpty())
            assertTrue(latest.state.turnQueue.isEmpty())
            assertNull(latest.state.currentTroopId)
            ports.values.forEach { client ->
                val final = client.read()
                assertEquals(latest.state, final.state)
                val rejected = client.submit(BattleSessionRequest(
                    final.lastSequence + 1, final.revision, BattleCommand.Skip(1)
                ))
                assertEquals(BattleSessionRejection.BATTLE_FINISHED, rejected.rejection)
                assertEquals(final.revision, rejected.revision)
                assertEquals(final.state, client.read().state)
            }
        } finally {
            server.close()
        }
    }
}
