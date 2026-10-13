package com.example.data.api

enum class ApiCategory(val displayName: String, val description: String) {
    CHAT_REASONING("Main Chat & Reasoning", "Powers primary conversational AI, general queries, and reasoning."),
    SPEECH_TO_TEXT("Speech Recognition (STT)", "Converts spoken voice audio into text."),
    TEXT_TO_SPEECH("Speech Synthesis (TTS)", "Synthesizes spoken audio output from text."),
    VISION("Image & Vision Analysis", "Processes uploaded images and multimodal visual prompts."),
    WEB_SEARCH("Web Search & Retrieval", "Searches the live web for real-time information."),
    EMBEDDING("Embeddings & Memory", "Generates vector embeddings for semantic user memory."),
    UTILITY("Utility / External Tools", "External automation, weather, calculators, or custom APIs."),
    OTHER("Custom / Generic REST", "General user-defined REST API endpoint.")
}

enum class ApiCompatibilityMode(val displayName: String, val defaultPath: String) {
    OPENAI_CHAT_COMPLETIONS("OpenAI-Compatible Chat Completions", "/v1/chat/completions"),
    GEMINI_GENERATE_CONTENT("Gemini-Compatible generateContent", "/v1beta/models/{model}:generateContent"),
    OPENAI_RESPONSES("OpenAI Responses API", "/v1/responses"),
    GENERIC_REST("Generic REST API (Custom Template)", "")
}

enum class ApiAuthType(val displayName: String) {
    BEARER_TOKEN("Bearer Token (Authorization: Bearer <token>)"),
    API_KEY_HEADER("API Key Header (Custom Header Name)"),
    API_KEY_QUERY_PARAM("API Key Query Param (?key=<token>)"),
    BASIC_AUTH("Basic Authentication (Username / Password)"),
    NO_AUTH("No Authentication Required"),
    CUSTOM_HEADER("Custom Headers Only")
}

data class ApiProfile(
    val id: String,
    val displayName: String,
    val providerName: String = "",
    val category: ApiCategory = ApiCategory.CHAT_REASONING,
    val compatibilityMode: ApiCompatibilityMode = ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS,
    val baseUrl: String = "",
    val endpointPath: String = "",
    val httpMethod: String = "POST",
    val authType: ApiAuthType = ApiAuthType.BEARER_TOKEN,
    val authHeaderName: String = "Authorization",
    val apiKey: String = "",
    val modelId: String = "",
    val customHeadersJson: String = "{}",
    val customQueryParamsJson: String = "{}",
    val customRequestBodyTemplate: String = "",
    val responseJsonPath: String = "",
    val timeoutSeconds: Int = 45,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val lastTestStatus: String? = null,
    val lastTestResponseTimeMs: Long? = null,
    val lastTestTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isConfigured: Boolean
        get() = when (authType) {
            ApiAuthType.NO_AUTH -> baseUrl.isNotBlank()
            else -> apiKey.isNotBlank() || (baseUrl.isNotBlank() && compatibilityMode == ApiCompatibilityMode.GENERIC_REST)
        }

    fun getEffectiveFullUrl(): String {
        val base = baseUrl.trim().trimEnd('/')
        var path = endpointPath.trim()
        if (path.isNotBlank() && !path.startsWith('/')) {
            path = "/$path"
        }
        val raw = if (path.isBlank()) base else "$base$path"
        return if (modelId.isNotBlank()) raw.replace("{model}", modelId.trim()) else raw
    }
}

data class ApiRoutingConfig(
    val primaryChatProfileId: String? = null,
    val fallbackChatProfileId: String? = null,
    val visionProfileId: String? = null,
    val webSearchProfileId: String? = null,
    val speechToTextProfileId: String? = null,
    val textToSpeechProfileId: String? = null
)

data class ApiTestResult(
    val isSuccess: Boolean,
    val httpStatusCode: Int?,
    val responseTimeMs: Long,
    val responseSample: String,
    val diagnosticMessage: String,
    val resolvedModel: String? = null
)
