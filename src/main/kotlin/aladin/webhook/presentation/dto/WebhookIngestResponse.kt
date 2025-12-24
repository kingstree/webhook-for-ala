package aladin.webhook.presentation.dto

data class WebhookIngestResponse(
    val eventId: String,
    val sourceSystem: String,
    val idempotency: String,  // CREATED / DUPLICATE_DONE ...
    val status: String,       // RECEIVED / PROCESSING / DONE / FAILED
    val message: String,
    val receivedAt: String? = null,
    val processedAt: String? = null
)
