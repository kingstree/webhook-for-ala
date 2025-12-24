package aladin.webhook.application

import aladin.webhook.application.dto.AccountWebhookPayload
import aladin.webhook.common.error.BusinessException
import aladin.webhook.common.error.InboxErrorType
import aladin.webhook.infra.mybatis.mapper.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InboxProcessorService(
    private val inboxEventMapper: InboxEventMapper,
    private val inboxAttemptMapper: InboxAttemptMapper,
    private val userMapper: UserMapper,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun processOne(eventId: String) {
        val locked = inboxEventMapper.updateToProcessingIfReceived(eventId) == 1
        if (!locked) return

        val attemptNo = inboxAttemptMapper.selectNextAttemptNo(eventId)
        inboxAttemptMapper.insertStarted(InsertAttemptParam(eventId, attemptNo))

        try {
            val ev = inboxEventMapper.selectByEventId(eventId)
                ?: throw BusinessException(InboxErrorType.EVENT_NOT_FOUND)

            val payloadJson = ev.payloadJson
                ?: throw BusinessException(InboxErrorType.PAYLOAD_JSON_MISSING)

            val payload = parsePayload(payloadJson)
            validatePayloadByEventType(ev.eventType, payload)

            when (ev.eventType) {
                "EMAIL_FORWARDING_CHANGED" -> {
                    val email = payload.email!!
                    if (userMapper.updateEmail(payload.accountKey, email) != 1) {
                        throw BusinessException(InboxErrorType.USER_NOT_FOUND)
                    }
                }

                "ACCOUNT_DELETED" -> {
                    if (userMapper.updateStatus(payload.accountKey, "DELETED") != 1) {
                        throw BusinessException(InboxErrorType.USER_NOT_FOUND)
                    }
                }
                /**
                 * TODO: 선택지 B (서버가 sourceSystem 기반으로 해석)  -> 코드 리뷰 과정 나온 개선 필요사항
                 *
                 * payload는 ACCOUNT_DELETED지만
                 * sourceSystem이 APPLE면 → APPLE_DELETED
                 */
                "APPLE_ACCOUNT_DELETED" -> {
                    if (userMapper.updateStatus(payload.accountKey, "APPLE_ACCOUNT_DELETED") != 1) {
                        throw BusinessException(InboxErrorType.USER_NOT_FOUND)
                    }
                }
            }

            inboxAttemptMapper.updateSucceeded(UpdateAttemptParam(eventId, attemptNo))
            inboxEventMapper.updateDone(eventId)

        } catch (e: Exception) {
            val code = when (e) {
                is BusinessException -> e.errorType.code
                else -> (e.message ?: "UNKNOWN_ERROR")
            }
            inboxAttemptMapper.updateFailed(UpdateAttemptFailedParam(eventId, attemptNo, code))
            inboxEventMapper.updateFailed(UpdateFailedParam(eventId, code))
        }
    }

    /**
     * RECEIVED 상태의 이벤트를 조회해 순차 처리합니다.
     * - SQLite + 단일 워커(풀 1) 전제에서 안전한 방식
     */
    fun processReceived(limit: Int = 50): Int {
        val received = inboxEventMapper.selectReceived(limit)
        received.forEach { processOne(it.eventId) }
        return received.size
    }

    private fun parsePayload(json: String): AccountWebhookPayload {
        if (json.isBlank()) {
            // blank payload should be treated as invalid JSON (do not call ObjectMapper)
            throw BusinessException(InboxErrorType.INVALID_PAYLOAD_JSON)
        }

        return try {
            objectMapper.readValue(json, AccountWebhookPayload::class.java)
        } catch (e: Exception) {
            throw BusinessException(InboxErrorType.INVALID_PAYLOAD_JSON, cause = e)
        }
    }

    private fun validatePayloadByEventType(
        eventType: String,
        payload: AccountWebhookPayload
    ) {
        when (eventType) {
            "EMAIL_FORWARDING_CHANGED" -> {
                if (payload.email.isNullOrBlank()) {
                    throw BusinessException(InboxErrorType.EMAIL_REQUIRED_FOR_EVENT)
                }
            }
            "ACCOUNT_DELETED", "APPLE_ACCOUNT_DELETED" -> {
                // no additional required fields
            }
            else -> throw BusinessException(InboxErrorType.UNSUPPORTED_EVENT_TYPE)
        }
    }
}
