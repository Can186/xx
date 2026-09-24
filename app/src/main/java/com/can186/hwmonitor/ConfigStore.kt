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

    fun saveMetrics(ctx: Context, metrics: List<Metric>) {
        val arr = JSONArray()
        metrics.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("name", m.name)
                put("path", m.path); put("type", m.type.name)
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
                    o.getString("id"), o.getString("name"),
                    o.getString("path"),
                    MetricType.valueOf(o.getString("type")),
                    enabled = o.optBoolean("enabled", true)
                ))
            }
            list
        } catch (e: Exception) { defaults() }
    }

    private fun defaults(): MutableList<Metric> = mutableListOf(
        Metric("cpu_usage", "CPU", "/proc/stat", MetricType.PERCENT),
        Metric("cpu_freq",  "CPU", "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", MetricType.FREQ_KHZ),
        Metric("cpu_temp",  "CPU", "/sys/class/thermal/thermal_zone0/temp", MetricType.TEMP_MC),
        Metric("gpu_usage", "GPU", "/sys/class/misc/mali0/device/utilization", MetricType.PERCENT),
        Metric("gpu_freq",  "GPU", "/sys/class/devfreq/13000000.mali/cur_freq", MetricType.FREQ_MHZ),
        Metric("mem",       "MEM", "/proc/meminfo", MetricType.RAW)
    )

    fun saveOverlay(ctx: Context, x: Int, y: Int, alpha: Float, font: Float) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_X, x).putInt(KEY_Y, y)
            .putFloat(KEY_ALPHA, alpha).putFloat(KEY_FONT, font)
            .apply()
    }

    fun loadOverlay(ctx: Context): FloatArray {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return floatArrayOf(
            sp.getInt(KEY_X, 0).toFloat(),
            sp.getInt(KEY_Y, 100).toFloat(),
            sp.getFloat(KEY_ALPHA, 0.8f),
            sp.getFloat(KEY_FONT, 11f)
        )
    }
}
