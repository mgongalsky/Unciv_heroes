package com.unciv.testing.simulations

import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

/** Launches headless workers without a shell. A manifest classpath avoids the Windows command-line limit. */
object BalanceWorkerProcess {
    fun start(source: String, config: BalanceExperimentConfig, reports: File): Process =
            launch(BattleBalanceCli::class.java.name, source, config, reports)

    fun startBoundary(source: String, config: BalanceExperimentConfig, reports: File): Process =
            launch(BattleBoundaryCli::class.java.name, source, config, reports)

    private fun launch(
        mainClass: String,
        source: String,
        config: BalanceExperimentConfig,
        reports: File
    ): Process {
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes[Attributes.Name.CLASS_PATH] = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .joinToString(" ") { File(it).absoluteFile.toURI().toASCIIString() }
        }
        val classpathJar = File.createTempFile("balance-classpath-", ".jar")
        classpathJar.deleteOnExit()
        JarOutputStream(classpathJar.outputStream(), manifest).use { }
        val executable = File(
            System.getProperty("java.home"), "bin/java" +
                    if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
        )
        return ProcessBuilder(
            executable.absolutePath,
            "-Djava.awt.headless=true",
            "-Dbattle.verbose=false",
            "-Dfile.encoding=UTF-8",
            "-cp",
            classpathJar.absolutePath,
            mainClass,
            source,
            config.battlesPerCell.toString(),
            config.maxAmount.toString(),
            config.firstSeed.toString(),
            reports.absolutePath
        ).redirectErrorStream(true).start()
    }
}
