package aladin.webhook.slice.inboxquery

import aladin.webhook.testkit.IntegrationTestBase
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class InboxEventQuerySliceTest : IntegrationTestBase() {

    @Test
    fun `이벤트 처리 결과 조회 - GET inbox events`() {
        // 최소 row 넣고 조회 동작 확인 (컬럼은 네 스키마에 맞게)
        jdbcTemplate.update(
            "INSERT INTO inbox_events(event_id, source_system, event_type, account_key, status, signature, request_timestamp, payload_json, received_at) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))",
            "evt-q-1", "INTERNAL", "EMAIL_FORWARDING_CHANGED", "u-1", "RECEIVED",
            "sig", 1234L, """{"x":"y"}"""
        )

        mockMvc.get("/inbox/events/evt-q-1")
            .andExpect { status().isOk }
            .andExpect { content().string(containsString("evt-q-1")) }
    }
}
