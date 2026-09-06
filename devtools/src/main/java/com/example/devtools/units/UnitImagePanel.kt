package com.example.devtools.units

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*

class UnitImagePanel(root: Path, name: String, private val kind: UnitImages.Kind) :
    JPanel(BorderLayout(4, 4)) {
    private val blackSilhouette =
            JCheckBox("Чёрный силуэт — только предпросмотр", kind == UnitImages.Kind.ICON)
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
                return
            }
            val availableWidth = (width - 16).coerceAtLeast(1)
            val availableHeight = (height - 16).coerceAtLeast(1)
            val factor = minOf(
                availableWidth.toDouble() / current.width,
                availableHeight.toDouble() / current.height
            )
            val w = (current.width * factor).toInt().coerceAtLeast(1)
            val h = (current.height * factor).toInt().coerceAtLeast(1)
            val drawing = graphics.create() as Graphics2D
            try {
                drawing.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    if (kind == UnitImages.Kind.ICON) RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                    else RenderingHints.VALUE_INTERPOLATION_BICUBIC
                )
                drawing.drawImage(current, (width - w) / 2, (height - h) / 2, w, h, null)
            } finally {
                drawing.dispose()
            }
        }
    }

    init {
        border = BorderFactory.createTitledBorder(kind.title)
        preview.preferredSize = Dimension(360, 320)
        preview.minimumSize = Dimension(120, 120)
        add(preview, BorderLayout.CENTER)
        val controls = JPanel()
        controls.layout = BoxLayout(controls, BoxLayout.Y_AXIS)
        controls.add(dimensions)
        controls.add(JLabel("Предпросмотр подогнан под окно; размер файла не меняется"))
        if (kind == UnitImages.Kind.ICON) {
            blackSilhouette.toolTipText =
                    "Для белых иконок с прозрачным фоном. Не изменяет изображение при сохранении."
            blackSilhouette.addActionListener { updatePreview() }
            controls.add(blackSilhouette)
        }
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
                        importImage(chooser.selectedFile.toPath())
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
        controls.add(JButton("Изменить размер…").apply {
            addActionListener { safely { resizeImage() } }
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
                    importImage(file.toPath())
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
        val showSilhouette = kind == UnitImages.Kind.ICON && blackSilhouette.isSelected
        previewImage = image?.let { source ->
            if (showSilhouette) {
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
        preview.toolTipText = if (showSilhouette)
            "Чёрный силуэт по прозрачности. Если фон непрозрачный, он тоже будет чёрным. Отключите силуэт, чтобы увидеть исходные цвета."
        else "Изображение целиком вписано в панель. Цвета и размер исходника не меняются."
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

    private fun resizeImage() {
        val source = image ?: error("Сначала загрузите изображение")
        val targetWidth = JSpinner(SpinnerNumberModel(100, 1, 4096, 1))
        val targetHeight = JSpinner(SpinnerNumberModel(100, 1, 4096, 1))
        val preserveAspect = JCheckBox("Сохранить пропорции (прозрачные поля)", true)
        val maskMode =
                JCheckBox("Маска: чёрный или прозрачный пиксель", kind == UnitImages.Kind.ICON)
        val threshold = JSpinner(SpinnerNumberModel(128, 1, 255, 1))
        threshold.isEnabled = maskMode.isSelected
        maskMode.addActionListener { threshold.isEnabled = maskMode.isSelected }
        val fields = JPanel(GridLayout(0, 1, 4, 4))
        fields.add(JLabel("Сейчас: ${source.width} × ${source.height} пикселей"))
        fields.add(JLabel("Новая ширина:"))
        fields.add(targetWidth)
        fields.add(JLabel("Новая высота:"))
        fields.add(targetHeight)
        fields.add(preserveAspect)
        fields.add(JLabel("Без сохранения пропорций изображение растягивается"))
        fields.add(maskMode)
        fields.add(JLabel("Порог прозрачности: меньше — толще контур, больше — тоньше"))
        fields.add(threshold)
        fields.add(JLabel("Для маски сначала удалите фон. Цвет фигуры не учитывается."))
        val choice = JOptionPane.showConfirmDialog(
            this, fields, "Изменить размер — ${kind.title}",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        )
        if (choice != JOptionPane.OK_OPTION) return
        targetWidth.commitEdit()
        targetHeight.commitEdit()
        val width = (targetWidth.value as Number).toInt()
        val height = (targetHeight.value as Number).toInt()
        val result = if (maskMode.isSelected) {
            threshold.commitEdit()
            UnitImages.resizeMask(
                source,
                width,
                height,
                preserveAspect.isSelected,
                (threshold.value as Number).toInt()
            )
        } else UnitImages.resize(source, width, height, preserveAspect.isSelected)
        replace(result)
    }
    private fun importImage(path: Path) {
        val loaded = UnitImages.read(path)
        blackSilhouette.isSelected = false
        replace(loaded)
    }
    fun prepareSave(): UnitAtlasPreparation.ImageEdit? {
        if (!dirty) return null
        val current = image ?: error("Изображение отсутствует")
        val output = if (kind == UnitImages.Kind.ICON) UnitImages.tintableIcon(current) else current
        return UnitAtlasPreparation.ImageEdit(
            kind,
            target,
            expected?.clone(),
            UnitImages.png(output)
        )
    }

    /** Accept exactly the image bytes included in the completed transaction. */
    fun acceptSaved(edit: UnitAtlasPreparation.ImageEdit) {
        expected = edit.bytes.clone()
        image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(edit.bytes))
        dirty = false
        previous = null
        previousDirty = false
        undoAvailable = false
        if (kind == UnitImages.Kind.ICON) blackSilhouette.isSelected = true
        updatePreview()
    }
    private val saveFormatHint = JLabel(
        if (kind == UnitImages.Kind.ICON)
            "В файл: белый силуэт с прозрачностью для окраски в игре"
        else "В файл: исходные цвета и прозрачность спрайта"
    ).apply {
        val controls = (this@UnitImagePanel.layout as BorderLayout)
            .getLayoutComponent(BorderLayout.SOUTH) as JPanel
        controls.add(this)
    }
}
