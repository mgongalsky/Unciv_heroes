package com.example.devtools.units

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*

class UnitImagePanel(root: Path, name: String, private val kind: UnitImages.Kind) :
    JPanel(BorderLayout(4, 4)) {
    private var previewImage: BufferedImage? = null
    private var undoAvailable = false
    private val target = UnitImages.target(root, name, kind)
    private var expected = if (Files.exists(target)) Files.readAllBytes(target) else null
    private var image: BufferedImage? =
        UnitImages.source(root, name, kind)?.let { UnitImages.read(it) }
    private var previous: BufferedImage? = null
    private var previousDirty = false
    var dirty = false
        private set
    private val dimensions = JLabel()
    private val preview = object : JPanel() {
        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            for (x in 0 until width step 16) for (y in 0 until height step 16) {
                graphics.color = if ((x / 16 + y / 16) % 2 == 0) Color(210, 210, 210) else Color(
                    245,
                    245,
                    245
                )
                graphics.fillRect(x, y, 16, 16)
            }
            val current = previewImage
            if (current == null) {
                graphics.color = Color.DARK_GRAY
                graphics.drawString("Перетащите PNG/JPEG сюда", 15, height / 2)
            } else {
                val factor = minOf(
                    width.toDouble() / current.width,
                    height.toDouble() / current.height,
                    1.0
                )
                val w = (current.width * factor).toInt()
                val h = (current.height * factor).toInt()
                graphics.drawImage(current, (width - w) / 2, (height - h) / 2, w, h, null)
            }
        }
    }

    init {
        border = BorderFactory.createTitledBorder(kind.title)
        preview.preferredSize = Dimension(360, 320)
        add(preview, BorderLayout.CENTER)
        val controls = JPanel()
        controls.layout = BoxLayout(controls, BoxLayout.Y_AXIS)
        controls.add(dimensions)
        controls.add(JButton("Выбрать изображение").apply {
            addActionListener {
                safely {
                    val chooser = JFileChooser()
                    chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                        "PNG / JPEG",
                        "png",
                        "jpg",
                        "jpeg"
                    )
                    if (chooser.showOpenDialog(this@UnitImagePanel) == JFileChooser.APPROVE_OPTION)
                        replace(UnitImages.read(chooser.selectedFile.toPath()))
                }
            }
        })
        val tolerance = JSlider(0, 100, 20)
        tolerance.majorTickSpacing = 20
        tolerance.paintLabels = true
        tolerance.paintTicks = true
        controls.add(JLabel("Допуск белого фона"))
        controls.add(tolerance)
        controls.add(JButton("Удалить белый фон от краёв").apply {
            addActionListener {
                safely {
                    replace(
                        UnitImages.removeWhite(
                            image ?: error("Сначала загрузите изображение"),
                            tolerance.value
                        )
                    )
                }
            }
        })
        controls.add(JButton("Отменить последнее изменение").apply {
            addActionListener {
                if (undoAvailable) {
                    image = previous
                    dirty = previousDirty
                    previous = null
                    previousDirty = false
                    undoAvailable = false
                    updatePreview()
                }
            }
        })
        controls.add(JLabel("Запись на диск: общая кнопка «Сохранить»"))
        controls.add(JLabel("Размер и расположение фигуры в PNG сохраняются"))
        add(controls, BorderLayout.SOUTH)
        preview.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean =
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false
                return try {
                    val files =
                            support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    require(files.size == 1) { "Переносите по одному изображению" }
                    val file =
                            files.single() as? java.io.File ?: error("Требуется файл изображения")
                    replace(UnitImages.read(file.toPath()))
                    true
                } catch (error: Exception) {
                    JOptionPane.showMessageDialog(this@UnitImagePanel, error.message)
                    false
                }
            }
        }
        updatePreview()
    }

    private fun replace(next: BufferedImage) {
        previous = image
        previousDirty = dirty
        undoAvailable = true
        image = next
        dirty = true
        updatePreview()
    }

    private fun updatePreview() {
        previewImage = image?.let { source ->
            if (kind == UnitImages.Kind.ICON) {
                val pixels = source.getRGB(0, 0, source.width, source.height, null, 0, source.width)
                for (index in pixels.indices) pixels[index] = pixels[index] and 0xff000000.toInt()
                BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB).also {
                    it.setRGB(0, 0, source.width, source.height, pixels, 0, source.width)
                }
            } else source
        }
        dimensions.text = image?.let {
            "${it.width} × ${it.height}${if (dirty) " — не сохранено" else ""}"
        } ?: "Изображение отсутствует"
        preview.toolTipText = if (kind == UnitImages.Kind.ICON)
            "Иконка показана чёрным для читаемости; исходные цвета сохраняются в файле"
        else null
        preview.repaint()
    }

    fun save(backups: Path) {
        if (!dirty) return
        UnitImages.save(target, image ?: error("Изображение отсутствует"), expected, backups)
        expected = Files.readAllBytes(target)
        dirty = false
        previous = null
        previousDirty = false
        undoAvailable = false
        updatePreview()
    }

    private fun safely(block: () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            JOptionPane.showMessageDialog(
                this,
                error.message,
                kind.title,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
