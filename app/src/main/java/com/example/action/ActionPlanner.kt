package com.example.action

import org.json.JSONObject
import java.util.UUID

object ActionPlanner {

    fun planActionFromPrompt(prompt: String): ActionRequest? {
        val clean = prompt.trim()
        val lower = clean.lowercase()

        // 1. Calculator / Math conversions
        if (lower.startsWith("calculate ") || lower.startsWith("compute ") || lower.startsWith("what is ") ||
            lower.matches(Regex("""^[\d\s+\-*/^().x×÷%]+$""")) ||
            lower.contains(" in f") || lower.contains(" in c") || lower.contains(" to fahrenheit") || lower.contains(" to celsius") ||
            lower.contains(" km in miles") || lower.contains(" miles in km") || lower.contains(" kg in lbs") || lower.contains(" lbs in kg")
        ) {
            val expr = when {
                lower.startsWith("calculate ") -> clean.substringAfter("calculate ").trim()
                lower.startsWith("compute ") -> clean.substringAfter("compute ").trim()
                lower.startsWith("what is ") && lower.any { it in "+-*/%^×÷" } -> clean.substringAfter("what is ").removeSuffix("?").trim()
                else -> clean
            }
            if (expr.isNotBlank() && (expr.any { it.isDigit() } || expr.contains(" in ") || expr.contains(" to "))) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.CALCULATOR,
                    arguments = mapOf("expression" to expr),
                    userPrompt = prompt
                )
            }
        }

        // 2. In-App Timer
        if (lower.contains("timer")) {
            if (lower.startsWith("cancel timer") || lower.startsWith("stop timer")) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.CANCEL_TIMER,
                    arguments = emptyMap(),
                    userPrompt = prompt
                )
            }
            val timerMatch = Regex("""(?:set|start)?\s*(?:a)?\s*timer\s*(?:for)?\s*(\d+\s*(?:sec|seconds?|min|minutes?|hours?|hrs?))(?:\s*for\s*(.+))?""").find(lower)
            if (timerMatch != null) {
                val duration = timerMatch.groupValues[1]
                val label = timerMatch.groupValues.getOrNull(2)?.trim()?.ifBlank { "Timer" } ?: "Timer"
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.START_TIMER,
                    arguments = mapOf("seconds" to duration, "label" to label),
                    userPrompt = prompt
                )
            }
        }

        // 3. Notes
        if (lower.contains("note")) {
            if (lower.startsWith("list notes") || lower.startsWith("show notes") || lower.startsWith("show my notes") || lower.startsWith("read notes")) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.LIST_NOTES,
                    arguments = emptyMap(),
                    userPrompt = prompt
                )
            }
            if (lower.startsWith("delete note") || lower.startsWith("remove note")) {
                val title = clean.substringAfter("note").trim().removePrefix(":").trim()
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.DELETE_NOTE,
                    arguments = mapOf("title" to title),
                    userPrompt = prompt
                )
            }
            if (lower.startsWith("create note") || lower.startsWith("take a note") || lower.startsWith("take note") ||
                lower.startsWith("save note") || lower.startsWith("write a note") || lower.startsWith("write note")
            ) {
                val rawNote = when {
                    lower.startsWith("create note") -> clean.substringAfter("create note").trim()
                    lower.startsWith("take a note") -> clean.substringAfter("take a note").trim()
                    lower.startsWith("take note") -> clean.substringAfter("take note").trim()
                    lower.startsWith("save note") -> clean.substringAfter("save note").trim()
                    lower.startsWith("write a note") -> clean.substringAfter("write a note").trim()
                    else -> clean.substringAfter("write note").trim()
                }.removePrefix(":").removePrefix("that").removePrefix("to").trim()

                val parts = rawNote.split(Regex("""[:\n]"""), 2)
                val title = parts[0].trim().take(30)
                val content = if (parts.size > 1) parts[1].trim() else rawNote
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.CREATE_NOTE,
                    arguments = mapOf("title" to title, "content" to content),
                    userPrompt = prompt
                )
            }
        }

        // 4. Reminders / To-dos
        if (lower.startsWith("remind me ") || lower.startsWith("set a reminder ") || lower.startsWith("add reminder ") || lower.startsWith("create reminder ")) {
            val task = when {
                lower.startsWith("remind me to ") -> clean.substringAfter("remind me to ").trim()
                lower.startsWith("remind me ") -> clean.substringAfter("remind me ").trim()
                lower.startsWith("set a reminder to ") -> clean.substringAfter("set a reminder to ").trim()
                lower.startsWith("set a reminder ") -> clean.substringAfter("set a reminder ").trim()
                else -> clean.substringAfter("reminder").removePrefix(":").trim()
            }
            if (task.isNotBlank()) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.CREATE_REMINDER,
                    arguments = mapOf("title" to task),
                    userPrompt = prompt
                )
            }
        }
        if (lower.startsWith("list reminders") || lower.startsWith("show reminders") || lower.startsWith("show my tasks") || lower.startsWith("list tasks")) {
            return ActionRequest(
                actionId = UUID.randomUUID().toString(),
                toolType = ActionToolType.LIST_REMINDERS,
                arguments = emptyMap(),
                userPrompt = prompt
            )
        }

        // 5. Open Apps
        if (lower.startsWith("open app ") || lower.startsWith("launch app ") || (lower.startsWith("open ") && !lower.startsWith("open url") && !lower.startsWith("open settings") && !lower.startsWith("open website"))) {
            val rawName = when {
                lower.startsWith("open app ") -> clean.substringAfter("open app ").trim()
                lower.startsWith("launch app ") -> clean.substringAfter("launch app ").trim()
                else -> clean.substringAfter("open ").trim()
            }
            if (rawName.isNotBlank() && !rawName.startsWith("http") && !rawName.contains("setting")) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.OPEN_APP,
                    arguments = mapOf("appName" to rawName),
                    userPrompt = prompt
                )
            }
        }

        // 6. Open Settings
        if (lower.contains("settings")) {
            val settingType = when {
                lower.contains("wifi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("sound") || lower.contains("volume") -> "sound"
                lower.contains("display") -> "display"
                lower.contains("app") -> "apps"
                lower.contains("battery") -> "battery"
                else -> "general"
            }
            if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("go to ")) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.OPEN_SETTINGS,
                    arguments = mapOf("setting" to settingType),
                    userPrompt = prompt
                )
            }
        }

        // 7. Open URL
        if (lower.startsWith("open url ") || lower.startsWith("open website ") || lower.startsWith("browse ") || (lower.startsWith("http://") || lower.startsWith("https://"))) {
            val url = when {
                lower.startsWith("open url ") -> clean.substringAfter("open url ").trim()
                lower.startsWith("open website ") -> clean.substringAfter("open website ").trim()
                lower.startsWith("browse ") -> clean.substringAfter("browse ").trim()
                else -> clean
            }
            val effectiveUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            return ActionRequest(
                actionId = UUID.randomUUID().toString(),
                toolType = ActionToolType.OPEN_URL,
                arguments = mapOf("url" to effectiveUrl),
                userPrompt = prompt
            )
        }

        // 8. Web Search
        if (lower.startsWith("search web for ") || lower.startsWith("search google for ") || lower.startsWith("web search ")) {
            val query = when {
                lower.startsWith("search web for ") -> clean.substringAfter("search web for ").trim()
                lower.startsWith("search google for ") -> clean.substringAfter("search google for ").trim()
                else -> clean.substringAfter("web search ").trim()
            }
            if (query.isNotBlank()) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.WEB_SEARCH,
                    arguments = mapOf("query" to query),
                    userPrompt = prompt
                )
            }
        }

        // 9. Copy to Clipboard
        if (lower.startsWith("copy to clipboard") || lower.startsWith("copy this")) {
            val text = clean.substringAfter(":").trim().ifBlank { clean.substringAfter("copy").trim() }
            if (text.isNotBlank()) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.COPY_TO_CLIPBOARD,
                    arguments = mapOf("text" to text),
                    userPrompt = prompt
                )
            }
        }

        // 10. Share
        if (lower.startsWith("share text") || lower.startsWith("share this")) {
            val text = clean.substringAfter(":").trim().ifBlank { clean.substringAfter("share").trim() }
            if (text.isNotBlank()) {
                return ActionRequest(
                    actionId = UUID.randomUUID().toString(),
                    toolType = ActionToolType.SHARE_CONTENT,
                    arguments = mapOf("text" to text),
                    userPrompt = prompt
                )
            }
        }

        return null
    }

    fun parseActionFromAiResponse(aiResponse: String, originalPrompt: String): ActionRequest? {
        val trimmed = aiResponse.trim()
        val jsonPattern = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""")
        val match = jsonPattern.find(trimmed)
        val jsonStr = match?.groupValues?.get(1) ?: if (trimmed.startsWith("{") && trimmed.endsWith("}")) trimmed else null
            ?: return null

        return try {
            val obj = JSONObject(jsonStr)
            val actionName = obj.optString("action").ifBlank { obj.optString("tool") }
            if (actionName.isBlank()) return null

            val toolType = ActionToolType.values().find {
                it.toolName.equals(actionName, ignoreCase = true) || it.name.equals(actionName, ignoreCase = true)
            } ?: return null

            val argsObj = obj.optJSONObject("arguments") ?: JSONObject()
            val argsMap = mutableMapOf<String, String>()
            val keys = argsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                argsMap[k] = argsObj.optString(k)
            }

            ActionRequest(
                actionId = UUID.randomUUID().toString(),
                toolType = toolType,
                arguments = argsMap,
                userPrompt = originalPrompt
            )
        } catch (_: Exception) {
            null
        }
    }
}
