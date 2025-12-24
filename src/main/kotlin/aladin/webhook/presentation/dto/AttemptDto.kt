package aladin.webhook.presentation.dto

data class AttemptDto(
    val attemptNo: Int,
    val attemptStatus: String,
    val startedAt: String,
    val endedAt: String?,
    val errorMessage: String?
)
