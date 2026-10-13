package com.example.data.api.client

import org.json.JSONArray
import org.json.JSONObject

object JsonPathExtractor {

    fun extract(jsonString: String, path: String): String? {
        val cleanPath = path.trim().removePrefix("$").removePrefix(".")
        if (cleanPath.isBlank()) {
            return extractCommonTextField(jsonString)
        }

        return try {
            val tokens = parsePathTokens(cleanPath)
            var current: Any = if (jsonString.trim().startsWith("[")) JSONArray(jsonString) else JSONObject(jsonString)

            for (token in tokens) {
                when {
                    token.isIndex -> {
                        val arr = current as? JSONArray ?: return null
                        if (token.index < 0 || token.index >= arr.length()) return null
                        current = arr.get(token.index)
                    }
                    else -> {
                        val obj = current as? JSONObject ?: return null
                        if (!obj.has(token.key)) return null
                        current = obj.get(token.key)
                    }
                }
            }

            when (current) {
                is String -> current
                is Number, is Boolean -> current.toString()
                is JSONObject, is JSONArray -> current.toString()
                else -> current.toString()
            }
        } catch (_: Exception) {
            extractCommonTextField(jsonString)
        }
    }

    fun extractCommonTextField(jsonString: String): String? {
        val trimmed = jsonString.trim()
        return try {
            val obj = JSONObject(trimmed)
            // 1. OpenAI chat completions
            obj.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")?.takeIf { it.isNotBlank() }
            // 2. Gemini
                ?: obj.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")?.takeIf { it.isNotBlank() }
            // 3. Simple root fields: text, response, output, message, content, answer
                ?: obj.optString("text").takeIf { it.isNotBlank() }
                ?: obj.optString("response").takeIf { it.isNotBlank() }
                ?: obj.optString("output").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("content").takeIf { it.isNotBlank() }
                ?: obj.optString("answer").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            trimmed.takeIf { it.isNotBlank() }
        }
    }

    private data class PathToken(val key: String = "", val isIndex: Boolean = false, val index: Int = 0)

    private fun parsePathTokens(path: String): List<PathToken> {
        val tokens = mutableListOf<PathToken>()
        val segments = path.split(".")
        for (seg in segments) {
            if (seg.isBlank()) continue
            if (seg.contains("[")) {
                val key = seg.substringBefore("[")
                if (key.isNotBlank()) tokens.add(PathToken(key = key))

                val indexMatches = Regex("""\[(\d+)\]""").findAll(seg)
                for (m in indexMatches) {
                    val idx = m.groupValues[1].toInt()
                    tokens.add(PathToken(isIndex = true, index = idx))
                }
            } else {
                tokens.add(PathToken(key = seg))
            }
        }
        return tokens
    }
}
