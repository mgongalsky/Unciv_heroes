package com.example.devtools.units

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

object UnitImages {
    enum class Kind(val title: String, val directory: String) {
        SPRITE("Спрайт", "graphics/overrides/AbsoluteUnits/TileSets/AbsoluteUnits/Units"),
        ICON("Иконка", "android/Images.Construction/UnitIcons")
    }

    fun target(root: Path, name: String, kind: Kind): Path {
        UnitDocument.validateName(name)
        val canonicalRoot = root.toFile().canonicalFile.toPath()
        val target = canonicalRoot.resolve(kind.directory).resolve("$name.png")
            .toFile().canonicalFile.toPath()
        require(target.startsWith(canonicalRoot)) { "Путь изображения выходит за пределы проекта" }
        return target
    }

    fun source(root: Path, name: String, kind: Kind): Path? {
        val target = target(root, name, kind)
        if (Files.isRegularFile(target)) return target
        if (kind == Kind.SPRITE) {
            val base =
                root.resolve("graphics/base/AbsoluteUnits/TileSets/AbsoluteUnits/Units/$name.png")
            if (Files.isRegularFile(base)) return base
        }
        return null
    }

    fun read(path: Path): BufferedImage {
        ImageIO.createImageInputStream(path.toFile()).use { stream ->
            require(stream != null) { "Не удалось открыть изображение" }
            val readers = ImageIO.getImageReaders(stream)
            require(readers.hasNext()) { "Неподдерживаемое изображение; используйте PNG или JPEG" }
            val reader = readers.next()
            try {
                reader.input = stream
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                require(width > 0 && height > 0 && width <= 4096 && height <= 4096 && width.toLong() * height <= 4_194_304) {
                    "Изображение слишком большое: $width × $height. Максимум 4 мегапикселя и 4096 по каждой стороне."
                }
                val image = reader.read(0)
                return copy(image)
            } finally {
                reader.dispose()
            }
        }
    }

    fun copy(source: BufferedImage): BufferedImage =
        BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB).also {
            val pixels = source.getRGB(0, 0, source.width, source.height, null, 0, source.width)
            it.setRGB(0, 0, source.width, source.height, pixels, 0, source.width)
        }

    /** Removes only near-white pixels connected to an image edge, preserving enclosed white details. */
    fun removeWhite(source: BufferedImage, tolerance: Int): BufferedImage {
        require(tolerance in 0..100)
        val result = copy(source)
        val width = source.width
        val height = source.height
        val seen = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0
        fun visit(x: Int, y: Int) {
            if (x !in 0 until width || y !in 0 until height) return
            val index = y * width + x
            if (seen[index]) return
            seen[index] = true
            val argb = source.getRGB(x, y)
            val red = (argb ushr 16) and 255
            val green = (argb ushr 8) and 255
            val blue = argb and 255
            if ((argb ushr 24) != 0 && minOf(red, green, blue) < 255 - tolerance) return
            queue[tail++] = index
        }
        for (x in 0 until width) {
            visit(x, 0); visit(x, height - 1)
        }
        for (y in 0 until height) {
            visit(0, y); visit(width - 1, y)
        }
        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            result.setRGB(x, y, source.getRGB(x, y) and 0x00ffffff)
            visit(x - 1, y); visit(x + 1, y); visit(x, y - 1); visit(x, y + 1)
        }
        return result
    }

    fun png(image: BufferedImage): ByteArray = ByteArrayOutputStream().use {
        check(ImageIO.write(image, "png", it)) { "PNG encoder unavailable" }
        it.toByteArray()
    }

    fun save(target: Path, image: BufferedImage, expected: ByteArray?, backups: Path) {
        val current = if (Files.exists(target)) Files.readAllBytes(target) else null
        check(
            if (expected == null) current == null else current != null && current.contentEquals(
                expected
            )
        ) {
            "Изображение изменено другой программой: $target. Откройте набор правил заново."
        }
        val bytes = png(image)
        val snapshot = backups.resolve("images").resolve(UUID.randomUUID().toString())
        Files.createDirectories(snapshot)
        Files.writeString(snapshot.resolve("target.txt"), target.toAbsolutePath().toString())
        if (current != null) Files.write(snapshot.resolve(target.fileName), current)
        else Files.writeString(
            snapshot.resolve("new-file.txt"),
            "Target did not exist before import"
        )
        UnitDocument.atomicWrite(target, bytes)
    }

    /** Resamples into an exact-sized transparent canvas without changing the source image. */
    fun resize(
        source: BufferedImage,
        width: Int,
        height: Int,
        preserveAspect: Boolean = true
    ): BufferedImage {
        require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= 4_194_304) {
            "Размер должен быть от 1 до 4096 пикселей по каждой стороне, максимум 4 мегапикселя"
        }
        val scale = minOf(width.toDouble() / source.width, height.toDouble() / source.height)
        val drawWidth = if (preserveAspect) kotlin.math.round(source.width * scale).toInt()
            .coerceIn(1, width) else width
        val drawHeight = if (preserveAspect) kotlin.math.round(source.height * scale).toInt()
            .coerceIn(1, height) else height
        var current = source
        // Reduce in stages to retain small details when importing large generated images.
        while (current.width != drawWidth || current.height != drawHeight) {
            val nextWidth = if (current.width > drawWidth) maxOf(
                drawWidth,
                current.width / 2
            ) else drawWidth
            val nextHeight = if (current.height > drawHeight) maxOf(
                drawHeight,
                current.height / 2
            ) else drawHeight
            val next = BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_ARGB)
            val graphics = next.createGraphics()
            try {
                graphics.composite = java.awt.AlphaComposite.Src
                graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
                )
                graphics.drawImage(current, 0, 0, nextWidth, nextHeight, null)
            } finally {
                graphics.dispose()
            }
            if (current !== source) current.flush()
            current = next
        }
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        try {
            graphics.composite = java.awt.AlphaComposite.Src
            graphics.drawImage(current, (width - drawWidth) / 2, (height - drawHeight) / 2, null)
        } finally {
            graphics.dispose()
        }
        if (current !== source) current.flush()
        return result
    }

    /** Resizes an alpha mask, then quantizes coverage to opaque black or fully transparent. */
    fun resizeMask(
        source: BufferedImage, width: Int, height: Int,
        preserveAspect: Boolean = true, alphaThreshold: Int = 128
    ): BufferedImage {
        require(alphaThreshold in 1..255) { "Порог маски должен быть от 1 до 255" }
        val result = resize(source, width, height, preserveAspect)
        val pixels = result.getRGB(0, 0, width, height, null, 0, width)
        for (index in pixels.indices) {
            pixels[index] = if ((pixels[index] ushr 24) >= alphaThreshold) 0xff000000.toInt() else 0
        }
        result.setRGB(0, 0, width, height, pixels, 0, width)
        return result
    }

    /** White RGB allows the game's multiplicative tint to select the displayed color. */
    fun tintableIcon(source: BufferedImage): BufferedImage {
        val result = copy(source)
        val pixels = result.getRGB(0, 0, result.width, result.height, null, 0, result.width)
        for (index in pixels.indices) pixels[index] = pixels[index] or 0x00ffffff
        result.setRGB(0, 0, result.width, result.height, pixels, 0, result.width)
        return result
    }
}
