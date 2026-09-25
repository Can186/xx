package com.can186.hwmonitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var textView: TextView
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var metrics: MutableList<Metric> = mutableListOf()
    private var running = false

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            MetricsProvider.refreshAll(metrics)
            updateView()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        metrics = ConfigStore.loadMetrics(this)
        createOverlay()
        startForegroundNotification()
        running = true
        handler.post(loop)
    }
    override fun onTouch(v: View, e: MotionEvent): Boolean {
    when (e.action) {
        MotionEvent.ACTION_DOWN -> { /* ... */ return true }
        MotionEvent.ACTION_MOVE -> { /* ... */ return true }
        MotionEvent.ACTION_UP -> { /* ... */ return true }
        else -> return false // 补全 else
    }
    }
    
    private fun createOverlay() {
        val alpha = ConfigStore.loadOverlayAlpha(this)
        val font = ConfigStore.loadOverlayFont(this)
        val textColor = ConfigStore.loadOverlayTextColor(this)
        val position = ConfigStore.loadOverlayPosition(this)
        val xy = ConfigStore.loadOverlayXY(this)

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.argb((alpha * 255).toInt(), 0, 0, 0))
        }

        textView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, font)
            setLineSpacing(0f, 1.15f)
            text = "loading..."
        }
        overlayView.addView(textView)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        private fun applyPosition(pos: OverlayPosition, x: Int, y: Int) {
    when (pos) {
        OverlayPosition.TOP_LEFT -> {
            params.gravity = Gravity.TOP or Gravity.START
            params.x = x; params.y = y
        }
        OverlayPosition.TOP_RIGHT -> {
            params.gravity = Gravity.TOP or Gravity.END
            params.x = x; params.y = y
        }
        OverlayPosition.BOTTOM_LEFT -> {
            params.gravity = Gravity.BOTTOM or Gravity.START
            params.x = x; params.y = y
        }
        OverlayPosition.BOTTOM_RIGHT -> { // 补全这个分支
            params.gravity = Gravity.BOTTOM or Gravity.END
            params.x = x; params.y = y
        }
    }
        }

        windowManager.addView(overlayView, params)
    }

    private fun applyPosition(pos: OverlayPosition, x: Int, y: Int) {
        when (pos) {
            OverlayPosition.TOP_LEFT -> {
                params.gravity = Gravity.TOP or Gravity.START
                params.x = x; params.y = y
            }
            OverlayPosition.TOP_RIGHT -> {
                params.gravity = Gravity.TOP or Gravity.END
                params.x = x; params.y = y
            }
            OverlayPosition.BOTTOM_LEFT -> {
                params.gravity = Gravity.BOTTOM or Gravity.START
                params.x = x; params.y = y
            }
        }
    }

    private fun updateView() {
        val sb = StringBuilder()
        val enabled = metrics.filter { it.enabled }
        enabled.groupBy { it.name }.forEach { (label, items) ->
            sb.append(String.format("%-4s", label))
            items.forEach { m ->
                sb.append(" ").append(m.value)
            }
            sb.append("\n")
        }
        textView.text = sb.toString().trimEnd()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun startForegroundNotification() {
        val ch = "hw_monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(ch) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        ch, "HW Monitor",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
            val n = Notification.Builder(this, ch)
                .setContentTitle("HW Monitor")
                .setContentText("running")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, n)
            }
        }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(loop)
        try {
            windowManager.removeView(overlayView)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
