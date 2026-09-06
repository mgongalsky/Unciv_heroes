package com.example.devtools.units

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage

class UnitImagesTest {
    @Test
    fun removesEdgeBackgroundButKeepsWhiteInsideOutline() {
        val image = BufferedImage(7, 7, BufferedImage.TYPE_INT_ARGB)
        for (x in 0..6) for (y in 0..6) image.setRGB(x, y, -1)
        for (i in 1..5) {
            image.setRGB(i, 1, 0xff000000.toInt())
            image.setRGB(i, 5, 0xff000000.toInt())
            image.setRGB(1, i, 0xff000000.toInt())
            image.setRGB(5, i, 0xff000000.toInt())
        }
        val result = UnitImages.removeWhite(image, 20)
        assertEquals(0, result.getRGB(0, 0) ushr 24)
        assertEquals(-1, result.getRGB(3, 3))
        assertEquals(0xff000000.toInt(), result.getRGB(1, 1))
        assertEquals(-1, image.getRGB(0, 0))
    }

    @Test
    fun toleranceAndExistingTransparencyAreRespected() {
        val image = BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0x00ffffff)
        image.setRGB(1, 0, 0xffeeeeee.toInt())
        image.setRGB(2, 0, 0xff0044ff.toInt())
        assertEquals(255, UnitImages.removeWhite(image, 0).getRGB(1, 0) ushr 24)
        val result = UnitImages.removeWhite(image, 20)
        assertEquals(0, result.getRGB(1, 0) ushr 24)
        assertEquals(image.getRGB(2, 0), result.getRGB(2, 0))
    }

    @Test
    fun resizeFitsNonSquareImageInsideTransparentCanvas() {
        val source = BufferedImage(1200, 600, BufferedImage.TYPE_INT_ARGB)
        val graphics = source.createGraphics()
        try {
            graphics.color = java.awt.Color(255, 0, 0, 128)
            graphics.fillRect(0, 0, source.width, source.height)
        } finally {
            graphics.dispose()
        }
        val result = UnitImages.resize(source, 100, 100)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
        assertEquals(0, result.getRGB(50, 24) ushr 24)
        assertEquals(128, result.getRGB(50, 25) ushr 24)
        assertEquals(128, result.getRGB(50, 74) ushr 24)
        assertEquals(0, result.getRGB(50, 75) ushr 24)
        assertEquals(255, (result.getRGB(50, 50) ushr 16) and 255)
        assertEquals(1200, source.width)
        assertEquals(128, source.getRGB(0, 0) ushr 24)
        val decoded =
                javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(UnitImages.png(result)))
        assertEquals(100, decoded.width)
        assertEquals(result.getRGB(50, 50), decoded.getRGB(50, 50))
        assertEquals(0, decoded.getRGB(50, 0) ushr 24)
    }

    @Test
    fun resizeSupportsStretchAndRejectsInvalidCanvasSizes() {
        val source = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB)
        source.setRGB(0, 0, 0xff00ff00.toInt())
        source.setRGB(1, 0, 0xff00ff00.toInt())
        val result = UnitImages.resize(source, 100, 100, false)
        assertEquals(0xff00ff00.toInt(), result.getRGB(0, 0))
        assertEquals(0xff00ff00.toInt(), result.getRGB(99, 99))
        assertThrows(IllegalArgumentException::class.java) { UnitImages.resize(source, 0, 100) }
        assertThrows(IllegalArgumentException::class.java) { UnitImages.resize(source, 4096, 4096) }
        val unchangedSize = UnitImages.resize(source, 2, 1)
        unchangedSize.setRGB(0, 0, 0)
        assertEquals(0xff00ff00.toInt(), source.getRGB(0, 0))
    }

    @Test
    fun maskResizeProducesOnlyBlackOrTransparentPixelsInPng() {
        val source = BufferedImage(120, 60, BufferedImage.TYPE_INT_ARGB)
        for (x in 20 until 100) for (y in 10 until 50)
            source.setRGB(x, y, 0xffffffff.toInt())
        val result = UnitImages.resizeMask(source, 25, 25)
        assertEquals(25, result.width)
        assertEquals(25, result.height)
        assertEquals(0, result.getRGB(12, 0))
        assertEquals(0xff000000.toInt(), result.getRGB(12, 12))
        val decoded =
                javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(UnitImages.png(result)))
        for (x in 0 until 25) for (y in 0 until 25) {
            assertTrue(decoded.getRGB(x, y) == 0 || decoded.getRGB(x, y) == 0xff000000.toInt())
        }
        assertEquals(0xffffffff.toInt(), source.getRGB(50, 30))
    }

    @Test
    fun maskThresholdControlsCoverageWithoutKeepingPartialAlpha() {
        val source = BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB)
        source.setRGB(0, 0, 0x00ffffff)
        source.setRGB(1, 0, 0x7fffffff)
        source.setRGB(2, 0, 0x80ffffff.toInt())
        val normal = UnitImages.resizeMask(source, 3, 1, true, 128)
        assertEquals(0, normal.getRGB(0, 0))
        assertEquals(0, normal.getRGB(1, 0))
        assertEquals(0xff000000.toInt(), normal.getRGB(2, 0))
        val thicker = UnitImages.resizeMask(source, 3, 1, true, 127)
        assertEquals(0xff000000.toInt(), thicker.getRGB(1, 0))
        assertThrows(IllegalArgumentException::class.java) {
            UnitImages.resizeMask(
                source,
                3,
                1,
                true,
                0
            )
        }
    }
}
