package com.dmax.nexus.ai

import com.dmax.nexus.data.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {

    private const val GROQ_MODEL = "openai/gpt-oss-20b"

    suspend fun sendMessage(
        endpoint: String,
        accessToken: String,
        history: List<ChatMessageEntity>
    ): String = withContext(Dispatchers.IO) {

        val target = endpoint.trim()

        require(target.isNotBlank()) {
            "AI backend is not configured. Open Profile > AI backend."
        }

        val url = URL(target)

        require(url.protocol.equals("https", ignoreCase = true)) {
            "Please configure a valid HTTPS endpoint."
        }

        require(accessToken.isNotBlank()) {
            "API key is missing. Open Profile > AI backend."
        }

        val lastUser = history.lastOrNull { it.role == "user" }
            ?.content
            .orEmpty()

        require(lastUser.isNotBlank()) {
            "Write a message first."
        }

        val messages = JSONArray()

        history.takeLast(12).forEach { item ->
            if (item.role == "user" || item.role == "assistant") {
                messages.put(
                    JSONObject()
                        .put("role", item.role)
                        .put("content", item.content.take(4000))
                )
            }
        }

        val isGroq =
            url.host.equals("api.groq.com", ignoreCase = true) &&
            url.path.contains("/chat/completions")

        val body = if (isGroq) {
            JSONObject()
                .put("model", GROQ_MODEL)
                .put("messages", messages)
                .put("max_completion_tokens", 1024)
        } else {
            JSONObject()
                .put("message", lastUser.take(4000))
                .put("history", messages)
        }

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true

            setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            setRequestProperty("Accept", "application/json")
            setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            val response = runCatching {
                JSONObject(responseText)
            }.getOrNull()

            if (status !in 200..299) {
                val errorObject = response?.optJSONObject("error")

                val errorMessage = errorObject
                    ?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: response?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                    ?: "AI request failed with HTTP $status."

                throw IllegalStateException(errorMessage.take(500))
            }

            if (response == null) {
                throw IllegalStateException(
                    "The AI backend returned invalid JSON."
                )
            }

            val reply = if (isGroq) {
                response
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
            } else {
                response.optString("reply")
            }

            reply.trim().takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(
                    "The AI backend returned an empty reply."
                )

        } finally {
            connection.disconnect()
        }
    }
}
