# 🎙️ BUSHRA AI — Voice Architecture & System Documentation

The Voice System in BUSHRA AI is a stateful, dual-engine voice pipeline integrating Android's native `SpeechRecognizer` with `TextToSpeech` and LLM orchestration.

---

## 1. Voice State Machine

The voice session lifecycle is managed through explicit states:

```
[IDLE]
  ↓ (User taps Mic)
[REQUESTING_PERMISSION] (if not granted)
  ↓ (Permission granted)
[INITIALIZING] (Creates Recognizer & configures audio focus)
  ↓ (onReadyForSpeech)
[LISTENING] (Microphone active; waveform animates to real RMS dB)
  ↓ (onEndOfSpeech / silence)
[PROCESSING] (Calling active AI Provider with conversation history)
  ↓ (AI response received)
[SPEAKING] (TextToSpeech audio playback; orbital visualizer pulses)
  ↓ (UtteranceProgressListener.onDone)
[LISTENING] (if Continuous mode enabled) OR [IDLE] (if Push-to-talk mode enabled)
```

Transitions to `[PAUSED]` or `[ERROR]` occur when the user taps Pause or if network/speech errors arise.

---

## 2. Controls & Actions

- **Mic Toggle Button**: Starts a voice session when idle; ends the session and releases audio focus when active.
- **Pause / Resume**: Pauses listening and TTS playback while retaining the monotonic session timer and context.
- **Stop Speaking**: Immediately cuts off active TTS audio without canceling the conversation.
- **Push-to-Talk vs Continuous Mode**: Configurable in Voice Settings. Continuous mode automatically loops back to listening after the assistant finishes speaking for a seamless hands-free conversation.
- **Conversational Tone**: Adjusts tone dynamically across 9 personalities (Normal, Friendly, Calm & Gentle, Professional, Playful, Warm, Romantic, Concise, Detailed).

---

## 3. Monotonic Session Timer

Measured via `SystemClock.elapsedRealtime()`. It tracks the true elapsed active duration, pauses during session pauses, stops on session termination, and never resets during Compose screen recomposition or orientation changes.

---

## 4. Hardware & Permissions

- **Permissions**: `android.permission.RECORD_AUDIO`, `android.permission.INTERNET`, `android.permission.MODIFY_AUDIO_SETTINGS`.
- **Manifest Queries**: Includes `<intent><action android:name="android.speech.RecognitionService" /></intent></queries>` to guarantee speech recognition service resolution on Android 11+ (API 30+).
