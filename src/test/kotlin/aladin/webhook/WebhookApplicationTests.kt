package aladin.webhook

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "mybatis.mapper-locations=classpath*:mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true",

        // Test DB: avoid relying on ./data directory during CI/local test runs
        // Use a real file (not :memory:) so Flyway/MyBatis/Hikari all see the same DB.
        "spring.datasource.url=jdbc:sqlite:\${java.io.tmpdir}/webhook-context-load.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC"

    ]
)
class WebhookApplicationTests {

    @Test
    fun contextLoads() {
    }

}
