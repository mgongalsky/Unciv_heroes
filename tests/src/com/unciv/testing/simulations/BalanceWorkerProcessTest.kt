package com.unciv.testing.simulations

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BalanceWorkerProcessTest {
    @Test
    fun `headless child emits every directed matchup and saves complete report`() {
        val directory = Files.createTempDirectory("balance-worker-test-").toFile()
        val reader = Executors.newSingleThreadExecutor()
        var child: Process? = null
        try {
            val process =
                BalanceWorkerProcess.start("preset", BalanceExperimentConfig(2, 1, 42), directory)
            child = process
            val output = reader.submit(Callable {
                process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            })
            assertTrue("Worker timed out", process.waitFor(60, TimeUnit.SECONDS))
            val text = output.get(10, TimeUnit.SECONDS)
            assertEquals(text, 0, process.exitValue())
            val lines = text.lines()
            assertTrue(text, lines.contains("BALANCE1\tSTART\t50\t25"))
            val cells = lines.filter { it.startsWith("BALANCE1\tCELL\t") }.map(BalanceCell::parse)
            assertEquals(25, cells.size)
            assertEquals(25, cells.map { it.key }.toSet().size)
            assertEquals(50, cells.sumOf { it.simulations })
            assertTrue(cells.any { it.attacker == 0 && it.defender == 1 })
            assertTrue(cells.any { it.attacker == 1 && it.defender == 0 })
            assertEquals(5, cells.count { it.attacker == it.defender })
            val done = lines.single { it.startsWith("BALANCE1\tDONE\t") }.split('\t')
            val report = java.io.File(String(Base64.getDecoder().decode(done[2]), Charsets.UTF_8))
            assertTrue(report.isFile)
            assertEquals(26, report.readLines().size)
            assertTrue(
                java.io.File(report.parentFile, "parameters.txt").readText()
                    .contains("firstSeed=42")
            )
        } finally {
            child?.let {
                if (it.isAlive) {
                    it.destroyForcibly(); it.waitFor(10, TimeUnit.SECONDS)
                }
            }
            reader.shutdownNow()
            directory.deleteRecursively()
        }
    }
}
