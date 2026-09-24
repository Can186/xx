package com.can186.hwmonitor

import android.app.*
import android.content.Context
import android.content.Intent
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

    private fun createOverlay() {
        val cfg = ConfigStore.loadOverlay(this)

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.argb((cfg[2] * 255).toInt(), 0, 0, 0))
        }

        textView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg[3])
            setLineSpacing(0f, 1.1f)
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
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cfg[0].toInt()
            y = cfg[1].toInt()
        }

        overlayView.setOnTouchListener(object : View.OnTouchListener {
            private var initX = 0
            private var initY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = params.x; initY = params.y
                        touchX = e.rawX; touchY = e.rawY; return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initX + (e.rawX - touchX).toInt()
                        params.y = initY + (e.rawY - touchY).toInt()
                        windowManager.updateViewLayout(overlayView, params); return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val old = ConfigStore.loadOverlay(this@OverlayService)
                        ConfigStore.saveOverlay(this@OverlayService,
                            params.x, params.y, old[2], old[3])
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(overlayView, params)
    }

    private fun updateView() {
        val sb = StringBuilder()
        metrics.filter { it.enabled }.forEach { m ->
            sb.append(String.format("%-4s %s\n", m.name, m.value))
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
                    NotificationChannel(ch, "HW Monitor",
                        NotificationManager.IMPORTANCE_LOW)
                )
            }
            val n = Notification.Builder(this, ch)
                .setContentTitle("HW Monitor")
                .setContentText("running")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()
            startForeground(1, n)
        }
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(loop)
        try { windowManager.removeView(overlayView) } catch (_: Exception) {}
        super.onDestroy()
    }
}
