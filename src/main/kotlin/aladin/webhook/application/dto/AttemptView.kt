package aladin.webhook.application.dto

data class AttemptView(
    val attemptNo: Int,
    val attemptStatus: String,
    val startedAt: String,
    val endedAt: String?,
    val errorMessage: String?
)
