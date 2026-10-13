package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.action.ActionStatus
import com.example.action.ActionToolType
import com.example.data.api.ApiAuthType
import com.example.data.api.ApiCategory
import com.example.data.api.ApiCompatibilityMode
import com.example.data.api.ApiProfile
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.model.UserMemory

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val previewLastMessage: String,
    val providerType: String,
    val modelId: String
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        previewLastMessage = previewLastMessage,
        providerType = try {
            AIProviderType.valueOf(providerType)
        } catch (_: Exception) {
            AIProviderType.GEMINI
        },
        modelId = modelId
    )

    companion object {
        fun fromDomain(conversation: Conversation): ConversationEntity = ConversationEntity(
            id = conversation.id,
            title = conversation.title,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            previewLastMessage = conversation.previewLastMessage,
            providerType = conversation.providerType.name,
            modelId = conversation.modelId
        )
    }
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val status: String,
    val imageUri: String?,
    val providerName: String?,
    val modelName: String?
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        conversationId = conversationId,
        sender = try {
            MessageSender.valueOf(sender)
        } catch (_: Exception) {
            MessageSender.ASSISTANT
        },
        content = content,
        timestamp = timestamp,
        status = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.SENT
        },
        imageUri = imageUri,
        providerName = providerName,
        modelName = modelName
    )

    companion object {
        fun fromDomain(message: ChatMessage): MessageEntity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            sender = message.sender.name,
            content = message.content,
            timestamp = message.timestamp,
            status = message.status.name,
            imageUri = message.imageUri,
            providerName = message.providerName,
            modelName = message.modelName
        )
    }
}

@Entity(tableName = "user_memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val fact: String,
    val createdAt: Long
) {
    fun toDomain(): UserMemory = UserMemory(
        id = id,
        fact = fact,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(memory: UserMemory): MemoryEntity = MemoryEntity(
            id = memory.id,
            fact = memory.fact,
            createdAt = memory.createdAt
        )
    }
}

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueTimeMillis: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey val actionId: String,
    val toolName: String,
    val userPrompt: String,
    val argumentsJson: String,
    val status: String,
    val resultSummary: String,
    val technicalDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_profiles")
data class ApiProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val providerName: String,
    val category: String,
    val compatibilityMode: String,
    val baseUrl: String,
    val endpointPath: String,
    val httpMethod: String,
    val authType: String,
    val authHeaderName: String,
    val apiKey: String,
    val modelId: String,
    val customHeadersJson: String,
    val customQueryParamsJson: String,
    val customRequestBodyTemplate: String,
    val responseJsonPath: String,
    val timeoutSeconds: Int,
    val isEnabled: Boolean,
    val isDefault: Boolean,
    val lastTestStatus: String?,
    val lastTestResponseTimeMs: Long?,
    val lastTestTimestamp: Long?,
    val createdAt: Long
) {
    fun toDomain(): ApiProfile = ApiProfile(
        id = id,
        displayName = displayName,
        providerName = providerName,
        category = try { ApiCategory.valueOf(category) } catch (_: Exception) { ApiCategory.CHAT_REASONING },
        compatibilityMode = try { ApiCompatibilityMode.valueOf(compatibilityMode) } catch (_: Exception) { ApiCompatibilityMode.OPENAI_CHAT_COMPLETIONS },
        baseUrl = baseUrl,
        endpointPath = endpointPath,
        httpMethod = httpMethod,
        authType = try { ApiAuthType.valueOf(authType) } catch (_: Exception) { ApiAuthType.BEARER_TOKEN },
        authHeaderName = authHeaderName,
        apiKey = apiKey,
        modelId = modelId,
        customHeadersJson = customHeadersJson,
        customQueryParamsJson = customQueryParamsJson,
        customRequestBodyTemplate = customRequestBodyTemplate,
        responseJsonPath = responseJsonPath,
        timeoutSeconds = timeoutSeconds,
        isEnabled = isEnabled,
        isDefault = isDefault,
        lastTestStatus = lastTestStatus,
        lastTestResponseTimeMs = lastTestResponseTimeMs,
        lastTestTimestamp = lastTestTimestamp,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(profile: ApiProfile): ApiProfileEntity = ApiProfileEntity(
            id = profile.id,
            displayName = profile.displayName,
            providerName = profile.providerName,
            category = profile.category.name,
            compatibilityMode = profile.compatibilityMode.name,
            baseUrl = profile.baseUrl,
            endpointPath = profile.endpointPath,
            httpMethod = profile.httpMethod,
            authType = profile.authType.name,
            authHeaderName = profile.authHeaderName,
            apiKey = profile.apiKey,
            modelId = profile.modelId,
            customHeadersJson = profile.customHeadersJson,
            customQueryParamsJson = profile.customQueryParamsJson,
            customRequestBodyTemplate = profile.customRequestBodyTemplate,
            responseJsonPath = profile.responseJsonPath,
            timeoutSeconds = profile.timeoutSeconds,
            isEnabled = profile.isEnabled,
            isDefault = profile.isDefault,
            lastTestStatus = profile.lastTestStatus,
            lastTestResponseTimeMs = profile.lastTestResponseTimeMs,
            lastTestTimestamp = profile.lastTestTimestamp,
            createdAt = profile.createdAt
        )
    }
}
