package com.ikazev.answerbubble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 1001)
            return
        }
        startService(Intent(this, BubbleService::class.java))
        Toast.makeText(this, "Bubble diaktifkan", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(AutomationAccessibilityService.PREFS, MODE_PRIVATE)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "Ikazev Answer Bubble"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "Baca soal dari layar, analisis dengan AI, dan opsional isi jawaban pada aplikasi kuis milikmu sendiri."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 20)
        })
        val packageInput = EditText(this).apply {
            hint = "Package ID aplikasi kuis milikmu"
            setText(prefs.getString("target_package", "").orEmpty())
            textSize = 14f
        }
        layout.addView(packageInput, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        layout.addView(Button(this).apply {
            text = "Simpan package target"
            isAllCaps = false
            setOnClickListener {
                val value = packageInput.text.toString().trim()
                if (!value.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
                    Toast.makeText(this@MainActivity, "Masukkan package ID valid, contoh com.contoh.kuis", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("target_package", value).apply()
                    Toast.makeText(this@MainActivity, "Package target tersimpan: $value", Toast.LENGTH_SHORT).show()
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Aktifkan Bubble"
            isAllCaps = false
            setOnClickListener { startBubble() }
        })
        layout.addView(Button(this).apply {
            text = "Atur Accessibility untuk isi otomatis"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "Matikan semua otomatisasi"
            isAllCaps = false
            setOnClickListener {
                prefs.edit().putBoolean("automation_armed", false).remove("pending_answer").apply()
                Toast.makeText(this@MainActivity, "Isi otomatis dimatikan dan jawaban antrean dihapus.", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(layout)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && Settings.canDrawOverlays(this)) startBubble()
    }
}
