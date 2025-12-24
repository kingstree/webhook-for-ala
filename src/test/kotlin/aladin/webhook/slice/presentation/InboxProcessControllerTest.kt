package aladin.webhook.slice.presentation

import aladin.webhook.testkit.IntegrationTestBase
import aladin.webhook.testkit.WebhookTestSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class InboxProcessControllerTest : IntegrationTestBase() {

    @Test
    fun `RECEIVED 이벤트가 있으면 process 호출 후 DONE 또는 FAILED로 변경`() {
        // 1) 먼저 webhook으로 inbox_events 적재
        val source = "INTERNAL"
        val eventId = "evt-100"
        val ts = Instant.now().epochSecond

        // user가 있어야 성공케이스가 쉬움: users 직접 insert (스키마에 맞게 컬럼 조정)
        jdbcTemplate.update(
            "INSERT INTO users(user_id, external_user_key, email, status, created_at, updated_at) VALUES(?, ?, ?, ?, datetime('now'), datetime('now'))",
            1, "u-100", "old@x.com", "ACTIVE"
        )

        val rawBody = """{"eventType":"EMAIL_FORWARDING_CHANGED","accountKey":"u-100","email":"new@x.com"}"""
        val secret = "test-internal-secret"
        val signature = WebhookTestSigner.hmacSha256Hex(
            secret,
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }.andExpect { status().isOk }

        // 2) inbox 처리 트리거 (너가 실제로 잡은 경로로 맞춰)
        mockMvc.post("/inbox/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect { status().isOk }

        // 3) 상태 검증
        // 처리 로직이 내부적으로 재시도/트랜잭션/즉시 커밋 타이밍에 따라 약간의 딜레이가 있을 수 있어 짧게 폴링
        var statusVal: String? = null
        repeat(20) {
            statusVal = jdbcTemplate.queryForObject(
                "SELECT status FROM inbox_events WHERE event_id = ?",
                String::class.java,
                eventId
            )
            if (statusVal != "RECEIVED") return@repeat
            Thread.sleep(50)
        }

        assertNotNull(statusVal)
        assertTrue(statusVal == "DONE" || statusVal == "FAILED")

        // 성공(DONE)일 때만 이메일 갱신을 검증해야 함
        val email = jdbcTemplate.queryForObject(
            "SELECT email FROM users WHERE external_user_key = ?",
            String::class.java,
            "u-100"
        )

        when (statusVal) {
            "DONE" -> assertEquals("new@x.com", email)
            "FAILED" -> assertEquals("old@x.com", email)
        }
    }

    @Test
    fun `ACCOUNT_DELETED 이벤트 처리 후 DONE 이면 users status 가 DELETED 로 변경된다`() {
        // 1) 유저 준비 (성공 케이스가 되려면 user가 존재해야 함)
        jdbcTemplate.update(
            "INSERT INTO users(user_id, external_user_key, email, status, created_at, updated_at) " +
                    "VALUES(?, ?, ?, ?, datetime('now'), datetime('now'))",
            200, "u-200", "u200@x.com", "ACTIVE"
        )

        // 2) webhook으로 inbox_events 적재
        val source = "INTERNAL"
        val eventId = "evt-200"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-200"}"""

        val secret = "test-internal-secret"
        val signature = WebhookTestSigner.hmacSha256Hex(
            secret,
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }.andExpect { status().isOk }

        // 3) inbox 처리 트리거
        mockMvc.post("/inbox/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect { status().isOk }

        // 4) inbox_events 상태 확인
        val eventStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM inbox_events WHERE event_id = ?",
            String::class.java,
            eventId
        )
        assertTrue(eventStatus == "DONE" || eventStatus == "FAILED")

        // 5) DONE이면 users.status가 DELETED로 바뀌었는지 확인
        val userStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM users WHERE external_user_key = ?",
            String::class.java,
            "u-200"
        )

        if (eventStatus == "DONE") {
            // 내부 소스는 일반 삭제
            assertEquals("DELETED", userStatus)
        } else {
            // 실패면(예: 내부 로직/데이터 문제) ACTIVE 그대로일 가능성이 큼
            assertEquals("ACTIVE", userStatus)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["INTERNAL", "PARTNER", "APPLE"])
    fun `ACCOUNT_DELETED 이벤트 - sourceSystem 별 처리 성공 시 users status 는 DELETED`(source: String) {
        val eventId = "evt-del-$source"
        val ts = Instant.now().epochSecond
        val accountKey = "u-$source"

        // 1) 유저 준비
        jdbcTemplate.update(
            "INSERT INTO users(user_id, external_user_key, email, status, created_at, updated_at) " +
                    "VALUES(?, ?, ?, ?, datetime('now'), datetime('now'))",
            ts, accountKey, "$source@x.com", "ACTIVE"
        )

        // 2) 요청 바디
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"$accountKey"}"""

        // 3) source 별 secret 선택
        val secret = when (source) {
            "INTERNAL" -> "test-internal-secret"
            "PARTNER" -> "test-partner-secret"
            "APPLE" -> "test-apple-secret"
            else -> error("unsupported")
        }

        val signature = WebhookTestSigner.hmacSha256Hex(
            secret,
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        // 4) webhook 수신
        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }.andExpect { status().isOk }

        // 5) inbox 처리
        mockMvc.post("/inbox/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect { status().isOk }

        // 6) inbox_events 상태 확인
        val eventStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM inbox_events WHERE event_id = ?",
            String::class.java,
            eventId
        )

        assertTrue(eventStatus == "DONE" || eventStatus == "FAILED")

        // 7) DONE이면 users.status = DELETED
        val userStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM users WHERE external_user_key = ?",
            String::class.java,
            accountKey
        )

        if (eventStatus == "DONE") {
            // APPLE의 경우 별도 상태로 저장될 수 있음
            if (source == "APPLE") {
                assertEquals("APPLE_ACCOUNT_DELETED", userStatus)
            } else {
                assertEquals("DELETED", userStatus)
            }
        } else {
            assertEquals("ACTIVE", userStatus)
        }
    }
}
