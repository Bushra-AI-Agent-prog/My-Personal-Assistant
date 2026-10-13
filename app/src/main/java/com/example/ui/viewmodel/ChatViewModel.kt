package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BushraDatabase
import com.example.data.local.SettingsPreferences
import com.example.data.model.AIProviderType
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.model.ProviderConfig
import com.example.data.model.ThemeMode
import com.example.data.model.UserMemory
import com.example.data.model.VoicePersonality
import com.example.data.model.VoiceSessionState
import com.example.data.model.VoiceSettings
import com.example.action.ActionRequest
import com.example.action.ActionToolType
import com.example.action.executor.ActiveTimerState
import com.example.data.local.entity.ActionHistoryEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.repository.ChatRepository
import com.example.domain.provider.AIProviderFactory
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BushraDatabase.getInstance(application)
    val preferences = SettingsPreferences(application)
    val repository = ChatRepository(application, database, preferences, viewModelScope)

    val voiceManager = VoiceAssistantManager(application, viewModelScope)

    val themeMode: StateFlow<ThemeMode> = preferences.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeMode.SYSTEM
    )

    val onboardingCompleted: StateFlow<Boolean> = preferences.onboardingCompletedFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val activeProviderType: StateFlow<AIProviderType> = preferences.activeProviderFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AIProviderType.GEMINI
    )

    val providerConfigs: StateFlow<Map<AIProviderType, ProviderConfig>> = preferences.getAllConfigsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyMap()
    )

    val voiceSettings: StateFlow<VoiceSettings> = preferences.voiceSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VoiceSettings()
    )

    val allConversations: StateFlow<List<Conversation>> = repository.getAllConversations().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allMemories: StateFlow<List<UserMemory>> = repository.getAllMemories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeTimerState: StateFlow<ActiveTimerState> = repository.timerManager.timerState

    val actionHistory: StateFlow<List<ActionHistoryEntity>> = repository.getActionHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allNotes: StateFlow<List<NoteEntity>> = repository.getAllNotes().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allReminders: StateFlow<List<ReminderEntity>> = repository.getAllReminders().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun cancelTimer() {
        repository.timerManager.cancelTimer()
    }

    fun clearActionHistory() {
        viewModelScope.launch {
            repository.clearActionHistory()
        }
    }

    fun executeManualAction(toolName: String, args: Map<String, String>) {
        viewModelScope.launch {
            val toolType = ActionToolType.entries.find { it.toolName == toolName } ?: return@launch
            val request = ActionRequest(
                actionId = UUID.randomUUID().toString(),
                toolType = toolType,
                arguments = args,
                userPrompt = "Manual: $toolName"
            )
            repository.actionExecutor.execute(request)
        }
    }

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _connectionTestStatus = MutableStateFlow<Map<AIProviderType, String?>>(emptyMap())
    val connectionTestStatus: StateFlow<Map<AIProviderType, String?>> = _connectionTestStatus.asStateFlow()

    private val _isTestingConnection = MutableStateFlow<Map<AIProviderType, Boolean>>(emptyMap())
    val isTestingConnection: StateFlow<Map<AIProviderType, Boolean>> = _isTestingConnection.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var messageCollectionJob: Job? = null

    init {
        // Wire voice manager callbacks
        voiceManager.onSpeechRecognized = { recognizedText ->
            viewModelScope.launch {
                handleVoiceInput(recognizedText)
            }
        }

        viewModelScope.launch {
            val conv = repository.getOrCreateLatestConversation()
            selectConversation(conv.id)
        }

        viewModelScope.launch {
            voiceSettings.collect { settings ->
                voiceManager.updateSettings(settings)
            }
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            val conv = allConversations.value.find { it.id == id }
                ?: database.conversationDao().getConversationById(id)?.toDomain()
            _currentConversation.value = conv

            messageCollectionJob?.cancel()
            messageCollectionJob = launch {
                repository.getMessagesForConversation(id).collect { msgList ->
                    _messages.value = msgList
                }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newConv = repository.createConversation()
            selectConversation(newConv.id)
            _selectedImageUri.value = null
        }
    }

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val imageUri = _selectedImageUri.value
        val conv = _currentConversation.value ?: return

        if (trimmed.isBlank() && imageUri == null) return
        if (_isGenerating.value) return

        _isGenerating.value = true
        _streamingText.value = ""
        _selectedImageUri.value = null

        activeGenerationJob = viewModelScope.launch {
            try {
                val result = repository.sendMessage(
                    conversationId = conv.id,
                    userPrompt = trimmed,
                    imageUriString = imageUri?.toString(),
                    onChunk = { chunk ->
                        _streamingText.value += chunk
                    }
                )

                if (result.isSuccess) {
                    val reply = result.getOrThrow()
                    // If in voice mode or auto-play speech is enabled, speak aloud
                    if (voiceSettings.value.autoPlaySpeech && voiceManager.sessionState.value != VoiceSessionState.IDLE) {
                        voiceManager.speak(reply.content)
                    }
                }
            } finally {
                _isGenerating.value = false
                _streamingText.value = ""
            }
        }
    }

    private suspend fun handleVoiceInput(recognizedText: String) {
        val conv = _currentConversation.value ?: repository.createConversation().also {
            selectConversation(it.id)
        }

        _isGenerating.value = true
        _streamingText.value = ""

        try {
            val result = repository.sendMessage(
                conversationId = conv.id,
                userPrompt = recognizedText,
                onChunk = { chunk ->
                    _streamingText.value += chunk
                }
            )

            if (result.isSuccess) {
                val reply = result.getOrThrow()
                voiceManager.speak(reply.content)
            } else {
                voiceManager.speak("Sorry, I could not connect to the AI provider. Please check your settings.")
            }
        } catch (_: Exception) {
            voiceManager.speak("An error occurred while generating the response.")
        } finally {
            _isGenerating.value = false
            _streamingText.value = ""
        }
    }

    fun cancelGeneration() {
        activeGenerationJob?.cancel()
        _isGenerating.value = false
        _streamingText.value = ""
    }

    fun retryMessage(failedMessage: ChatMessage) {
        val conv = _currentConversation.value ?: return
        val lastUserMsg = _messages.value.findLast { it.sender == MessageSender.USER } ?: return
        sendMessage(lastUserMsg.content)
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_currentConversation.value?.id == id) {
                createNewChat()
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllConversations()
            createNewChat()
        }
    }

    fun exportConversation(id: String, onExported: (String) -> Unit) {
        viewModelScope.launch {
            val text = repository.exportConversation(id)
            onExported(text)
        }
    }

    // --- Provider and API Key Configuration ---
    fun saveProviderConfig(config: ProviderConfig) {
        viewModelScope.launch {
            preferences.saveProviderConfig(config)
        }
    }

    fun removeProviderKey(type: AIProviderType) {
        viewModelScope.launch {
            preferences.removeProviderKey(type)
            val updated = _connectionTestStatus.value.toMutableMap()
            updated.remove(type)
            _connectionTestStatus.value = updated
        }
    }

    fun setActiveProvider(type: AIProviderType) {
        viewModelScope.launch {
            preferences.setActiveProvider(type)
        }
    }

    fun testProviderConnection(type: AIProviderType) {
        viewModelScope.launch {
            val loadingMap = _isTestingConnection.value.toMutableMap()
            loadingMap[type] = true
            _isTestingConnection.value = loadingMap

            val config = preferences.getProviderConfig(type)
            val provider = AIProviderFactory.getProvider(type)
            val result = provider.testConnection(
                apiKey = config.apiKey,
                baseUrl = config.customBaseUrl,
                modelId = config.selectedModel
            )

            val statusMap = _connectionTestStatus.value.toMutableMap()
            statusMap[type] = if (result.isSuccess) {
                result.getOrThrow()
            } else {
                "Error: ${result.exceptionOrNull()?.message}"
            }
            _connectionTestStatus.value = statusMap

            loadingMap[type] = false
            _isTestingConnection.value = loadingMap
        }
    }

    // --- Voice Settings ---
    fun updateVoiceSettings(settings: VoiceSettings) {
        viewModelScope.launch {
            preferences.updateVoiceSettings(settings)
        }
    }

    // --- Theme & Onboarding ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
        }
    }

    // --- Memories ---
    fun addMemory(fact: String) {
        viewModelScope.launch {
            repository.addMemory(fact)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
