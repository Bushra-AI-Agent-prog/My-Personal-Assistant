package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.action.ActionPlanner
import com.example.action.ActionResult
import com.example.action.ActionStatus
import com.example.action.executor.AndroidActionExecutor
import com.example.action.executor.InAppTimerManager
import com.example.data.api.ApiCategory
import com.example.data.api.ApiProfileRepository
import com.example.data.api.client.UniversalApiClient
import com.example.data.local.BushraDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.local.entity.ActionHistoryEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.model.UserMemory
import com.example.domain.provider.AIProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val database: BushraDatabase,
    private val preferences: SettingsPreferences,
    private val scope: CoroutineScope
) {
    val conversationDao = database.conversationDao()
    val messageDao = database.messageDao()
    val memoryDao = database.memoryDao()
    val noteDao = database.noteDao()
    val reminderDao = database.reminderDao()
    val actionHistoryDao = database.actionHistoryDao()
    val apiProfileDao = database.apiProfileDao()

    val timerManager = InAppTimerManager(scope)
    val actionExecutor = AndroidActionExecutor(
        context = context,
        noteDao = noteDao,
        reminderDao = reminderDao,
        actionHistoryDao = actionHistoryDao,
        timerManager = timerManager
    )

    val apiProfileRepository = ApiProfileRepository(context, apiProfileDao)

    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun searchConversations(query: String): Flow<List<Conversation>> {
        return conversationDao.searchConversations(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllMemories(): Flow<List<UserMemory>> {
        return memoryDao.getAllMemories().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    fun getActionHistory(): Flow<List<ActionHistoryEntity>> = actionHistoryDao.getRecentActions()

    suspend fun clearActionHistory() = withContext(Dispatchers.IO) {
        actionHistoryDao.deleteAll()
    }

    suspend fun createConversation(
        title: String = "New Chat",
        providerType: AIProviderType? = null,
        modelId: String? = null
    ): Conversation = withContext(Dispatchers.IO) {
        val activeProvider = providerType ?: preferences.activeProviderFlow.first()
        val config = preferences.getProviderConfig(activeProvider)
        val selectedModel = modelId ?: config.selectedModel

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            previewLastMessage = "",
            providerType = activeProvider,
            modelId = selectedModel
        )
        conversationDao.insertOrUpdate(ConversationEntity.fromDomain(conversation))
        conversation
    }

    suspend fun getOrCreateLatestConversation(): Conversation = withContext(Dispatchers.IO) {
        val existing = conversationDao.getAllConversations().first()
        if (existing.isNotEmpty()) {
            existing.first().toDomain()
        } else {
            createConversation()
        }
    }

    suspend fun addMemory(fact: String) = withContext(Dispatchers.IO) {
        if (fact.isNotBlank()) {
            val memory = UserMemory(id = UUID.randomUUID().toString(), fact = fact.trim())
            memoryDao.insertOrUpdate(MemoryEntity.fromDomain(memory))
        }
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteById(id)
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteForConversation(id)
        conversationDao.deleteById(id)
    }

    suspend fun clearAllConversations() = withContext(Dispatchers.IO) {
        messageDao.deleteAll()
        conversationDao.deleteAll()
    }

    suspend fun exportConversation(conversationId: String): String = withContext(Dispatchers.IO) {
        val conversation = conversationDao.getConversationById(conversationId)?.toDomain()
        val messages = messageDao.getMessagesList(conversationId).map { it.toDomain() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append("BUSHRA AI — Conversation Export\n")
        sb.append("Title: ").append(conversation?.title ?: "Chat").append("\n")
        sb.append("Date: ").append(dateFormat.format(Date(conversation?.createdAt ?: System.currentTimeMillis()))).append("\n")
        sb.append("------------------------------------------\n\n")

        for (msg in messages) {
            val time = dateFormat.format(Date(msg.timestamp))
            val senderLabel = if (msg.sender == MessageSender.USER) "User" else "BUSHRA AI"
            sb.append("[$time] $senderLabel:\n")
            sb.append(msg.content).append("\n\n")
        }
        sb.toString()
    }

    suspend fun sendMessage(
        conversationId: String,
        userPrompt: String,
        imageUriString: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val convEntity = conversationDao.getConversationById(conversationId)
            ?: return@withContext Result.failure(IllegalArgumentException("Conversation not found"))

        val trimmedPrompt = userPrompt.trim()

        // 1. SAVE USER MESSAGE
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id = userMsgId,
            conversationId = conversationId,
            sender = MessageSender.USER,
            content = trimmedPrompt,
            imageUri = imageUriString,
            status = MessageStatus.SENT
        )
        messageDao.insertOrUpdate(MessageEntity.fromDomain(userMessage))

        // Update conversation title if first message
        val currentHistory = messageDao.getMessagesList(conversationId).map { it.toDomain() }
        if (currentHistory.size <= 2 && trimmedPrompt.isNotBlank()) {
            val autoTitle = if (trimmedPrompt.length > 30) trimmedPrompt.take(27) + "..." else trimmedPrompt
            conversationDao.update(
                convEntity.copy(
                    title = autoTitle,
                    previewLastMessage = trimmedPrompt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            conversationDao.update(
                convEntity.copy(
                    previewLastMessage = trimmedPrompt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // 2. CHECK DETERMINISTIC LOCAL ACTION ENGINE FIRST (Free-first, zero API key required!)
        val localAction = ActionPlanner.planActionFromPrompt(trimmedPrompt)
        if (localAction != null) {
            val execResult: ActionResult = actionExecutor.execute(localAction)
            val assistantMsgId = UUID.randomUUID().toString()
            val assistantMessage = ChatMessage(
                id = assistantMsgId,
                conversationId = conversationId,
                sender = MessageSender.ASSISTANT,
                content = execResult.userFeedbackMessage,
                status = if (execResult.status == ActionStatus.SUCCESS) MessageStatus.SENT else MessageStatus.ERROR,
                providerName = "Action Engine",
                modelName = localAction.toolType.displayName,
                actionSummary = "Executed: ${localAction.toolType.displayName}"
            )
            messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
            conversationDao.update(
                convEntity.copy(
                    previewLastMessage = execResult.userFeedbackMessage.take(60),
                    updatedAt = System.currentTimeMillis()
                )
            )
            return@withContext Result.success(assistantMessage)
        }

        // 3. RETRIEVE ACTIVE API PROFILE FROM UNIVERSAL API REPOSITORY
        val effectiveProfile = apiProfileRepository.getEffectiveProfileForCategory(ApiCategory.CHAT_REASONING)
        val voiceSettings = preferences.voiceSettingsFlow.first()
        val memories = memoryDao.getAllMemoriesList().map { it.toDomain() }

        val systemInstruction = buildSystemPrompt(voiceSettings.personality, memories)

        var imageBase64: String? = null
        var mimeType: String? = null
        if (!imageUriString.isNullOrBlank()) {
            val parsed = convertUriToBase64(imageUriString)
            imageBase64 = parsed.first
            mimeType = parsed.second
        }

        if (effectiveProfile != null && effectiveProfile.isConfigured) {
            // Use Universal Custom API Profile
            val result = UniversalApiClient.executeChat(
                profile = effectiveProfile,
                userPrompt = trimmedPrompt,
                conversationHistory = currentHistory,
                systemInstruction = systemInstruction,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                onChunk = onChunk
            )

            val assistantMsgId = UUID.randomUUID().toString()
            if (result.isSuccess) {
                val rawReply = result.getOrThrow()

                // Check if AI response includes a structured tool action call
                val aiToolAction = ActionPlanner.parseActionFromAiResponse(rawReply, trimmedPrompt)
                val finalContent = if (aiToolAction != null) {
                    val toolResult = actionExecutor.execute(aiToolAction)
                    "$rawReply\n\n[Action Result: ${toolResult.userFeedbackMessage}]"
                } else {
                    rawReply
                }

                val assistantMessage = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    sender = MessageSender.ASSISTANT,
                    content = finalContent,
                    status = MessageStatus.SENT,
                    providerName = effectiveProfile.displayName,
                    modelName = effectiveProfile.modelId.ifBlank { "Custom" }
                )
                messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
                conversationDao.update(
                    convEntity.copy(
                        previewLastMessage = finalContent.take(60),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success(assistantMessage)
            } else {
                val err = result.exceptionOrNull()?.message ?: "AI generation failed."
                val assistantMessage = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    sender = MessageSender.ASSISTANT,
                    content = err,
                    status = MessageStatus.ERROR,
                    providerName = effectiveProfile.displayName,
                    modelName = effectiveProfile.modelId
                )
                messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
                return@withContext Result.failure(Exception(err))
            }
        }

        // 4. FALLBACK TO LEGACY PROVIDER CONFIG IF CONFIGURED
        val activeProviderType = try {
            AIProviderType.valueOf(convEntity.providerType)
        } catch (_: Exception) {
            AIProviderType.GEMINI
        }
        val legacyConfig = preferences.getProviderConfig(activeProviderType)

        if (legacyConfig.isConfigured) {
            val provider = AIProviderFactory.getProvider(activeProviderType)
            val assistantMsgId = UUID.randomUUID().toString()

            val result = provider.sendMessage(
                apiKey = legacyConfig.apiKey,
                modelId = legacyConfig.selectedModel,
                prompt = trimmedPrompt,
                conversationHistory = currentHistory,
                personality = voiceSettings.personality,
                memories = memories,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                customBaseUrl = legacyConfig.customBaseUrl,
                onChunk = onChunk
            )

            if (result.isSuccess) {
                val replyText = result.getOrThrow()
                val assistantMessage = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    sender = MessageSender.ASSISTANT,
                    content = replyText,
                    status = MessageStatus.SENT,
                    providerName = activeProviderType.displayName,
                    modelName = legacyConfig.selectedModel
                )
                messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
                conversationDao.update(
                    convEntity.copy(
                        previewLastMessage = replyText.take(60),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success(assistantMessage)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Generation failed."
                val assistantMessage = ChatMessage(
                    id = assistantMsgId,
                    conversationId = conversationId,
                    sender = MessageSender.ASSISTANT,
                    content = err,
                    status = MessageStatus.ERROR,
                    providerName = activeProviderType.displayName,
                    modelName = legacyConfig.selectedModel
                )
                messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
                return@withContext Result.failure(Exception(err))
            }
        }

        // 5. NO API CONFIGURED - PROVIDE CLEAR HELPFUL GUIDANCE
        val notConfiguredMsg = "AI API is not configured yet. You can add your API key or create a profile in Settings anytime.\n\nNote: Offline actions work right now without an API key! Try asking:\n• \"Calculate 25 * 4\"\n• \"Create note: Buy groceries\"\n• \"Set timer for 5 minutes\"\n• \"Open settings\"\n• \"Open YouTube\""
        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            sender = MessageSender.ASSISTANT,
            content = notConfiguredMsg,
            status = MessageStatus.SENT,
            providerName = "Offline Action Engine",
            modelName = "Free-First"
        )
        messageDao.insertOrUpdate(MessageEntity.fromDomain(assistantMessage))
        return@withContext Result.success(assistantMessage)
    }

    private fun buildSystemPrompt(personality: com.example.data.model.VoicePersonality, memories: List<UserMemory>): String {
        val sb = StringBuilder()
        sb.append(personality.promptInstruction)
        sb.append("\n\nYou can perform real actions on this device. If the user asks you to take an action, reply with standard text or you can output a tool call block:")
        sb.append("\n```json\n{\"action\": \"create_note\", \"arguments\": {\"title\": \"Title\", \"content\": \"Content\"}}\n```")
        sb.append("\nSupported actions: calculator, create_note, list_notes, delete_note, create_reminder, list_reminders, start_timer, cancel_timer, open_app, open_settings, web_search, copy_to_clipboard, share_content.")
        if (memories.isNotEmpty()) {
            sb.append("\n\nKnown facts about the user:")
            for (m in memories) {
                sb.append("\n- ").append(m.fact)
            }
        }
        return sb.toString()
    }

    private fun convertUriToBase64(uriString: String): Pair<String?, String?> {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val maxDim = 1024
                val scale = minOf(1.0f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else bitmap

                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                Pair(Base64.encodeToString(bytes, Base64.NO_WRAP), "image/jpeg")
            } else {
                Pair(null, null)
            }
        } catch (_: Exception) {
            Pair(null, null)
        }
    }
}
