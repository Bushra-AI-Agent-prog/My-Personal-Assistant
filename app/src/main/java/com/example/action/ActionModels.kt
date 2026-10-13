package com.example.action

enum class ActionToolType(
    val toolName: String,
    val displayName: String,
    val description: String,
    val requiresConfirmation: Boolean = false,
    val requiredPermission: String? = null
) {
    CALCULATOR(
        toolName = "calculator",
        displayName = "Calculator & Unit Converter",
        description = "Performs mathematical calculations and unit conversions offline."
    ),
    CREATE_NOTE(
        toolName = "create_note",
        displayName = "Create Note",
        description = "Saves a new personal note to local storage."
    ),
    LIST_NOTES(
        toolName = "list_notes",
        displayName = "List Notes",
        description = "Retrieves stored notes."
    ),
    DELETE_NOTE(
        toolName = "delete_note",
        displayName = "Delete Note",
        description = "Deletes a specified note from local storage.",
        requiresConfirmation = true
    ),
    CREATE_REMINDER(
        toolName = "create_reminder",
        displayName = "Create Reminder / Task",
        description = "Creates a local reminder or to-do task."
    ),
    LIST_REMINDERS(
        toolName = "list_reminders",
        displayName = "List Reminders",
        description = "Lists pending and completed reminders."
    ),
    TOGGLE_REMINDER(
        toolName = "toggle_reminder",
        displayName = "Complete / Uncomplete Reminder",
        description = "Marks a reminder as completed or pending."
    ),
    DELETE_REMINDER(
        toolName = "delete_reminder",
        displayName = "Delete Reminder",
        description = "Removes a reminder.",
        requiresConfirmation = true
    ),
    START_TIMER(
        toolName = "start_timer",
        displayName = "In-App Timer",
        description = "Starts an in-app countdown timer."
    ),
    CANCEL_TIMER(
        toolName = "cancel_timer",
        displayName = "Cancel Timer",
        description = "Cancels the active in-app timer."
    ),
    COPY_TO_CLIPBOARD(
        toolName = "copy_to_clipboard",
        displayName = "Copy to Clipboard",
        description = "Copies text to the system clipboard."
    ),
    SHARE_CONTENT(
        toolName = "share_content",
        displayName = "Share via Android Sheet",
        description = "Opens the Android system share sheet with text or content."
    ),
    OPEN_APP(
        toolName = "open_app",
        displayName = "Open Installed App",
        description = "Launches an installed application via Android package manager."
    ),
    OPEN_SETTINGS(
        toolName = "open_settings",
        displayName = "Open System Settings",
        description = "Opens specific Android system settings (WiFi, Bluetooth, Apps, Sound, etc.)."
    ),
    OPEN_URL(
        toolName = "open_url",
        displayName = "Open Web URL",
        description = "Validates and opens a website in the default browser."
    ),
    WEB_SEARCH(
        toolName = "web_search",
        displayName = "Web Search",
        description = "Performs a web search via browser or configured search API."
    ),
    DRAFT_SMS(
        toolName = "draft_sms",
        displayName = "Draft SMS Message",
        description = "Opens SMS app with pre-filled recipient or body.",
        requiresConfirmation = true
    ),
    DRAFT_EMAIL(
        toolName = "draft_email",
        displayName = "Draft Email",
        description = "Opens email app with pre-filled subject and body.",
        requiresConfirmation = true
    )
}

enum class ActionStatus {
    PENDING,
    NEEDS_CONFIRMATION,
    NEEDS_PERMISSION,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}

data class ActionRequest(
    val actionId: String,
    val toolType: ActionToolType,
    val arguments: Map<String, String>,
    val userPrompt: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActionResult(
    val actionId: String,
    val toolType: ActionToolType,
    val status: ActionStatus,
    val userFeedbackMessage: String,
    val technicalDetails: String? = null,
    val payload: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)
