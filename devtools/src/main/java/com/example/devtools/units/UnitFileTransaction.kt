package com.example.devtools.units

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Coordinated file replacement with recovery for ordinary I/O failures.
 * Individual replacements are atomic; the complete set is not crash-atomic.
 */
object UnitFileTransaction {
    data class Change(val target: Path, val expected: ByteArray?, val bytes: ByteArray?)

    private fun read(path: Path): ByteArray? =
        if (Files.exists(path)) Files.readAllBytes(path) else null

    private fun equal(first: ByteArray?, second: ByteArray?): Boolean =
        if (first == null) second == null else second != null && first.contentEquals(second)

    private fun verify(path: Path, expected: ByteArray?) {
        check(equal(read(path), expected)) {
            "Файл изменён другой программой: $path. Повторная запись отменена."
        }
    }

    private fun write(path: Path, bytes: ByteArray?) {
        if (bytes == null) Files.deleteIfExists(path)
        else UnitDocument.atomicWrite(path, bytes)
        check(equal(read(path), bytes)) { "Не удалось проверить запись: $path" }
    }

    /** Caller marks editor state saved only after this returns successfully. */
    fun commit(
        changes: List<Change>,
        backupDirectory: Path,
        beforeWrite: (Int, Path) -> Unit = { _, _ -> }
    ): Path {
        require(changes.isNotEmpty()) { "Нет файлов для записи" }
        // Freeze both content and canonical destinations before making any changes.
        val frozen = changes.map {
            Change(
                it.target.toFile().canonicalFile.toPath(),
                it.expected?.clone(),
                it.bytes?.clone()
            )
        }
        require(frozen.map { it.target }.distinct().size == frozen.size) {
            "Один файл указан для записи несколько раз"
        }
        frozen.forEach { verify(it.target, it.expected) }
        Files.createDirectories(backupDirectory)
        val snapshot = Files.createDirectory(backupDirectory.resolve("save-${UUID.randomUUID()}"))
        val manifest = UnitDocument.mapper.createObjectNode()
        manifest.put("format", 1)
        manifest.put("status", "prepared")
        val entries = manifest.putArray("files")
        frozen.forEachIndexed { index, change ->
            val entry = entries.addObject()
            entry.put("target", change.target.toString())
            entry.put("existed", change.expected != null)
            if (change.expected != null) {
                val name = "$index.before"
                val backup = snapshot.resolve(name)
                Files.write(backup, change.expected)
                check(Files.readAllBytes(backup).contentEquals(change.expected)) {
                    "Ошибка проверки резервной копии: $backup"
                }
                entry.put("backup", name)
            }
            if (change.bytes != null) {
                val name = "$index.after"
                val prepared = snapshot.resolve(name)
                Files.write(prepared, change.bytes)
                check(Files.readAllBytes(prepared).contentEquals(change.bytes)) {
                    "Ошибка проверки подготовленного файла: $prepared"
                }
                entry.put("prepared", name)
            }
        }
        fun recordStatus(status: String) {
            manifest.put("status", status)
            UnitDocument.atomicWrite(
                snapshot.resolve("manifest.json"),
                UnitDocument.mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest)
            )
        }
        recordStatus("prepared")
        // Recheck after backup: the original set must still match the captured state.
        frozen.forEach { verify(it.target, it.expected) }
        val attempted = mutableListOf<Change>()
        try {
            frozen.forEachIndexed { index, change ->
                beforeWrite(index, change.target)
                verify(change.target, change.expected)
                attempted.add(change)
                write(change.target, change.bytes)
            }
            recordStatus("saved")
        } catch (failure: Exception) {
            var restored = true
            for (change in attempted.asReversed()) {
                try {
                    val current = read(change.target)
                    if (equal(current, change.expected)) continue
                    check(equal(current, change.bytes)) {
                        "Во время отката файл изменился извне: ${change.target}. Копия сохранена в $snapshot"
                    }
                    write(change.target, change.expected)
                } catch (rollbackFailure: Exception) {
                    restored = false
                    failure.addSuppressed(rollbackFailure)
                }
            }
            try {
                recordStatus(if (restored) "rolled-back" else "recovery-required")
            } catch (statusFailure: Exception) {
                failure.addSuppressed(statusFailure)
            }
            val result = if (restored) "Записанные файлы восстановлены." else
                "Не все файлы удалось восстановить: ${failure.suppressed.joinToString { it.message.orEmpty() }}"
            throw IllegalStateException(
                "Сохранение не завершено. $result Резервная копия: $snapshot. Причина: ${failure.message}",
                failure
            )
        }
        return snapshot
    }
}
