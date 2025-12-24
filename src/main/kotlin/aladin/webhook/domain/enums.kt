package aladin.webhook.domain

enum class EventStatus { RECEIVED, PROCESSING, DONE, FAILED }
enum class AttemptStatus { STARTED, SUCCEEDED, FAILED }
enum class EventType { EMAIL_FORWARDING_CHANGED, ACCOUNT_DELETED, APPLE_ACCOUNT_DELETED }

enum class IdempotencyResult {
    CREATED, DUPLICATE_RECEIVED, DUPLICATE_PROCESSING, DUPLICATE_DONE, DUPLICATE_FAILED
}

enum class InboxEventStatus {
    RECEIVED,
    PROCESSING,
    DONE,
    FAILED,
    UNKNOWN;

    companion object {
        fun fromDb(v: String?): InboxEventStatus {
            val raw = v?.trim()?.uppercase()
            return when (raw) {
                "RECEIVED" -> RECEIVED
                "PROCESSING" -> PROCESSING
                "DONE" -> DONE
                "FAILED" -> FAILED
                "UNKNOWN" -> UNKNOWN
                null, "" -> UNKNOWN
                else -> UNKNOWN
            }
        }
    }
}
