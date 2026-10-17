package com.ikazev.answerbubble

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject

class CaptureActivity : AppCompatActivity() {
    private val requestCapture = 101
    private lateinit var resultText: TextView
    private lateinit var progressText: TextView
    private lateinit var targetScoreInput: EditText
    private lateinit var autoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(AutomationAccessibilityService.PREFS, MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF101018.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Ikazev • Question Reader"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Target nilai hanya untuk mencatat tujuan sesi. Capture memerlukan persetujuan Android. Isi otomatis hanya berjalan pada package kuis milikmu yang sudah kamu izinkan di pengaturan aplikasi."
            textSize = 13f
            setTextColor(0xFFB7B7C5.toInt())
            setPadding(0, dp(8), 0, dp(12))
        })
        targetScoreInput = EditText(this).apply {
            hint = "Target nilai (1–100)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("target_score", 80).toString())
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8C8C9A.toInt())
        }
        root.addView(targetScoreInput)
        root.addView(Button(this).apply {
            text = "Capture layar & baca soal"
            isAllCaps = false
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                @Suppress("DEPRECATION")
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        })
        autoButton = Button(this).apply {
            isAllCaps = false
            text = if (prefs.getBoolean("automation_armed", false)) "Matikan isi otomatis" else "Izinkan isi otomatis"
            setOnClickListener {
                val enabled = !prefs.getBoolean("automation_armed", false)
                prefs.edit().putBoolean("automation_armed", enabled).apply()
                text = if (enabled) "Matikan isi otomatis" else "Izinkan isi otomatis"
                Toast.makeText(this@CaptureActivity, if (enabled) "Isi otomatis diaktifkan untuk package yang diizinkan." else "Isi otomatis dimatikan.", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(autoButton)
        root.addView(Button(this).apply {
            text = "Buka pengaturan Accessibility"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        progressText = TextView(this).apply {
            text = "Siap • aksi otomatis tercatat: ${prefs.getInt("answered_count", 0)}"
            setTextColor(0xFF25F4EE.toInt())
            setPadding(0, dp(10), 0, dp(4))
        }
        root.addView(progressText)
        resultText = TextView(this).apply {
            text = "Hasil OCR akan tampil di sini."
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(12))
        }
        val scroll = ScrollView(this)
        scroll.addView(resultText)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "Analisis soal dengan AI"
            isAllCaps = false
            setOnClickListener {
                val text = resultText.text.toString()
                if (text.isBlank() || text == "Hasil OCR akan tampil di sini.") {
                    Toast.makeText(this@CaptureActivity, "Capture soal terlebih dahulu.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val target = targetScoreInput.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 80
                prefs.edit().putInt("target_score", target).apply()
                progressText.text = "Menganalisis soal…"
                Thread {
                    val raw = AnswerEngine.answer(this@CaptureActivity, text, target)
                    runOnUiThread {
                        progressText.text = "Analisis selesai. Periksa jawaban dan package target sebelum mengaktifkan otomatisasi."
                        val structured = AnswerEngine.parseStructuredAnswer(raw)
                        if (structured != null && structured.optString("answer").isNotBlank()) {
                            prefs.edit().putString("pending_answer", structured.toString()).apply()
                            val explanation = structured.optString("explanation")
                            val confidence = (structured.optDouble("confidence", 0.0) * 100).toInt().coerceIn(0, 100)
                            resultText.text = "$text\n\n— Jawaban AI —\n${structured.optString("answer")}\n\nPenjelasan: $explanation\nKeyakinan AI: $confidence%\n\nJawaban disiapkan. Isi otomatis hanya aktif pada package target yang kamu konfigurasi."
                        } else {
                            resultText.text = "$text\n\n— Respons AI —\n$raw"
                        }
                    }
                }.start()
            }
        })
        setContentView(root)
    }

    @Deprecated("Legacy result callback kept for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != requestCapture) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            progressText.text = "Capture dibatalkan."
            return
        }
        progressText.text = "Capture layar dan OCR berjalan…"
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("projectionData", data)
        }
        ScreenCaptureService.onOcrResult = { text ->
            runOnUiThread {
                resultText.text = text.ifBlank { "Teks tidak terbaca. Coba layar yang lebih jelas." }
                progressText.text = "OCR selesai. Periksa teks sebelum mengirimnya ke AI."
            }
        }
        try {
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            progressText.text = "Capture gagal: ${e.message}"
            ScreenCaptureService.onOcrResult = null
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
