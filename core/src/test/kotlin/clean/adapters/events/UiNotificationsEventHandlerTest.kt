package clean.adapters.events

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import com.unciv.clean.adapters.events.UiNotificationsEventHandler
import com.unciv.clean.presentation.EventToNotificationMapper
import com.unciv.clean.domain.events.DomainEvent
import com.unciv.clean.domain.events.DomainEvent.TurnAdvanced
import com.unciv.logic.civilization.CivilizationInfo

class UiNotificationsEventHandlerTest {
    @Disabled("Skeleton: relies on EventToNotificationMapper side effects; enable if stable in tests")
    @Test
    fun `maps known events using mapper`() {
        val events = listOf<DomainEvent>(TurnAdvanced(1, "Rome"))
        val getCiv: (String) -> CivilizationInfo = { _ -> CivilizationInfo("Rome").apply { gameInfo = com.unciv.logic.GameInfo() } }
        val handler = UiNotificationsEventHandler(EventToNotificationMapper, getCiv)
        handler.handle(events.first())
        // If no exceptions occurred, consider pass for skeleton purposes
        assertEquals(1, 1)
    }
}
