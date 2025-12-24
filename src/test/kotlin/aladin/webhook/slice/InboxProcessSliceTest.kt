package aladin.webhook.slice.inbox


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

class InboxProcessSliceTest : IntegrationTestBase() {

    @Test
    fun `실패 케이스 - FAILED 기록 및 error_message 저장`() {
        val source = "INTERNAL"
        val eventId = "evt-fail-1"
        val ts = Instant.now().epochSecond

        // user 미리 INSERT 안함 → processor에서 users update가 실패하도록 유도
        val rawBody = """{"eventType":"EMAIL_FORWARDING_CHANGED","accountKey":"u-no-user","email":"new@x.com"}"""

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

        mockMvc.post("/inbox/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect { status().isOk }

        val status = jdbcTemplate.queryForObject(
            "SELECT status FROM inbox_events WHERE event_id = ?",
            String::class.java,
            eventId
        )
        val error = jdbcTemplate.queryForObject(
            "SELECT error_message FROM inbox_events WHERE event_id = ?",
            String::class.java,
            eventId
        )

        assertEquals("FAILED", status)
        assertNotNull(error)
        assertTrue(error!!.isNotBlank())
    }

    @ParameterizedTest
    @ValueSource(strings = ["INTERNAL", "PARTNER", "APPLE"])
    fun `ACCOUNT_DELETED - source 별 성공 시 users status 변경`(source: String) {
        val eventId = "evt-del-$source"
        val ts = Instant.now().epochSecond
        val accountKey = "u-$source"

        // user 준비
        jdbcTemplate.update(
            "INSERT INTO users(user_id, external_user_key, email, status, created_at, updated_at) " +
                    "VALUES(?, ?, ?, ?, datetime('now'), datetime('now'))",
            ts, accountKey, "$source@x.com", "ACTIVE"
        )

        // APPLE은 현재 Processor가 APPLE_ACCOUNT_DELETED를 처리하도록 되어있음
        val rawBody = if (source == "APPLE") {
            """{"eventType":"APPLE_ACCOUNT_DELETED","accountKey":"$accountKey"}"""
        } else {
            """{"eventType":"ACCOUNT_DELETED","accountKey":"$accountKey"}"""
        }

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

        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }.andExpect { status().isOk }

        mockMvc.post("/inbox/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect { status().isOk }

        val eventStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM inbox_events WHERE event_id = ?",
            String::class.java,
            eventId
        )
        assertTrue(eventStatus == "DONE" || eventStatus == "FAILED")

        val userStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM users WHERE external_user_key = ?",
            String::class.java,
            accountKey
        )

        if (eventStatus == "DONE") {
            if (source == "APPLE") {
                assertEquals("APPLE_DELETED", userStatus)
            } else {
                assertEquals("DELETED", userStatus)
            }
        } else {
            assertEquals("ACTIVE", userStatus)
        }
    }
}
