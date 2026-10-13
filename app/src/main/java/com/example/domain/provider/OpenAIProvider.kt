package com.example.domain.provider

import com.example.data.model.AIModel
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.UserMemory
import com.example.data.model.VoicePersonality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIProvider : AIProvider {
    override val providerType: AIProviderType = AIProviderType.OPENAI

    override val availableModels: List<AIModel> = listOf(
        AIModel(
            id = "gpt-4o",
            name = "GPT-4o",
            providerType = AIProviderType.OPENAI,
            description = "Flagship high-intelligence model with vision and reasoning",
            supportsVision = true
        ),
        AIModel(
            id = "gpt-4o-mini",
            name = "GPT-4o Mini",
            providerType = AIProviderType.OPENAI,
            description = "Fast, affordable, lightweight model for everyday tasks",
            supportsVision = true
        ),
        AIModel(
            id = "gpt-4-turbo",
            name = "GPT-4 Turbo",
            providerType = AIProviderType.OPENAI,
            description = "Complex technical and coding capabilities",
            supportsVision = true
        )
    )

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(apiKey: String, baseUrl: String?, modelId: String?): Result<String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("OpenAI API key is missing."))
            }
            val model = modelId?.ifBlank { "gpt-4o-mini" } ?: "gpt-4o-mini"
            val targetUrl = (baseUrl?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1").trimEnd('/') + "/chat/completions"

            val bodyJson = JSONObject().apply {
                put("model", model)
                put("max_tokens", 5)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Test")
                    })
                })
            }

            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseErrorMessage(body, response.code)))
                    }
                    Result.success("Connection verified successfully! (OpenAI $model responded)")
                }
            } catch (e: Exception) {
                Result.failure(Exception("Connection failed: ${e.localizedMessage}"))
            }
        }

    override suspend fun sendMessage(
        apiKey: String,
        modelId: String,
        prompt: String,
        conversationHistory: List<ChatMessage>,
        personality: VoicePersonality,
        memories: List<UserMemory>,
        imageBase64: String?,
        mimeType: String?,
        customBaseUrl: String?,
        onChunk: ((String) -> Unit)?
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("AI provider not configured. Add your API key in Settings to continue."))
        }

        val targetModel = modelId.ifBlank { "gpt-4o-mini" }
        val isStreaming = onChunk != null
        val baseUrl = (customBaseUrl?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1").trimEnd('/')
        val url = "$baseUrl/chat/completions"

        val messagesArray = JSONArray()

        // 1. System Prompt
        val systemContent = buildSystemPrompt(personality, memories)
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemContent)
        })

        // 2. Recent History
        val recentHistory = conversationHistory.takeLast(10)
        for (msg in recentHistory) {
            if (msg.content.isBlank()) continue
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.sender == MessageSender.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }

        // 3. User message (multimodal or text)
        if (!imageBase64.isNullOrBlank()) {
            val contentList = JSONArray().apply {
                if (prompt.isNotBlank()) {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                }
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        val format = mimeType ?: "image/jpeg"
                        put("url", "data:$format;base64,$imageBase64")
                    })
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", contentList)
            })
        } else {
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", targetModel)
            put("messages", messagesArray)
            if (isStreaming) {
                put("stream", true)
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception(parseErrorMessage(errBody, response.code)))
                }

                if (isStreaming) {
                    val fullResponse = StringBuilder()
                    response.body?.byteStream()?.bufferedReader()?.use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val cur = line?.trim() ?: continue
                            if (cur.startsWith("data:")) {
                                val data = cur.removePrefix("data:").trim()
                                if (data == "[DONE]") break
                                if (data.isEmpty()) continue
                                try {
                                    val obj = JSONObject(data)
                                    val delta = obj.optJSONArray("choices")
                                        ?.optJSONObject(0)
                                        ?.optJSONObject("delta")
                                        ?.optString("content")
                                    if (!delta.isNullOrEmpty()) {
                                        fullResponse.append(delta)
                                        onChunk(delta)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    val output = fullResponse.toString()
                    if (output.isNotBlank()) Result.success(output) else Result.failure(Exception("Empty response from OpenAI"))
                } else {
                    val bodyString = response.body?.string() ?: ""
                    val json = JSONObject(bodyString)
                    val reply = json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                    if (!reply.isNullOrBlank()) {
                        Result.success(reply)
                    } else {
                        Result.failure(Exception("OpenAI returned no text."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "OpenAI connection error"))
        }
    }

    private fun buildSystemPrompt(personality: VoicePersonality, memories: List<UserMemory>): String {
        val sb = StringBuilder()
        sb.append(personality.promptInstruction)
        if (memories.isNotEmpty()) {
            sb.append("\n\nUser profile memory:")
            for (m in memories) {
                sb.append("\n- ").append(m.fact)
            }
        }
        return sb.toString()
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val err = json.optJSONObject("error")
            val msg = err?.optString("message")
            when {
                code == 401 -> "Invalid OpenAI API key. Check key in Settings."
                code == 429 -> "OpenAI quota limit reached or billing limit exceeded."
                !msg.isNullOrBlank() -> "OpenAI Error ($code): $msg"
                else -> "HTTP $code"
            }
        } catch (_: Exception) {
            "HTTP $code: $body"
        }
    }
}
