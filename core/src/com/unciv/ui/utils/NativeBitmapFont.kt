package com.unciv.ui.utils

import com.badlogic.gdx.graphics.g2d.BitmapFont

/** Владеет текстурами страниц и упаковщиком динамического нативного шрифта. */
class NativeBitmapFont(private val nativeData: NativeBitmapFontData) :
    BitmapFont(nativeData, nativeData.regions, false) {
    private var disposed = false

    init {
        setOwnsTexture(true)
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        try {
            // Текстуры PixmapPacker освобождают также связанные с ними Pixmap.
            super.dispose()
        } finally {
            // Освобождает страницы без текстур и запрещает дальнейшую упаковку.
            nativeData.dispose()
        }
    }
}
