package com.example.action.executor

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.action.ActionRequest
import com.example.action.ActionResult
import com.example.action.ActionStatus
import com.example.action.ActionToolType
import com.example.data.local.dao.ActionHistoryDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.ActionHistoryEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class AndroidActionExecutor(
    private val context: Context,
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
    private val actionHistoryDao: ActionHistoryDao,
    val timerManager: InAppTimerManager
) {

    suspend fun execute(request: ActionRequest): ActionResult = withContext(Dispatchers.IO) {
        val actionId = request.actionId
        val args = request.arguments
        val tool = request.toolType

        var status = ActionStatus.RUNNING
        var feedback = ""
        var technicalDetails: String? = null

        try {
            when (tool) {
                ActionToolType.CALCULATOR -> {
                    val query = args["expression"] ?: args["query"] ?: ""
                    val eval = CalculatorEngine.evaluate(query)
                    if (eval.isSuccess) {
                        status = ActionStatus.SUCCESS
                        feedback = "Result: ${eval.getOrThrow()}"
                        technicalDetails = "Computed: $query = ${eval.getOrThrow()}"
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Calculation failed: ${eval.exceptionOrNull()?.message}"
                    }
                }

                ActionToolType.CREATE_NOTE -> {
                    val title = args["title"]?.ifBlank { "Quick Note" } ?: "Quick Note"
                    val content = args["content"] ?: args["text"] ?: ""
                    if (content.isBlank() && title.isBlank()) {
                        status = ActionStatus.FAILED
                        feedback = "Cannot create empty note."
                    } else {
                        val noteId = UUID.randomUUID().toString()
                        val entity = NoteEntity(id = noteId, title = title, content = content)
                        noteDao.insertOrUpdate(entity)
                        status = ActionStatus.SUCCESS
                        feedback = "Note saved: \"$title\""
                        technicalDetails = "Note ID: $noteId"
                    }
                }

                ActionToolType.LIST_NOTES -> {
                    val notes = noteDao.getAllNotesList()
                    status = ActionStatus.SUCCESS
                    feedback = if (notes.isEmpty()) {
                        "You have no saved notes."
                    } else {
                        val count = notes.size
                        val listStr = notes.take(5).joinToString("\n") { "• ${it.title}: ${it.content.take(40)}" }
                        "Found $count notes:\n$listStr"
                    }
                }

                ActionToolType.DELETE_NOTE -> {
                    val noteId = args["noteId"] ?: args["id"]
                    val title = args["title"]
                    var deleted = 0
                    if (!noteId.isNullOrBlank()) {
                        deleted = noteDao.deleteById(noteId)
                    } else if (!title.isNullOrBlank()) {
                        val matches = noteDao.searchNotes(title)
                        for (m in matches) {
                            deleted += noteDao.deleteById(m.id)
                        }
                    }
                    if (deleted > 0) {
                        status = ActionStatus.SUCCESS
                        feedback = "Deleted $deleted note(s)."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "No matching note found to delete."
                    }
                }

                ActionToolType.CREATE_REMINDER -> {
                    val title = args["title"] ?: args["task"] ?: args["reminder"] ?: ""
                    if (title.isBlank()) {
                        status = ActionStatus.FAILED
                        feedback = "Cannot create empty reminder."
                    } else {
                        val reminderId = UUID.randomUUID().toString()
                        val entity = ReminderEntity(id = reminderId, title = title)
                        reminderDao.insertOrUpdate(entity)
                        status = ActionStatus.SUCCESS
                        feedback = "Reminder created: \"$title\""
                    }
                }

                ActionToolType.LIST_REMINDERS -> {
                    val reminders = reminderDao.getAllRemindersList()
                    status = ActionStatus.SUCCESS
                    feedback = if (reminders.isEmpty()) {
                        "You have no active reminders."
                    } else {
                        val pending = reminders.filter { !it.isCompleted }
                        val done = reminders.filter { it.isCompleted }
                        val sb = StringBuilder()
                        if (pending.isNotEmpty()) {
                            sb.append("Pending (${pending.size}):\n")
                            sb.append(pending.take(5).joinToString("\n") { "• [ ] ${it.title}" })
                        }
                        if (done.isNotEmpty()) {
                            if (sb.isNotEmpty()) sb.append("\n\n")
                            sb.append("Completed (${done.size}):\n")
                            sb.append(done.take(3).joinToString("\n") { "• [✓] ${it.title}" })
                        }
                        sb.toString()
                    }
                }

                ActionToolType.TOGGLE_REMINDER -> {
                    val id = args["reminderId"] ?: args["id"] ?: ""
                    val isComplete = args["completed"]?.toBooleanStrictOrNull() ?: true
                    val updated = reminderDao.updateCompletion(id, isComplete)
                    if (updated > 0) {
                        status = ActionStatus.SUCCESS
                        feedback = if (isComplete) "Marked task as completed." else "Marked task as pending."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Reminder not found."
                    }
                }

                ActionToolType.DELETE_REMINDER -> {
                    val id = args["reminderId"] ?: args["id"] ?: ""
                    val deleted = reminderDao.deleteById(id)
                    if (deleted > 0) {
                        status = ActionStatus.SUCCESS
                        feedback = "Deleted reminder."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Reminder not found."
                    }
                }

                ActionToolType.START_TIMER -> {
                    val secondsStr = args["seconds"] ?: args["duration"] ?: "60"
                    val label = args["label"] ?: "Timer"
                    val totalSecs = parseSeconds(secondsStr)
                    if (totalSecs > 0) {
                        timerManager.startTimer(totalSecs, label)
                        status = ActionStatus.SUCCESS
                        feedback = "Timer started for ${formatDuration(totalSecs)} ($label)."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Invalid timer duration."
                    }
                }

                ActionToolType.CANCEL_TIMER -> {
                    timerManager.cancelTimer()
                    status = ActionStatus.SUCCESS
                    feedback = "Active timer cancelled."
                }

                ActionToolType.COPY_TO_CLIPBOARD -> {
                    val text = args["text"] ?: ""
                    if (text.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bushra AI", text)
                        clipboard.setPrimaryClip(clip)
                        status = ActionStatus.SUCCESS
                        feedback = "Copied to clipboard."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "No text provided to copy."
                    }
                }

                ActionToolType.SHARE_CONTENT -> {
                    val text = args["text"] ?: ""
                    if (text.isNotBlank()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share with").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                        status = ActionStatus.SUCCESS
                        feedback = "Opened share sheet."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "No text provided to share."
                    }
                }

                ActionToolType.OPEN_APP -> {
                    val appName = args["appName"] ?: args["name"] ?: ""
                    val targetPackage = resolvePackageName(appName)
                    if (targetPackage != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            status = ActionStatus.SUCCESS
                            feedback = "Opening $appName."
                        } else {
                            status = ActionStatus.FAILED
                            feedback = "Could not open $appName. App launch intent not found."
                        }
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "App \"$appName\" is not installed on this device."
                    }
                }

                ActionToolType.OPEN_SETTINGS -> {
                    val settingType = args["setting"]?.lowercase() ?: "general"
                    val intentAction = when (settingType) {
                        "wifi" -> Settings.ACTION_WIFI_SETTINGS
                        "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                        "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                        "display" -> Settings.ACTION_DISPLAY_SETTINGS
                        "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
                        "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                        else -> Settings.ACTION_SETTINGS
                    }
                    val intent = Intent(intentAction).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    status = ActionStatus.SUCCESS
                    feedback = "Opening Android Settings ($settingType)."
                }

                ActionToolType.OPEN_URL -> {
                    val urlStr = args["url"] ?: ""
                    if (isValidUrl(urlStr)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        status = ActionStatus.SUCCESS
                        feedback = "Opening $urlStr in browser."
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Invalid or insecure URL: $urlStr"
                    }
                }

                ActionToolType.WEB_SEARCH -> {
                    val query = args["query"] ?: ""
                    if (query.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, query)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                            status = ActionStatus.SUCCESS
                            feedback = "Searching web for: \"$query\""
                        } catch (_: Exception) {
                            // Fallback to direct browser search URL
                            val fallbackUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                            val browserIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                            status = ActionStatus.SUCCESS
                            feedback = "Searching web for: \"$query\""
                        }
                    } else {
                        status = ActionStatus.FAILED
                        feedback = "Search query is empty."
                    }
                }

                ActionToolType.DRAFT_SMS -> {
                    val recipient = args["recipient"] ?: args["phone"] ?: ""
                    val body = args["body"] ?: args["message"] ?: ""
                    val uri = if (recipient.isNotBlank()) Uri.parse("smsto:$recipient") else Uri.parse("smsto:")
                    val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        if (body.isNotBlank()) putExtra("sms_body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    status = ActionStatus.SUCCESS
                    feedback = "Opened SMS app to draft message."
                }

                ActionToolType.DRAFT_EMAIL -> {
                    val to = args["to"] ?: args["email"] ?: ""
                    val subject = args["subject"] ?: ""
                    val body = args["body"] ?: ""
                    val uri = if (to.isNotBlank()) Uri.parse("mailto:$to") else Uri.parse("mailto:")
                    val emailIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
                        if (body.isNotBlank()) putExtra(Intent.EXTRA_TEXT, body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(emailIntent)
                    status = ActionStatus.SUCCESS
                    feedback = "Opened Email app with prepared draft."
                }
            }
        } catch (e: Exception) {
            status = ActionStatus.FAILED
            feedback = "Action failed: ${e.message}"
            technicalDetails = e.stackTraceToString()
        }

        // Record in Action History database
        val historyEntry = ActionHistoryEntity(
            actionId = actionId,
            toolName = tool.toolName,
            userPrompt = request.userPrompt,
            argumentsJson = JSONObject(args).toString(),
            status = status.name,
            resultSummary = feedback,
            technicalDetails = technicalDetails,
            timestamp = System.currentTimeMillis()
        )
        actionHistoryDao.insert(historyEntry)

        return@withContext ActionResult(
            actionId = actionId,
            toolType = tool,
            status = status,
            userFeedbackMessage = feedback,
            technicalDetails = technicalDetails
        )
    }

    private fun parseSeconds(str: String): Int {
        val clean = str.trim().lowercase()
        val num = Regex("""\d+""").find(clean)?.value?.toIntOrNull() ?: 0
        return when {
            clean.contains("hour") || clean.contains("hr") -> num * 3600
            clean.contains("min") -> num * 60
            clean.contains("sec") -> num
            else -> num
        }
    }

    private fun formatDuration(totalSecs: Int): String {
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return when {
            mins > 0 && secs > 0 -> "$mins min $secs sec"
            mins > 0 -> "$mins minutes"
            else -> "$secs seconds"
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) &&
                !url.contains(" ")
    }

    private fun resolvePackageName(appName: String): String? {
        val clean = appName.trim().lowercase()

        // Known common packages
        val common = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "gmail" to "com.google.android.gm",
            "play store" to "com.android.vending",
            "settings" to "com.android.settings",
            "telegram" to "org.telegram.messenger",
            "spotify" to "com.spotify.music"
        )
        if (common.containsKey(clean)) {
            val pkg = common[clean]!!
            if (isPackageInstalled(pkg)) return pkg
        }

        // Try scanning installed applications via PackageManager
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
        for (app in installed) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == clean || label.contains(clean)) {
                return app.packageName
            }
        }
        return null
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
