package com.can186.hwmonitor

enum class MetricType {
    PERCENT,
    FREQ_MHZ,
    FREQ_KHZ,
    TEMP_MC,
    TEMP_C,
    TEMP_0_1C,
    BYTES_MB,
    RAW,
    TRI_FIRST_PERCENT,
    TRI_SECOND_FREQ_KHZ,
    MEM_INFO,
    CURRENT_UA,
    VOLTAGE_UV,
    POWER_W
}

data class Metric(
    val id: String,
    val name: String,
    val label: String,
    var path: String,
    val type: MetricType,
    var value: String = "--",
    var enabled: Boolean = true
)

enum class ReadMode { BASIC, ROOT, SHIZUKU }

enum class OverlayPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
