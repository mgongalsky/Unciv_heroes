package com.example.devtools.units

import java.nio.file.Path

/** Coordinates atlas preparation and publication without accessing Swing components. */
object UnitEditorSave {
    data class Result(val backup: Path, val atlases: Set<String>)

    fun save(
        projectRoot: Path,
        document: UnitFileTransaction.Change,
        images: List<UnitAtlasPreparation.ImageEdit>,
        backupDirectory: Path,
        progress: (String) -> Unit = {},
        beforeWrite: (Int, Path) -> Unit = { _, _ -> }
    ): Result {
        val prepared = UnitAtlasPreparation.prepare(projectRoot, images, progress)
        progress("Проверка исходников…")
        prepared.verifyUnchanged()
        progress("Резервная копия и запись файлов…")
        val snapshot = UnitFileTransaction.commit(
            prepared.changes + listOf(document), backupDirectory
        ) { index, path ->
            // Backup creation may take time. Check again before the first modification.
            if (index == 0) prepared.verifyUnchanged()
            beforeWrite(index, path)
        }
        return Result(snapshot, prepared.atlasNames)
    }
}
