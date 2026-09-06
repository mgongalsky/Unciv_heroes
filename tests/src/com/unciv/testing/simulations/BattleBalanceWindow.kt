package com.unciv.testing.simulations

import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.Base64
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

object BattleBalanceWindow {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            BalanceFrame().isVisible = true
        }
    }
}

private class BalanceFrame : JFrame("Сколько войск нужно для равного боя") {
    private val names = BalanceUnitSources.preset.map { it.name }
    private val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .firstOrNull { File(it, "settings.gradle.kts").isFile }
        ?: File(System.getProperty("user.dir"))
    private val source = JTextField("preset", 36)
    private val browse = JButton("Units.json…")
    private val preset = JButton("Набор sim-test")
    private val battles = JSpinner(SpinnerNumberModel(20, 1, 10000, 10))
    private val scale = JSpinner(SpinnerNumberModel(20, 1, 20, 1))
    private val seed = JTextField("0", 8)
    private val start = JButton("Найти соотношения")
    private val stop = JButton("Остановить")
    private val open = JButton("Открыть отчёт")
    private val progress = JProgressBar(0, 25)
    private val status = JLabel("Готов к поиску")
    private val explanation = JTextArea(4, 80)
    private val attacker = JComboBox(names.toTypedArray())
    private val defender = JComboBox(names.toTypedArray())
    private val samples = mutableListOf<BalanceCell>()
    private val results = mutableMapOf<Pair<Int, Int>, BalanceBoundarySearch.Result>()
    private val child = AtomicReference<Process?>()
    private val stopping = AtomicBoolean(false)
    private var active = false
    private var startedAt = 0L
    private var completedBattles = 0
    private var report: File? = null
    private var currentPair: Pair<Int, Int>? = null
    private val matrixModel = object : AbstractTableModel() {
        override fun getRowCount() = names.size
        override fun getColumnCount() = names.size + 1
        override fun getColumnName(column: Int) =
                if (column == 0) "Атакующий ↓ / защитник →" else names[column - 1]

        override fun getValueAt(row: Int, column: Int): Any {
            if (column == 0) return names[row]
            val pair = row to column - 1
            return results[pair] ?: if (pair == currentPair) "Ищем границу…" else "—"
        }
    }
    private val detailModel = object : AbstractTableModel() {
        private val columns = listOf(
            "Атакующих",
            "Защитников",
            "Битв",
            "Побед атаки",
            "Побед защиты",
            "Без победителя",
            "Победы атаки, %"
        )

        private fun rows() =
                samples.filter { it.attacker == attacker.selectedIndex && it.defender == defender.selectedIndex }
                    .sortedBy { it.defenderAmount }

        override fun getRowCount() = rows().size
        override fun getColumnCount() = columns.size
        override fun getColumnName(column: Int) = columns[column]
        override fun getValueAt(row: Int, column: Int): Any {
            val cell = rows()[row]
            return listOf(
                cell.attackerAmount,
                cell.defenderAmount,
                cell.simulations,
                cell.attackerWins,
                cell.defenderWins,
                cell.simulations - cell.attackerWins - cell.defenderWins,
                cell.decisiveRate?.let { "%.1f".format(it * 100) } ?: "нет победителя")[column]
        }
    }
    private val timer = Timer(500) { updateProgress() }

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        size = Dimension(1300, 800)
        minimumSize = Dimension(1050, 700)
        setLocationRelativeTo(null)
        val controls = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(row(JLabel("Характеристики:"), source, browse, preset))
            add(
                row(
                    JLabel("Битв на пробу:"), battles, JLabel("Размер атакующей армии:"), scale,
                    JLabel("Seed:"), seed, start, stop, open
                )
            )
            add(row(JLabel("В таблице: 1 атакующий : N защитников для примерно равных шансов. Направления атаки считаются отдельно.")))
            add(row(JLabel("Например, 1 : 2,5 означает около 2,5 защитников на одного атакующего. Нажмите результат для подробностей.")))
            add(row(JLabel("Цвет по силе атакующего: зелёный — N > 1,1; жёлтый — N от 0,9 до 1,1; красный — N < 0,9. Серый — нет оценки.")))
        }
        add(controls, BorderLayout.NORTH)
        val matrix = JTable(matrixModel).apply {
            rowHeight = 65
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            tableHeader.reorderingAllowed = false
            columnModel.getColumn(0).preferredWidth = 220
            setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable,
                    value: Any?,
                    selected: Boolean,
                    focus: Boolean,
                    row: Int,
                    column: Int
                ): Component {
                    super.getTableCellRendererComponent(table, value, selected, focus, row, column)
                    horizontalAlignment = SwingConstants.CENTER
                    foreground = Color(25, 30, 35)
                    val result = value as? BalanceBoundarySearch.Result
                    val ratio = result?.defendersPerAttacker
                    text = when {
                        result == null -> value.toString()
                        ratio != null -> "≈ 1 : " + String.format(
                            Locale.forLanguageTag("ru"),
                            "%.2f",
                            ratio
                        )

                        result.status == BalanceBoundarySearch.Status.INCONCLUSIVE -> "Недостаточно данных"
                        else -> "За пределами поиска"
                    }
                    background = when {
                        ratio == null -> Color(239, 242, 246)
                        ratio > 1.1 -> Color(180, 225, 188)
                        ratio < 0.9 -> Color(249, 192, 187)
                        else -> Color(251, 231, 158)
                    }
                    border = if (selected) BorderFactory.createLineBorder(Color(60, 90, 140), 2)
                    else BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    toolTipText = if (result != null) {
                        val strength = when {
                            ratio == null -> "Соотношение не определено."
                            ratio > 1.1 -> "Один атакующий противостоит более чем 1,1 защитника."
                            ratio < 0.9 -> "Один атакующий противостоит менее чем 0,9 защитника."
                            else -> "Сила одного атакующего и одного защитника примерно одинакова."
                        }
                        "$strength Нажмите, чтобы увидеть проверенные армии и точность оценки."
                    } else null
                    return this
                }
            })
        }
        val tabs = JTabbedPane()
        tabs.addTab("Соотношения для равного боя", JScrollPane(matrix))
        explanation.isEditable = false
        explanation.lineWrap = true
        explanation.wrapStyleWord = true
        val detail = JTable(detailModel).apply {
            rowHeight = 30
            tableHeader.reorderingAllowed = false
        }
        tabs.addTab("Как найдено соотношение", JPanel(BorderLayout()).apply {
            add(JPanel(BorderLayout()).apply {
                add(
                    row(JLabel("Атакующий:"), attacker, JLabel("Защитник:"), defender),
                    BorderLayout.NORTH
                )
                add(explanation, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(JScrollPane(detail), BorderLayout.CENTER)
        })
        add(tabs, BorderLayout.CENTER)
        progress.isStringPainted = true
        add(JPanel(BorderLayout()).apply {
            add(status, BorderLayout.NORTH)
            add(progress, BorderLayout.SOUTH)
        }, BorderLayout.SOUTH)
        stop.isEnabled = false
        open.isEnabled = false
        browse.addActionListener {
            val chooser = JFileChooser(File(root, "android/assets/jsons"))
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) source.text =
                    chooser.selectedFile.absolutePath
        }
        preset.addActionListener { source.text = "preset" }
        attacker.addActionListener { updateDetails() }
        defender.addActionListener { updateDetails() }
        start.addActionListener { launch() }
        stop.addActionListener { stopWorker() }
        open.addActionListener {
            try {
                report?.let { Desktop.getDesktop().open(it) }
            } catch (failure: Exception) {
                JOptionPane.showMessageDialog(this, failure.message)
            }
        }
        matrix.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) {
                val r = matrix.rowAtPoint(event.point)
                val c = matrix.columnAtPoint(event.point)
                if (r >= 0 && c > 0) {
                    attacker.selectedIndex = r
                    defender.selectedIndex = c - 1
                    updateDetails()
                    tabs.selectedIndex = 1
                }
            }
        })
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                stopWorker()
                timer.stop()
            }
        })
        updateDetails()
    }

    private fun row(vararg components: Component) =
            JPanel(FlowLayout(FlowLayout.LEFT)).apply { components.forEach { add(it) } }

    private fun updateDetails() {
        detailModel.fireTableDataChanged()
        val result = results[attacker.selectedIndex to defender.selectedIndex]
        explanation.text = when {
            result == null -> "Поиск ещё не завершён. В таблице ниже появляются выполненные пробы."
            result.status == BalanceBoundarySearch.Status.NEAR_HALF ->
                "Проверено: ${result.attackerAmount} атакующих против ${result.lowerDefenders} защитников. Доля побед атаки среди боёв с победителем попала в 45–55%. Отношение пересчитано на одного атакующего; это оценка по выборке."

            result.status == BalanceBoundarySearch.Status.BRACKETED ->
                "При ${result.attackerAmount} атакующих и ${result.lowerDefenders} защитниках чаще побеждает атака; при ${result.upperDefenders} защитниках — защита. Граница находится между этими армиями. На одного атакующего: %.3f–%.3f защитника; в обзоре показана середина интервала. Это оценка, а не проверенный бой с дробным юнитом.".format(
                    result.lowerDefenders!!.toDouble() / result.attackerAmount,
                    result.upperDefenders!!.toDouble() / result.attackerAmount
                )

            result.status == BalanceBoundarySearch.Status.INCONCLUSIVE ->
                "В одной из необходимых проб не было боёв с победителем. По этим результатам нельзя определить границу равных шансов."

            result.probes.first().decisiveRate!! < 0.45 ->
                "Даже один защитник чаще побеждает ${result.attackerAmount} атакующих. Для поиска нужна большая атакующая армия. Соотношение не определено."

            else -> "Даже ${result.probes.last().defenderAmount} защитников недостаточно против ${result.attackerAmount} атакующих. Достигнут предел поиска; соотношение не определено."
        } + "\nПроцент в последнем столбце — доля побед атаки только среди боёв с победителем. Поиск предполагает, что увеличение числа защитников снижает шанс победы атаки."
    }

    private fun launch() {
        if (active) return
        val config: BalanceExperimentConfig
        val selectedSource: String
        try {
            battles.commitEdit(); scale.commitEdit()
            config = BalanceExperimentConfig(
                (battles.value as Number).toInt(),
                (scale.value as Number).toInt(),
                seed.text.trim().toLong()
            )
            selectedSource = source.text.trim().let {
                if (it == "preset") it else {
                    val file = File(it).let { f -> if (f.isAbsolute) f else File(root, it) }
                    require(file.isFile) { "Не найден файл: $file" }
                    file.absolutePath
                }
            }
        } catch (failure: Exception) {
            JOptionPane.showMessageDialog(this, failure.message); return
        }
        samples.clear(); results.clear(); currentPair = null; completedBattles = 0; report = null
        matrixModel.fireTableDataChanged(); updateDetails()
        stopping.set(false); setActive(true); open.isEnabled = false
        startedAt = System.nanoTime(); updateProgress(); timer.start()
        object : SwingWorker<Int, String>() {
            private var finished = false
            private var error: String? = null
            override fun doInBackground(): Int {
                val process = BalanceWorkerProcess.startBoundary(
                    selectedSource,
                    config,
                    File(root, "tests/balance-results")
                )
                child.set(process)
                try {
                    if (stopping.get()) process.destroyForcibly()
                    process.inputStream.bufferedReader(Charsets.UTF_8)
                        .useLines { lines -> lines.forEach { publish(it) } }
                    return process.waitFor()
                } finally {
                    if (process.isAlive) process.destroyForcibly(); child.compareAndSet(
                        process,
                        null
                    )
                }
            }

            override fun process(chunks: MutableList<String>) {
                for (line in chunks) {
                    try {
                        val fields = line.split('\t')
                        if (fields.firstOrNull() != "BOUNDARY1") continue
                        when (fields[1]) {
                            "START" -> require(fields[2].toInt() == 25)
                            "SAMPLE" -> {
                                val cell = BalanceCell.parse(
                                    line.replaceFirst(
                                        "BOUNDARY1\tSAMPLE",
                                        "BALANCE1\tCELL"
                                    )
                                )
                                require(cell.attacker in names.indices && cell.defender in names.indices)
                                samples += cell; completedBattles += cell.simulations
                                currentPair = cell.attacker to cell.defender
                            }

                            "RESULT" -> {
                                require(fields.size == 9)
                                val pair = fields[2].toInt() to fields[3].toInt()
                                require(pair.first in names.indices && pair.second in names.indices && pair !in results)
                                val probes =
                                        samples.filter { it.attacker == pair.first && it.defender == pair.second }
                                require(probes.size == fields[8].toInt() && probes.isNotEmpty())
                                results[pair] =
                                        BalanceBoundarySearch.Result(
                                            BalanceBoundarySearch.Status.valueOf(fields[4]),
                                            fields[5].toInt(),
                                            fields[6].toInt().takeIf { it > 0 },
                                            fields[7].toInt().takeIf { it > 0 },
                                            probes
                                        )
                            }

                            "DONE" -> {
                                report = File(decode(fields[2])); finished = true
                            }

                            "ERROR" -> error = decode(fields[2])
                            else -> error("Неизвестное сообщение поиска")
                        }
                    } catch (failure: Exception) {
                        error = failure.message; child.get()?.destroyForcibly()
                    }
                }
                matrixModel.fireTableDataChanged(); updateDetails(); updateProgress()
            }

            override fun done() {
                timer.stop(); setActive(false)
                val exit = runCatching { get() }
                status.text = when {
                    stopping.get() -> "Остановлено. Завершено пар: ${results.size} из 25; найденные соотношения сохранены на экране."
                    error != null -> "Ошибка: $error"
                    exit.isFailure -> "Ошибка запуска: ${exit.exceptionOrNull()?.cause?.message ?: exit.exceptionOrNull()?.message}"
                    exit.getOrNull() != 0 || !finished || results.size != 25 -> "Поиск прерван. Завершено пар: ${results.size} из 25."
                    else -> "Готово. Найдено соотношений: ${results.values.count { it.defendersPerAttacker != null }} из 25. Отчёт: ${report?.absolutePath}"
                }
                currentPair = null; matrixModel.fireTableDataChanged()
                open.isEnabled = report?.isFile == true
            }
        }.execute()
    }

    private fun decode(value: String) = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
    private fun setActive(value: Boolean) {
        active = value
        listOf<Component>(
            source,
            browse,
            preset,
            battles,
            scale,
            seed,
            start
        ).forEach { it.isEnabled = !value }
        stop.isEnabled = value
    }

    private fun stopWorker() {
        if (!active) return
        stopping.set(true); stop.isEnabled = false; status.text = "Останавливаем поиск…"
        child.get()?.destroyForcibly()
    }

    private fun updateProgress() {
        progress.value = results.size
        progress.string = "Завершено пар войск: ${results.size} из 25"
        if (!stopping.get()) status.text =
                "Выполнено боёв: $completedBattles. Прошло %.1f с.".format((System.nanoTime() - startedAt) / 1_000_000_000.0)
    }
}
