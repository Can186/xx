package com.can186.hwmonitor

enum class MetricType {
    PERCENT,
    FREQ_MHZ,
    FREQ_KHZ,
    TEMP_MC,
    TEMP_C,
    BYTES_MB,
    RAW,
    TRI_FIRST_PERCENT
}

data class Metric(
    val id: String,
    val name: String,
    var path: String,
    val type: MetricType,
    var value: String = "--",
    var enabled: Boolean = true
)

enum class ReadMode { BASIC, ROOT, SHIZUKU }
