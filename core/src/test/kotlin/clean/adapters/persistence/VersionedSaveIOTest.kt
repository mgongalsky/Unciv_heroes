package clean.adapters.persistence

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import com.badlogic.gdx.utils.Json
import com.unciv.clean.adapters.persistence.SaveMigrationRegistry
import com.unciv.clean.adapters.persistence.VersionedSaveIO
import com.unciv.clean.application.dto.GameInfoDto
import com.unciv.clean.application.ports.persistence.SaveWritePolicy

class VersionedSaveIOTest {
    private val json = Json()

    @Disabled("Skeleton: enable when adding canonical JSON fixtures")
    @Test
    fun `reads legacy as DTO raw`() {
        val raw = "{\"turns\":1}"
        val io = VersionedSaveIO(json, SaveMigrationRegistry())
        val dto = io.readToDto(raw)
        assert(dto.raw == raw)
    }

    @Disabled("Skeleton: enable when adding canonical JSON fixtures")
    @Test
    fun `writes V1 when policy is VERSIONED`() {
        val io = VersionedSaveIO(json, SaveMigrationRegistry(), SaveWritePolicy.VERSIONED_V1)
        val out = io.writeFromDto(GameInfoDto(raw = "{\"turns\":1}"))
        assert(out.contains("\"formatVersion\""))
        assert(out.contains("\"payloadRaw\""))
    }
}
