package com.ikazev.answerbubble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1001)
            return
        }

        startService(Intent(this, BubbleService::class.java))
        Toast.makeText(this, "Bubble diaktifkan", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Ikazev Answer Bubble"
            textSize = 24f
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Aktifkan bubble untuk mulai menggunakan fitur."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 28)
        }

        val button = Button(this).apply {
            text = "Aktifkan Bubble"
            setOnClickListener { startBubble() }
        }

        layout.addView(title, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        layout.addView(description)
        layout.addView(button)

        setContentView(layout)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && Settings.canDrawOverlays(this)) {
            startBubble()
        }
    }
}
