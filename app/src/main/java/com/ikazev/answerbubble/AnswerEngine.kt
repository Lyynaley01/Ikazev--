package com.ikazev.answerbubble

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AnswerEngine {
    private const val BACKEND_URL = "https://ikazev-gemini-api.justlyy256.workers.dev/answer"

    fun answer(context: Context, question: String, targetScore: Int): String {
        if (question.isBlank()) return "Pertanyaan kosong."
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(BACKEND_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 45000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = JSONObject()
                .put("question", question)
                .put("targetScore", targetScore.coerceIn(1, 100))
                .toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = JSONObject(response)
            if (status in 200..299) json.optString("answer", "Jawaban AI kosong.")
            else json.optString("error", "Backend gagal (HTTP $status).")
        } catch (e: Exception) {
            "Gagal menghubungi AI: ${e.localizedMessage ?: e.javaClass.simpleName}"
        } finally { connection?.disconnect() }
    }

    fun parseStructuredAnswer(raw: String): JSONObject? = try {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) null else JSONObject(raw.substring(start, end + 1))
    } catch (_: Exception) { null }
}
