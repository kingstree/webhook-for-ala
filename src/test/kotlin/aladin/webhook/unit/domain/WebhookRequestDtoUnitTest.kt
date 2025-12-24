
package aladin.webhook.unit.domain

import WebhookRequestDto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WebhookRequestDtoUnitTest {

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `JSON 역직렬화 - 최소 필드(ACCOUNT_DELETED)`() {
        val json = """{"eventType":"ACCOUNT_DELETED","accountKey":"u-1"}"""

        val dto: WebhookRequestDto = objectMapper.readValue(json)

        assertThat(dto.eventType).isEqualTo("ACCOUNT_DELETED")
        assertThat(dto.accountKey).isEqualTo("u-1")
        // email 같은 선택 필드는 null 이어도 정상
        assertThat(dto.email).isNull()
    }

    @Test
    fun `JSON 역직렬화 - email 포함(EMAIL_FORWARDING_CHANGED)`() {
        val json = """{"eventType":"EMAIL_FORWARDING_CHANGED","accountKey":"u-100","email":"new@x.com"}"""

        val dto: WebhookRequestDto = objectMapper.readValue(json)

        assertThat(dto.eventType).isEqualTo("EMAIL_FORWARDING_CHANGED")
        assertThat(dto.accountKey).isEqualTo("u-100")
        assertThat(dto.email).isEqualTo("new@x.com")
    }

    @Test
    fun `Bean Validation - eventType, accountKey는 비어있으면 안된다`() {
        val dto = WebhookRequestDto(
            eventType = "",
            accountKey = "",
            email = null
        )

        val violations = validator.validate(dto)
        val paths = violations.map { it.propertyPath.toString() }.toSet()

        // @NotBlank / @NotNull 같은 제약이 걸려있다는 전제
        assertThat(paths).contains("eventType", "accountKey")
    }
}
