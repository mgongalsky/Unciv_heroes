package com.unciv.build

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/** Resolves project-owned source images without modifying the base or override trees. */
object GraphicsSources {
    data class Prepared(val directory: File, val fingerprint: String, val overrides: Int)

    private fun validateAtlasName(name: String) {
        require(name.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid atlas name: $name" }
    }

    fun hasBase(projectRoot: File, atlasName: String): Boolean {
        validateAtlasName(atlasName)
        return File(projectRoot, "graphics/base/$atlasName").isDirectory
    }

    private fun collect(root: File): Map<String, File> {
        if (!root.exists()) return emptyMap()
        require(root.isDirectory) { "Source path is not a directory: $root" }
        val canonicalRoot = root.canonicalFile.toPath()
        val result = sortedMapOf<String, File>()
        root.walkTopDown().onEnter { directory ->
            require(!Files.isSymbolicLink(directory.toPath())) { "Symbolic source directory: $directory" }
            require(directory.canonicalFile.toPath().startsWith(canonicalRoot)) {
                "Source directory escapes its root: $directory"
            }
            true
        }.onFail { file, error ->
            throw IllegalStateException("Cannot read source directory: $file", error)
        }.filter { it.isFile }.forEach { file ->
            require(
                !Files.isSymbolicLink(file.toPath()) &&
                        file.canonicalFile.toPath().startsWith(canonicalRoot)
            ) { "Unsafe source file: $file" }
            val relative = file.relativeTo(root).invariantSeparatorsPath
            if (file.extension.lowercase() in listOf("png", "jpg", "jpeg") ||
                file.name == "pack.json" || file.name == "TexturePacker.settings"
            ) {
                result[relative] = file
            }
        }
        return result
    }

    fun prepare(projectRoot: File, atlasName: String): Prepared {
        validateAtlasName(atlasName)
        val baseRoot = File(projectRoot, "graphics/base/$atlasName")
        require(baseRoot.isDirectory) { "Missing base sources: $baseRoot" }
        val base = collect(baseRoot)
        val overrides = collect(File(projectRoot, "graphics/overrides/$atlasName"))
        val combined = (base + overrides).toSortedMap()
        require(combined.keys.any {
            it.substringAfterLast('.').lowercase() in listOf(
                "png",
                "jpg",
                "jpeg"
            )
        }) {
            "No images found for $atlasName"
        }
        val folded = combined.keys.groupBy { it.lowercase(java.util.Locale.ROOT) }
        require(folded.values.none { it.size > 1 }) {
            "Source paths differ only in letter case: ${folded.values.filter { it.size > 1 }}"
        }
        val imageKeys = combined.keys.filter {
            it.substringAfterLast('.').lowercase() in listOf("png", "jpg", "jpeg")
        }.groupBy { it.substringBeforeLast('.').removeSuffix(".9") }
        require(imageKeys.values.none { it.size > 1 }) {
            "Multiple source images produce the same region: ${imageKeys.values.filter { it.size > 1 }}"
        }

        val workRoot = File(projectRoot, "build/graphics/sources")
        require(workRoot.isDirectory || workRoot.mkdirs()) { "Cannot create $workRoot" }
        val staging = Files.createTempDirectory(workRoot.toPath(), "$atlasName-").toFile()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("graphics-sources-v1\n".toByteArray(Charsets.UTF_8))
        for ((relative, source) in combined) {
            val target = File(staging, relative)
            require(target.canonicalFile.toPath().startsWith(staging.canonicalFile.toPath())) {
                "Invalid source destination: $relative"
            }
            require(target.parentFile.isDirectory || target.parentFile.mkdirs()) {
                "Cannot create source staging directory: ${target.parentFile}"
            }
            val before = GraphicsBackups.checksum(source)
            source.copyTo(target, overwrite = false)
            check(GraphicsBackups.checksum(target) == before) { "Source changed during copy: $source" }
            digest.update(relative.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
            digest.update(before.toByteArray(Charsets.UTF_8))
            digest.update(0.toByte())
        }
        val fingerprint = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        println("$atlasName sources: ${base.size} base files, ${overrides.size} override files")
        return Prepared(staging, fingerprint, overrides.size)
    }
}
