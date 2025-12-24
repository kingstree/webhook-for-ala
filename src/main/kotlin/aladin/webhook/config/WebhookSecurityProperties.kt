package aladin.webhook.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "webhook")
data class WebhookSecurityProperties(
    @field:NotNull
    val security: Security = Security(),

    @field:NotNull
    val secrets: Secrets = Secrets()
) {
    data class Security(
        @field:Min(0)
        val allowedSkewSeconds: Long = 300,

        /**
         * signature-encoding: hex | base64
         */
        @field:NotNull
        val signatureEncoding: SignatureEncoding = SignatureEncoding.HEX,

        /**
         * canonical-format: SOURCE_EVENT_TS_BODY
         */
        @field:NotNull
        val canonicalFormat: CanonicalFormat = CanonicalFormat.SOURCE_EVENT_TS_BODY
    )

    data class Secrets(
        @field:NotBlank
        val apple: String = "",

        @field:NotBlank
        val partner: String = "",

        @field:NotBlank
        val internal: String = ""
    )

    enum class SignatureEncoding { HEX, BASE64 }

    enum class CanonicalFormat { SOURCE_EVENT_TS_BODY }

    enum class SourceSystem { APPLE, PARTNER, INTERNAL }

    fun secretOf(sourceSystem: SourceSystem): String =
        when (sourceSystem) {
            SourceSystem.APPLE -> secrets.apple
            SourceSystem.PARTNER -> secrets.partner
            SourceSystem.INTERNAL -> secrets.internal
        }
}
