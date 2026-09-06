package com.unciv.app.desktop

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import com.unciv.utils.Log
import com.unciv.utils.debug
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * Packs mod images at desktop startup.
 *
 * Built-in atlases are maintained by the protected Gradle graphics tasks. Some of
 * their source PNGs are absent from this repository, so startup must not rebuild
 * them or rewrite the built-in Atlases.json from the incomplete source folders.
 */
internal object ImagePacker {
    private fun getDefaultSettings() = TexturePacker.Settings().apply {
        // Keep pages within the limit supported by older GPUs.
        maxWidth = 2048
        maxHeight = 2048
        combineSubdirectories = true
        pot = true
        fast = true
        // Do not enable rotation: some border rendering overwrites it.
        paddingX = 8
        paddingY = 8
        duplicatePadding = true
        filterMin = Texture.TextureFilter.MipMapLinearLinear
        filterMag = Texture.TextureFilter.MipMapLinearLinear
    }

    // Retain the parameter for existing launchers; both launch modes preserve
    // built-in atlases and only pack mods.
    @Suppress("UNUSED_PARAMETER")
    fun packImages(isRunFromJAR: Boolean) {
        val startTime = System.currentTimeMillis()
        val defaultSettings = getDefaultSettings()

        val modDirectory = File("mods")
        if (modDirectory.exists()) {
            for (mod in modDirectory.listFiles()!!) {
                if (!mod.isHidden) {
                    try {
                        packImagesPerMod(mod.path, mod.path, defaultSettings)
                    } catch (ex: Throwable) {
                        Log.error("Exception in ImagePacker: %s", ex.message)
                    }
                }
            }
        }

        val texturePackingTime = System.currentTimeMillis() - startTime
        debug("Packing mod textures - %sms", texturePackingTime)
    }

    private fun packImagesPerMod(
        input: String,
        output: String,
        defaultSettings: TexturePacker.Settings
    ) {
        if (!File("$input${File.separator}Images").exists()) return
        val atlasList = mutableListOf<String>()
        for ((file, packFileName) in imageFolders(input)) {
            atlasList += packFileName
            packImagesIfOutdated(defaultSettings, file, output, packFileName)
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
        packFileName: String
    ) {
        fun File.listTree(): Sequence<File> = when {
            this.isFile -> sequenceOf(this)
            this.isDirectory -> this.listFiles()!!.asSequence().flatMap { it.listTree() }
            else -> sequenceOf()
        }

        val atlasFile = File("$output${File.separator}$packFileName.atlas")
        if (atlasFile.exists() && File("$output${File.separator}$packFileName.png").exists()) {
            val atlasModTime = atlasFile.lastModified()
            if (File(input).listTree().none {
                    val attr: BasicFileAttributes =
                        Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                    val createdAt: Long = attr.creationTime().toMillis()
                    it.extension in listOf("png", "jpg", "jpeg") &&
                            (it.lastModified() > atlasModTime || createdAt > atlasModTime)
                }) return
        }

        val settingsFile = File("$input${File.separator}TexturePacker.settings")
        val settings = if (settingsFile.exists())
            Json().fromJson(TexturePacker.Settings::class.java, settingsFile.reader())
        else defaultSettings

        TexturePacker.process(settings, input, output, packFileName)
    }

    private data class ImageFolderResult(val folder: String, val atlasName: String)

    private fun imageFolders(path: String) = sequence {
        val parent = File(path)
        for (folder in parent.listFiles()!!) {
            if (!folder.isDirectory) continue
            if (folder.nameWithoutExtension != "Images") continue
            val atlasName = if (folder.name == "Images") "game" else folder.extension
            yield(ImageFolderResult(folder.path, atlasName))
        }
    }
}
