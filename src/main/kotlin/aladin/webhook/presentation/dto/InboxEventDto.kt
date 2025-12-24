package aladin.webhook.presentation.dto



data class InboxEventDto(
    val eventId: String,
    val sourceSystem: String,
    val eventType: String,
    val accountKey: String,
    val status: String,
    val errorMessage: String?,
    val receivedAt: String,
    val processedAt: String?,
    val attempts: List<AttemptDto>
)
