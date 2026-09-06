package com.example.devtools.units

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

/** Integration check against project-owned sources. Only staging under build/ is written. */
class UnitProjectAtlasTest {
    @Test
    fun projectSourcesRebuildBothAtlasesWithoutChangingInstalledFiles() {
        val root = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .firstOrNull { Files.isRegularFile(it.resolve("android/assets/Construction.atlas")) }
            ?: error("Запустите проверку из корня проекта или модуля devtools")
        val edits = listOf(
            UnitImages.Kind.ICON to "Archer",
            UnitImages.Kind.SPRITE to "Peasant"
        ).map { (kind, name) ->
            val source = UnitImages.source(root, name, kind)
                ?: error("Нет исходного изображения $kind / $name")
            val target = UnitImages.target(root, name, kind)
            val expected = if (Files.exists(target)) Files.readAllBytes(target) else null
            val image = UnitImages.read(source)
            val output = if (kind == UnitImages.Kind.ICON) UnitImages.tintableIcon(image) else image
            UnitAtlasPreparation.ImageEdit(kind, target, expected, UnitImages.png(output))
        }
        val prepared = UnitAtlasPreparation.prepare(root, edits) { println(it) }
        assertEquals(setOf("Construction", "AbsoluteUnits"), prepared.atlasNames)
        assertTrue(prepared.changes.any { it.target.fileName.toString() == "Construction.atlas" })
        assertTrue(prepared.changes.any { it.target.fileName.toString() == "AbsoluteUnits.atlas" })
        prepared.verifyUnchanged()
        println("Оба атласа подготовлены; исходники, установленные атласы и реестр не изменены.")
    }
}
