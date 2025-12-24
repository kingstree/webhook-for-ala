package aladin.webhook.infra

import aladin.webhook.testkit.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertTrue

class FlywaySchemaTest : IntegrationTestBase() {


    @Autowired
    lateinit var flyway: org.flywaydb.core.Flyway
    @Test
    fun `classpath에 V1이 존재해야 한다`() {
        val r = javaClass.classLoader.getResource("db/migration/V1__Initial_schema.sql")
        println("V1 resource = $r")
        kotlin.test.assertNotNull(r, "classpath에 db/migration/V1__Initial_schema.sql이 없습니다.")
    }
    @Test
    fun `flyway가 인식한 migration 목록`() {
        val pending = flyway.info().pending()
        val applied = flyway.info().applied()

        println("applied = " + applied.map { "${it.version}:${it.description}" })
        println("pending = " + pending.map { "${it.version}:${it.description}" })
    }

    @Test
    fun `flyway migration 결과 inbox_events 테이블이 존재해야 한다`() {
        val tables = jdbcTemplate.queryForList(
            "SELECT name FROM sqlite_master WHERE type='table'",
            String::class.java
        )

        println("tables = $tables")

        assertTrue(
            tables.contains("inbox_events"),
            "❌ inbox_events 테이블이 생성되지 않았습니다. 현재 테이블 목록=$tables"
        )
    }

    @Test
    fun `flyway_schema_history 에 V1 이 성공적으로 적용되어야 한다`() {
        val rows = jdbcTemplate.queryForList(
            """
            SELECT version, description, success
            FROM flyway_schema_history
            ORDER BY installed_rank
            """.trimIndent()
        )

        println("flyway history = $rows")

        assertTrue(
            rows.any { it["version"] == "1" && it["success"] == 1 },
            "❌ V1__Initial_schema.sql 이 성공적으로 적용되지 않았습니다."
        )
    }

    @Autowired
    lateinit var applicationContext: org.springframework.context.ApplicationContext

    @Test
    fun `flyway bean exists`() {
        val names = applicationContext.getBeanNamesForType(org.flywaydb.core.Flyway::class.java)
        println("Flyway beans = ${names.toList()}")
    }
    @Test
    fun `flyway 확인`() {
        val flywayTable = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name='flyway_schema_history'",
            Long::class.java
        )!!
        println("flyway_schema_history exists = $flywayTable")
    }
}
