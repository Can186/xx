package com.can186.hwmonitor

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity() {

    private lateinit var metrics: MutableList<Metric>
    private lateinit var alphaLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        metrics = ConfigStore.loadMetrics(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val isRoot = Shell.isAppGrantedRoot() == true
        val rootStatus = if (isRoot) "Root: 已获取" else "Root: 未获取"
        root.addView(TextView(this).apply {
            text = rootStatus
            setTextColor(if (isRoot) 0xFF00AA00.toInt() else 0xFFFF0000.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 32)
        })

        root.addView(Button(this).apply {
            text = "申请悬浮窗权限"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                } else {
                    Toast.makeText(this@MainActivity, "已授权", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // ===== 标题：美化设置 =====
        root.addView(TextView(this).apply {
            text = "── 悬浮窗美化 ──"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 32, 0, 16)
        })

        // 背景透明度
        alphaLabel = TextView(this).apply {
            text = "背景透明度：${(ConfigStore.loadOverlayAlpha(this@MainActivity) * 100).toInt()}%"
            textSize = 14f
        }
        root.addView(alphaLabel)

        root.addView(SeekBar(this).apply {
            max = 100
            progress = (ConfigStore.loadOverlayAlpha(this@MainActivity) * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    alphaLabel.text = "背景透明度：$progress%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    val sp = getSharedPreferences("hw_monitor", MODE_PRIVATE)
                    sp.edit().putFloat("alpha", sb!!.progress / 100f).apply()
                    restartOverlay()
                }
            })
        })

        // 文字颜色
        val colors = arrayOf("白色", "绿色", "红色", "黄色", "青色", "品红")
        val colorValues = intArrayOf(
            0xFFFFFFFF.toInt(), 0xFF00FF00.toInt(), 0xFFFF5555.toInt(),
            0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt()
        )
        var colorIdx = colorValues.indexOfFirst { it == ConfigStore.loadOverlayTextColor(this) }
        if (colorIdx < 0) colorIdx = 0

        root.addView(TextView(this).apply {
            text = "文字颜色"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        })
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, colors)
            setSelection(colorIdx)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val sp = getSharedPreferences("hw_monitor", MODE_PRIVATE)
                    sp.edit().putInt("text_color", colorValues[position]).apply()
                    restartOverlay()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        })

        // 位置预设
        val positions = arrayOf("左上角", "右上角", "左下角", "右下角")
        val posValues = arrayOf(OverlayPosition.TOP_LEFT, OverlayPosition.TOP_RIGHT, OverlayPosition.BOTTOM_LEFT, OverlayPosition.BOTTOM_RIGHT)
        var posIdx = posValues.indexOf(ConfigStore.loadOverlayPosition(this))
        if (posIdx < 0) posIdx = 0

        root.addView(TextView(this).apply {
            text = "悬浮窗位置"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        })
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, positions)
            setSelection(posIdx)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val sp = getSharedPreferences("hw_monitor", MODE_PRIVATE)
                    sp.edit().putString("position", posValues[position].name).apply()
                    sp.edit().putInt("x", 0).putInt("y", 100).apply()
                    restartOverlay()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        })

        // ===== 标题：监控项 =====
        root.addView(TextView(this).apply {
            text = "── 监控项 ──"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 32, 0, 16)
        })

        metrics.forEach { m ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 16)
            }

            row.addView(Switch(this).apply {
                isChecked = m.enabled
                setOnCheckedChangeListener { _, c ->
                    m.enabled = c
                    ConfigStore.saveMetrics(this@MainActivity, metrics)
                    restartOverlay()
                }
            })

            // 分组标签：CPU / GPU / MEM / BAT
            row.addView(TextView(this).apply {
                text = m.name
                width = 120
                textSize = 14f
                setTextColor(0xFF333333.toInt())
            })

            // 具体含义：占用 / 频率 / 温度 / 功耗
            row.addView(TextView(this).apply {
                text = m.label
                width = 100
                textSize = 13f
                setTextColor(0xFF888888.toInt())
            })

            row.addView(EditText(this).apply {
                setText(m.path)
                hint = "path"
                width = 500
                textSize = 12f
                inputType = InputType.TYPE_TEXT_VARIATION_URI
                setOnFocusChangeListener { _, has ->
                    if (!has) {
                        m.path = text.toString()
                        ConfigStore.saveMetrics(this@MainActivity, metrics)
                        restartOverlay()
                    }
                }
            })

            root.addView(row)
        }

        // ===== 操作按钮 =====
        root.addView(Button(this).apply {
            text = "重置为默认路径"
            setOnClickListener {
                ConfigStore.reset(this@MainActivity)
                Toast.makeText(this@MainActivity, "重启 App 生效", Toast.LENGTH_LONG).show()
            }
        })

        root.addView(Button(this).apply {
            text = "启动悬浮窗"
            setOnClickListener {
                startService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })

        root.addView(Button(this).apply {
            text = "停止悬浮窗"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun restartOverlay() {
        try {
            stopService(Intent(this@MainActivity, OverlayService::class.java))
            startService(Intent(this@MainActivity, OverlayService::class.java))
        } catch (_: Exception) {
        }
    }
}
