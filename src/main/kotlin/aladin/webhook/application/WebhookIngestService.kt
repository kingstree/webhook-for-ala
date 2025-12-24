package aladin.webhook.application


import aladin.webhook.common.error.BusinessException
import aladin.webhook.common.error.CommonErrorType
import aladin.webhook.domain.InboxEventStatus
import aladin.webhook.domain.InboxEventStatus.*
import aladin.webhook.infra.mybatis.mapper.InboxEventMapper
import aladin.webhook.infra.mybatis.mapper.InsertInboxEventParam
import aladin.webhook.presentation.dto.WebhookIngestResponse
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class WebhookIngestService(
    private val inboxEventMapper: InboxEventMapper
) {
    fun ingest(eventId: String,
        sourceSystem: String,
        eventType: String,
        accountKey: String,
        signature: String,
        requestTimestamp: Long,
        payloadJson: String
    ): WebhookIngestResponse {
        val param = InsertInboxEventParam(
            eventId = eventId,
            sourceSystem = sourceSystem,
            eventType = eventType,
            accountKey = accountKey,
            signature = signature,
            requestTimestamp = requestTimestamp,
            payloadJson = payloadJson
        )

        val inserted = try {
            // MyBatis + SQLite 에서 UNIQUE 위반이 DuplicateKeyException으로 번역되지 않고
            // UncategorizedSQLException / DataIntegrityViolationException으로 올라오는 경우가 있어
            // 중복으로 처리한다.
            inboxEventMapper.insertReceived(param) > 0
        } catch (e: Exception) {
            when (e) {
                is DuplicateKeyException,
                is org.springframework.dao.DataIntegrityViolationException,
                is org.springframework.jdbc.UncategorizedSQLException -> false
                else -> throw e
            }
        }

        val row = inboxEventMapper.selectByEventId(eventId)
            ?: throw BusinessException(CommonErrorType.INTERNAL_ERROR, "EVENT_NOT_FOUND_AFTER_INSERT")

        val status = InboxEventStatus.fromDb(row.status)

        if (inserted) {
            return WebhookIngestResponse(
                eventId = row.eventId,
                sourceSystem = row.sourceSystem,
                idempotency = "CREATED",
                status = RECEIVED.toString() ,
                message = "접수됨",
                receivedAt = row.receivedAt,
                processedAt = row.processedAt
            )
        }

        val idem = when (status) {
            RECEIVED -> "DUPLICATE_RECEIVED"
            PROCESSING -> "DUPLICATE_PROCESSING"
            DONE -> "DUPLICATE_DONE"
            FAILED -> "DUPLICATE_FAILED"
            UNKNOWN -> "DUPLICATE_UNKNOWN"
            null -> TODO()
        }

        val msg = when (status) {
            RECEIVED -> "접수됨"
            PROCESSING -> "처리 중"
            DONE -> "이미 처리됨"
            FAILED -> "처리 실패(조회 API 확인)"
            UNKNOWN -> "중복 이벤트"
        }

        return WebhookIngestResponse(
            eventId = row.eventId,
            sourceSystem = row.sourceSystem,
            idempotency = idem,
            status = status.name,
            message = msg,
            receivedAt = row.receivedAt,
            processedAt = row.processedAt
        )
    }
}
