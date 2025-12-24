package aladin.webhook.infra.mybatis.mapper

data class InsertInboxEventParam(
    val eventId: String,
    val sourceSystem: String,
    val eventType: String,
    val accountKey: String,
    val signature: String,
    val requestTimestamp: Long,
    val payloadJson: String
)
