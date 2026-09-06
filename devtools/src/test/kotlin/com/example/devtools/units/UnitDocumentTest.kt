package com.example.devtools.units

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class UnitDocumentTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun preservesUnknownFieldsTypesAndOriginalBackup() {
        val directory = temporary.newFolder().toPath()
        val source = directory.resolve("Units.json")
        val original = """[
        // game-style comment
        {"name":"Archer", "unitType":"Archery", "health":"15", "future":{"flag":true}}
        {"name":"Peasant", "unitType":"Sword", "health":5}
    ]"""
        Files.writeString(source, original)
        val document = UnitDocument(source)
        document.setText(document.units[0], "health", "19")
        val backups = temporary.newFolder().toPath()
        document.save(backups)
        val reloaded = UnitDocument(source)
        assertTrue(reloaded.units[0].path("health").isTextual)
        assertEquals("19", reloaded.units[0].path("health").asText())
        assertTrue(reloaded.units[1].path("health").isIntegralNumber)
        assertTrue(reloaded.units[0].path("future").path("flag").asBoolean())
        val snapshots = Files.list(backups)
        val snapshot = try {
            snapshots.findFirst().get()
        } finally {
            snapshots.close()
        }
        assertEquals(original, Files.readString(snapshot.resolve("Units.json")))
        assertFalse(document.isDirty())
    }

    @Test
    fun refusesExternalChangesWithoutOverwriting() {
        val file = temporary.newFile("Units.json").toPath()
        Files.writeString(file, """[{"name":"Archer","unitType":"Archery"}]""")
        val document = UnitDocument(file)
        document.setText(document.units[0], "health", "20")
        Files.writeString(file, "[]")
        assertThrows(IllegalStateException::class.java) {
            document.save(
                temporary.newFolder().toPath()
            )
        }
        assertEquals("[]", Files.readString(file))
        assertTrue(document.isDirty())
    }

    @Test
    fun readsNestedTechnologiesAndRejectsBrokenReferences() {
        val directory = temporary.newFolder().toPath()
        Files.writeString(directory.resolve("Techs.json"), """[{"techs":[{"name":"Archery"}]}]""")
        val file = directory.resolve("Units.json")
        Files.writeString(
            file,
            """[{"name":"Archer","unitType":"Archery","requiredTech":"Missing"}]"""
        )
        val document = UnitDocument(file)
        assertEquals(listOf("Archery"), document.choices("requiredTech"))
        assertTrue(document.validate().any { "Missing" in it })
        assertThrows(IllegalArgumentException::class.java) { document.addUnit("archer") }
        assertThrows(IllegalArgumentException::class.java) { document.addUnit("../Escape") }
        document.units[0].put("requiredTech", "Archery")
        assertTrue(document.validate().isEmpty())
        val copy = document.addUnit("New Archer", document.units[0])
        copy.put("requiredTech", "Other")
        assertEquals("Archery", document.units[0].path("requiredTech").asText())
    }

    @Test
    fun missingReferencesDoNotBlockSavingButInvalidNumbersDo() {
        val directory = temporary.newFolder().toPath()
        Files.writeString(directory.resolve("Techs.json"), """[{"techs":[{"name":"The Wheel"}]}]""")
        val file = directory.resolve("Units.json")
        Files.writeString(
            file,
            """[{"name":"Bowman","unitType":"Archery","requiredTech":"Archery","health":"15"}]"""
        )
        val document = UnitDocument(file)
        assertTrue(document.validationErrors().isEmpty())
        assertTrue(document.validationWarnings().any { "requiredTech" in it && "Archery" in it })
        document.setText(document.units[0], "health", "20")
        document.save(temporary.newFolder().toPath())
        val saved = Files.readString(file)
        val reloaded = UnitDocument(file)
        assertEquals("Archery", reloaded.units[0].path("requiredTech").asText())
        assertEquals("20", reloaded.units[0].path("health").asText())
        assertFalse(document.isDirty())
        document.units[0].put("health", "not a number")
        assertThrows(IllegalArgumentException::class.java) {
            document.save(
                temporary.newFolder().toPath()
            )
        }
        assertEquals(saved, Files.readString(file))
    }
}
