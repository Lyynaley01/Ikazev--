package com.ikazev.answerbubble

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CaptureActivity : AppCompatActivity() {
    private val requestCapture = 101
    private lateinit var resultText: TextView
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFF101018.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Read a question"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Android will ask for screen-capture permission. Only continue if you consent to capturing the currently visible screen."
            textSize = 13f
            setTextColor(0xFFB7B7C5.toInt())
            setPadding(0, dp(8), 0, dp(12))
        })
        root.addView(Button(this).apply {
            text = "Capture screen and read text"
            isAllCaps = false
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                @Suppress("DEPRECATION")
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        })
        root.addView(Button(this).apply {
            text = "Close"
            isAllCaps = false
            setOnClickListener { finish() }
        })
        progressText = TextView(this).apply {
            text = ""
            setTextColor(0xFF25F4EE.toInt())
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(progressText)
        resultText = TextView(this).apply {
            text = "OCR result will appear here."
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(12))
        }
        val scroll = ScrollView(this)
        scroll.addView(resultText)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "Ask Gemini about this text"
            isAllCaps = false
            setOnClickListener {
                val text = resultText.text.toString()
                if (text.isBlank() || text == "OCR result will appear here.") {
                    Toast.makeText(this@CaptureActivity, "Capture a question first.", Toast.LENGTH_SHORT).show()
                } else {
                    progressText.text = "Checking answer..."
                    Thread {
                        val answer = AnswerEngine.answer(this@CaptureActivity, text)
                        runOnUiThread { progressText.text = ""; resultText.text = "$text\n\n--- AI answer ---\n$answer" }
                    }.start()
                }
            }
        })
        setContentView(root)
    }

    @Deprecated("Legacy result callback kept for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != requestCapture) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            progressText.text = "Screen capture cancelled."
            return
        }
        progressText.text = "Capturing and running on-device OCR..."
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("projectionData", data)
        }
        ScreenCaptureService.onOcrResult = { text ->
            runOnUiThread {
                resultText.text = text.ifBlank { "No readable text found. Try a clearer screen." }
                progressText.text = "OCR complete. Review the text before sending it to AI."
            }
        }
        try {
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            progressText.text = "Could not start capture: ${e.message}"
            ScreenCaptureService.onOcrResult = null
            return
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
