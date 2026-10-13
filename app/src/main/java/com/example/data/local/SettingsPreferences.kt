package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.AIProviderType
import com.example.data.model.ProviderConfig
import com.example.data.model.ThemeMode
import com.example.data.model.VoicePersonality
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bushra_settings")

class SettingsPreferences(private val context: Context) {

    companion object {
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        private val KEY_GROK_API_KEY = stringPreferencesKey("grok_api_key")
        private val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        private val KEY_CUSTOM_BASE_URL = stringPreferencesKey("custom_base_url")

        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_OPENAI_MODEL = stringPreferencesKey("openai_model")
        private val KEY_GROK_MODEL = stringPreferencesKey("grok_model")
        private val KEY_CUSTOM_MODEL = stringPreferencesKey("custom_model")

        private val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Voice settings keys
        private val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        private val KEY_SPEECH_OUTPUT_ENABLED = booleanPreferencesKey("speech_output_enabled")
        private val KEY_VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        private val KEY_SPEAKING_RATE = floatPreferencesKey("speaking_rate")
        private val KEY_PITCH = floatPreferencesKey("speaking_pitch")
        private val KEY_AUTO_PLAY_SPEECH = booleanPreferencesKey("auto_play_speech")
        private val KEY_PUSH_TO_TALK = booleanPreferencesKey("push_to_talk")
        private val KEY_PERSONALITY = stringPreferencesKey("voice_personality")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val activeProviderFlow: Flow<AIProviderType> = context.dataStore.data.map { prefs ->
        val providerStr = prefs[KEY_ACTIVE_PROVIDER] ?: AIProviderType.GEMINI.name
        try {
            AIProviderType.valueOf(providerStr)
        } catch (_: Exception) {
            AIProviderType.GEMINI
        }
    }

    val voiceSettingsFlow: Flow<VoiceSettings> = context.dataStore.data.map { prefs ->
        val personalityStr = prefs[KEY_PERSONALITY] ?: VoicePersonality.NORMAL.name
        val personality = try {
            VoicePersonality.valueOf(personalityStr)
        } catch (_: Exception) {
            VoicePersonality.NORMAL
        }
        VoiceSettings(
            isEnabled = prefs[KEY_VOICE_ENABLED] ?: true,
            speechOutputEnabled = prefs[KEY_SPEECH_OUTPUT_ENABLED] ?: true,
            language = prefs[KEY_VOICE_LANGUAGE] ?: "en-US",
            speakingRate = prefs[KEY_SPEAKING_RATE] ?: 1.0f,
            pitch = prefs[KEY_PITCH] ?: 1.0f,
            autoPlaySpeech = prefs[KEY_AUTO_PLAY_SPEECH] ?: true,
            pushToTalkMode = prefs[KEY_PUSH_TO_TALK] ?: false,
            personality = personality
        )
    }

    suspend fun getProviderConfig(providerType: AIProviderType): ProviderConfig {
        val prefs = context.dataStore.data.first()
        return when (providerType) {
            AIProviderType.GEMINI -> {
                val storedKey = prefs[KEY_GEMINI_API_KEY] ?: ""
                val effectiveKey = if (storedKey.isNotBlank()) storedKey else getBuildConfigKey("GEMINI_API_KEY")
                ProviderConfig(
                    providerType = AIProviderType.GEMINI,
                    apiKey = effectiveKey,
                    selectedModel = prefs[KEY_GEMINI_MODEL] ?: "gemini-2.5-flash",
                    customBaseUrl = "",
                    isEnabled = true
                )
            }
            AIProviderType.OPENAI -> {
                val storedKey = prefs[KEY_OPENAI_API_KEY] ?: ""
                val effectiveKey = if (storedKey.isNotBlank()) storedKey else getBuildConfigKey("OPENAI_API_KEY")
                ProviderConfig(
                    providerType = AIProviderType.OPENAI,
                    apiKey = effectiveKey,
                    selectedModel = prefs[KEY_OPENAI_MODEL] ?: "gpt-4o-mini",
                    customBaseUrl = "",
                    isEnabled = true
                )
            }
            AIProviderType.GROK -> {
                val storedKey = prefs[KEY_GROK_API_KEY] ?: ""
                val effectiveKey = if (storedKey.isNotBlank()) storedKey else getBuildConfigKey("GROK_API_KEY")
                ProviderConfig(
                    providerType = AIProviderType.GROK,
                    apiKey = effectiveKey,
                    selectedModel = prefs[KEY_GROK_MODEL] ?: "grok-beta",
                    customBaseUrl = "",
                    isEnabled = true
                )
            }
            AIProviderType.CUSTOM -> {
                ProviderConfig(
                    providerType = AIProviderType.CUSTOM,
                    apiKey = prefs[KEY_CUSTOM_API_KEY] ?: "",
                    selectedModel = prefs[KEY_CUSTOM_MODEL] ?: "custom-model",
                    customBaseUrl = prefs[KEY_CUSTOM_BASE_URL] ?: "https://api.openai.com/v1",
                    isEnabled = true
                )
            }
        }
    }

    private fun getBuildConfigKey(name: String): String {
        return try {
            val field = BuildConfig::class.java.getField(name)
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getAllConfigsFlow(): Flow<Map<AIProviderType, ProviderConfig>> = context.dataStore.data.map { prefs ->
        mapOf(
            AIProviderType.GEMINI to ProviderConfig(
                providerType = AIProviderType.GEMINI,
                apiKey = (prefs[KEY_GEMINI_API_KEY] ?: "").ifBlank { getBuildConfigKey("GEMINI_API_KEY") },
                selectedModel = prefs[KEY_GEMINI_MODEL] ?: "gemini-2.5-flash",
                customBaseUrl = ""
            ),
            AIProviderType.OPENAI to ProviderConfig(
                providerType = AIProviderType.OPENAI,
                apiKey = (prefs[KEY_OPENAI_API_KEY] ?: "").ifBlank { getBuildConfigKey("OPENAI_API_KEY") },
                selectedModel = prefs[KEY_OPENAI_MODEL] ?: "gpt-4o-mini",
                customBaseUrl = ""
            ),
            AIProviderType.GROK to ProviderConfig(
                providerType = AIProviderType.GROK,
                apiKey = (prefs[KEY_GROK_API_KEY] ?: "").ifBlank { getBuildConfigKey("GROK_API_KEY") },
                selectedModel = prefs[KEY_GROK_MODEL] ?: "grok-beta",
                customBaseUrl = ""
            ),
            AIProviderType.CUSTOM to ProviderConfig(
                providerType = AIProviderType.CUSTOM,
                apiKey = prefs[KEY_CUSTOM_API_KEY] ?: "",
                selectedModel = prefs[KEY_CUSTOM_MODEL] ?: "custom-model",
                customBaseUrl = prefs[KEY_CUSTOM_BASE_URL] ?: "https://api.openai.com/v1"
            )
        )
    }

    suspend fun saveProviderConfig(config: ProviderConfig) {
        context.dataStore.edit { prefs ->
            when (config.providerType) {
                AIProviderType.GEMINI -> {
                    prefs[KEY_GEMINI_API_KEY] = config.apiKey.trim()
                    prefs[KEY_GEMINI_MODEL] = config.selectedModel
                }
                AIProviderType.OPENAI -> {
                    prefs[KEY_OPENAI_API_KEY] = config.apiKey.trim()
                    prefs[KEY_OPENAI_MODEL] = config.selectedModel
                }
                AIProviderType.GROK -> {
                    prefs[KEY_GROK_API_KEY] = config.apiKey.trim()
                    prefs[KEY_GROK_MODEL] = config.selectedModel
                }
                AIProviderType.CUSTOM -> {
                    prefs[KEY_CUSTOM_API_KEY] = config.apiKey.trim()
                    prefs[KEY_CUSTOM_MODEL] = config.selectedModel
                    prefs[KEY_CUSTOM_BASE_URL] = config.customBaseUrl.trim()
                }
            }
        }
    }

    suspend fun removeProviderKey(providerType: AIProviderType) {
        context.dataStore.edit { prefs ->
            when (providerType) {
                AIProviderType.GEMINI -> prefs.remove(KEY_GEMINI_API_KEY)
                AIProviderType.OPENAI -> prefs.remove(KEY_OPENAI_API_KEY)
                AIProviderType.GROK -> prefs.remove(KEY_GROK_API_KEY)
                AIProviderType.CUSTOM -> prefs.remove(KEY_CUSTOM_API_KEY)
            }
        }
    }

    suspend fun setActiveProvider(providerType: AIProviderType) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER] = providerType.name
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateVoiceSettings(settings: VoiceSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOICE_ENABLED] = settings.isEnabled
            prefs[KEY_SPEECH_OUTPUT_ENABLED] = settings.speechOutputEnabled
            prefs[KEY_VOICE_LANGUAGE] = settings.language
            prefs[KEY_SPEAKING_RATE] = settings.speakingRate
            prefs[KEY_PITCH] = settings.pitch
            prefs[KEY_AUTO_PLAY_SPEECH] = settings.autoPlaySpeech
            prefs[KEY_PUSH_TO_TALK] = settings.pushToTalkMode
            prefs[KEY_PERSONALITY] = settings.personality.name
        }
    }
}
