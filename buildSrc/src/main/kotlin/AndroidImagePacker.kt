package com.unciv.build

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/** Re-packs source images into atlas + PNG file pairs loaded by the game. */
object AndroidImagePacker {
    private fun getDefaultSettings() = TexturePacker.Settings().apply {
        maxWidth = 2048
        maxHeight = 2048
        combineSubdirectories = true
        pot = true
        fast = true
        paddingX = 8
        paddingY = 8
        duplicatePadding = true
        filterMin = Texture.TextureFilter.MipMapLinearLinear
        filterMag = Texture.TextureFilter.MipMapLinearLinear
    }

    fun packImages(
        workingPath: String,
        force: Boolean = false,
        atlasName: String? = null,
        allowIncompleteSources: Boolean = false
    ) {
        val defaultSettings = getDefaultSettings()
        val availableAtlases = imageFolders(workingPath).map { it.atlasName }.toList()
        require(availableAtlases.isNotEmpty()) {
            "No image source folders were found in $workingPath"
        }

        val selectedAtlas = atlasName?.let { requestedName ->
            availableAtlases.firstOrNull { it.equals(requestedName, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown atlas '$requestedName'. Available atlases: ${
                        availableAtlases.sorted().joinToString()
                    }"
                )
        }

        val mode = if (force) "forced" else "incremental"
        println("Packing graphics ($mode mode)${selectedAtlas?.let { ", atlas: $it" } ?: ""}...")

        packImagesPerMod(
            input = workingPath,
            output = "$workingPath/assets/",
            defaultSettings = defaultSettings,
            force = force,
            atlasName = selectedAtlas,
            allowIncompleteSources = allowIncompleteSources
        )

        val modDirectory = File("mods")
        if (modDirectory.exists()) {
            for (mod in modDirectory.listFiles().orEmpty()) {
                if (mod.isHidden) continue
                try {
                    packImagesPerMod(
                        input = mod.path,
                        output = mod.path,
                        defaultSettings = defaultSettings,
                        force = force,
                        atlasName = selectedAtlas,
                        allowIncompleteSources = allowIncompleteSources
                    )
                } catch (ex: Throwable) {
                    println("  ${mod.name}: failed - ${ex.message}")
                }
            }
        }
        println("Graphics packing complete.")
    }

    private fun packImagesPerMod(
        input: String,
        output: String,
        defaultSettings: TexturePacker.Settings,
        force: Boolean,
        atlasName: String?,
        allowIncompleteSources: Boolean
    ) {
        if (!File("$input${File.separator}Images").exists()) return

        val atlasList = mutableListOf<String>()
        for ((file, packFileName) in imageFolders(input)) {
            atlasList += packFileName
            if (atlasName != null && packFileName != atlasName) continue

            val packed = packImagesIfOutdated(
                defaultSettings = defaultSettings,
                input = file,
                output = output,
                packFileName = packFileName,
                force = force,
                allowIncompleteSources = allowIncompleteSources
            )
            if (!packed) {
                println("  $packFileName: up to date")
                continue
            }

            val pagePattern = Regex("^${Regex.escape(packFileName)}(?:\\d+)?\\.png$")
            val pageCount =
                    File(output).listFiles().orEmpty().count { pagePattern.matches(it.name) }
            println("  $packFileName: packed ($pageCount page${if (pageCount == 1) "" else "s"})")
        }

        atlasList.remove("game")
        val listFile = File("$output${File.separator}Atlases.json")
        if (atlasList.isEmpty()) listFile.delete()
        else listFile.writeText(atlasList.sorted().joinToString(",", "[", "]"))
    }

    private fun packImagesIfOutdated(
        defaultSettings: TexturePacker.Settings,
        input: String,
        output: String,
        packFileName: String,
        force: Boolean,
        allowIncompleteSources: Boolean
    ): Boolean {
        fun File.listTree(): Sequence<File> = when {
            isFile -> sequenceOf(this)
            isDirectory -> listFiles().orEmpty().asSequence().flatMap { it.listTree() }
            else -> sequenceOf()
        }

        val sourceRoot = File(input).canonicalFile
        val sourceImages = sourceRoot.listTree()
            .filter { it.extension.lowercase() in listOf("png", "jpg", "jpeg") }
            .toList()
        val atlasFile = File("$output${File.separator}$packFileName.atlas")

        if (!force && atlasFile.exists() && File("$output${File.separator}$packFileName.png").exists()) {
            val atlasModTime = atlasFile.lastModified()
            val hasChangedSource = sourceImages.any {
                val attr = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                it.lastModified() > atlasModTime || attr.creationTime().toMillis() > atlasModTime
            }
            if (!hasChangedSource) return false
        }

        protectExistingAtlas(
            sourceRoot,
            sourceImages,
            atlasFile,
            packFileName,
            allowIncompleteSources
        )

        val settingsFile = File("$input${File.separator}TexturePacker.settings")
        val settings = if (settingsFile.exists())
            Json().fromJson(TexturePacker.Settings::class.java, settingsFile.reader())
        else defaultSettings

        TexturePacker.process(settings, input, output, packFileName)
        return true
    }

    private fun protectExistingAtlas(
        sourceRoot: File,
        sourceImages: List<File>,
        atlasFile: File,
        packFileName: String,
        allowIncompleteSources: Boolean
    ) {
        if (!atlasFile.exists() || allowIncompleteSources) return

        val sourceRegions = sourceImages.mapTo(HashSet()) {
            it.relativeTo(sourceRoot).invariantSeparatorsPath.substringBeforeLast('.')
        }
        val existingRegions = atlasFile.useLines { lines ->
            lines.map { it.trimEnd() }
                .filter {
                    it.isNotBlank() &&
                            !it.first().isWhitespace() &&
                            ':' !in it &&
                            !it.endsWith(".png", ignoreCase = true)
                }
                .toSet()
        }
        val missingSources = (existingRegions - sourceRegions).sorted()
        check(missingSources.isEmpty()) {
            val examples = missingSources.take(10).joinToString("\n    ")
            "Refusing to rebuild '$packFileName': the existing atlas contains " +
                    "${missingSources.size} region(s) missing from ${sourceRoot.path}.\n" +
                    "    $examples\n" +
                    "Restore the complete source images, or explicitly accept losing these regions " +
                    "with -PallowIncompleteSources=true. No atlas files were changed."
        }
    }

    private data class ImageFolderResult(val folder: String, val atlasName: String)

    private fun imageFolders(path: String) = sequence {
        val parent = File(path)
        for (folder in parent.listFiles().orEmpty()) {
            if (!folder.isDirectory) continue
            if (folder.nameWithoutExtension != "Images") continue
            val atlasName = if (folder.name == "Images") "game" else folder.extension
            yield(ImageFolderResult(folder.path, atlasName))
        }
    }
}
