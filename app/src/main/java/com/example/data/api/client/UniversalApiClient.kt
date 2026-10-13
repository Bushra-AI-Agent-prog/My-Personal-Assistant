package com.example.data.api.client

import android.os.SystemClock
import com.example.data.api.ApiAuthType
import com.example.data.api.ApiCompatibilityMode
import com.example.data.api.ApiProfile
import com.example.data.api.ApiTestResult
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UniversalApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun executeChat(
        profile: ApiProfile,
        userPrompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        imageBase64: String? = null,
        mimeType: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val fullUrl = profile.getEffectiveFullUrl()
        if (fullUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API endpoint URL is empty in profile: ${profile.displayName}"))
        }

        return@withContext try {
            when (profile.compatibilityMode) {
                ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS -> {
                    executeOpenAiChat(profile, fullUrl, userPrompt, conversationHistory, systemInstruction, imageBase64, mimeType, onChunk)
                }
                ApiCompatibilityMode.GEMINI_GENERATE_CONTENT -> {
                    executeGeminiGenerate(profile, fullUrl, userPrompt, conversationHistory, systemInstruction, imageBase64, mimeType, onChunk)
                }
                ApiCompatibilityMode.OPENAI_RESPONSES -> {
                    executeOpenAiResponses(profile, fullUrl, userPrompt, systemInstruction)
                }
                ApiCompatibilityMode.GENERIC_REST -> {
                    executeGenericRest(profile, fullUrl, userPrompt, conversationHistory, systemInstruction)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("API Error (${profile.displayName}): ${sanitizeErrorMessage(e.localizedMessage ?: "Network failed")}"))
        }
    }

    suspend fun testConnection(profile: ApiProfile): ApiTestResult = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        val fullUrl = profile.getEffectiveFullUrl()

        if (fullUrl.isBlank()) {
            return@withContext ApiTestResult(
                isSuccess = false,
                httpStatusCode = null,
                responseTimeMs = 0,
                responseSample = "",
                diagnosticMessage = "Base URL is not specified."
            )
        }

        try {
            val testPrompt = "Ping"
            val result = executeChat(
                profile = profile,
                userPrompt = testPrompt,
                conversationHistory = emptyList(),
                systemInstruction = "Respond with one short word: OK"
            )
            val elapsed = SystemClock.elapsedRealtime() - start

            if (result.isSuccess) {
                val sample = result.getOrThrow().take(120)
                ApiTestResult(
                    isSuccess = true,
                    httpStatusCode = 200,
                    responseTimeMs = elapsed,
                    responseSample = sample,
                    diagnosticMessage = "Connected successfully in ${elapsed}ms! Response: $sample",
                    resolvedModel = profile.modelId.ifBlank { "Default" }
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown test failure"
                ApiTestResult(
                    isSuccess = false,
                    httpStatusCode = extractStatusCode(err),
                    responseTimeMs = elapsed,
                    responseSample = "",
                    diagnosticMessage = sanitizeErrorMessage(err),
                    resolvedModel = profile.modelId
                )
            }
        } catch (e: Exception) {
            val elapsed = SystemClock.elapsedRealtime() - start
            ApiTestResult(
                isSuccess = false,
                httpStatusCode = null,
                responseTimeMs = elapsed,
                responseSample = "",
                diagnosticMessage = "Connection failed: ${sanitizeErrorMessage(e.localizedMessage ?: "Unknown network error")}"
            )
        }
    }

    private fun executeOpenAiChat(
        profile: ApiProfile,
        url: String,
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        imageBase64: String?,
        mimeType: String?,
        onChunk: ((String) -> Unit)?
    ): Result<String> {
        val messagesArray = JSONArray()

        if (systemInstruction.isNotBlank()) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }

        for (msg in history.takeLast(10)) {
            if (msg.content.isBlank()) continue
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.sender == MessageSender.USER) "user" else "assistant")
                put("content", msg.content)
            })
        }

        if (!imageBase64.isNullOrBlank()) {
            val contentParts = JSONArray().apply {
                if (prompt.isNotBlank()) {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                }
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        val fmt = mimeType ?: "image/jpeg"
                        put("url", "data:$fmt;base64,$imageBase64")
                    })
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", contentParts)
            })
        } else {
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", profile.modelId.ifBlank { "gpt-4o-mini" })
            put("messages", messagesArray)
            if (onChunk != null) {
                put("stream", true)
            }
        }

        val requestBuilder = Request.Builder().url(url)
        applyAuthHeaders(requestBuilder, profile)
        applyCustomHeaders(requestBuilder, profile.customHeadersJson)

        val request = requestBuilder
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $bodyStr"))
            }

            val extracted = JsonPathExtractor.extract(bodyStr, profile.responseJsonPath.ifBlank { "choices[0].message.content" })
            return if (!extracted.isNullOrBlank()) {
                Result.success(extracted)
            } else {
                Result.failure(Exception("Could not extract content from model response: $bodyStr"))
            }
        }
    }

    private fun executeGeminiGenerate(
        profile: ApiProfile,
        url: String,
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        imageBase64: String?,
        mimeType: String?,
        onChunk: ((String) -> Unit)?
    ): Result<String> {
        val effectiveUrl = if (!url.contains("key=") && profile.apiKey.isNotBlank()) {
            val sep = if (url.contains("?")) "&" else "?"
            "$url${sep}key=${profile.apiKey.trim()}"
        } else url

        val requestJson = JSONObject().apply {
            if (systemInstruction.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }

            val contentsArray = JSONArray()
            for (msg in history.takeLast(10)) {
                if (msg.content.isBlank()) continue
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                if (prompt.isNotBlank()) {
                    parts.put(JSONObject().apply { put("text", prompt) })
                }
                if (!imageBase64.isNullOrBlank()) {
                    parts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType ?: "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }
                put("parts", parts)
            })

            put("contents", contentsArray)
        }

        val requestBuilder = Request.Builder().url(effectiveUrl)
        applyCustomHeaders(requestBuilder, profile.customHeadersJson)
        val request = requestBuilder
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }
            val extracted = JsonPathExtractor.extract(body, profile.responseJsonPath.ifBlank { "candidates[0].content.parts[0].text" })
            return if (!extracted.isNullOrBlank()) {
                Result.success(extracted)
            } else {
                Result.failure(Exception("No content returned from Gemini model"))
            }
        }
    }

    private fun executeOpenAiResponses(
        profile: ApiProfile,
        url: String,
        prompt: String,
        systemInstruction: String
    ): Result<String> {
        val requestJson = JSONObject().apply {
            put("model", profile.modelId.ifBlank { "gpt-4o" })
            put("input", prompt)
            if (systemInstruction.isNotBlank()) {
                put("instructions", systemInstruction)
            }
        }

        val requestBuilder = Request.Builder().url(url)
        applyAuthHeaders(requestBuilder, profile)
        applyCustomHeaders(requestBuilder, profile.customHeadersJson)
        val request = requestBuilder
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}: $body"))
            val text = JsonPathExtractor.extract(body, profile.responseJsonPath.ifBlank { "output" })
            return if (!text.isNullOrBlank()) Result.success(text) else Result.failure(Exception("Empty responses output: $body"))
        }
    }

    private fun executeGenericRest(
        profile: ApiProfile,
        url: String,
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String
    ): Result<String> {
        var processedUrl = url
            .replace("{{api_key}}", profile.apiKey.trim())
            .replace("{{model}}", profile.modelId.trim())
            .replace("{{user_message}}", prompt)

        var requestBody: RequestBody? = null

        val method = profile.httpMethod.uppercase()
        if (method in listOf("POST", "PUT", "PATCH")) {
            val template = profile.customRequestBodyTemplate.ifBlank {
                """{"prompt":"{{user_message}}","model":"{{model}}"}"""
            }

            val historySummary = history.takeLast(6).joinToString("\\n") {
                "${it.sender.name}: ${escapeJson(it.content)}"
            }

            val substitutedBody = template
                .replace("{{api_key}}", escapeJson(profile.apiKey.trim()))
                .replace("{{model}}", escapeJson(profile.modelId.trim()))
                .replace("{{user_message}}", escapeJson(prompt))
                .replace("{{system_prompt}}", escapeJson(systemInstruction))
                .replace("{{conversation_history}}", historySummary)

            requestBody = substitutedBody.toRequestBody("application/json".toMediaType())
        }

        val requestBuilder = Request.Builder().url(processedUrl)
        applyAuthHeaders(requestBuilder, profile)
        applyCustomHeaders(requestBuilder, profile.customHeadersJson)

        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            "PUT" -> requestBuilder.put(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            "PATCH" -> requestBuilder.patch(requestBody ?: "".toRequestBody("application/json".toMediaType()))
            "DELETE" -> requestBuilder.delete()
        }

        val request = requestBuilder.build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: $body"))
            }

            val extracted = JsonPathExtractor.extract(body, profile.responseJsonPath)
            return if (!extracted.isNullOrBlank()) {
                Result.success(extracted)
            } else {
                Result.failure(Exception("Could not extract response from JSON body"))
            }
        }
    }

    private fun applyAuthHeaders(builder: Request.Builder, profile: ApiProfile) {
        val key = profile.apiKey.trim()
        when (profile.authType) {
            ApiAuthType.BEARER_TOKEN -> {
                if (key.isNotBlank()) builder.addHeader("Authorization", "Bearer $key")
            }
            ApiAuthType.API_KEY_HEADER -> {
                if (key.isNotBlank()) {
                    val headerName = profile.authHeaderName.ifBlank { "x-api-key" }
                    builder.addHeader(headerName, key)
                }
            }
            ApiAuthType.BASIC_AUTH -> {
                if (key.isNotBlank()) {
                    val encoded = android.util.Base64.encodeToString(key.toByteArray(), android.util.Base64.NO_WRAP)
                    builder.addHeader("Authorization", "Basic $encoded")
                }
            }
            ApiAuthType.API_KEY_QUERY_PARAM, ApiAuthType.NO_AUTH, ApiAuthType.CUSTOM_HEADER -> {
                // Handled via URL or custom headers
            }
        }
    }

    private fun applyCustomHeaders(builder: Request.Builder, customHeadersJson: String) {
        if (customHeadersJson.isBlank() || customHeadersJson == "{}") return
        try {
            val obj = JSONObject(customHeadersJson)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k)
                if (k.isNotBlank() && v.isNotBlank()) {
                    builder.addHeader(k, v)
                }
            }
        } catch (_: Exception) {}
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractStatusCode(msg: String): Int? {
        val match = Regex("""HTTP\s*(\d{3})""").find(msg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    fun sanitizeErrorMessage(message: String): String {
        // Redact any possible raw API keys (e.g. sk-..., AIzaSy...)
        return message
            .replace(Regex("""sk-[a-zA-Z0-9_-]{10,}"""), "sk-***")
            .replace(Regex("""AIzaSy[a-zA-Z0-9_-]{10,}"""), "AIzaSy***")
            .replace(Regex("""Bearer\s+[a-zA-Z0-9_.-]+"""), "Bearer ***")
    }
}
