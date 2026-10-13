package com.example.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.client.UniversalApiClient
import com.example.data.local.dao.ApiProfileDao
import com.example.data.local.entity.ApiProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.routingDataStore by preferencesDataStore(name = "bushra_api_routing")

class ApiProfileRepository(
    private val context: Context,
    private val apiProfileDao: ApiProfileDao
) {

    companion object {
        private val KEY_PRIMARY_CHAT_ID = stringPreferencesKey("primary_chat_profile_id")
        private val KEY_FALLBACK_CHAT_ID = stringPreferencesKey("fallback_chat_profile_id")
        private val KEY_VISION_ID = stringPreferencesKey("vision_profile_id")
        private val KEY_SEARCH_ID = stringPreferencesKey("search_profile_id")
        private val KEY_STT_ID = stringPreferencesKey("stt_profile_id")
        private val KEY_TTS_ID = stringPreferencesKey("tts_profile_id")
    }

    val routingConfigFlow: Flow<ApiRoutingConfig> = context.routingDataStore.data.map { prefs ->
        ApiRoutingConfig(
            primaryChatProfileId = prefs[KEY_PRIMARY_CHAT_ID],
            fallbackChatProfileId = prefs[KEY_FALLBACK_CHAT_ID],
            visionProfileId = prefs[KEY_VISION_ID],
            webSearchProfileId = prefs[KEY_SEARCH_ID],
            speechToTextProfileId = prefs[KEY_STT_ID],
            textToSpeechProfileId = prefs[KEY_TTS_ID]
        )
    }

    fun getAllProfiles(): Flow<List<ApiProfile>> {
        return apiProfileDao.getAllProfiles().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getProfileById(id: String): ApiProfile? = withContext(Dispatchers.IO) {
        apiProfileDao.getProfileById(id)?.toDomain()
    }

    suspend fun saveProfile(profile: ApiProfile) = withContext(Dispatchers.IO) {
        apiProfileDao.insertOrUpdate(ApiProfileEntity.fromDomain(profile))
    }

    suspend fun deleteProfile(id: String) = withContext(Dispatchers.IO) {
        apiProfileDao.deleteById(id)
    }

    suspend fun testConnection(profile: ApiProfile): ApiTestResult = withContext(Dispatchers.IO) {
        val testResult = UniversalApiClient.testConnection(profile)
        // Update profile in DB with last test result
        val updated = profile.copy(
            lastTestStatus = if (testResult.isSuccess) "Success (200 OK)" else "Failed: ${testResult.diagnosticMessage.take(40)}",
            lastTestResponseTimeMs = testResult.responseTimeMs,
            lastTestTimestamp = System.currentTimeMillis()
        )
        apiProfileDao.insertOrUpdate(ApiProfileEntity.fromDomain(updated))
        testResult
    }

    suspend fun saveRoutingConfig(config: ApiRoutingConfig) {
        context.routingDataStore.edit { prefs ->
            if (config.primaryChatProfileId != null) prefs[KEY_PRIMARY_CHAT_ID] = config.primaryChatProfileId else prefs.remove(KEY_PRIMARY_CHAT_ID)
            if (config.fallbackChatProfileId != null) prefs[KEY_FALLBACK_CHAT_ID] = config.fallbackChatProfileId else prefs.remove(KEY_FALLBACK_CHAT_ID)
            if (config.visionProfileId != null) prefs[KEY_VISION_ID] = config.visionProfileId else prefs.remove(KEY_VISION_ID)
            if (config.webSearchProfileId != null) prefs[KEY_SEARCH_ID] = config.webSearchProfileId else prefs.remove(KEY_SEARCH_ID)
            if (config.speechToTextProfileId != null) prefs[KEY_STT_ID] = config.speechToTextProfileId else prefs.remove(KEY_STT_ID)
            if (config.textToSpeechProfileId != null) prefs[KEY_TTS_ID] = config.textToSpeechProfileId else prefs.remove(KEY_TTS_ID)
        }
    }

    suspend fun getEffectiveProfileForCategory(category: ApiCategory): ApiProfile? = withContext(Dispatchers.IO) {
        val routing = routingConfigFlow.first()
        val targetId = when (category) {
            ApiCategory.CHAT_REASONING -> routing.primaryChatProfileId
            ApiCategory.VISION -> routing.visionProfileId ?: routing.primaryChatProfileId
            ApiCategory.WEB_SEARCH -> routing.webSearchProfileId
            ApiCategory.SPEECH_TO_TEXT -> routing.speechToTextProfileId
            ApiCategory.TEXT_TO_SPEECH -> routing.textToSpeechProfileId
            else -> null
        }

        if (!targetId.isNullOrBlank()) {
            val p = apiProfileDao.getProfileById(targetId)?.toDomain()
            if (p != null && p.isEnabled && p.isConfigured) return@withContext p
        }

        // Fallback: search for first enabled & configured profile matching category, or any chat profile
        val all = apiProfileDao.getAllProfilesList().map { it.toDomain() }
        all.firstOrNull { it.category == category && it.isEnabled && it.isConfigured }
            ?: all.firstOrNull { it.category == ApiCategory.CHAT_REASONING && it.isEnabled && it.isConfigured }
    }

    suspend fun seedInitialProfilesIfEmpty() = withContext(Dispatchers.IO) {
        val count = apiProfileDao.getAllProfilesList().size
        if (count == 0) {
            val defaults = listOf(
                ApiProfile(
                    id = "gemini-default",
                    displayName = "Google Gemini Flash",
                    providerName = "Google AI Studio",
                    category = ApiCategory.CHAT_REASONING,
                    compatibilityMode = ApiCompatibilityMode.GEMINI_GENERATE_CONTENT,
                    baseUrl = "https://generativelanguage.googleapis.com",
                    endpointPath = "/v1beta/models/{model}:generateContent",
                    authType = ApiAuthType.API_KEY_QUERY_PARAM,
                    modelId = "gemini-2.5-flash",
                    isDefault = true
                ),
                ApiProfile(
                    id = "openai-default",
                    displayName = "OpenAI GPT-4o Mini",
                    providerName = "OpenAI",
                    category = ApiCategory.CHAT_REASONING,
                    compatibilityMode = ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS,
                    baseUrl = "https://api.openai.com",
                    endpointPath = "/v1/chat/completions",
                    authType = ApiAuthType.BEARER_TOKEN,
                    modelId = "gpt-4o-mini"
                ),
                ApiProfile(
                    id = "deepseek-default",
                    displayName = "DeepSeek Chat",
                    providerName = "DeepSeek",
                    category = ApiCategory.CHAT_REASONING,
                    compatibilityMode = ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS,
                    baseUrl = "https://api.deepseek.com",
                    endpointPath = "/v1/chat/completions",
                    authType = ApiAuthType.BEARER_TOKEN,
                    modelId = "deepseek-chat"
                ),
                ApiProfile(
                    id = "ollama-local",
                    displayName = "Local Ollama LLM",
                    providerName = "Ollama (Local / Offline)",
                    category = ApiCategory.CHAT_REASONING,
                    compatibilityMode = ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS,
                    baseUrl = "http://10.0.2.2:11434",
                    endpointPath = "/v1/chat/completions",
                    authType = ApiAuthType.NO_AUTH,
                    modelId = "llama3.2"
                ),
                ApiProfile(
                    id = "openrouter-default",
                    displayName = "OpenRouter Universal",
                    providerName = "OpenRouter",
                    category = ApiCategory.CHAT_REASONING,
                    compatibilityMode = ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS,
                    baseUrl = "https://openrouter.ai/api",
                    endpointPath = "/v1/chat/completions",
                    authType = ApiAuthType.BEARER_TOKEN,
                    modelId = "meta-llama/llama-3.3-70b-instruct:free"
                )
            )
            for (p in defaults) {
                apiProfileDao.insertOrUpdate(ApiProfileEntity.fromDomain(p))
            }
            saveRoutingConfig(ApiRoutingConfig(primaryChatProfileId = "gemini-default", fallbackChatProfileId = "openai-default"))
        }
    }

    suspend fun exportTemplatesJson(): String = withContext(Dispatchers.IO) {
        val list = apiProfileDao.getAllProfilesList().map { it.toDomain() }
        val arr = JSONArray()
        for (p in list) {
            val obj = JSONObject().apply {
                put("displayName", p.displayName)
                put("providerName", p.providerName)
                put("category", p.category.name)
                put("compatibilityMode", p.compatibilityMode.name)
                put("baseUrl", p.baseUrl)
                put("endpointPath", p.endpointPath)
                put("httpMethod", p.httpMethod)
                put("authType", p.authType.name)
                put("authHeaderName", p.authHeaderName)
                put("modelId", p.modelId)
                put("customHeadersJson", p.customHeadersJson)
                put("customQueryParamsJson", p.customQueryParamsJson)
                put("customRequestBodyTemplate", p.customRequestBodyTemplate)
                put("responseJsonPath", p.responseJsonPath)
                // Note: API key is intentionally excluded for security!
            }
            arr.put(obj)
        }
        arr.toString(2)
    }

    suspend fun importTemplatesJson(jsonStr: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val arr = JSONArray(jsonStr.trim())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val profile = ApiProfile(
                    id = UUID.randomUUID().toString(),
                    displayName = obj.optString("displayName", "Imported API"),
                    providerName = obj.optString("providerName", "Custom"),
                    category = try { ApiCategory.valueOf(obj.optString("category")) } catch (_: Exception) { ApiCategory.CHAT_REASONING },
                    compatibilityMode = try { ApiCompatibilityMode.valueOf(obj.optString("compatibilityMode")) } catch (_: Exception) { ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS },
                    baseUrl = obj.optString("baseUrl"),
                    endpointPath = obj.optString("endpointPath"),
                    httpMethod = obj.optString("httpMethod", "POST"),
                    authType = try { ApiAuthType.valueOf(obj.optString("authType")) } catch (_: Exception) { ApiAuthType.BEARER_TOKEN },
                    authHeaderName = obj.optString("authHeaderName", "Authorization"),
                    modelId = obj.optString("modelId"),
                    customHeadersJson = obj.optString("customHeadersJson", "{}"),
                    customQueryParamsJson = obj.optString("customQueryParamsJson", "{}"),
                    customRequestBodyTemplate = obj.optString("customRequestBodyTemplate", ""),
                    responseJsonPath = obj.optString("responseJsonPath", "")
                )
                apiProfileDao.insertOrUpdate(ApiProfileEntity.fromDomain(profile))
                count++
            }
        } catch (_: Exception) {}
        count
    }
}
