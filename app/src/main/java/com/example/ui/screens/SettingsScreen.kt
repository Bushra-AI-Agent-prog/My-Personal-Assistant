package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIProviderType
import com.example.data.model.ProviderConfig
import com.example.data.model.ThemeMode
import com.example.data.model.VoicePersonality
import com.example.domain.provider.AIProviderFactory
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val providerConfigs by viewModel.providerConfigs.collectAsState()
    val activeProviderType by viewModel.activeProviderType.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val userMemories by viewModel.allMemories.collectAsState()
    val connectionTestStatus by viewModel.connectionTestStatus.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    var selectedProviderForConfig by remember { mutableStateOf(activeProviderType) }
    var newMemoryInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // Top Header
        Text(
            text = "Settings & Configuration",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // ================= SECTION 1: AI PROVIDERS & API KEYS =================
        SettingsSectionHeader(title = "AI Providers & API Keys", icon = Icons.Default.Key)

        // Provider Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AIProviderType.values().forEach { provider ->
                val isSelected = selectedProviderForConfig == provider
                val isConfigured = providerConfigs[provider]?.isConfigured == true
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) CyanNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CyanNeon else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedProviderForConfig = provider }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = provider.displayName.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) CyanNeon else MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isConfigured) EmeraldSuccess else RoseError)
                        )
                    }
                }
            }
        }

        // Active Provider Config Card
        val currentConfig = providerConfigs[selectedProviderForConfig] ?: ProviderConfig(selectedProviderForConfig)
        ProviderConfigurationCard(
            config = currentConfig,
            isActive = activeProviderType == selectedProviderForConfig,
            testResult = connectionTestStatus[selectedProviderForConfig],
            isTesting = isTestingConnection[selectedProviderForConfig] == true,
            onSave = { updated ->
                viewModel.saveProviderConfig(updated)
                Toast.makeText(context, "Saved configuration for ${updated.providerType.displayName}", Toast.LENGTH_SHORT).show()
            },
            onRemove = {
                viewModel.removeProviderKey(selectedProviderForConfig)
                Toast.makeText(context, "Removed key for ${selectedProviderForConfig.displayName}", Toast.LENGTH_SHORT).show()
            },
            onTestConnection = {
                viewModel.testProviderConnection(selectedProviderForConfig)
            },
            onSetAsActive = {
                viewModel.setActiveProvider(selectedProviderForConfig)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ================= SECTION 2: VOICE & SPEECH =================
        SettingsSectionHeader(title = "Voice & Speech Engine", icon = Icons.Default.RecordVoiceOver)

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Speech Output Enabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Speech Output (Text-to-Speech)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Speak AI replies out loud",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = voiceSettings.speechOutputEnabled,
                        onCheckedChange = {
                            viewModel.updateVoiceSettings(voiceSettings.copy(speechOutputEnabled = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Conversational Personality Selector
                Text(
                    text = "Conversational Tone & Style",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = voiceSettings.personality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanNeon,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                var personalityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = personalityExpanded,
                    onExpandedChange = { personalityExpanded = !personalityExpanded }
                ) {
                    OutlinedTextField(
                        value = voiceSettings.personality.title,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = personalityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = personalityExpanded,
                        onDismissRequest = { personalityExpanded = false }
                    ) {
                        VoicePersonality.values().forEach { personality ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(personality.title, fontWeight = FontWeight.Bold)
                                        Text(personality.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    viewModel.updateVoiceSettings(voiceSettings.copy(personality = personality))
                                    personalityExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Speaking Speed Slider
                Text(
                    text = "Speaking Speed: ${String.format("%.1fx", voiceSettings.speakingRate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = voiceSettings.speakingRate,
                    onValueChange = {
                        viewModel.updateVoiceSettings(voiceSettings.copy(speakingRate = it))
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pitch Slider
                Text(
                    text = "Voice Pitch: ${String.format("%.1fx", voiceSettings.pitch)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = voiceSettings.pitch,
                    onValueChange = {
                        viewModel.updateVoiceSettings(voiceSettings.copy(pitch = it))
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = VioletNeon, activeTrackColor = VioletNeon)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Push-to-talk toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Push-to-Talk Mode",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Listen only when mic is tapped (disables continuous loop)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = voiceSettings.pushToTalkMode,
                        onCheckedChange = {
                            viewModel.updateVoiceSettings(voiceSettings.copy(pushToTalkMode = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Audio Test Button
                OutlinedButton(
                    onClick = {
                        viewModel.voiceManager.speak("Hello! I am Bushra AI, your intelligent personal assistant. My voice engine is fully working.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Voice Speech Output", color = CyanNeon)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ================= SECTION 3: PERSONALIZATION & MEMORY =================
        SettingsSectionHeader(title = "Personalization & Memory", icon = Icons.Default.Psychology)

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BUSHRA AI remembers facts you save here and references them across all your conversations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemoryInput,
                        onValueChange = { newMemoryInput = it },
                        placeholder = { Text("e.g. My favorite programming language is Kotlin") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newMemoryInput.isNotBlank()) {
                                viewModel.addMemory(newMemoryInput)
                                newMemoryInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyanNeon)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add memory", tint = Color.Black)
                    }
                }

                if (userMemories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        userMemories.forEach { memory ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = memory.fact,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(memory.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete memory",
                                            tint = RoseError.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ================= SECTION 4: APPEARANCE & THEMES =================
        SettingsSectionHeader(title = "Appearance & Theme", icon = Icons.Default.Palette)

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf(
                    ThemeMode.SYSTEM to "Follow System Theme",
                    ThemeMode.DARK to "Cosmic Dark (Glassmorphism)",
                    ThemeMode.LIGHT to "Crystal Light"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemeMode(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanNeon)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ================= SECTION 5: DIAGNOSTICS & ABOUT =================
        SettingsSectionHeader(title = "Diagnostics & About", icon = Icons.Default.Info)

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BUSHRA AI — v1.0.0",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Architecture: Kotlin + Jetpack Compose + Room + Clean MVVM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Android API: Level ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Active Provider: ${activeProviderType.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanNeon
                )
                Text(
                    text = "Security: User API keys are stored in encrypted client preferences and never exported or logged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigurationCard(
    config: ProviderConfig,
    isActive: Boolean,
    testResult: String?,
    isTesting: Boolean,
    onSave: (ProviderConfig) -> Unit,
    onRemove: () -> Unit,
    onTestConnection: () -> Unit,
    onSetAsActive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember(config.providerType, config.apiKey) { mutableStateOf(config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember(config.providerType, config.selectedModel) { mutableStateOf(config.selectedModel) }
    var customBaseUrlInput by remember(config.providerType, config.customBaseUrl) { mutableStateOf(config.customBaseUrl) }

    val providerInstance = AIProviderFactory.getProvider(config.providerType)
    val availableModels = providerInstance.availableModels

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = config.providerType.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (isActive) {
                        Text(
                            text = "Currently Active Provider",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon
                        )
                    }
                }
                StatusBadge(
                    text = if (config.isConfigured) "Configured" else "Key Required",
                    isActive = config.isConfigured
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // API Key Input
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key") },
                placeholder = { Text("Enter your ${config.providerType.displayName} key") },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input_${config.providerType.name}"),
                shape = RoundedCornerShape(12.dp)
            )

            // Custom Base URL if custom provider
            if (config.providerType == AIProviderType.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customBaseUrlInput,
                    onValueChange = { customBaseUrlInput = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Model Selection Dropdown
            var modelDropdownExpanded by remember { mutableStateOf(false) }
            val currentModelObj = availableModels.find { it.id == selectedModel } ?: availableModels.firstOrNull()

            ExposedDropdownMenuBox(
                expanded = modelDropdownExpanded,
                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = currentModelObj?.name ?: selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false }
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(model.name, fontWeight = FontWeight.Bold)
                                    Text(model.description, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                selectedModel = model.id
                                modelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Save, Remove, Set Active
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val effectiveModel = selectedModel.ifBlank { availableModels.firstOrNull()?.id ?: "" }
                        onSave(
                            config.copy(
                                apiKey = apiKeyInput.trim(),
                                selectedModel = effectiveModel,
                                customBaseUrl = customBaseUrlInput.trim()
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_api_key_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                ) {
                    Text("Save Key")
                }

                if (config.apiKey.isNotBlank()) {
                    OutlinedButton(
                        onClick = onRemove,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove", color = RoseError)
                    }
                }

                if (!isActive) {
                    OutlinedButton(
                        onClick = onSetAsActive,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Active")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Connection Button with Real Verification
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_connection_button_${config.providerType.name}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Connection...")
                } else {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Connection", color = CyanNeon)
                }
            }

            // Connection Test Result Display
            if (!testResult.isNullOrBlank()) {
                val isSuccess = !testResult.startsWith("Error", ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSuccess) EmeraldSuccess.copy(alpha = 0.12f) else RoseError.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSuccess) EmeraldSuccess.copy(alpha = 0.4f) else RoseError.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) EmeraldSuccess else RoseError,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
