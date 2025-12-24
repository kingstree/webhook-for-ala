package aladin.webhook.slice.presentation

import aladin.webhook.testkit.IntegrationTestBase
import aladin.webhook.testkit.WebhookTestSigner
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class WebhookControllerTest : IntegrationTestBase() {
    @Test
    fun `정상 서명 요청이면 200 OK and inbox_events 1건 저장`() {
        val source = "APPLE"
        val eventId = "evt-001"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"EMAIL_FORWARDING_CHANGED","accountKey":"u-1","email":"a@b.com"}"""

        val secret = "test-apple-secret"
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
        }.andExpect {
            status().isOk
        }

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inbox_events WHERE event_id = ?",
            Long::class.java,
            eventId
        )!!
        assert(count == 1L)
    }

    @Test
    fun `서명이 틀리면 403`() {
        val source = "APPLE"
        val eventId = "evt-002"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-1"}"""

        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", "deadbeef") // wrong
        }.andExpect {
            status().isForbidden
        }
    }

    @Test
    fun `동일 eventId 재요청은 멱등 - inbox_events는 1건만`() {
        val source = "PARTNER"
        val eventId = "evt-003"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-1"}"""

        val secret = "test-partner-secret"
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
            }.andExpect {
                status().isOk
            }
        }

        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inbox_events WHERE event_id = ?",
            Long::class.java,
            eventId
        )!!
        assert(count == 1L)
    }
}
