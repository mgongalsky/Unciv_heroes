package clean.app

import org.junit.jupiter.api.Test
import com.badlogic.gdx.utils.Json
import com.unciv.clean.app.CompositionRoot

class CompositionRootWiringTest {
    @Test
    fun `default wiring has no handlers and legacy writes`() {
        val root = CompositionRoot(json = Json(), featureNotificationsViaBus = false)
        // publish пустого события не падает
        root.eventBus.publish(emptyList())
        // saveIO при write по умолчанию пишет сырое dto.raw — проверим на простом раунде
        val raw = "{\"turns\":1}"
        val out = root.saveIO.writeFromDto(com.unciv.clean.application.dto.GameInfoDto(raw))
        assert(out == raw)
    }
}
