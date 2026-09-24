package com.can186.hwmonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var metrics: MutableList<Metric>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        metrics = ConfigStore.loadMetrics(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

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
                }
            })
            row.addView(TextView(this).apply {
                text = m.name; width = 120
            })
            row.addView(EditText(this).apply {
                setText(m.path); hint = "路径"; width = 500
                setOnFocusChangeListener { _, has ->
                    if (!has) {
                        m.path = text.toString()
                        ConfigStore.saveMetrics(this@MainActivity, metrics)
                    }
                }
            })
            root.addView(row)
        }

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
}
