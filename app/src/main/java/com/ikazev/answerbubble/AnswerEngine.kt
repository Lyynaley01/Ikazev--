package com.ikazev.answerbubble

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AnswerEngine {
    fun answer(context: Context, question: String): String {
        val key = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("gemini_key", "")?.trim().orEmpty()
        if (key.isBlank()) {
            return "No Gemini API key saved. OCR worked locally; to get an AI answer, add your own optional API key in the app. You can also copy the recognized text into a search engine. Accuracy is not guaranteed."
        }
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val prompt = """
                You are a careful research assistant. Answer the question below in Indonesian unless the question clearly requires another language.
                Give the best-supported answer, briefly explain the reasoning, and state uncertainty when evidence is insufficient. Do not claim certainty without support.
                If this is a multiple-choice question, identify the option and explain why. Do not invent citations or pretend you browsed the web.
                Question text:
                $question
            """.trimIndent()
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("temperature", 0.2).put("maxOutputTokens", 1200))
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) {
                "Gemini request failed (HTTP $code). Check the API key, model availability, connection, and free-tier quota.\n$response"
            } else {
                val json = JSONObject(response)
                json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?: "The AI response did not contain readable text."
            }
        } catch (e: Exception) {
            "Could not contact the AI service: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }
}
