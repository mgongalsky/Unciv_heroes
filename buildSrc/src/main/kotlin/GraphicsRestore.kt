package com.unciv.build

import java.io.File
import java.util.Properties

object GraphicsRestore {
    private fun validatedFiles(snapshot: File): List<File> {
        val manifestFile = File(snapshot, "manifest.properties")
        require(manifestFile.isFile) { "Incomplete or missing backup: $snapshot" }
        val manifest = Properties().apply {
            manifestFile.inputStream().use { load(it) }
        }
        require(manifest.getProperty("format") == "1") { "Unsupported backup format" }
        val payload = File(snapshot, "assets").canonicalFile
        val names = manifest.stringPropertyNames().filter { it.startsWith("sha256.") }
            .map { it.removePrefix("sha256.") }.sorted()
        require(
            names.isNotEmpty() && names.size == manifest.getProperty("fileCount")?.toIntOrNull()
        ) {
            "Invalid backup manifest: $snapshot"
        }
        val files = names.map { name ->
            require(
                name.isNotBlank() && name != "." && name != ".." &&
                        '/' !in name && '\\' !in name && ':' !in name
            ) { "Invalid backup filename: $name" }
            require(
                name.endsWith(".atlas") || name.endsWith(".png", ignoreCase = true) ||
                        name == "Atlases.json"
            ) { "Unexpected backup file: $name" }
            File(payload, name).also { file ->
                require(file.canonicalFile.parentFile == payload && file.isFile) {
                    "Missing or unsafe backup file: $name"
                }
                require(GraphicsBackups.checksum(file) == manifest.getProperty("sha256.$name")) {
                    "Backup checksum mismatch: $name"
                }
            }
        }
        require(GraphicsBackups.installedFiles(payload).map { it.name }.toSet() == names.toSet()) {
            "Backup file list does not match its atlas descriptors"
        }
        require(
            manifest.getProperty("atlasesJsonPresent") == names.contains("Atlases.json").toString()
        ) {
            "Invalid Atlases.json presence flag"
        }
        return files
    }

    private fun install(files: List<File>, assets: File, previousNames: Set<String>) {
        val destinationRoot = assets.canonicalFile
        val wanted = files.map { it.name }.toSet()
        // Only touch files belonging to the validated atlas sets, never unrelated assets.
        for (source in files.sortedBy { if (it.extension.equals("png", true)) 0 else 1 }) {
            val target = File(assets, source.name)
            require(target.canonicalFile.parentFile == destinationRoot) { "Unsafe asset destination: $target" }
            source.copyTo(target, overwrite = true)
            check(GraphicsBackups.checksum(source) == GraphicsBackups.checksum(target)) {
                "Restored file verification failed: ${source.name}"
            }
        }
        for (name in previousNames - wanted) {
            val target = File(assets, name)
            require(target.canonicalFile.parentFile == destinationRoot) { "Unsafe obsolete asset: $target" }
            check(!target.exists() || target.delete()) { "Cannot remove obsolete atlas file: $target" }
        }
    }

    fun restore(projectRoot: File, backupId: String) {
        require(backupId.matches(Regex("[A-Za-z0-9_-]+"))) {
            "Pass a snapshot directory name with -Pbackup=<id>"
        }
        val backupRoot = File(projectRoot, "graphics/backups").canonicalFile
        val snapshot = File(backupRoot, backupId).canonicalFile
        require(snapshot.parentFile == backupRoot) { "Backup must be inside graphics/backups" }
        val files = validatedFiles(snapshot)
        val assets = File(projectRoot, "android/assets")
        val previous = GraphicsBackups.installedFiles(assets).map { it.name }.toSet()
        val recovery = GraphicsBackups.backup(projectRoot)
        val recoveryFiles = validatedFiles(recovery)
        try {
            install(files, assets, previous)
        } catch (failure: Exception) {
            try {
                install(recoveryFiles, assets, previous + files.map { it.name })
            } catch (rollbackFailure: Exception) {
                failure.addSuppressed(rollbackFailure)
            }
            throw IllegalStateException(
                "Restore failed. Recovery snapshot: ${recovery.absolutePath}",
                failure
            )
        }
        println("Restored graphics backup: $backupId")
        println("Previous graphics preserved in: ${recovery.name}")
        println("Obsolete atlas files removed: ${(previous - files.map { it.name }.toSet()).size}")
        println("Source sprites were not changed. Restart the game to reload textures.")
    }
}
