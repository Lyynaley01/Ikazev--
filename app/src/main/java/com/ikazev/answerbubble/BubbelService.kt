package com.ikazev.answerbubble

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class BubbleService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            dp(58), dp(58), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(220) }

        bubble = TextView(this).apply {
            text = "AI"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundResource(android.R.drawable.btn_default)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        touchX = event.rawX; touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        wm.updateViewLayout(bubble, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = kotlin.math.abs(event.rawX - touchX)
                        val dy = kotlin.math.abs(event.rawY - touchY)
                        if (dx < dp(8) && dy < dp(8)) {
                            val i = Intent(this@BubbleService, CaptureActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            startActivity(i)
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        wm.addView(bubble, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() {
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
