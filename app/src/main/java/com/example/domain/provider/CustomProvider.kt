package com.example.domain.provider

import com.example.data.model.AIModel
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.UserMemory
import com.example.data.model.VoicePersonality

class CustomProvider(
    private val delegate: OpenAIProvider = OpenAIProvider()
) : AIProvider {
    override val providerType: AIProviderType = AIProviderType.CUSTOM

    override val availableModels: List<AIModel> = listOf(
        AIModel(
            id = "custom-model",
            name = "Default Custom Model",
            providerType = AIProviderType.CUSTOM,
            description = "Custom endpoint model (OpenRouter, Ollama, LM Studio, vLLM)",
            supportsVision = true
        )
    )

    override suspend fun testConnection(apiKey: String, baseUrl: String?, modelId: String?): Result<String> {
        val effectiveUrl = baseUrl?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1"
        return delegate.testConnection(
            apiKey = apiKey,
            baseUrl = effectiveUrl,
            modelId = modelId ?: "custom-model"
        )
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
    ): Result<String> {
        val effectiveUrl = customBaseUrl?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1"
        return delegate.sendMessage(
            apiKey = apiKey,
            modelId = modelId,
            prompt = prompt,
            conversationHistory = conversationHistory,
            personality = personality,
            memories = memories,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            customBaseUrl = effectiveUrl,
            onChunk = onChunk
        )
    }
}
