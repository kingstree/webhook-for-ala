package aladin.webhook.slice.webhooks


import aladin.webhook.testkit.IntegrationTestBase
import aladin.webhook.testkit.WebhookTestSigner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.time.Instant

class WebhookReceiveSliceTest : IntegrationTestBase() {

    @Test
    fun `서명 검증 성공 - CREATED`() {
        val source = "INTERNAL"
        val eventId = "evt-sig-ok-1"
        val ts = Instant.now().epochSecond
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
        }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.eventId") { value(eventId) } }
            .andExpect { jsonPath("$.idempotency") { value("CREATED") } }
            .andExpect { jsonPath("$.status") { value("RECEIVED") } }

        val cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inbox_events WHERE event_id = ?",
            Long::class.java,
            eventId
        )
        assertEquals(1L, cnt)
    }

    @Test
    fun `서명 검증 실패 - 에러 응답`() {
        val source = "INTERNAL"
        val eventId = "evt-sig-bad-1"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"EMAIL_FORWARDING_CHANGED","accountKey":"u-100","email":"new@x.com"}"""

        // 일부러 틀린 secret
        val wrongSecret = "wrong-secret"
        val signature = WebhookTestSigner.hmacSha256Hex(
            wrongSecret,
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        val mvcResult = mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }
            // 현재 네 전역 예외처리가 UnauthorizedException을 500으로 만들 수도 있어.
            // 정책을 FORBIDDEN(403)로 맞추면 여기 isForbidden()으로 변경.
            .andExpect { status { isForbidden() } }
            .andReturn()

        // 현재 구현은 403만 내려주고 바디/콘텐트타입이 비어있을 수 있음
        val body = mvcResult.response.contentAsString
        assertTrue(body.isBlank())
    }

    @Test
    fun `동일 eventId 재전송 - DB는 1건만`() {
        val source = "INTERNAL"
        val eventId = "evt-dup-1"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-1"}"""

        val secret = "test-internal-secret"
        val signature = WebhookTestSigner.hmacSha256Hex(
            secret,
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        repeat(2) {
            mockMvc.post("/webhooks/account-changes") {
                contentType = MediaType.APPLICATION_JSON
                content = rawBody
                header("X-Source-System", source)
                header("X-Event-Id", eventId)
                header("X-Timestamp", ts)
                header("X-Signature", signature)
            }.andExpect { status { isOk() } }
        }

        val cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inbox_events WHERE event_id = ?",
            Long::class.java,
            eventId
        )
        assertEquals(1L, cnt)
    }
}
