package aladin.webhook.unit.application

import aladin.webhook.application.InboxQueryService
import aladin.webhook.application.dto.AttemptView
import aladin.webhook.application.dto.InboxEventView
import aladin.webhook.infra.mybatis.mapper.InboxAttemptMapper
import aladin.webhook.infra.mybatis.mapper.InboxEventMapper
import aladin.webhook.infra.mybatis.model.InboxAttemptRow
import aladin.webhook.infra.mybatis.model.InboxEventRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class InboxQueryServiceUnitTest {

    private val inboxEventMapper: InboxEventMapper = mock()
    private val inboxAttemptMapper: InboxAttemptMapper = mock()

    private val sut = InboxQueryService(
        inboxEventMapper = inboxEventMapper,
        inboxAttemptMapper = inboxAttemptMapper
    )

    @Test
    fun `이벤트가 없으면 null을 반환하고 attempt 조회를 하지 않는다`() {
        // given
        whenever(inboxEventMapper.selectByEventId("evt-404")).thenReturn(null)

        // when
        val result = sut.getEvent("evt-404")

        // then
        assertNull(result)
        verify(inboxAttemptMapper, never()).selectByEventId("evt-404")
    }

    @Test
    fun `이벤트가 있으면 event와 attempts를 view로 매핑해서 반환한다`() {
        // given
        val eventRow = InboxEventRow(
            eventId = "evt-100",
            sourceSystem = "INTERNAL",
            eventType = "EMAIL_FORWARDING_CHANGED",
            accountKey = "u-100",
            status = "RECEIVED",
            errorMessage = null,
            receivedAt = "2025-12-23 12:57:32",
            processedAt = null,
            inboxEventId = 1L,
            signature = "test-signature",
            requestTimestamp = 1766494651L,
            payloadJson = """{"type":"EMAIL_FORWARDING_CHANGED","before":false,"after":true}"""
        )

        val attemptRows = listOf(
            InboxAttemptRow(
                eventId = "evt-100",
                attemptNo = 1,
                attemptStatus = "PROCESSING",
                startedAt = "2025-12-23 12:58:00",
                endedAt = null,
                errorMessage = null
            ),
            InboxAttemptRow(
                eventId = "evt-100",
                attemptNo = 2,
                attemptStatus = "FAILED",
                startedAt = "2025-12-23 12:59:00",
                endedAt = "2025-12-23 12:59:02",
                errorMessage = "boom"
            )
        )

        whenever(inboxEventMapper.selectByEventId("evt-100")).thenReturn(eventRow)
        whenever(inboxAttemptMapper.selectByEventId("evt-100")).thenReturn(attemptRows)

        // when
        val result = sut.getEvent("evt-100")

        // then
        val expected = InboxEventView(
            eventId = "evt-100",
            sourceSystem = "INTERNAL",
            eventType = "EMAIL_FORWARDING_CHANGED",
            accountKey = "u-100",
            status = "RECEIVED",
            errorMessage = null,
            receivedAt = "2025-12-23 12:57:32",
            processedAt = null,
            attempts = listOf(
                AttemptView(
                    attemptNo = 1,
                    attemptStatus = "PROCESSING",
                    startedAt = "2025-12-23 12:58:00",
                    endedAt = null,
                    errorMessage = null
                ),
                AttemptView(
                    attemptNo = 2,
                    attemptStatus = "FAILED",
                    startedAt = "2025-12-23 12:59:00",
                    endedAt = "2025-12-23 12:59:02",
                    errorMessage = "boom"
                )
            )
        )

        assertEquals(expected, result)

        verify(inboxEventMapper).selectByEventId("evt-100")
        verify(inboxAttemptMapper).selectByEventId("evt-100")
    }
}
