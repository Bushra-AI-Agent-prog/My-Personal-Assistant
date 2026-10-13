package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoicePersonality
import com.example.data.model.VoiceSessionState
import com.example.ui.components.AudioVisualizerOrb
import com.example.ui.components.GlassCard
import com.example.ui.components.WaveformBars
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun VoiceScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceManager = viewModel.voiceManager

    val sessionState by voiceManager.sessionState.collectAsState()
    val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
    val formattedDuration by voiceManager.formattedDuration.collectAsState()
    val currentTranscript by voiceManager.currentTranscript.collectAsState()
    val partialTranscript by voiceManager.partialTranscript.collectAsState()
    val errorMessage by voiceManager.errorMessage.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val activeProviderType by viewModel.activeProviderType.collectAsState()
    val providerConfigs by viewModel.providerConfigs.collectAsState()

    val isConfigured = providerConfigs[activeProviderType]?.isConfigured == true
    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            voiceManager.startVoiceSession(continuousMode = !voiceSettings.pushToTalkMode)
        } else {
            showPermissionRationale = true
        }
    }

    fun handleStartVoice() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startVoiceSession(continuousMode = !voiceSettings.pushToTalkMode)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Microphone Access Required") },
            text = { Text("BUSHRA AI needs microphone access to listen to your voice and enable real-time speech conversation.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Voice Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BUSHRA VOICE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${activeProviderType.displayName} • ${voiceSettings.personality.title}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = CyanNeon
                )
            }

            // Visible Monotonic Session Timer
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (sessionState != VoiceSessionState.IDLE) CyanNeon.copy(alpha = 0.5f) else Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (sessionState) {
                                    VoiceSessionState.LISTENING -> EmeraldSuccess
                                    VoiceSessionState.SPEAKING -> VioletNeon
                                    VoiceSessionState.PROCESSING -> AmberWarning
                                    VoiceSessionState.PAUSED -> Color.Gray
                                    else -> Color.DarkGray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Central Animated Waveform Orb
        AudioVisualizerOrb(
            amplitude = audioAmplitude,
            isListening = sessionState == VoiceSessionState.LISTENING,
            isSpeaking = sessionState == VoiceSessionState.SPEAKING,
            isProcessing = sessionState == VoiceSessionState.PROCESSING
        )

        Spacer(modifier = Modifier.height(16.dp))

        // State Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = when (sessionState) {
                VoiceSessionState.LISTENING -> EmeraldSuccess.copy(alpha = 0.15f)
                VoiceSessionState.SPEAKING -> VioletNeon.copy(alpha = 0.15f)
                VoiceSessionState.PROCESSING -> AmberWarning.copy(alpha = 0.15f)
                VoiceSessionState.PAUSED -> Color.Gray.copy(alpha = 0.15f)
                VoiceSessionState.ERROR -> RoseError.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (sessionState) {
                    VoiceSessionState.LISTENING -> EmeraldSuccess.copy(alpha = 0.4f)
                    VoiceSessionState.SPEAKING -> VioletNeon.copy(alpha = 0.4f)
                    VoiceSessionState.PROCESSING -> AmberWarning.copy(alpha = 0.4f)
                    VoiceSessionState.ERROR -> RoseError.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                }
            )
        ) {
            Text(
                text = sessionState.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = when (sessionState) {
                    VoiceSessionState.LISTENING -> EmeraldSuccess
                    VoiceSessionState.SPEAKING -> VioletNeon
                    VoiceSessionState.PROCESSING -> AmberWarning
                    VoiceSessionState.ERROR -> RoseError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Waveform Bars reflecting actual microphone amplitude
        WaveformBars(
            amplitude = audioAmplitude,
            isListening = sessionState == VoiceSessionState.LISTENING,
            isSpeaking = sessionState == VoiceSessionState.SPEAKING,
            modifier = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Transcript Box
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LIVE TRANSCRIPT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val transcriptToDisplay = when {
                    partialTranscript.isNotBlank() -> partialTranscript
                    currentTranscript.isNotBlank() -> currentTranscript
                    sessionState == VoiceSessionState.LISTENING -> "Listening... speak now"
                    sessionState == VoiceSessionState.PROCESSING -> "Bushra is thinking..."
                    sessionState == VoiceSessionState.SPEAKING -> "Bushra is speaking response..."
                    sessionState == VoiceSessionState.PAUSED -> "Voice session paused"
                    else -> "Tap the microphone below to start talking"
                }

                Text(
                    text = transcriptToDisplay,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoseError,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pause / Resume Button
            IconButton(
                onClick = {
                    if (sessionState == VoiceSessionState.PAUSED) {
                        voiceManager.resumeSession()
                    } else if (sessionState != VoiceSessionState.IDLE) {
                        voiceManager.pauseSession()
                    }
                },
                enabled = sessionState != VoiceSessionState.IDLE,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("voice_pause_resume_button")
            ) {
                Icon(
                    imageVector = if (sessionState == VoiceSessionState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (sessionState == VoiceSessionState.PAUSED) "Resume Voice" else "Pause Voice",
                    tint = if (sessionState != VoiceSessionState.IDLE) CyanNeon else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Primary Microphone Toggle Button (Start / Stop)
            val isSessionActive = sessionState != VoiceSessionState.IDLE
            val micBgBrush = if (isSessionActive) {
                Brush.radialGradient(listOf(RoseError, RoseError.copy(alpha = 0.7f)))
            } else {
                Brush.linearGradient(listOf(CyanNeon, VioletNeon))
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(micBgBrush)
                    .border(
                        2.dp,
                        if (isSessionActive) RoseError.copy(alpha = 0.5f) else CyanNeon.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .clickable {
                        if (isSessionActive) {
                            voiceManager.endVoiceSession()
                        } else {
                            handleStartVoice()
                        }
                    }
                    .testTag("primary_mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSessionActive) Icons.Default.CallEnd else Icons.Default.Mic,
                    contentDescription = if (isSessionActive) "End Voice Session" else "Start Voice Session",
                    tint = if (isSessionActive) Color.White else Color.Black,
                    modifier = Modifier.size(34.dp)
                )
            }

            // 3. Stop Speaking Button (Immediately stops TTS without ending conversation)
            IconButton(
                onClick = { voiceManager.stopSpeaking() },
                enabled = sessionState == VoiceSessionState.SPEAKING,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("stop_speaking_button")
            ) {
                Icon(
                    imageVector = if (sessionState == VoiceSessionState.SPEAKING) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Stop Speaking",
                    tint = if (sessionState == VoiceSessionState.SPEAKING) RoseError else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Personality Chip row
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onNavigateToSettings() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tone: ${voiceSettings.personality.title} • Tap to customize",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
