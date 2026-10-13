package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.components.BushraBottomNavigation
import com.example.ui.components.NavigationTab
import com.example.ui.screens.ActionCenterScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceScreen
import com.example.ui.theme.BushraTheme
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by chatViewModel.themeMode.collectAsState()
            val onboardingCompleted by chatViewModel.onboardingCompleted.collectAsState()

            BushraTheme(themeMode = themeMode) {
                if (!onboardingCompleted) {
                    OnboardingScreen(
                        onComplete = {
                            chatViewModel.completeOnboarding()
                        }
                    )
                } else {
                    BushraMainContent(viewModel = chatViewModel)
                }
            }
        }
    }
}

@Composable
fun BushraMainContent(viewModel: ChatViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.CHAT) }

    // Custom back navigation handling
    BackHandler(enabled = currentTab != NavigationTab.CHAT) {
        currentTab = NavigationTab.CHAT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BushraBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.CHAT -> {
                    ChatScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentTab = NavigationTab.SETTINGS },
                        onNavigateToVoice = { currentTab = NavigationTab.VOICE }
                    )
                }
                NavigationTab.VOICE -> {
                    VoiceScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentTab = NavigationTab.SETTINGS }
                    )
                }
                NavigationTab.ACTIONS -> {
                    ActionCenterScreen(viewModel = viewModel)
                }
                NavigationTab.HISTORY -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onConversationSelected = { currentTab = NavigationTab.CHAT }
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
