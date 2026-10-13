package com.example.domain.provider

import com.example.data.model.AIModel
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.UserMemory
import com.example.data.model.VoicePersonality

class GrokProvider(
    private val delegate: OpenAIProvider = OpenAIProvider()
) : AIProvider {
    override val providerType: AIProviderType = AIProviderType.GROK

    override val availableModels: List<AIModel> = listOf(
        AIModel(
            id = "grok-2-latest",
            name = "Grok 2 (Latest)",
            providerType = AIProviderType.GROK,
            description = "State-of-the-art Grok model from xAI with advanced reasoning and real-time knowledge",
            supportsVision = true
        ),
        AIModel(
            id = "grok-beta",
            name = "Grok Beta",
            providerType = AIProviderType.GROK,
            description = "Fast, direct Grok language model from xAI",
            supportsVision = false
        )
    )

    private val xAiBaseUrl = "https://api.x.ai/v1"

    override suspend fun testConnection(apiKey: String, baseUrl: String?, modelId: String?): Result<String> {
        val model = modelId?.ifBlank { "grok-beta" } ?: "grok-beta"
        return delegate.testConnection(
            apiKey = apiKey,
            baseUrl = xAiBaseUrl,
            modelId = model
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
        val model = modelId.ifBlank { "grok-beta" }
        return delegate.sendMessage(
            apiKey = apiKey,
            modelId = model,
            prompt = prompt,
            conversationHistory = conversationHistory,
            personality = personality,
            memories = memories,
            imageBase64 = imageBase64,
            mimeType = mimeType,
            customBaseUrl = xAiBaseUrl,
            onChunk = onChunk
        )
    }
}
