package aladin.webhook.infra.mybatis.model

data class InboxAttemptRow(
    val attemptId: Long? = null,
    val eventId: String,
    val attemptNo: Int,
    val attemptStatus: String,
    val startedAt: String,
    val endedAt: String? = null,
    val errorMessage: String? = null
)
