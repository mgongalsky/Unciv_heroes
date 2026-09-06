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
}
