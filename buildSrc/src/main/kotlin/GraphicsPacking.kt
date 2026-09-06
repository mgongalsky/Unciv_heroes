package com.unciv.build

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import java.io.File
import java.nio.file.Files
import java.util.Properties
import javax.imageio.ImageIO

/** Builds into isolated directories; never writes to installed assets. */
object GraphicsPacking {
    private fun data(atlas: File) = TextureAtlas.TextureAtlasData(
        FileHandle(atlas), FileHandle(atlas.parentFile), false
    )

    fun validate(atlas: File): Set<String> {
        require(atlas.isFile) { "Missing atlas: $atlas" }
        val parsed = data(atlas)
        require(parsed.pages.size > 0 && parsed.regions.size > 0) { "Empty atlas: $atlas" }
        val root = atlas.parentFile.canonicalFile
        for (page in parsed.pages) {
            val file = page.textureFile.file().canonicalFile
            require(file.parentFile == root && file.isFile) { "Missing or unsafe atlas page: $file" }
            val image = requireNotNull(ImageIO.read(file)) { "Unreadable PNG: $file" }
            try {
                require(image.width > 0 && image.height > 0) { "Invalid image size: $file" }
                for (region in parsed.regions) {
                    if (region.page !== page) continue
                    val width = if (region.rotate) region.height else region.width
                    val height = if (region.rotate) region.width else region.height
                    require(
                        region.left >= 0 && region.top >= 0 && width > 0 && height > 0 &&
                                region.left.toLong() + width <= image.width &&
                                region.top.toLong() + height <= image.height
                    ) {
                        "Region outside page: ${region.name} in $file"
                    }
                }
            } finally {
                image.flush()
            }
        }
        return parsed.regions.map { "${it.name}#${it.index}" }.toSet()
    }

    fun preview(projectRoot: File, requestedAtlas: String?): File {
        val baseRoot = File(projectRoot, "graphics/base")
        val available =
            baseRoot.listFiles().orEmpty().filter { it.isDirectory }.map { it.name }.sorted()
        require(available.isNotEmpty()) { "No base source directories in $baseRoot" }
        val selected = if (requestedAtlas == null) available else listOf(
            available.firstOrNull { it.equals(requestedAtlas, ignoreCase = true) }
                ?: error("Unknown atlas '$requestedAtlas'. Available: ${available.joinToString()}")
        )
        val previews = File(projectRoot, "build/graphics/previews")
        require(previews.isDirectory || previews.mkdirs()) { "Cannot create $previews" }
        val output = Files.createTempDirectory(previews.toPath(), "preview-").toFile()
        val manifest = Properties().apply {
            setProperty("format", "1")
            setProperty("atlases", selected.joinToString(","))
        }
        for (name in selected) {
            val prepared = GraphicsSources.prepare(projectRoot, name)
            val settings = TexturePacker.Settings().apply {
                maxWidth = 2048
                maxHeight = 2048
                combineSubdirectories = true
                pot = true
                fast = true
                rotation = false
                paddingX = 8
                paddingY = 8
                duplicatePadding = true
                filterMin = Texture.TextureFilter.MipMapLinearLinear
                filterMag = Texture.TextureFilter.MipMapLinearLinear
            }
            val custom = File(prepared.directory, "TexturePacker.settings")
            val effectiveSettings = if (custom.isFile) custom.reader().use {
                com.badlogic.gdx.utils.Json().fromJson(TexturePacker.Settings::class.java, it)
            } else settings
            TexturePacker.process(effectiveSettings, prepared.directory.path, output.path, name)
            val generated = File(output, "$name.atlas")
            val newRegions = validate(generated)
            val installed = File(projectRoot, "android/assets/$name.atlas")
            if (installed.exists()) {
                val missing = validate(installed) - newRegions
                require(missing.isEmpty()) {
                    "$name preview omitted ${missing.size} installed regions: ${
                        missing.take(10).joinToString()
                    }. Installed assets are unchanged."
                }
            }
            manifest.setProperty("sources.$name", prepared.fingerprint)
            println("$name preview validated: ${newRegions.size} region keys")
        }
        for (file in GraphicsBackups.installedFiles(output)) {
            manifest.setProperty("sha256.${file.name}", GraphicsBackups.checksum(file))
        }
        File(output, "preview.properties").outputStream().use {
            manifest.store(it, "Validated preview; installed assets unchanged")
        }
        println("Graphics preview: ${output.absolutePath}")
        return output
    }
}
