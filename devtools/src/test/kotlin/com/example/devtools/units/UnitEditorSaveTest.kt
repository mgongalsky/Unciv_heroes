package com.example.devtools.units

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path

class UnitEditorSaveTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private fun icon(): ByteArray {
        val image = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        for (x in 2..5) for (y in 1..6) image.setRGB(x, y, -1)
        return UnitImages.png(image)
    }

    private fun document(root: Path): UnitDocument {
        val file = root.resolve("Units.json")
        Files.writeString(
            file,
            """[{"name":"Peasant","unitType":"Sword","health":"15","future":true}]"""
        )
        return UnitDocument(file)
    }

    private fun prepareRoot(): Path {
        val root = temporary.newFolder().toPath()
        Files.createDirectories(root.resolve("android/assets"))
        Files.createDirectories(root.resolve("android/Images.Construction/UnitIcons"))
        return root
    }

    @Test
    fun jsonOnlySaveNeedsNoGraphicsDirectoriesAndPreservesTypes() {
        val root = temporary.newFolder().toPath()
        val doc = document(root)
        doc.setText(doc.units[0], "health", "20")
        val change = doc.prepareSave()
        val result = UnitEditorSave.save(root, change, emptyList(), root.resolve("backups"))
        assertTrue(result.atlases.isEmpty())
        assertFalse(Files.exists(root.resolve("build/graphics")))
        assertTrue(doc.isDirty())
        doc.acceptSaved(change)
        assertFalse(doc.isDirty())
        val reloaded = UnitDocument(doc.file)
        assertTrue(reloaded.units[0].path("health").isTextual)
        assertEquals("20", reloaded.units[0].path("health").asText())
        assertTrue(reloaded.units[0].path("future").asBoolean())
    }

    @Test
    fun savesNewIconIntoConstructionTogetherWithJson() {
        val root = prepareRoot()
        val doc = document(root)
        doc.setText(doc.units[0], "health", "20")
        val target = UnitImages.target(root, "Peasant", UnitImages.Kind.ICON)
        val bytes = icon()
        val result = UnitEditorSave.save(
            root, doc.prepareSave(),
            listOf(UnitAtlasPreparation.ImageEdit(UnitImages.Kind.ICON, target, null, bytes)),
            root.resolve("backups")
        )
        assertEquals(setOf("Construction"), result.atlases)
        assertArrayEquals(bytes, Files.readAllBytes(target))
        assertEquals("20", UnitDocument(doc.file).units[0].path("health").asText())
        val assets = root.resolve("android/assets")
        val data = TextureAtlas.TextureAtlasData(
            FileHandle(assets.resolve("Construction.atlas").toFile()),
            FileHandle(assets.toFile()),
            false
        )
        assertTrue(data.regions.any { it.name == "UnitIcons/Peasant" })
        assertTrue(data.pages.all { it.textureFile.exists() })
        val registry = UnitDocument.parse(Files.readString(assets.resolve("Atlases.json")))
        assertTrue(registry.any { it.asText() == "Construction" })
        assertFalse(Files.exists(assets.resolve("AbsoluteUnits.atlas")))
    }

    @Test
    fun failureDuringPublicationRestoresJsonSourcesAndAtlasFiles() {
        val root = prepareRoot()
        val doc = document(root)
        val original = Files.readAllBytes(doc.file)
        doc.setText(doc.units[0], "health", "20")
        val target = UnitImages.target(root, "Peasant", UnitImages.Kind.ICON)
        assertThrows(IllegalStateException::class.java) {
            UnitEditorSave.save(
                root, doc.prepareSave(),
                listOf(UnitAtlasPreparation.ImageEdit(UnitImages.Kind.ICON, target, null, icon())),
                root.resolve("backups"),
                beforeWrite = { _, path ->
                    if (path == doc.file.toFile().canonicalFile.toPath()) error(
                        "Injected failure"
                    )
                }
            )
        }
        assertArrayEquals(original, Files.readAllBytes(doc.file))
        assertFalse(Files.exists(target))
        val stream = Files.list(root.resolve("android/assets"))
        try {
            assertEquals(0L, stream.count())
        } finally {
            stream.close()
        }
        assertTrue(doc.isDirty())
    }

    @Test
    fun whiteIconConversionPreservesEveryAlphaValueAndOriginal() {
        val image = BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0)
        image.setRGB(1, 0, 0x7f000000)
        image.setRGB(2, 0, 0xff123456.toInt())
        val result = UnitImages.tintableIcon(image)
        assertEquals(0x00ffffff, result.getRGB(0, 0))
        assertEquals(0x7fffffff, result.getRGB(1, 0))
        assertEquals(-1, result.getRGB(2, 0))
        assertEquals(0xff123456.toInt(), image.getRGB(2, 0))
    }

    @Test
    fun repeatedIconSavePreservesSpriteAtlasAndRegistryEntries() {
        val root = prepareRoot()
        Files.createDirectories(root.resolve("graphics/base/AbsoluteUnits/TileSets/AbsoluteUnits/Units"))
        val doc = document(root)
        val iconTarget = UnitImages.target(root, "Peasant", UnitImages.Kind.ICON)
        val spriteTarget = UnitImages.target(root, "Peasant", UnitImages.Kind.SPRITE)
        val bytes = icon()
        val first = UnitEditorSave.save(
            root, doc.prepareSave(), listOf(
                UnitAtlasPreparation.ImageEdit(UnitImages.Kind.ICON, iconTarget, null, bytes),
                UnitAtlasPreparation.ImageEdit(UnitImages.Kind.SPRITE, spriteTarget, null, bytes)
            ), root.resolve("backups")
        )
        assertEquals(setOf("Construction", "AbsoluteUnits"), first.atlases)
        // Reload to capture the saved JSON as the next expected version.
        val reloaded = UnitDocument(doc.file)
        val assets = root.resolve("android/assets")
        val spriteAtlas = assets.resolve("AbsoluteUnits.atlas")
        val oldDescriptor = Files.readAllBytes(spriteAtlas)
        val parsed = TextureAtlas.TextureAtlasData(
            FileHandle(spriteAtlas.toFile()), FileHandle(assets.toFile()), false
        )
        assertTrue(parsed.regions.any { it.name == "TileSets/AbsoluteUnits/Units/Peasant" })
        val oldPages = parsed.pages.associate {
            it.textureFile.file().toPath() to it.textureFile.readBytes()
        }
        Files.writeString(
            assets.resolve("Atlases.json"),
            "[\"Other\",\"Construction\",\"AbsoluteUnits\"]"
        )
        val changedImage = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        changedImage.setRGB(4, 4, -1)
        val second = UnitEditorSave.save(
            root, reloaded.prepareSave(),
            listOf(
                UnitAtlasPreparation.ImageEdit(
                    UnitImages.Kind.ICON,
                    iconTarget,
                    bytes,
                    UnitImages.png(changedImage)
                )
            ),
            root.resolve("backups")
        )
        assertEquals(setOf("Construction"), second.atlases)
        assertArrayEquals(oldDescriptor, Files.readAllBytes(spriteAtlas))
        oldPages.forEach { (path, original) ->
            assertArrayEquals(
                original,
                Files.readAllBytes(path)
            )
        }
        assertArrayEquals(bytes, Files.readAllBytes(spriteTarget))
        val registry = UnitDocument.parse(Files.readString(assets.resolve("Atlases.json")))
            .map { it.asText() }
        assertEquals(listOf("Other", "Construction", "AbsoluteUnits"), registry)
    }

    @Test
    fun refusesRepackThatWouldLoseAnInstalledRegion() {
        val root = prepareRoot()
        val doc = document(root)
        val target = UnitImages.target(root, "Peasant", UnitImages.Kind.ICON)
        val bytes = icon()
        UnitEditorSave.save(
            root, doc.prepareSave(),
            listOf(UnitAtlasPreparation.ImageEdit(UnitImages.Kind.ICON, target, null, bytes)),
            root.resolve("backups")
        )
        val assets = root.resolve("android/assets")
        val oldAtlas = Files.readAllBytes(assets.resolve("Construction.atlas"))
        val oldJson = Files.readAllBytes(doc.file)
        Files.delete(target)
        val other = UnitImages.target(root, "Archer", UnitImages.Kind.ICON)
        assertThrows(IllegalArgumentException::class.java) {
            UnitEditorSave.save(
                root, UnitDocument(doc.file).prepareSave(),
                listOf(UnitAtlasPreparation.ImageEdit(UnitImages.Kind.ICON, other, null, bytes)),
                root.resolve("backups")
            )
        }
        assertArrayEquals(oldAtlas, Files.readAllBytes(assets.resolve("Construction.atlas")))
        assertArrayEquals(oldJson, Files.readAllBytes(doc.file))
        assertFalse(Files.exists(other))
    }
}
