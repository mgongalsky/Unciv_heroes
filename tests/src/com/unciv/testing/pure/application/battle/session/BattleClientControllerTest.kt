package com.unciv.testing.pure.application.battle.session

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class BattleClientControllerTest {
    private class Client : BattleClient {
        var state = BattleSessionSnapshot(0, 0, null,
            BattleStateSnapshot(emptyList(), listOf(1), 1, 1, BattleSide.ATTACKER, false, null))
        val requests = mutableListOf<BattleSessionRequest>()
        var executions = 0
        var loseBeforeDelivery = false
        var loseAfterDelivery = false
        var failRead = false
        var rejectByRules = false
        override suspend fun read(): BattleSessionSnapshot {
            if (failRead) throw IOException("Read failed")
            return state
        }
        override suspend fun submit(request: BattleSessionRequest): BattleSessionReply {
            requests.add(request)
            if (loseBeforeDelivery) {
                loseBeforeDelivery = false
                throw IOException("Not delivered")
            }
            if (state.lastSequence == request.sequence) return requireNotNull(state.lastReply)
            executions++
            val reply = BattleSessionReply(request.sequence, state.revision + 1,
                result = BattleCommandResult(success = !rejectByRules))
            state = state.copy(revision = reply.revision, lastSequence = request.sequence, lastReply = reply)
            if (loseAfterDelivery) {
                loseAfterDelivery = false
                throw IOException("Response lost")
            }
            return reply
        }
    }

    @Test
    fun lostResponseIsRecoveredByReadingReceiptWithoutExecutingAgain() = runBlocking {
        val client = Client()
        val controller = BattleClientController(client, BattleSide.ATTACKER)
        controller.refresh()
        client.loseAfterDelivery = true
        try {
            controller.submit(BattleCommand.Skip(1))
            fail("Expected transport failure")
        } catch (_: IOException) { }
        assertNotNull(controller.pendingRequest)
        controller.refresh()
        assertNull(controller.pendingRequest)
        assertEquals(1L, controller.snapshot!!.revision)
        assertEquals(1, client.executions)
        assertEquals(1, client.requests.size)
    }

    @Test
    fun undeliveredRequestBlocksNewCommandsAndRetriesIdenticalPayload() = runBlocking {
        val client = Client()
        val controller = BattleClientController(client, BattleSide.ATTACKER)
        controller.refresh()
        client.loseBeforeDelivery = true
        try {
            controller.submit(BattleCommand.Skip(1))
            fail("Expected transport failure")
        } catch (_: IOException) { }
        val pending = requireNotNull(controller.pendingRequest)
        controller.refresh()
        assertEquals(pending, controller.pendingRequest)
        try {
            controller.submit(BattleCommand.Skip(1))
            fail("Pending command must block new input")
        } catch (_: IllegalStateException) { }
        controller.retry()
        assertEquals(listOf(pending, pending), client.requests)
        assertEquals(1, client.executions)
        assertNull(controller.pendingRequest)
    }

    @Test
    fun failedStateReadAfterRuleRejectionRetainsRequestAndDoesNotExecuteTwice() = runBlocking {
        val client = Client()
        val controller = BattleClientController(client, BattleSide.ATTACKER)
        controller.refresh()
        client.rejectByRules = true
        client.failRead = true
        try {
            controller.submit(BattleCommand.Skip(1))
            fail("Expected read failure")
        } catch (_: IOException) { }
        assertNotNull(controller.pendingRequest)
        client.failRead = false
        controller.retry()
        assertEquals(1, client.executions)
        assertFalse(requireNotNull(controller.lastReply?.result).success)
        assertEquals(1L, controller.snapshot!!.lastSequence)
        assertNull(controller.pendingRequest)
    }

    @Test
    fun otherSideCannotSubmitThroughController() = runBlocking {
        val client = Client()
        val controller = BattleClientController(client, BattleSide.DEFENDER)
        controller.refresh()
        try {
            controller.submit(BattleCommand.Skip(1))
            fail("Wrong side must be rejected")
        } catch (_: IllegalStateException) { }
        assertTrue(client.requests.isEmpty())
    }
}
