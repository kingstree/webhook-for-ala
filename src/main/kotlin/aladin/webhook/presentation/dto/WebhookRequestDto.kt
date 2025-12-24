import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "계정 변경 Webhook 요청 payload")
data class WebhookRequestDto(
    @field:NotBlank
    @field:Schema(
        description = "이벤트 타입",
        example = "EMAIL_FORWARDING_CHANGED",
        required = true
    )
    val eventType: String,
    @field:NotBlank
    @field:Schema(
        description = "외부 시스템의 계정 식별자",
        example = "external-user-123",
        required = true

    )
    val accountKey: String,

    @field:Schema(
        description = "변경된 이메일 (EMAIL_FORWARDING_CHANGED 이벤트에서만 사용)",
        example = "user@example.com",
        nullable = true
    )
    val email: String? = null
)
