package aladin.webhook.application.dto

data class AccountWebhookPayload(
    val eventType: String,
    val accountKey: String,
    val email: String? = null
)
