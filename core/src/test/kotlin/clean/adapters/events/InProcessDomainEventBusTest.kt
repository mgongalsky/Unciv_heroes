package clean.adapters.events

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.unciv.clean.adapters.events.InProcessDomainEventBus
import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.application.ports.events.EventPriority
import com.unciv.clean.domain.events.DomainEvent

class InProcessDomainEventBusTest {

    @Test
    fun `handlers are called by priority and isolated on errors`() {
        val calls = mutableListOf<String>()
        val h1 = object: DomainEventHandler {
            override val priority: Int = EventPriority.HIGH
            override fun canHandle(event: DomainEvent) = true
            override fun handle(event: DomainEvent) { calls += "h1"; throw RuntimeException("boom") }
        }
        val h2 = object: DomainEventHandler {
            override val priority: Int = EventPriority.NORMAL
            override fun canHandle(event: DomainEvent) = true
            override fun handle(event: DomainEvent) { calls += "h2" }
        }
        val h0 = object: DomainEventHandler {
            override val priority: Int = EventPriority.CRITICAL
            override fun canHandle(event: DomainEvent) = true
            override fun handle(event: DomainEvent) { calls += "h0" }
        }
        val bus = InProcessDomainEventBus(listOf(h1, h2, h0), logger = null)

        bus.publish(listOf(com.unciv.clean.domain.events.DomainEvent.TurnAdvanced(1, "Rome")))

        assertEquals(listOf("h0", "h1", "h2"), calls)
    }
}
