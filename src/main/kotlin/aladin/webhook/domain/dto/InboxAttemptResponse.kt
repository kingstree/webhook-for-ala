package aladin.webhook.domain.dto

data class InboxAttemptResponse(
    val attemptNo: Int,
    val attemptStatus: String,
    val startedAt: String,
    val endedAt: String?,
    val errorMessage: String?
)
