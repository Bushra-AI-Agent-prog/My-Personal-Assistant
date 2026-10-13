package com.example.domain.provider

import com.example.data.model.AIModel
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.UserMemory
import com.example.data.model.VoicePersonality

interface AIProvider {
    val providerType: AIProviderType
    val availableModels: List<AIModel>

    suspend fun testConnection(apiKey: String, baseUrl: String? = null, modelId: String? = null): Result<String>

    suspend fun sendMessage(
        apiKey: String,
        modelId: String,
        prompt: String,
        conversationHistory: List<ChatMessage>,
        personality: VoicePersonality,
        memories: List<UserMemory>,
        imageBase64: String? = null,
        mimeType: String? = null,
        customBaseUrl: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Result<String>
}
