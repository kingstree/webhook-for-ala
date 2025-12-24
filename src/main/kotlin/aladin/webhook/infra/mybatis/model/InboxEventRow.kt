package aladin.webhook.infra.mybatis.model

data class InboxEventRow(
    val inboxEventId: Long? = null,
    val eventId: String,
    val sourceSystem: String,
    val eventType: String,
    val accountKey: String,
    val signature: String,
    val requestTimestamp: Long,
    val payloadJson: String,
    val status: String,
    val errorMessage: String? = null,
    val receivedAt: String,
    val processedAt: String? = null
)
