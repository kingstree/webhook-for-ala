package aladin.webhook.slice.presentation

import aladin.webhook.testkit.IntegrationTestBase
import aladin.webhook.testkit.WebhookTestSigner
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

class InboxQueryControllerTest : IntegrationTestBase() {

    @Test
    fun `GET inbox event 상세 조회 - attempts 배열 포함`() {
        jdbcTemplate.update(
            "INSERT INTO users(external_user_key, email, status) VALUES(?, ?, ?)",
            "u-200", "old@x.com", "ACTIVE"
        )

        val source = "APPLE"
        val eventId = "evt-200"
        val ts = Instant.now().epochSecond
        val rawBody = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-200"}"""
        val signature = WebhookTestSigner.hmacSha256Hex(
            "test-apple-secret",
            WebhookTestSigner.canonical(source, eventId, ts, rawBody)
        )

        mockMvc.post("/webhooks/account-changes") {
            contentType = MediaType.APPLICATION_JSON
            content = rawBody
            header("X-Source-System", source)
            header("X-Event-Id", eventId)
            header("X-Timestamp", ts)
            header("X-Signature", signature)
        }.andExpect { status { isOk() } }

        mockMvc.post("/inbox/process") {
            param("limit", "50")
        }.andExpect { status { isOk() } }

        mockMvc.get("/inbox/events/$eventId") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.eventId").value(eventId)
            jsonPath("$.sourceSystem").value(source)
            jsonPath("$.attempts").isArray
        }
    }
}
