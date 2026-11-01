package com.unciv.ui.story

import com.badlogic.gdx.files.FileHandle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.CancellationException

/** Подготавливает сжатый файл в памяти; большие ролики остаются потоковыми, распакованные кадры здесь не хранятся. */
internal object StoryVideoFilePreparation {
    const val MAX_CACHED_BYTES = 32 * 1024 * 1024

    /** Вызывается в рабочем потоке. Проверка фактического объёма защищает и файлы с неизвестной длиной. */
    fun prepare(source: FileHandle, limit: Int = MAX_CACHED_BYTES): FileHandle {
        require(limit > 0)
        if (source.length() > limit) return source
        val bytes = source.read().use { input ->
            val output = ByteArrayOutputStream(minOf(limit, 64 * 1024))
            val chunk = ByteArray(64 * 1024)
            while (true) {
                if (Thread.currentThread().isInterrupted) throw CancellationException("Video preparation cancelled")
                val count = input.read(chunk)
                if (count < 0) break
                if (count == 0) continue
                if (count > limit - output.size()) return source
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        }
        return object : FileHandle(source.path()) {
            override fun exists() = true
            override fun length() = bytes.size.toLong()
            override fun read(): InputStream = ByteArrayInputStream(bytes)
        }
    }
}
