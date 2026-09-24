package com.can186.hwmonitor

import android.util.Log
import java.io.File

object MetricsProvider {

    private const val TAG = "MetricsProvider"
    private var prevCpuTotal = 0L
    private var prevCpuBusy = 0L

    private fun readRaw(path: String): String {
        if (path.isBlank()) return "--"
        return try {
            val f = File(path)
            if (!f.exists() || !f.canRead()) return "--"
            val text = f.readText().trim()
            if (text.isEmpty()) "--" else text
        } catch (e: Exception) {
            Log.w(TAG, "read fail: $path -> ${e.message}")
            "--"
        }
    }

    fun readMetric(m: Metric): String {
        if (!m.enabled) return "--"
        if (m.id == "cpu_usage") return readCpuUsage()

        val raw = readRaw(m.path)
        if (raw == "--") return "--"

        return try {
            when (m.type) {
                MetricType.PERCENT -> "${raw.toFloat().toInt()}%"
                MetricType.FREQ_MHZ -> "${raw.toLong() / 1_000_000}MHz"
                MetricType.FREQ_KHZ -> "${raw.toLong() / 1000}MHz"
                MetricType.TEMP_MC -> "${raw.toLong() / 1000}°C"
                MetricType.TEMP_C -> "${raw.toFloat().toInt()}°C"
                MetricType.BYTES_MB -> "${raw.toLong() / 1024 / 1024}MB"
                MetricType.RAW -> raw
            }
        } catch (e: Exception) {
            "--"
        }
    }

    private fun readCpuUsage(): String {
        return try {
            val line = File("/proc/stat").readLines()
                .firstOrNull { it.startsWith("cpu ") } ?: return "--"
            val p = line.split("\\s+".toRegex())
            if (p.size < 8) return "--"

            val total = p[1].toLong() + p[2].toLong() + p[3].toLong() +
                    p[4].toLong() + p[5].toLong() + p[6].toLong() + p[7].toLong()
            val busy = total - p[4].toLong() - p[5].toLong()

            if (prevCpuTotal == 0L) {
                prevCpuTotal = total; prevCpuBusy = busy; return "--"
            }
            val dT = total - prevCpuTotal
            val dB = busy - prevCpuBusy
            prevCpuTotal = total; prevCpuBusy = busy

            if (dT <= 0) "--" else "${(dB * 100 / dT).toInt()}%"
        } catch (e: Exception) { "--" }
    }

    fun refreshAll(metrics: List<Metric>) {
        metrics.forEach { it.value = readMetric(it) }
    }
}
