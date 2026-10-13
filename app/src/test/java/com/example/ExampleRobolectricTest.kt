package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AIProviderType
import com.example.data.model.ProviderConfig
import com.example.data.model.VoicePersonality
import com.example.data.model.VoiceSettings
import com.example.domain.provider.AIProviderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun applicationContext_isAvailable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
        assertTrue(context.packageName.isNotBlank())
    }

    @Test
    fun providerConfig_reportsConfiguredStatusCorrectly() {
        val emptyConfig = ProviderConfig(providerType = AIProviderType.GEMINI, apiKey = "")
        assertFalse(emptyConfig.isConfigured)

        val populatedConfig = ProviderConfig(providerType = AIProviderType.GEMINI, apiKey = "AIzaSyTestKey123")
        assertTrue(populatedConfig.isConfigured)

        val customWithUrl = ProviderConfig(
            providerType = AIProviderType.CUSTOM,
            apiKey = "",
            customBaseUrl = "https://custom.endpoint.com"
        )
        assertTrue(customWithUrl.isConfigured)
    }

    @Test
    fun aiProviderFactory_resolvesAllProviders() {
        for (providerType in AIProviderType.values()) {
            val provider = AIProviderFactory.getProvider(providerType)
            assertNotNull(provider)
            assertEquals(providerType, provider.providerType)
            assertTrue(provider.availableModels.isNotEmpty())
        }
    }

    @Test
    fun voicePersonality_hasValidPromptInstruction() {
        for (personality in VoicePersonality.values()) {
            assertTrue(personality.title.isNotBlank())
            assertTrue(personality.promptInstruction.contains("BUSHRA AI"))
        }
    }

    @Test
    fun defaultVoiceSettings_areValid() {
        val settings = VoiceSettings()
        assertTrue(settings.isEnabled)
        assertTrue(settings.speechOutputEnabled)
        assertEquals("en-US", settings.language)
        assertEquals(VoicePersonality.NORMAL, settings.personality)
        assertEquals(1.0f, settings.speakingRate, 0.01f)
        assertEquals(1.0f, settings.pitch, 0.01f)
    }
}
