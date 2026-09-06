package com.unciv.build

import java.io.File
import java.nio.file.Files
import java.util.Properties
import com.badlogic.gdx.utils.Json

/** Publishes an explicitly selected, validated preview; source directories are never modified. */
object GraphicsPublish {
    fun publish(projectRoot: File, previewId: String) {
        require(previewId.matches(Regex("preview-[0-9]+"))) { "Use -Ppreview=preview-<id> printed by previewImages" }
        val previews = File(projectRoot, "build/graphics/previews").canonicalFile
        val preview = File(previews, previewId).canonicalFile
        require(preview.parentFile == previews) { "Invalid preview directory" }
        val manifest = Properties().apply {
            File(preview, "preview.properties").inputStream().use { load(it) }
        }
        require(manifest.getProperty("format") == "1") { "Unsupported preview format" }
        val names = manifest.getProperty("atlases", "").split(',')
        require(
            names.isNotEmpty() && names.distinct().size == names.size &&
                    names.all { it.matches(Regex("[A-Za-z0-9_-]+")) }) { "Invalid preview atlas names" }
        val expectedFiles = manifest.stringPropertyNames().filter { it.startsWith("sha256.") }
            .associate { it.removePrefix("sha256.") to manifest.getProperty(it) }
        val files = GraphicsBackups.installedFiles(preview)
        require(files.map { it.name }
            .toSet() == expectedFiles.keys) { "Preview manifest does not match files" }
        require(files.filter { it.extension == "atlas" }.map { it.nameWithoutExtension }
            .toSet() == names.toSet()) {
            "Unexpected atlas in preview"
        }
        require(files.none { it.name == "Atlases.json" }) { "Preview must not replace the complete atlas registry" }
        files.forEach {
            require(GraphicsBackups.checksum(it) == expectedFiles.getValue(it.name)) {
                "Preview checksum mismatch: ${it.name}"
            }
        }
        for (name in names) {
            val prepared = GraphicsSources.prepare(projectRoot, name)
            require(prepared.fingerprint == manifest.getProperty("sources.$name")) {
                "Sources changed for $name. Run previewImages again before publishing."
            }
        }
        val assets = File(projectRoot, "android/assets").canonicalFile
        val installed = GraphicsBackups.installedFiles(assets)
        val selectedOld = linkedSetOf<String>()
        val ownedByOther = linkedSetOf<String>()
        for (atlas in installed.filter { it.extension == "atlas" }) {
            val parsed = com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData(
                com.badlogic.gdx.files.FileHandle(atlas),
                com.badlogic.gdx.files.FileHandle(assets),
                false
            )
            val owned = parsed.pages.map { it.textureFile.name() } + atlas.name
            if (atlas.nameWithoutExtension in names) selectedOld.addAll(owned)
            else ownedByOther.addAll(owned)
        }
        require(files.none { it.name in ownedByOther }) { "Preview would overwrite another atlas's page" }
        for (name in names) {
            val generatedKeys = GraphicsPacking.validate(File(preview, "$name.atlas"))
            val current = File(assets, "$name.atlas")
            if (current.exists()) require((GraphicsPacking.validate(current) - generatedKeys).isEmpty()) {
                "Preview would lose installed regions from $name"
            }
        }
        val control = File(assets, "Atlases.json")
        val registry =
            if (control.exists()) Json().fromJson(Array<String>::class.java, control.readText())
                .toMutableList()
            else mutableListOf()
        names.filter { it != "game" && it !in registry }.forEach { registry.add(it) }
        val workRoot = File(projectRoot, "build/graphics/publications")
        require(workRoot.isDirectory || workRoot.mkdirs()) { "Cannot create publication staging" }
        val staged = Files.createTempDirectory(workRoot.toPath(), "publish-").toFile()
        for (file in files) {
            val copy = file.copyTo(File(staged, file.name), overwrite = false)
            require(GraphicsBackups.checksum(copy) == expectedFiles.getValue(file.name)) { "Preview changed during staging" }
        }
        File(staged, "Atlases.json").writeText(Json().toJson(registry.toTypedArray()))
        // A fresh recovery snapshot never replaces the user's earlier approved backups.
        val recovery = GraphicsBackups.backup(projectRoot)
        val targets = files.map { it.name }.toSet() + "Atlases.json"
        val obsolete = selectedOld - targets - ownedByOther
        val touched = targets + obsolete
        for (name in touched) require(File(assets, name).canonicalFile.parentFile == assets) {
            "Unsafe publication target: $name"
        }
        try {
            for (name in targets.sortedBy { if (it.endsWith(".png", true)) 0 else 1 }) {
                val source = File(staged, name)
                val target = source.copyTo(File(assets, name), overwrite = true)
                check(GraphicsBackups.checksum(source) == GraphicsBackups.checksum(target)) { "Write verification failed: $name" }
            }
            for (name in obsolete) check(
                File(
                    assets,
                    name
                ).delete()
            ) { "Cannot remove obsolete page: $name" }
            names.forEach { GraphicsPacking.validate(File(assets, "$it.atlas")) }
        } catch (failure: Exception) {
            try {
                for (name in touched) {
                    val saved = File(recovery, "assets/$name")
                    val target = File(assets, name)
                    if (saved.exists()) {
                        saved.copyTo(target, overwrite = true)
                        check(GraphicsBackups.checksum(saved) == GraphicsBackups.checksum(target)) { "Rollback verification failed: $name" }
                    } else check(!target.exists() || target.delete()) { "Cannot remove new file during rollback: $name" }
                }
            } catch (rollbackFailure: Exception) {
                failure.addSuppressed(rollbackFailure)
            }
            throw IllegalStateException(
                "Publication failed. Recovery snapshot: ${recovery.absolutePath}",
                failure
            )
        }
        println("Published preview $previewId: ${names.joinToString()}")
        println("Removed obsolete atlas files: ${obsolete.joinToString().ifEmpty { "none" }}")
        println("Previous files are recoverable from snapshot: ${recovery.name}")
        println("Restart the game to inspect the result. Use backupImages after accepting the graphics.")
    }
}
