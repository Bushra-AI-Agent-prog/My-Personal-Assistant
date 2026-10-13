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

class GeminiProvider : AIProvider {
    override val providerType: AIProviderType = AIProviderType.GEMINI

    override val availableModels: List<AIModel> = listOf(
        AIModel(
            id = "gemini-2.5-flash",
            name = "Gemini 2.5 Flash",
            providerType = AIProviderType.GEMINI,
            description = "Fast, efficient, highly responsive model with multimodal vision support",
            supportsVision = true
        ),
        AIModel(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            providerType = AIProviderType.GEMINI,
            description = "Next-generation model for general reasoning and fast conversations",
            supportsVision = true
        ),
        AIModel(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro (Preview)",
            providerType = AIProviderType.GEMINI,
            description = "Deep reasoning and complex technical analysis",
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
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing. Please enter your key."))
            }
            val model = modelId?.ifBlank { "gemini-2.5-flash" } ?: "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${apiKey.trim()}"

            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Respond with one word: Verified") })
                        })
                    })
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorDetail = parseErrorMessage(responseBody, response.code)
                        return@withContext Result.failure(Exception(errorDetail))
                    }
                    val json = JSONObject(responseBody)
                    val text = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")?.trim() ?: "Connected"
                    Result.success("Connection verified successfully! ($model replied: $text)")
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network or connection failure: ${e.localizedMessage}"))
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

        val targetModel = modelId.ifBlank { "gemini-2.5-flash" }
        val isStreaming = onChunk != null
        val endpoint = if (isStreaming) "streamGenerateContent?alt=sse&key=${apiKey.trim()}" else "generateContent?key=${apiKey.trim()}"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:$endpoint"

        // Build system instructions
        val systemPrompt = buildSystemInstruction(personality, memories)

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            val contentsArray = JSONArray()

            // Include last 10 previous turns for context
            val recentHistory = conversationHistory.takeLast(10)
            for (msg in recentHistory) {
                if (msg.content.isBlank()) continue
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                })
            }

            // Current user turn with optional image
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                val partsArray = JSONArray()
                if (prompt.isNotBlank()) {
                    partsArray.put(JSONObject().apply { put("text", prompt) })
                }
                if (!imageBase64.isNullOrBlank()) {
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType ?: "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }
                put("parts", partsArray)
            })

            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = parseErrorMessage(errorBody, response.code)
                    return@withContext Result.failure(Exception(errorMsg))
                }

                if (isStreaming) {
                    val fullResponseBuilder = StringBuilder()
                    response.body?.byteStream()?.bufferedReader()?.use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line?.trim() ?: continue
                            if (currentLine.startsWith("data:")) {
                                val jsonStr = currentLine.removePrefix("data:").trim()
                                if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                                try {
                                    val chunkObj = JSONObject(jsonStr)
                                    val textChunk = chunkObj.optJSONArray("candidates")
                                        ?.optJSONObject(0)
                                        ?.optJSONObject("content")
                                        ?.optJSONArray("parts")
                                        ?.optJSONObject(0)
                                        ?.optString("text")
                                    if (!textChunk.isNullOrEmpty()) {
                                        fullResponseBuilder.append(textChunk)
                                        onChunk(textChunk)
                                    }
                                } catch (_: Exception) {
                                    // continue streaming even if single chunk parse fails
                                }
                            }
                        }
                    }
                    val resultText = fullResponseBuilder.toString()
                    if (resultText.isNotBlank()) {
                        Result.success(resultText)
                    } else {
                        Result.failure(Exception("Gemini returned an empty response."))
                    }
                } else {
                    val bodyString = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyString)
                    val text = jsonObj.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")

                    if (!text.isNullOrBlank()) {
                        Result.success(text)
                    } else {
                        Result.failure(Exception("No content returned from Gemini model."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gemini communication failed"))
        }
    }

    private fun buildSystemInstruction(personality: VoicePersonality, memories: List<UserMemory>): String {
        val builder = StringBuilder()
        builder.append(personality.promptInstruction)
        builder.append("\n\nAlways maintain this personality consistently.")
        if (memories.isNotEmpty()) {
            builder.append("\n\nKnown facts about the user:")
            for (mem in memories) {
                builder.append("\n- ").append(mem.fact)
            }
        }
        return builder.toString()
    }

    private fun parseErrorMessage(errorBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(errorBody)
            val err = json.optJSONObject("error")
            val message = err?.optString("message")
            val status = err?.optString("status")
            when {
                statusCode == 400 && message?.contains("API_KEY_INVALID", true) == true ->
                    "Invalid Gemini API key. Please check your key in Settings."
                statusCode == 429 ->
                    "Gemini quota exceeded or rate limit reached. Please wait a moment or check your Google AI account."
                statusCode == 403 ->
                    "Access denied: Please verify your Gemini API key permissions."
                !message.isNullOrBlank() -> "Gemini API Error ($statusCode): $message"
                else -> "HTTP $statusCode: ${status ?: "Unknown error"}"
            }
        } catch (_: Exception) {
            "HTTP error $statusCode: $errorBody"
        }
    }
}
