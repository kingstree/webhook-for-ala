package aladin.webhook.slice.accounts


import aladin.webhook.testkit.IntegrationTestBase
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AccountQuerySliceTest : IntegrationTestBase() {

    @Test
    fun `계정 상태 조회 - GET accounts`() {
        jdbcTemplate.update(
            "INSERT INTO users(user_id, external_user_key, email, status, created_at, updated_at) " +
                    "VALUES(?, ?, ?, ?, datetime('now'), datetime('now'))",
            999, "u-999", "u999@x.com", "ACTIVE"
        )

        mockMvc.get("/accounts/u-999")
            .andExpect { status().isOk }
            .andExpect { content().string(containsString("u-999")) }
    }
}
