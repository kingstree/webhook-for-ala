package aladin.webhook.domain.dto

import aladin.webhook.domain.EventStatus
import aladin.webhook.domain.IdempotencyResult

data class WebhookIngestResult(
    val eventId: String,
    val sourceSystem: String,
    val idempotency: IdempotencyResult,
    val status: EventStatus,
    val message: String,
    val receivedAt: String? = null,
    val processedAt: String? = null
)
