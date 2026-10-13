# 🌟 BUSHRA AI — Next-Generation Personal AI Assistant

An intelligent, stateful Android AI assistant built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Clean MVVM Architecture**. BUSHRA AI features multi-provider AI support (Google Gemini, OpenAI GPT-4o, xAI Grok, and Custom OpenAI-compatible endpoints), an advanced real-time voice conversation system with audio amplitude visualizer, persistent memory, and a luxury Glassmorphic user interface.

---

## ✨ Key Capabilities

- **🤖 Multi-Provider AI Architecture**:
  - **Google Gemini**: Gemini 2.5 Flash, Gemini 3.5 Flash, Gemini 3.1 Pro (Preview).
  - **OpenAI**: GPT-4o, GPT-4o Mini, GPT-4 Turbo.
  - **xAI / Grok**: Grok 2, Grok Beta.
  - **Custom Endpoints**: OpenAI-compatible base URL (e.g., OpenRouter, Ollama, LM Studio).
- **🎙️ Advanced Voice Assistant System**:
  - Full speech recognition (`SpeechRecognizer`) with real-time partial transcript.
  - Spoken AI responses (`TextToSpeech`) with customizable speed, pitch, and language.
  - Stateful session state machine (`IDLE`, `INITIALIZING`, `LISTENING`, `PROCESSING`, `SPEAKING`, `PAUSED`, `ERROR`).
  - Animated audio waveform & glowing energy orb responding directly to microphone amplitude.
  - Monotonic session timer counter resilient to screen recomposition.
  - Distinct controls: Microphone toggle, Pause/Resume, Stop Speaking immediately, and End Session.
- **🎭 Conversational Personalities**:
  - Normal Assistant, Friendly Companion, Calm & Gentle, Professional, Playful & Witty, Warm & Caring, Light Romantic (consenting adults), Concise, In-depth Expert.
- **🧠 Personalization & Persistent Memory**:
  - Add, inspect, and delete user facts that BUSHRA AI injects into AI conversations.
- **🖼️ Multimodal Vision Analysis**:
  - Zero-permission Android Photo Picker for attaching images to queries.
- **💎 Luxury Glassmorphism UI/UX**:
  - Translucent surfaces, glowing cyan & violet accents, dark cosmic & light crystal themes.
- **🔒 Privacy-First BYOK (Bring Your Own Key)**:
  - The application builds and launches without any API keys.
  - Securely configure, update, test, or remove API keys in Settings anytime.

---

## 🚀 Quick Setup & Installation

1. **Install APK**: Download the generated Release APK from the releases/artifacts and install on your device (Android 7.0+ / API 24+).
2. **Open BUSHRA AI**: The app opens directly without requiring any pre-configured keys or cloud login.
3. **Configure Your API Key**:
   - Go to the **Settings** tab.
   - Choose your provider (e.g., Google Gemini or OpenAI).
   - Enter your API Key.
   - Tap **Save Key** and then **Test Connection** to verify live connectivity.
4. **Chat or Speak**: Tap the **Chat** tab to type or the **Voice** tab to speak hands-free!

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: Clean Architecture + MVVM + Repository Pattern
- **Local Persistence**: Room Database (`conversations`, `messages`, `user_memories`)
- **Key & Preference Storage**: Android Jetpack DataStore Preferences
- **Networking**: OkHttp 4 + Retrofit + SSE Streaming
- **Voice Engines**: Android SpeechRecognizer + TextToSpeech
- **Image Loading**: Coil Compose
- **Testing**: JUnit4 + Robolectric local JVM testing
