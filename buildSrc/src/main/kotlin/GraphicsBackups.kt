package com.unciv.build

import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.UUID

/** Immutable, explicitly created snapshots of the installed texture atlases. */
object GraphicsBackups {
    fun checksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun assetFile(assets: File, name: String): File {
        require(
            name.isNotBlank() && name != "." && name != ".." &&
                    '/' !in name && '\\' !in name && ':' !in name
        ) {
            "Unsupported atlas asset name: $name"
        }
        val file = File(assets, name)
        require(file.canonicalFile.parentFile == assets.canonicalFile) {
            "Atlas asset escapes its directory: $name"
        }
        return file
    }

    fun installedFiles(assets: File): List<File> {
        require(assets.isDirectory) { "Assets directory not found: $assets" }
        val atlases = assets.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "atlas" }
            .sortedBy { it.name }
        require(atlases.isNotEmpty()) { "No installed atlases found in $assets" }
        val files = linkedMapOf<String, File>()
        for (atlas in atlases) {
            files[atlas.name] = assetFile(assets, atlas.name)
            // The project's atlas format lists page names without indentation.
            val pages = atlas.readLines().filter {
                it.isNotBlank() && !it.first().isWhitespace() &&
                        it.trimEnd().endsWith(".png", ignoreCase = true)
            }.map { it.trimEnd() }
            require(pages.isNotEmpty()) { "No PNG pages found in ${atlas.name}" }
            for (page in pages) files[page] = assetFile(assets, page)
        }
        val control = assetFile(assets, "Atlases.json")
        if (control.exists()) files[control.name] = control
        files.values.forEach { require(it.isFile) { "Missing atlas asset: $it" } }
        return files.values.toList()
    }

    fun backup(projectRoot: File): File {
        val assets = File(projectRoot, "android/assets")
        val files = installedFiles(assets)
        // Hash before copying; refuse to mark a changing set as a valid snapshot.
        val expected = files.associate { it.name to checksum(it) }
        val backupRoot = File(projectRoot, "graphics/backups")
        require(backupRoot.isDirectory || backupRoot.mkdirs()) {
            "Cannot create backup directory: $backupRoot"
        }
        val id = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                "-" + UUID.randomUUID().toString().take(8)
        val snapshot = File(backupRoot, id)
        check(snapshot.mkdir()) { "Cannot create snapshot: $snapshot" }
        val payload = File(snapshot, "assets")
        check(payload.mkdir()) { "Cannot create snapshot assets directory" }
        for (source in files) {
            val target = File(payload, source.name)
            source.copyTo(target, overwrite = false)
            check(checksum(target) == expected.getValue(source.name)) {
                "Asset changed while creating backup: ${source.name}. Snapshot is incomplete."
            }
        }
        check(
            installedFiles(assets).map { it.name }.toSet() == expected.keys &&
                    files.all { checksum(it) == expected.getValue(it.name) }) {
            "Installed atlases changed during backup. Snapshot is incomplete."
        }
        val manifest = Properties().apply {
            setProperty("format", "1")
            setProperty("fileCount", files.size.toString())
            setProperty("atlasesJsonPresent", File(assets, "Atlases.json").exists().toString())
            expected.forEach { (name, hash) -> setProperty("sha256.$name", hash) }
        }
        // A snapshot is usable only after its manifest has been written.
        File(snapshot, "manifest.properties").outputStream().use {
            manifest.store(it, "Graphics snapshot $id")
        }
        println("Graphics backup: ${snapshot.absolutePath} (${files.size} files)")
        return snapshot
    }
}
