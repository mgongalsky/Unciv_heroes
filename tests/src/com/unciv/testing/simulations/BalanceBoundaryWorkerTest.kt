package com.unciv.testing.simulations

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BalanceBoundaryWorkerTest {
    @Test
    fun `child searches every direction and reports only supported ratios`() {
        val directory = Files.createTempDirectory("boundary-worker-test-").toFile()
        val reader = Executors.newSingleThreadExecutor()
        var child: Process? = null
        try {
            val process = BalanceWorkerProcess.startBoundary(
                "preset",
                BalanceExperimentConfig(2, 1, 42),
                directory
            )
            child = process
            val output = reader.submit(Callable {
                process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            })
            assertTrue("Worker timed out", process.waitFor(90, TimeUnit.SECONDS))
            val text = output.get(10, TimeUnit.SECONDS)
            assertEquals(text, 0, process.exitValue())
            val lines = text.lines()
            assertTrue(lines.contains("BOUNDARY1\tSTART\t25"))
            val results =
                lines.filter { it.startsWith("BOUNDARY1\tRESULT\t") }.map { it.split('\t') }
            assertEquals(25, results.size)
            assertEquals(25, results.map { it[2] to it[3] }.toSet().size)
            val samples = lines.filter { it.startsWith("BOUNDARY1\tSAMPLE\t") }
                .map { BalanceCell.parse(it.replaceFirst("BOUNDARY1\tSAMPLE", "BALANCE1\tCELL")) }
            results.forEach { fields ->
                val status = BalanceBoundarySearch.Status.valueOf(fields[4])
                val probes =
                    samples.filter { it.attacker == fields[2].toInt() && it.defender == fields[3].toInt() }
                assertEquals(fields[8].toInt(), probes.size)
                val lower = fields[6].toInt()
                val upper = fields[7].toInt()
                when (status) {
                    BalanceBoundarySearch.Status.NEAR_HALF -> {
                        assertEquals(lower, upper)
                        assertTrue(probes.single { it.defenderAmount == lower }.decisiveRate!! in 0.45..0.55)
                    }

                    BalanceBoundarySearch.Status.BRACKETED -> {
                        assertEquals(lower + 1, upper)
                        assertTrue(probes.single { it.defenderAmount == lower }.decisiveRate!! > 0.55)
                        assertTrue(probes.single { it.defenderAmount == upper }.decisiveRate!! < 0.45)
                    }

                    else -> {
                        assertEquals(0, lower); assertEquals(0, upper)
                    }
                }
            }
            val done = lines.single { it.startsWith("BOUNDARY1\tDONE\t") }.split('\t')
            val report = File(String(Base64.getDecoder().decode(done[2]), Charsets.UTF_8))
            assertEquals(26, report.readLines().size)
            assertEquals(samples.size + 1, File(report.parentFile, "probes.csv").readLines().size)
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
