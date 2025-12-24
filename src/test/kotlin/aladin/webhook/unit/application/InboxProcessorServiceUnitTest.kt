package aladin.webhook.unit.application


import aladin.webhook.application.InboxProcessorService
import aladin.webhook.application.dto.AccountWebhookPayload
import aladin.webhook.common.error.InboxErrorType
import aladin.webhook.infra.mybatis.mapper.*
import aladin.webhook.infra.mybatis.model.InboxEventRow
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Test


class InboxProcessorServiceUnitTest {

    private val inboxEventMapper: InboxEventMapper = mockk()
    private val inboxAttemptMapper: InboxAttemptMapper = mockk()
    private val userMapper: UserMapper = mockk()
    private val objectMapper: ObjectMapper = mockk()

    private val inboxProcessorService =
        InboxProcessorService(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)

    @Test
    fun `JSON 파싱이 실패하면 INVALID_PAYLOAD_JSON 으로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-1"
        val attemptNo = 1

        // lock 획득 성공
        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        // attempt 시작
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        // event 조회 결과: payloadJson은 있으나 JSON 파싱이 깨진 상태
        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "EMAIL_FORWARDING_CHANGED"
        every { ev.accountKey } returns "acc-1"
        every { ev.payloadJson } returns """{ "accountKey": "acc-1", "email": "a@b.com" """ // intentionally invalid JSON
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        // objectMapper 파싱 실패 유도
        every { objectMapper.readValue(any<String>(), any<Class<*>>()) } throws RuntimeException("boom")

        // 실패 처리/성공 처리 stub
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1
        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert: 호출 순서 + 실패 코드
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), any<Class<*>>())
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                        it.eventId == eventId &&
                        it.attemptNo == attemptNo &&
                        it.errorMessage == InboxErrorType.INVALID_PAYLOAD_JSON.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                        it.eventId == eventId &&
                        it.errorMessage == InboxErrorType.INVALID_PAYLOAD_JSON.code
            })
        }

        // 성공 처리 호출되면 안 됨
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }

        // user 변경도 호출되면 안 됨
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }
    }

    @Test
    fun `지원하지 않는 eventType 이면 UNSUPPORTED_EVENT_TYPE 으로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-2"
        val attemptNo = 1

        // lock 획득 성공
        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        // attempt 시작
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        // event 조회 결과: 지원하지 않는 eventType, valid payloadJson
        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "SOMETHING_NEW"
        every { ev.accountKey } returns "acc-2"
        every { ev.payloadJson } returns """{"accountKey":"acc-2"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        // objectMapper 정상 파싱
        every { objectMapper.readValue(any<String>(), any<Class<*>>()) } returns
            AccountWebhookPayload(
                accountKey = "acc-2",
                email = null,
                eventType = "EMAIL_FORWARDING_CHANGED"
            )

        // 실패 처리/성공 처리 stub
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1
        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert: 호출 순서 + 실패 코드
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), any<Class<*>>())
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                        it.eventId == eventId &&
                        it.attemptNo == attemptNo &&
                        it.errorMessage == InboxErrorType.UNSUPPORTED_EVENT_TYPE.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                        it.eventId == eventId &&
                        it.errorMessage == InboxErrorType.UNSUPPORTED_EVENT_TYPE.code
            })
        }

        // 성공 처리 호출되면 안 됨
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }

        // user 변경도 호출되면 안 됨
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `USER_NOT_FOUND 이면 USER_NOT_FOUND 로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-3"
        val attemptNo = 1

        // lock 획득 성공
        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        // attempt 시작
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        // event 조회 결과: EMAIL_FORWARDING_CHANGED, 정상 payloadJson
        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "EMAIL_FORWARDING_CHANGED"
        every { ev.accountKey } returns "acc-3"
        every { ev.payloadJson } returns """{"accountKey":"acc-3","email":"a@b.com"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        // objectMapper 정상 파싱
        every { objectMapper.readValue(any<String>(), any<Class<*>>()) } returns
            AccountWebhookPayload(
                accountKey = "acc-3",
                email = "a@b.com",
                eventType = "EMAIL_FORWARDING_CHANGED"
            )

        // userMapper update 실패 (user not found)
        every { userMapper.updateEmail("acc-3", "a@b.com") } returns 0

        // 실패 처리/성공 처리 stub
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1
        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert: 호출 순서 + 실패 코드
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), any<Class<*>>())
            userMapper.updateEmail("acc-3", "a@b.com")
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                    it.eventId == eventId &&
                    it.attemptNo == attemptNo &&
                    it.errorMessage == InboxErrorType.USER_NOT_FOUND.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                    it.eventId == eventId &&
                    it.errorMessage == InboxErrorType.USER_NOT_FOUND.code
            })
        }

        // 성공 처리 호출되면 안 됨
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }

        // user status 변경도 호출되면 안 됨
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }
    @Test
    fun `EMAIL_FORWARDING_CHANGED 성공이면 attempt 는 SUCCEEDED, event 는 DONE 으로 기록되고 user email 이 변경된다`() {
        val eventId = "ev-ok-1"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "EMAIL_FORWARDING_CHANGED"
        every { ev.accountKey } returns "acc-ok-1"
        every { ev.payloadJson } returns """{"accountKey":"acc-ok-1","email":"ok@x.com"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        every { objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java) } returns
            AccountWebhookPayload(
                accountKey = "acc-ok-1",
                email = "ok@x.com",
                eventType = "EMAIL_FORWARDING_CHANGED"
            )

        every { userMapper.updateEmail("acc-ok-1", "ok@x.com") } returns 1

        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(eventId) } returns 1

        // 실패 처리 stub (호출되면 안 되지만 relaxed 방지용)
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java)
            userMapper.updateEmail("acc-ok-1", "ok@x.com")
            inboxAttemptMapper.updateSucceeded(match {
                it is UpdateAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.updateDone(eventId)
        }

        verify(exactly = 0) { inboxAttemptMapper.updateFailed(any()) }
        verify(exactly = 0) { inboxEventMapper.updateFailed(any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `ACCOUNT_DELETED 성공이면 attempt 는 SUCCEEDED, event 는 DONE 으로 기록되고 user status 가 DELETED 로 변경된다`() {
        val eventId = "ev-ok-2"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "ACCOUNT_DELETED"
        every { ev.accountKey } returns "acc-ok-2"
        every { ev.payloadJson } returns """{"accountKey":"acc-ok-2"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        every { objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java) } returns
            AccountWebhookPayload(
                accountKey = "acc-ok-2",
                email = null,
                eventType = "ACCOUNT_DELETED"
            )

        every { userMapper.updateStatus("acc-ok-2", "DELETED") } returns 1

        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(eventId) } returns 1

        // 실패 처리 stub (호출되면 안 되지만 relaxed 방지용)
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java)
            userMapper.updateStatus("acc-ok-2", "DELETED")
            inboxAttemptMapper.updateSucceeded(match {
                it is UpdateAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.updateDone(eventId)
        }

        verify(exactly = 0) { inboxAttemptMapper.updateFailed(any()) }
        verify(exactly = 0) { inboxEventMapper.updateFailed(any()) }
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }
    @Test
    fun `lock 획득 실패(updateToProcessingIfReceived=0)면 아무 처리도 하지 않는다`() {
        val eventId = "ev-lock-0"

        // lock 획득 실패(이미 다른 스레드/프로세스가 처리 중이거나 DONE)
        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 0

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert: lock 실패면 attempt 시작/조회/업데이트, user 변경, JSON 파싱 등이 전혀 일어나면 안 됨
        verify(exactly = 1) { inboxEventMapper.updateToProcessingIfReceived(eventId) }
        verify(exactly = 0) { inboxAttemptMapper.selectNextAttemptNo(any()) }
        verify(exactly = 0) { inboxAttemptMapper.insertStarted(any()) }
        verify(exactly = 0) { inboxEventMapper.selectByEventId(any()) }
        verify(exactly = 0) { objectMapper.readValue(any<String>(), any<Class<*>>()) }
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxAttemptMapper.updateFailed(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }
        verify(exactly = 0) { inboxEventMapper.updateFailed(any()) }
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `event 조회가 null이면 EVENT_NOT_FOUND 로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-not-found"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        // event row가 없다
        every { inboxEventMapper.selectByEventId(eventId) } returns null

        // 실패 처리 stub
        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                        it.eventId == eventId &&
                        it.attemptNo == attemptNo &&
                        it.errorMessage == InboxErrorType.EVENT_NOT_FOUND.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                        it.eventId == eventId &&
                        it.errorMessage == InboxErrorType.EVENT_NOT_FOUND.code
            })
        }

        verify(exactly = 0) { objectMapper.readValue(any<String>(), any<Class<*>>()) }
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `payloadJson이 빈 값이면 INVALID_PAYLOAD_JSON 으로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-payload-missing"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "EMAIL_FORWARDING_CHANGED"
        every { ev.accountKey } returns "acc-payload-missing"
        every { ev.payloadJson } returns "" // missing
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                        it.eventId == eventId &&
                        it.attemptNo == attemptNo &&
                        it.errorMessage == InboxErrorType.INVALID_PAYLOAD_JSON.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                        it.eventId == eventId &&
                        it.errorMessage == InboxErrorType.INVALID_PAYLOAD_JSON.code
            })
        }

        verify(exactly = 0) { objectMapper.readValue(any<String>(), any<Class<*>>()) }
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `EMAIL_FORWARDING_CHANGED 인데 email 누락이면 EMAIL_REQUIRED 으로 attempt 와 event 가 FAILED 로 기록된다`() {
        val eventId = "ev-email-missing"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "EMAIL_FORWARDING_CHANGED"
        every { ev.accountKey } returns "acc-email-missing"
        every { ev.payloadJson } returns """{"accountKey":"acc-email-missing"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        // objectMapper는 정상 파싱되지만 email이 null
        every { objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java) } returns
                AccountWebhookPayload(
                    accountKey = "acc-email-missing",
                    email = null,
                    eventType = "EMAIL_FORWARDING_CHANGED"
                )

        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java)
            inboxAttemptMapper.updateFailed(match {
                it is UpdateAttemptFailedParam &&
                        it.eventId == eventId &&
                        it.attemptNo == attemptNo &&
                        it.errorMessage == InboxErrorType.EMAIL_REQUIRED_FOR_EVENT.code
            })
            inboxEventMapper.updateFailed(match {
                it is UpdateFailedParam &&
                        it.eventId == eventId &&
                        it.errorMessage == InboxErrorType.EMAIL_REQUIRED_FOR_EVENT.code
            })
        }

        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }
        verify(exactly = 0) { userMapper.updateStatus(any(), any()) }
        verify(exactly = 0) { inboxAttemptMapper.updateSucceeded(any()) }
        verify(exactly = 0) { inboxEventMapper.updateDone(any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

    @Test
    fun `APPLE_ACCOUNT_DELETED 성공이면 attempt 는 SUCCEEDED, event 는 DONE 으로 기록되고 user status 가 APPLE_DELETED 로 변경된다`() {
        val eventId = "ev-apple-ok"
        val attemptNo = 1

        every { inboxEventMapper.updateToProcessingIfReceived(eventId) } returns 1
        every { inboxAttemptMapper.selectNextAttemptNo(eventId) } returns attemptNo
        every { inboxAttemptMapper.insertStarted(any()) } returns 1

        val ev = mockk<InboxEventRow>(relaxed = true)
        every { ev.eventType } returns "APPLE_ACCOUNT_DELETED"
        every { ev.accountKey } returns "acc-apple-1"
        every { ev.payloadJson } returns """{"accountKey":"acc-apple-1"}"""
        every { inboxEventMapper.selectByEventId(eventId) } returns ev

        every { objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java) } returns
                AccountWebhookPayload(
                    accountKey = "acc-apple-1",
                    email = null,
                    eventType = "APPLE_ACCOUNT_DELETED"
                )

        // 현재 서비스 로직은 user status를 eventType 문자열로 저장한다.
        // (APPLE_ACCOUNT_DELETED 요청이면 APPLE_ACCOUNT_DELETED 로 updateStatus 호출됨)
        every { userMapper.updateStatus("acc-apple-1", "APPLE_ACCOUNT_DELETED") } returns 1

        every { inboxAttemptMapper.updateSucceeded(any()) } returns 1
        every { inboxEventMapper.updateDone(eventId) } returns 1

        every { inboxAttemptMapper.updateFailed(any()) } returns 1
        every { inboxEventMapper.updateFailed(any()) } returns 1

        // Act
        inboxProcessorService.processOne(eventId)

        // Assert
        verifyOrder {
            inboxEventMapper.updateToProcessingIfReceived(eventId)
            inboxAttemptMapper.selectNextAttemptNo(eventId)
            inboxAttemptMapper.insertStarted(match {
                it is InsertAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.selectByEventId(eventId)
            objectMapper.readValue(any<String>(), AccountWebhookPayload::class.java)
            userMapper.updateStatus("acc-apple-1", "APPLE_ACCOUNT_DELETED")
            inboxAttemptMapper.updateSucceeded(match {
                it is UpdateAttemptParam && it.eventId == eventId && it.attemptNo == attemptNo
            })
            inboxEventMapper.updateDone(eventId)
        }

        verify(exactly = 0) { inboxAttemptMapper.updateFailed(any()) }
        verify(exactly = 0) { inboxEventMapper.updateFailed(any()) }
        verify(exactly = 0) { userMapper.updateEmail(any(), any()) }

        confirmVerified(inboxEventMapper, inboxAttemptMapper, userMapper, objectMapper)
    }

}


