# 🔑 BUSHRA AI — In-App API Configuration Guide

BUSHRA AI is designed with a **Bring Your Own Key (BYOK)** model. The app compiles and installs without any hardcoded credentials. After installing the APK, you can configure your API keys directly inside the app.

---

## 1. Google Gemini (Recommended)
1. Get an API key from [Google AI Studio](https://aistudio.google.com/).
2. Open BUSHRA AI and tap the **Settings** tab.
3. Select the **Google Gemini** chip at the top.
4. Paste your API key into the **API Key** field.
5. Select your desired model:
   - `gemini-2.5-flash`: Best balance of speed, multimodal vision, and intelligence.
   - `gemini-3.5-flash`: Next-gen flagship reasoning.
   - `gemini-3.1-pro-preview`: Deep reasoning for STEM and complex tasks.
6. Tap **Save Key**.
7. Tap **Test Connection** to verify your key against Google's servers.
8. Tap **Set Active** if Gemini is not already your active provider.

---

## 2. OpenAI
1. Generate a secret key from your [OpenAI Dashboard](https://platform.openai.com/api-keys).
2. In BUSHRA AI Settings, select the **OpenAI** chip.
3. Paste your key (`sk-...`).
4. Select a model (`gpt-4o-mini`, `gpt-4o`, or `gpt-4-turbo`).
5. Tap **Save Key** and then **Test Connection**.

---

## 3. xAI / Grok
1. Obtain an API key from the [xAI Console](https://console.x.ai/).
2. In Settings, select **xAI / Grok**.
3. Paste your xAI key.
4. Select `grok-2-latest` or `grok-beta`.
5. Tap **Save Key** and test.

---

## 4. Custom OpenAI-Compatible Endpoints
Supports local LLMs (Ollama, LM Studio, vLLM) and aggregators (OpenRouter):
1. In Settings, select **Custom**.
2. Enter your Base URL (e.g., `https://openrouter.ai/api/v1` or `http://10.0.2.2:11434/v1`).
3. Enter your API key (or any placeholder if running local Ollama).
4. Enter your custom model name (e.g., `llama3.3:70b` or `deepseek/deepseek-r1`).
5. Tap **Save Key** and **Test Connection**.

---

## 🔒 Security Note
- Your keys are saved in private, encrypted Android application preferences.
- Keys are never logged, uploaded to any third-party analytics, or exported.
- You can tap **Remove** at any time to purge the stored key from device storage.
