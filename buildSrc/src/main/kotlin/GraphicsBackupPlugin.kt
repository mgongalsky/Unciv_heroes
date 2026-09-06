package com.unciv.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/** Explicit graphics operations; no snapshot or restore runs during application startup. */
class GraphicsBackupPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("backupImages") {
            group = "graphics"
            description =
                    "Save all installed atlases and their PNG pages to a new graphics/backups snapshot."
            doLast { GraphicsBackups.backup(project.rootDir) }
        }
        project.tasks.register("listImageBackups") {
            group = "graphics"
            description = "List graphics snapshot IDs available for restoreImages."
            doLast {
                val backups = File(project.rootDir, "graphics/backups").listFiles().orEmpty()
                    .filter { it.isDirectory && File(it, "manifest.properties").isFile }
                    .sortedBy { it.name }
                if (backups.isEmpty()) logger.lifecycle("No graphics backups yet. Run backupImages first.")
                else backups.forEach { logger.lifecycle(it.name) }
            }
        }
        project.tasks.register("restoreImages") {
            group = "graphics"
            description =
                    "Restore installed atlases from -Pbackup=<id>, preserving current graphics in another snapshot."
            doLast {
                val id = project.providers.gradleProperty("backup").orNull
                require(!id.isNullOrBlank()) { "Choose a snapshot with -Pbackup=<id>; use listImageBackups to list IDs." }
                GraphicsRestore.restore(project.rootDir, id)
            }
        }
        project.tasks.register("recoverRoadSources") {
            group = "graphics"
            description =
                    "Recover six missing legacy road PNGs from the installed Tilesets atlas without overwriting sources."
            doLast { GraphicsRoadRecovery.recover(project.rootDir) }
        }
        project.tasks.register("previewImages") {
            group = "graphics"
            description =
                    "Pack base + overrides into build/graphics/previews without changing installed assets. Optional -Patlas=Name."
            doLast {
                GraphicsPacking.preview(
                    project.rootDir,
                    project.providers.gradleProperty("atlas").orNull
                )
            }
        }
        project.tasks.register("publishImages") {
            group = "graphics"
            description =
                    "Publish a validated preview with -Ppreview=preview-<id>, saving current graphics first."
            doLast {
                val id = project.providers.gradleProperty("preview").orNull
                require(!id.isNullOrBlank()) { "Pass -Ppreview=preview-<id> printed by previewImages." }
                GraphicsPublish.publish(project.rootDir, id)
            }
        }
    }
}
