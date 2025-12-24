package aladin.webhook.unit.application

import aladin.webhook.application.WebhookIngestService
import aladin.webhook.common.error.BusinessException
import aladin.webhook.infra.mybatis.mapper.InboxEventMapper
import aladin.webhook.infra.mybatis.mapper.InsertInboxEventParam
import aladin.webhook.infra.mybatis.model.InboxEventRow
import aladin.webhook.testkit.UnitTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import java.util.stream.Stream

class WebhookIngestServiceUnitTest : UnitTestBase() {

    private val inboxEventMapper: InboxEventMapper = mock()
    private val sut = WebhookIngestService(inboxEventMapper)

    @Test
    fun `신규 이벤트는 insert 성공 - CREATED, RECEIVED 반환`() {
        // given
        val eventId = "evt-1"
        whenever(inboxEventMapper.insertReceived(any<InsertInboxEventParam>())).thenReturn(1)

        val row = InboxEventRow(
            eventId = eventId,
            sourceSystem = "INTERNAL",
            eventType = "EMAIL_FORWARDING_CHANGED",
            accountKey = "u-1",
            signature = "sig",
            requestTimestamp = 123L,
            payloadJson = "{}",
            status = "RECEIVED",
            receivedAt = "2025-12-23 00:00:00",
            processedAt = null
        )
        whenever(inboxEventMapper.selectByEventId(eventId)).thenReturn(row)

        // when
        val res = sut.ingest(
            eventId = eventId,
            sourceSystem = "INTERNAL",
            eventType = "EMAIL_FORWARDING_CHANGED",
            accountKey = "u-1",
            signature = "sig",
            requestTimestamp = 123L,
            payloadJson = "{}"
        )

        // then
        assertEquals(eventId, res.eventId)
        assertEquals("INTERNAL", res.sourceSystem)
        assertEquals("CREATED", res.idempotency)
        assertEquals("RECEIVED", res.status)
        assertEquals("접수됨", res.message)
        assertEquals(row.receivedAt, res.receivedAt)
        assertNull(res.processedAt)

        val paramCaptor = argumentCaptor<InsertInboxEventParam>()
        verify(inboxEventMapper).insertReceived(paramCaptor.capture())
        val p = paramCaptor.firstValue
        assertEquals(eventId, p.eventId)
        assertEquals("INTERNAL", p.sourceSystem)
        assertEquals("EMAIL_FORWARDING_CHANGED", p.eventType)
        assertEquals("u-1", p.accountKey)
        assertEquals("sig", p.signature)
        assertEquals(123L, p.requestTimestamp)
        assertEquals("{}", p.payloadJson)

        verify(inboxEventMapper, times(1)).selectByEventId(eventId)
    }

    @ParameterizedTest(name = "중복 이벤트 dbStatus={0} -> outStatus={1}, idempotency={2}, message={3}")
    @MethodSource("duplicateCases")
    fun `중복 이벤트 - DuplicateKeyException이면 기존 status에 맞는 idempotency와 message 반환`(
        dbStatus: String,
        expectedOutStatus: String,
        expectedIdempotency: String,
        expectedMessage: String
    ) {
        // given
        val eventId = "evt-dup-$dbStatus"
        whenever(inboxEventMapper.insertReceived(any<InsertInboxEventParam>()))
            .thenThrow(DuplicateKeyException("dup"))

        val processedAt = if (dbStatus == "DONE" || dbStatus == "FAILED") "2025-12-23 00:01:00" else null

        val row = InboxEventRow(
            eventId = eventId,
            sourceSystem = "PARTNER",
            eventType = "ACCOUNT_DELETED",
            accountKey = "u-1",
            signature = "sig",
            requestTimestamp = 456L,
            payloadJson = "{}",
            status = dbStatus,
            receivedAt = "2025-12-23 00:00:00",
            processedAt = processedAt
        )
        whenever(inboxEventMapper.selectByEventId(eventId)).thenReturn(row)

        // when
        val res = sut.ingest(
            eventId = eventId,
            sourceSystem = "PARTNER",
            eventType = "ACCOUNT_DELETED",
            accountKey = "u-1",
            signature = "sig",
            requestTimestamp = 456L,
            payloadJson = "{}"
        )

        // then
        assertEquals(expectedIdempotency, res.idempotency)
        assertEquals(expectedOutStatus, res.status)
        assertEquals(expectedMessage, res.message)
        assertEquals(row.processedAt, res.processedAt)

        val paramCaptor = argumentCaptor<InsertInboxEventParam>()
        verify(inboxEventMapper).insertReceived(paramCaptor.capture())
        val p = paramCaptor.firstValue
        assertEquals(eventId, p.eventId)
        assertEquals("PARTNER", p.sourceSystem)
        assertEquals("ACCOUNT_DELETED", p.eventType)
        assertEquals("u-1", p.accountKey)
        assertEquals("sig", p.signature)
        assertEquals(456L, p.requestTimestamp)
        assertEquals("{}", p.payloadJson)

        verify(inboxEventMapper, times(1)).selectByEventId(eventId)
    }

    @Test
    fun `insert 후 selectByEventId가 null이면 BusinessException 발생`() {
        // given
        val eventId = "evt-missing"
        whenever(inboxEventMapper.insertReceived(any<InsertInboxEventParam>())).thenReturn(1)
        whenever(inboxEventMapper.selectByEventId(eventId)).thenReturn(null)

        // when & then
        val ex = assertThrows(BusinessException::class.java) {
            sut.ingest(
                eventId = eventId,
                sourceSystem = "INTERNAL",
                eventType = "EMAIL_FORWARDING_CHANGED",
                accountKey = "u-1",
                signature = "sig",
                requestTimestamp = 1L,
                payloadJson = "{}"
            )
        }
        assertNotNull(ex)

        val paramCaptor = argumentCaptor<InsertInboxEventParam>()
        verify(inboxEventMapper).insertReceived(paramCaptor.capture())
        val p = paramCaptor.firstValue
        assertEquals(eventId, p.eventId)
        assertEquals("INTERNAL", p.sourceSystem)
        assertEquals("EMAIL_FORWARDING_CHANGED", p.eventType)
        assertEquals("u-1", p.accountKey)
        assertEquals("sig", p.signature)
        assertEquals(1L, p.requestTimestamp)
        assertEquals("{}", p.payloadJson)

        verify(inboxEventMapper, times(1)).selectByEventId(eventId)
    }

    companion object {
        @JvmStatic
        fun duplicateCases(): Stream<Arguments> = Stream.of(
            // known statuses -> 그대로 반환
            Arguments.of("RECEIVED", "RECEIVED", "DUPLICATE_RECEIVED", "접수됨"),
            Arguments.of("PROCESSING", "PROCESSING", "DUPLICATE_PROCESSING", "처리 중"),
            Arguments.of("DONE", "DONE", "DUPLICATE_DONE", "이미 처리됨"),
            Arguments.of("FAILED", "FAILED", "DUPLICATE_FAILED", "처리 실패(조회 API 확인)"),
            Arguments.of("UNKNOWN", "UNKNOWN", "DUPLICATE_UNKNOWN", "중복 이벤트"),
            // DB에 예상치 못한 값이 들어온 경우 -> UNKNOWN 으로 매핑되어야 함
            Arguments.of("SOMETHING_ELSE", "UNKNOWN", "DUPLICATE_UNKNOWN", "중복 이벤트")
        )
    }
}
