package aladin.webhook.testkit

import aladin.webhook.application.WebhookIngestService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import java.nio.file.Files
import java.nio.file.Path

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate
    @Autowired
    lateinit var webhookIngestService: WebhookIngestService
    companion object {
        private val tempDir: Path = Files.createTempDirectory("webhook-test-")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { "jdbc:sqlite:${tempDir.resolve("test-webhook.db").toAbsolutePath()}" }
            registry.add("spring.datasource.driver-class-name") { "org.sqlite.JDBC" }

            // 너 프로젝트는 resources/db.migration 이니까 이거 고정
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
            registry.add("spring.flyway.baseline-on-migrate") { "false" }

            // WebhookSecurityProperties(prefix=webhook) 기준 키
            registry.add("webhook.secrets.apple") { "test-apple-secret" }
            registry.add("webhook.secrets.partner") { "test-partner-secret" }
            registry.add("webhook.secrets.internal") { "test-internal-secret" }

            registry.add("webhook.security.allowed-skew-seconds") { "300" }
            registry.add("webhook.security.signature-encoding") { "HEX" }
        }
    }

    @BeforeEach
    fun cleanDb() {
        fun exists(table: String): Boolean {
            val cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name=?",
                Long::class.java,
                table
            ) ?: 0L
            return cnt > 0L
        }

        if (exists("inbox_event_attempts")) jdbcTemplate.execute("DELETE FROM inbox_event_attempts")
        if (exists("inbox_events")) jdbcTemplate.execute("DELETE FROM inbox_events")
        if (exists("users")) jdbcTemplate.execute("DELETE FROM users")
    }
}
