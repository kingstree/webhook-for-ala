package aladin.webhook.slice.presentation

import aladin.webhook.testkit.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.servlet.get

class AccountQueryControllerTest : IntegrationTestBase() {

    @Test
    fun `계정 조회`() {
        jdbcTemplate.update(
            "INSERT INTO users(external_user_key, email, status) VALUES(?, ?, ?)",
            "u-300", "user@x.com", "ACTIVE"
        )

        mockMvc.get("/accounts/u-300") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.accountKey").value("u-300")
            jsonPath("$.email").value("user@x.com")
            jsonPath("$.status").value("ACTIVE")
            jsonPath("$.createdAt").exists()
            jsonPath("$.updatedAt").exists()
        }
    }

    @Test
    fun `없는 계정 조회는 404`() {
        mockMvc.get("/accounts/nope") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
        }
    }
}
