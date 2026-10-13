package com.example.data.model

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR
}

enum class AIProviderType(val displayName: String) {
    GEMINI("Google Gemini"),
    OPENAI("OpenAI"),
    GROK("xAI / Grok"),
    CUSTOM("Custom Universal API")
}

data class AIModel(
    val id: String,
    val name: String,
    val providerType: AIProviderType,
    val description: String,
    val supportsVision: Boolean = false
)

enum class VoicePersonality(
    val title: String,
    val description: String,
    val promptInstruction: String
) {
    NORMAL(
        "Normal Assistant",
        "Helpful, balanced, and articulate",
        "You are BUSHRA AI, a helpful, intelligent, polite, and articulate personal assistant."
    ),
    FRIENDLY(
        "Friendly Companion",
        "Warm, enthusiastic, and approachable",
        "You are BUSHRA AI, a cheerful, warm, and highly supportive friend and companion. Speak encouragingly."
    ),
    CALM_GENTLE(
        "Calm & Gentle",
        "Soothing, mindful, and peaceful",
        "You are BUSHRA AI, speaking with a serene, patient, soothing, and reassuring tone."
    ),
    PROFESSIONAL(
        "Professional & Executive",
        "Crisp, structured, and business-focused",
        "You are BUSHRA AI, an executive consultant. Give structured, concise, and highly professional insights."
    ),
    PLAYFUL(
        "Playful & Witty",
        "Humorous, clever, and energetic",
        "You are BUSHRA AI, witty, clever, and entertaining with light humor and energy."
    ),
    WARM_AFFECTIONATE(
        "Warm & Caring",
        "Empathetic, heartfelt, and deeply attentive",
        "You are BUSHRA AI, deeply caring, empathetic, and attentive to the user's emotional state and well-being."
    ),
    ROMANTIC_FLIRTATIOUS(
        "Light Romantic",
        "Charming, sweet, and playful banter for consenting adults",
        "You are BUSHRA AI in Light Romantic mode for adult conversation. Be charming, sweet, affectionate, and engaging with tasteful playful banter."
    ),
    CONCISE(
        "Concise & Direct",
        "Short, direct answers with zero fluff",
        "You are BUSHRA AI. Be extremely concise, direct, and factual. Limit answers to the essential points."
    ),
    DETAILED(
        "In-depth Expert",
        "Comprehensive, step-by-step explanations",
        "You are BUSHRA AI, an expert academic and technical specialist. Provide rich, detailed, comprehensive breakdowns."
    )
}

enum class VoiceSessionState(val label: String) {
    IDLE("Idle"),
    REQUESTING_PERMISSION("Requesting Mic Permission"),
    INITIALIZING("Initializing Audio Engine"),
    LISTENING("Listening..."),
    PROCESSING("Bushra is Thinking..."),
    EXECUTING_ACTION("Executing Action..."),
    CONFIRMATION_REQUIRED("Waiting for Confirmation"),
    SPEAKING("Bushra is Speaking"),
    PAUSED("Session Paused"),
    SUCCESS("Action Successful"),
    ERROR("Voice Error")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED_BLACK
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val imageUri: String? = null,
    val providerName: String? = null,
    val modelName: String? = null,
    val isStreaming: Boolean = false,
    val actionSummary: String? = null
)

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val previewLastMessage: String = "",
    val providerType: AIProviderType = AIProviderType.CUSTOM,
    val modelId: String = "default"
)

data class UserMemory(
    val id: String,
    val fact: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProviderConfig(
    val providerType: AIProviderType,
    val apiKey: String = "",
    val selectedModel: String = "",
    val customBaseUrl: String = "",
    val isEnabled: Boolean = true
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() || (providerType == AIProviderType.CUSTOM && customBaseUrl.isNotBlank())
}

data class VoiceSettings(
    val isEnabled: Boolean = true,
    val speechOutputEnabled: Boolean = true,
    val language: String = "en-US",
    val recognitionLanguage: String = "en-US",
    val responseLanguage: String = "en-US",
    val speakingRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val autoPlaySpeech: Boolean = true,
    val pushToTalkMode: Boolean = false,
    val interruptSpeechEnabled: Boolean = true,
    val personality: VoicePersonality = VoicePersonality.NORMAL
)
