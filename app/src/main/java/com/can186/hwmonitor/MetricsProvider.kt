package com.can186.hwmonitor

import android.util.Log
import com.topjohnwu.superuser.Shell

object MetricsProvider {

    private const val TAG = "MetricsProvider"
    private var prevCpuTotal = 0L
    private var prevCpuBusy = 0L

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    private fun readRaw(path: String): String {
        if (path.isBlank()) return "--"
        return try {
            val result = Shell.cmd("cat $path").exec()
            if (!result.isSuccess) return "--"
            val text = result.out.joinToString("").trim()
            if (text.isEmpty()) "--" else text
        } catch (e: Exception) {
            Log.w(TAG, "read fail: $path -> ${e.message}")
            "--"
        }
    }

    fun readMetric(m: Metric): String {
        if (!m.enabled) return "--"
        if (m.id == "cpu_usage") return readCpuUsage()
        if (m.type == MetricType.MEM_INFO) return readMemInfo()

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
                MetricType.TRI_FIRST_PERCENT -> {
                    val first = raw.split("\\s+".toRegex()).firstOrNull() ?: return "--"
                    "${first.toInt()}%"
                }
                MetricType.TRI_SECOND_FREQ_KHZ -> {
                    val parts = raw.split("\\s+".toRegex())
                    val freq = parts.getOrNull(1) ?: return "--"
                    "${freq.toLong() / 1000}MHz"
                }
                MetricType.MEM_INFO -> readMemInfo()
            }
        } catch (e: Exception) {
            "--"
        }
    }

    private fun readCpuUsage(): String {
        return try {
            val result = Shell.cmd("cat /proc/stat").exec()
            if (!result.isSuccess) return "--"
            val line = result.out.firstOrNull { it.startsWith("cpu ") } ?: return "--"
            val p = line.split("\\s+".toRegex())
            if (p.size < 8) return "--"

            val total = p[1].toLong() + p[2].toLong() + p[3].toLong() +
                    p[4].toLong() + p[5].toLong() + p[6].toLong() + p[7].toLong()
            val busy = total - p[4].toLong() - p[5].toLong()

            if (prevCpuTotal == 0L) {
                prevCpuTotal = total
                prevCpuBusy = busy
                return "--"
            }
            val dT = total - prevCpuTotal
            val dB = busy - prevCpuBusy
            prevCpuTotal = total
            prevCpuBusy = busy

            if (dT <= 0) "--" else "${(dB * 100 / dT).toInt()}%"
        } catch (e: Exception) {
            "--"
        }
    }

    private fun readMemInfo(): String {
        return try {
            val result = Shell.cmd("cat /proc/meminfo").exec()
            if (!result.isSuccess) return "--"
            var total = 0L
            var available = 0L
            for (line in result.out) {
                when {
                    line.startsWith("MemTotal:") -> {
                        total = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0L
                    }
                    line.startsWith("MemAvailable:") -> {
                        available = line.split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0L
                    }
                }
            }
            if (total == 0L) return "--"
            val used = total - available
            val usedGB = used / 1024.0 / 1024.0
            val totalGB = total / 1024.0 / 1024.0
            String.format("%.1f/%.1fGB", usedGB, totalGB)
        } catch (e: Exception) {
            "--"
        }
    }

    fun refreshAll(metrics: List<Metric>) {
        metrics.forEach { it.value = readMetric(it) }
    }
}
