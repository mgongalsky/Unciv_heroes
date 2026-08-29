package com.unciv.build

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * Entry point: _ImagePacker.[packImages] ()_
 *
 * Re-packs our texture assets into atlas + png File pairs, which will be loaded by the game.
 * With the exception of the ExtraImages folder and the Font system these are the only
 * graphics used (The source Image folders are unused at run time except here).
 *
 * [TexturePacker] documentation is [here](https://github.com/libgdx/libgdx/wiki/Texture-packer)
 */
object AndroidImagePacker {
    private fun getDefaultSettings() = TexturePacker.Settings().apply {
        // Apparently some chipsets, like NVIDIA Tegra 3 graphics chipset (used in Asus TF700T tablet),
        // don't support non-power-of-two texture sizes - kudos @yuroller!
        // https://github.com/yairm210/Unciv/issues/1340

        /**
         * These should be as big as possible in order to accommodate ALL the images together in one big file.
         * Why? Because the rendering function of the main screen renders all the images consecutively, and every time it needs to switch between textures,
         * this causes a delay, leading to horrible lag if there are enough switches.
         * The cost of this specific solution is that the entire game.png needs be be kept in-memory constantly.
         * Now here we come to what Fred Colon would call an Imp Arse.
         * On the one hand, certain tilesets (ahem 5hex ahem) are really big.
         * You wouldn't believe how hugely mindbogglingly big they are. So theoretically we should want all of their images to be together.
         * HOWEVER certain chipsets (see https://github.com/yairm210/Unciv/issues/3330) only seem to support to up to 2048 width*height so this is maximum we can have.
         * Practically this means that big custom tilesets will have to reload the texture a lot when covering the map and so the
         *    panning on the map will tend to lag a lot :(
         *
         *    TL;DR this should be 2048.
         */
        maxWidth = 2048
        maxHeight = 2048

        // Trying to disable the subdirectory combine lead to even worse results. Don't.
        combineSubdirectories = true
        pot = true  // powers of two only for width/height
        fast = true  // with pot on this just sorts by width
        // settings.rotation - do not set. Allows rotation, potentially packing tighter.
        //      Proper rendering is mostly automatic - except borders which overwrite rotation.

        // Set some additional padding and enable duplicatePadding to prevent image edges from bleeding into each other due to mipmapping
        paddingX = 8
        paddingY = 8
        duplicatePadding = true
        filterMin = Texture.TextureFilter.MipMapLinearLinear
        filterMag = Texture.TextureFilter.MipMapLinearLinear // I'm pretty sure this doesn't make sense for magnification, but setting it to Linear gives strange results
    }

    fun packImages(workingPath: String, force: Boolean = false, atlasName: String? = null) {
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
            atlasName = selectedAtlas
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
                        atlasName = selectedAtlas
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
        atlasName: String?
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
                force = force
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
        force: Boolean
    ): Boolean {
        fun File.listTree(): Sequence<File> = when {
            isFile -> sequenceOf(this)
            isDirectory -> listFiles().orEmpty().asSequence().flatMap { it.listTree() }
            else -> sequenceOf()
        }

        val atlasFile = File("$output${File.separator}$packFileName.atlas")
        if (!force && atlasFile.exists() && File("$output${File.separator}$packFileName.png").exists()) {
            val atlasModTime = atlasFile.lastModified()
            val hasChangedSource = File(input).listTree().any {
                val attr: BasicFileAttributes =
                        Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                val createdAt = attr.creationTime().toMillis()
                it.extension.lowercase() in listOf("png", "jpg", "jpeg") &&
                        (it.lastModified() > atlasModTime || createdAt > atlasModTime)
            }
            if (!hasChangedSource) return false
        }

        val settingsFile = File("$input${File.separator}TexturePacker.settings")
        val settings = if (settingsFile.exists())
            Json().fromJson(TexturePacker.Settings::class.java, settingsFile.reader())
        else defaultSettings

        TexturePacker.process(settings, input, output, packFileName)
        return true
    }

    // Iterator providing all Image folders to process with the destination atlas name
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

