package com.can186.hwmonitor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ConfigStore {

    private const val PREF = "hw_monitor"
    private const val KEY_METRICS = "metrics"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_ALPHA = "alpha"
    private const val KEY_FONT = "font"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_POSITION = "position"

    fun saveMetrics(ctx: Context, metrics: List<Metric>) {
        val arr = JSONArray()
        metrics.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("label", m.label)
                put("path", m.path)
                put("type", m.type.name)
                put("enabled", m.enabled)
            })
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_METRICS, arr.toString()).apply()
    }

    fun loadMetrics(ctx: Context): MutableList<Metric> {
        val json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_METRICS, null)
        if (json.isNullOrEmpty()) return defaults()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Metric>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(Metric(
                    o.getString("id"),
                    o.getString("name"),
                    o.optString("label", ""),
                    o.getString("path"),
                    MetricType.valueOf(o.getString("type")),
                    enabled = o.optBoolean("enabled", true)
                ))
            }
            list
        } catch (e: Exception) {
            defaults()
        }
    }

    fun reset(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_METRICS).apply()
    }

    private fun defaults(): MutableList<Metric> = mutableListOf(
        Metric("cpu_usage", "CPU", "占用", "/proc/stat", MetricType.PERCENT),
        Metric("cpu_freq",  "CPU", "频率", "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", MetricType.FREQ_KHZ),
        Metric("cpu_temp",  "CPU", "温度", "/sys/class/thermal/thermal_zone1/temp", MetricType.TEMP_MC),
        Metric("gpu_usage", "GPU", "占用", "/sys/kernel/ged/hal/gpu_utilization", MetricType.TRI_FIRST_PERCENT),
        Metric("gpu_freq",  "GPU", "频率", "/sys/kernel/ged/hal/current_freqency", MetricType.TRI_SECOND_FREQ_KHZ),
        Metric("mem",       "MEM", "内存", "/proc/meminfo", MetricType.MEM_INFO),
        Metric("bat_cap",   "BAT", "电量", "/sys/class/power_supply/battery/capacity", MetricType.PERCENT),
        Metric("bat_temp",  "BAT", "温度", "/sys/class/power_supply/battery/temp", MetricType.TEMP_0_1C),
        Metric("bat_pwr",   "BAT", "功耗", "/sys/class/power_supply/battery/current_now", MetricType.POWER_W)
    )

    fun saveOverlay(ctx: Context, x: Int, y: Int, alpha: Float, font: Float,
                    textColor: Int, position: OverlayPosition) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_X, x).putInt(KEY_Y, y)
            .putFloat(KEY_ALPHA, alpha).putFloat(KEY_FONT, font)
            .putInt(KEY_TEXT_COLOR, textColor)
            .putString(KEY_POSITION, position.name)
            .apply()
    }

    fun loadOverlayAlpha(ctx: Context): Float =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getFloat(KEY_ALPHA, 0.8f)

    fun loadOverlayFont(ctx: Context): Float =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getFloat(KEY_FONT, 11f)

    fun loadOverlayTextColor(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY_TEXT_COLOR, 0xFFFFFFFF.toInt())

    fun loadOverlayPosition(ctx: Context): OverlayPosition {
        // ✅ 注意看这里，必须是 "val s ="，不能写成 "val ="
        val v = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_POSITION, OverlayPosition.TOP_LEFT.name)
        return try {
            OverlayPosition.valueOf(s ?: "TOP_LEFT")
        } catch (e: Exception) {
            OverlayPosition.TOP_LEFT
        }
    }

    fun loadOverlayXY(ctx: Context): IntArray {
        val v = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return intArrayOf(sp.getInt(KEY_X, 0), sp.getInt(KEY_Y, 100))
    }

    fun saveOverlayXY(ctx: Context, x: Int, y: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_X, x).putInt(KEY_Y, y).apply()
    }
}
