package com.example.devtools.units

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class UnitFileTransactionTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun savesCompleteSetAndRetainsExactOriginals() {
        val root = temporary.newFolder().toPath()
        val json = root.resolve("Units.json")
        val page = root.resolve("Construction.png")
        val original = "// original JSON\n[]".toByteArray()
        Files.write(json, original)
        val replacement = "[{\"name\":\"Peasant\"}]".toByteArray()
        val snapshot = UnitFileTransaction.commit(
            listOf(
                UnitFileTransaction.Change(page, null, byteArrayOf(1, 2, 3)),
                UnitFileTransaction.Change(json, original, replacement)
            ), root.resolve("backups")
        )
        assertArrayEquals(replacement, Files.readAllBytes(json))
        assertArrayEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(page))
        assertArrayEquals(original, Files.readAllBytes(snapshot.resolve("1.before")))
        val manifest =
            UnitDocument.mapper.readTree(Files.readAllBytes(snapshot.resolve("manifest.json")))
        assertEquals("saved", manifest.path("status").asText())
        assertFalse(manifest.path("files")[0].path("existed").asBoolean())
    }

    @Test
    fun failureRestoresReplacedAndDeletedFilesAndRemovesNewFiles() {
        val root = temporary.newFolder().toPath()
        val json = root.resolve("Units.json")
        val obsolete = root.resolve("old-page.png")
        val added = root.resolve("new-page.png")
        val last = root.resolve("Construction.atlas")
        Files.write(json, byteArrayOf(10))
        Files.write(obsolete, byteArrayOf(20))
        assertThrows(IllegalStateException::class.java) {
            UnitFileTransaction.commit(
                listOf(
                    UnitFileTransaction.Change(json, byteArrayOf(10), byteArrayOf(11)),
                    UnitFileTransaction.Change(obsolete, byteArrayOf(20), null),
                    UnitFileTransaction.Change(added, null, byteArrayOf(30)),
                    UnitFileTransaction.Change(last, null, byteArrayOf(40))
                ), root.resolve("backups")
            ) { index, _ -> if (index == 3) error("Injected publication failure") }
        }
        assertArrayEquals(byteArrayOf(10), Files.readAllBytes(json))
        assertArrayEquals(byteArrayOf(20), Files.readAllBytes(obsolete))
        assertFalse(Files.exists(added))
        assertFalse(Files.exists(last))
    }

    @Test
    fun rejectsExternalChangesBeforeWritingAnything() {
        val root = temporary.newFolder().toPath()
        val first = root.resolve("first")
        val external = root.resolve("external")
        Files.write(external, byteArrayOf(99))
        assertThrows(IllegalStateException::class.java) {
            UnitFileTransaction.commit(
                listOf(
                    UnitFileTransaction.Change(first, null, byteArrayOf(1)),
                    UnitFileTransaction.Change(external, byteArrayOf(2), byteArrayOf(3))
                ), root.resolve("backups")
            )
        }
        assertFalse(Files.exists(first))
        assertArrayEquals(byteArrayOf(99), Files.readAllBytes(external))
    }

    @Test
    fun rollsBackEarlierWritesWhenLaterTargetChangesDuringPublication() {
        val root = temporary.newFolder().toPath()
        val first = root.resolve("first")
        val external = root.resolve("external")
        Files.write(first, byteArrayOf(1))
        Files.write(external, byteArrayOf(2))
        assertThrows(IllegalStateException::class.java) {
            UnitFileTransaction.commit(
                listOf(
                    UnitFileTransaction.Change(first, byteArrayOf(1), byteArrayOf(10)),
                    UnitFileTransaction.Change(external, byteArrayOf(2), byteArrayOf(20))
                ), root.resolve("backups")
            ) { index, _ -> if (index == 1) Files.write(external, byteArrayOf(99)) }
        }
        assertArrayEquals(byteArrayOf(1), Files.readAllBytes(first))
        assertArrayEquals(byteArrayOf(99), Files.readAllBytes(external))
    }
}
