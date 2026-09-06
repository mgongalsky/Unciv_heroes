package com.example.devtools

import com.example.devtools.units.UnitEditorWindow
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

object UnitEditor {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val start = Path.of(args.firstOrNull() ?: ".").toAbsolutePath().normalize()
            val root = generateSequence(start) { it.parent }.firstOrNull {
                Files.isDirectory(it.resolve("android/assets/jsons"))
            } ?: run {
                val chooser = JFileChooser(start.toFile())
                chooser.dialogTitle = "Выберите корневую папку Unciv_heroes"
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@invokeLater
                chooser.selectedFile.toPath().toAbsolutePath()
            }
            UnitEditorWindow(root).isVisible = true
        }
    }
}
