package aladin.webhook.domain.dto

data class InboxEventResponse(
    val eventId: String,
    val sourceSystem: String,
    val eventType: String,
    val accountKey: String,
    val status: String,
    val errorMessage: String?,
    val receivedAt: String,
    val processedAt: String?,
    val attempts: List<InboxAttemptResponse> = emptyList()
)
