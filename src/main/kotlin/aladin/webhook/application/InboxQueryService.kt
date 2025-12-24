package aladin.webhook.application


import aladin.webhook.application.dto.AttemptView
import aladin.webhook.application.dto.InboxEventView
import aladin.webhook.infra.mybatis.mapper.InboxAttemptMapper
import aladin.webhook.infra.mybatis.mapper.InboxEventMapper

import org.springframework.stereotype.Service

@Service
class InboxQueryService(
    private val inboxEventMapper: InboxEventMapper,
    private val inboxAttemptMapper: InboxAttemptMapper
) {

    fun getEvent(eventId: String): InboxEventView? {
        val ev = inboxEventMapper.selectByEventId(eventId) ?: return null
        val attempts = inboxAttemptMapper.selectByEventId(eventId).map {
            AttemptView(
                attemptNo = it.attemptNo,
                attemptStatus = it.attemptStatus,
                startedAt = it.startedAt,
                endedAt = it.endedAt,
                errorMessage = it.errorMessage
            )
        }

        return InboxEventView(
            eventId = ev.eventId,
            sourceSystem = ev.sourceSystem,
            eventType = ev.eventType,
            accountKey = ev.accountKey,
            status = ev.status,
            errorMessage = ev.errorMessage,
            receivedAt = ev.receivedAt,
            processedAt = ev.processedAt,
            attempts = attempts
        )
    }
}
