package com.dmax.nexus.ai

import com.dmax.nexus.data.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    suspend fun sendMessage(
        endpoint: String,
        accessToken: String,
        history: List<ChatMessageEntity>
    ): String = withContext(Dispatchers.IO) {
        val target = endpoint.trim()
        require(target.isNotBlank()) {
            "AI backend is not configured. Open Profile → AI backend to add your HTTPS /api/chat URL."
        }
        val url = URL(target)
        require(url.protocol.equals("https", ignoreCase = true)) {
            "Use an HTTPS endpoint. Configure TLS on the AI backend before connecting the app."
        }

        val historyJson = JSONArray()
        history.takeLast(12).forEach { item ->
            if (item.role == "user" || item.role == "assistant") {
                historyJson.put(JSONObject().put("role", item.role).put("content", item.content.take(4000)))
            }
        }
        val lastUser = history.lastOrNull { it.role == "user" }?.content.orEmpty()
        require(lastUser.isNotBlank()) { "Write a message first." }
        val body = JSONObject()
            .put("message", lastUser.take(4000))
            .put("history", historyJson)

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (accessToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $accessToken")
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrNull()
            if (status !in 200..299) {
                val message = response?.optString("error").orEmpty().ifBlank { "HTTP $status from AI backend." }
                throw IllegalStateException(message.take(400))
            }
            response?.optString("reply")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The AI backend returned an empty reply.")
        } finally {
            connection.disconnect()
        }
    }
}
