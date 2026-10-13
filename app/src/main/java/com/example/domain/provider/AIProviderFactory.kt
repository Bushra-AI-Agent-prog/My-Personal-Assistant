package com.example.domain.provider

import com.example.data.model.AIProviderType

object AIProviderFactory {
    private val geminiProvider by lazy { GeminiProvider() }
    private val openAIProvider by lazy { OpenAIProvider() }
    private val grokProvider by lazy { GrokProvider() }
    private val customProvider by lazy { CustomProvider() }

    fun getProvider(type: AIProviderType): AIProvider {
        return when (type) {
            AIProviderType.GEMINI -> geminiProvider
            AIProviderType.OPENAI -> openAIProvider
            AIProviderType.GROK -> grokProvider
            AIProviderType.CUSTOM -> customProvider
        }
    }

    fun getAllProviders(): List<AIProvider> = listOf(
        geminiProvider,
        openAIProvider,
        grokProvider,
        customProvider
    )
}
