package clean.adapters.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.unciv.clean.adapters.persistence.JsonGameStateRepository
import com.unciv.logic.GameInfo
import com.unciv.json.json

class JsonGameStateRepositoryTest {

    @Disabled("Enable when JSON serializer guarantees canonical ordering; here as a Stage 8 skeleton")
    @Test
    fun `load-save keeps identical json`() {
        // Arrange: a placeholder save string. In real test, point to a known-good fixture.
        val original = """{"version":${GameInfo.CURRENT_COMPATIBILITY_VERSION}}"""
        var storage = original
        val repo = JsonGameStateRepository(
            readBytes = { storage },
            writeBytes = { s -> storage = s }
        )

        // Act
        val game = repo.load()
        repo.save(game)

        val saved = storage

        // Assert
        assertEquals(original, saved)
    }
}
