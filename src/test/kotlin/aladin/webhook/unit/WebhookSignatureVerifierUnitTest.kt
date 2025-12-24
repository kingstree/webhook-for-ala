package aladin.webhook.unit

import aladin.webhook.config.WebhookSecurityProperties
import aladin.webhook.security.UnauthorizedException
import aladin.webhook.security.WebhookSignatureVerifier
import org.apache.commons.codec.binary.Hex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.stream.Stream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookSignatureVerifierUnitTest{

    @DisplayName("정상 canonical + secret + signature 이면 검증에 성공한다 (소스/인코딩 전체)")
    @ParameterizedTest(name = "source={0}, encoding={1}, rawSourceInput={2}")
    @MethodSource("validCases")
    fun validSignature_shouldPass(
        sourceSystem: WebhookSecurityProperties.SourceSystem,
        encoding: WebhookSecurityProperties.SignatureEncoding,
        sourceSystemRawInput: String
    ) {
        // given
        val props = propsOf(encoding)
        val verifier = WebhookSignatureVerifier(props)

        val rawBody = "{\"eventType\":\"ACCOUNT_DELETED\",\"accountKey\":\"u-1\"}"
        val eventId = "evt-100"
        val timestampSec = 1_700_000_000L
        val nowEpochSec = timestampSec + 10 // allowedSkewSeconds=300

        val canonical = "${sourceSystem.name}.${eventId}.${timestampSec}.${rawBody}"
        val secret = props.secretOf(sourceSystem)
        val signature = sign(secret = secret, canonical = canonical, encoding = encoding)

        // when & then
        Assertions.assertDoesNotThrow {
            verifier.verify(
                sourceSystemRaw = sourceSystemRawInput,
                eventId = eventId,
                timestampSec = timestampSec,
                signatureRaw = signature,
                rawBody = rawBody,
                nowEpochSec = nowEpochSec
            )
        }
    }

    @DisplayName("서명이 다르면 SIGNATURE_MISMATCH 예외가 발생한다 (소스/인코딩 전체)")
    @ParameterizedTest(name = "source={0}, encoding={1}")
    @MethodSource("mismatchCases")
    fun signatureMismatch_shouldThrow(
        sourceSystem: WebhookSecurityProperties.SourceSystem,
        encoding: WebhookSecurityProperties.SignatureEncoding
    ) {
        // given
        val props = propsOf(encoding)
        val verifier = WebhookSignatureVerifier(props)

        val rawBody = "{\"eventType\":\"ACCOUNT_DELETED\",\"accountKey\":\"u-1\"}"
        val eventId = "evt-101"
        val timestampSec = 1_700_000_000L
        val nowEpochSec = timestampSec

        val canonical = "${sourceSystem.name}.${eventId}.${timestampSec}.${rawBody}"
        val secret = props.secretOf(sourceSystem)
        val validSignature = sign(secret = secret, canonical = canonical, encoding = encoding)

        // ensure we keep a syntactically valid signature string, but force mismatch
        val invalidButWellFormedSignature = when (encoding) {
            WebhookSecurityProperties.SignatureEncoding.HEX -> {
                // flip last hex char
                validSignature.dropLast(1) + if (validSignature.last() != '0') '0' else '1'
            }
            WebhookSecurityProperties.SignatureEncoding.BASE64 -> {
                // re-sign with a different secret to keep base64 shape but mismatch
                sign(secret = secret + "-x", canonical = canonical, encoding = encoding)
            }
        }

        // when
        val ex = Assertions.assertThrows(UnauthorizedException::class.java) {
            verifier.verify(
                sourceSystemRaw = sourceSystem.name,
                eventId = eventId,
                timestampSec = timestampSec,
                signatureRaw = invalidButWellFormedSignature,
                rawBody = rawBody,
                nowEpochSec = nowEpochSec
            )
        }

        // then
        Assertions.assertEquals("SIGNATURE_MISMATCH", ex.message)
    }

    @Test
    @DisplayName("타임스탬프 스큐가 허용치를 넘으면 TIMESTAMP_SKEW_EXCEEDED 예외가 발생한다")
    fun timestampSkewExceeded_shouldThrow() {
        // given
        val props = propsOf(WebhookSecurityProperties.SignatureEncoding.HEX)
        val verifier = WebhookSignatureVerifier(props)

        val rawBody = "{\"eventType\":\"ACCOUNT_DELETED\",\"accountKey\":\"u-1\"}"
        val eventId = "evt-102"
        val timestampSec = 1_700_000_000L
        val nowEpochSec = timestampSec + 301 // allowedSkewSeconds=300 초과
        val sourceSystem = WebhookSecurityProperties.SourceSystem.INTERNAL

        val canonical = "${sourceSystem.name}.${eventId}.${timestampSec}.${rawBody}"
        val signatureHex = sign(
            secret = props.secretOf(sourceSystem),
            canonical = canonical,
            encoding = WebhookSecurityProperties.SignatureEncoding.HEX
        )

        // when
        val ex = Assertions.assertThrows(UnauthorizedException::class.java) {
            verifier.verify(
                sourceSystemRaw = sourceSystem.name,
                eventId = eventId,
                timestampSec = timestampSec,
                signatureRaw = signatureHex,
                rawBody = rawBody,
                nowEpochSec = nowEpochSec
            )
        }

        // then
        Assertions.assertEquals("TIMESTAMP_SKEW_EXCEEDED", ex.message)
    }

    @Test
    @DisplayName("알 수 없는 SourceSystem이면 INVALID_SOURCE_SYSTEM 예외가 발생한다")
    fun invalidSourceSystem_shouldThrow() {
        // given
        val props = propsOf(WebhookSecurityProperties.SignatureEncoding.HEX)
        val verifier = WebhookSignatureVerifier(props)

        val rawBody = "{\"eventType\":\"ACCOUNT_DELETED\",\"accountKey\":\"u-1\"}"
        val eventId = "evt-103"
        val timestampSec = 1_700_000_000L
        val nowEpochSec = timestampSec
        val sourceSystemRaw = "UNKNOWN"

        // 서명은 뭐든 상관없음(소스시스템 파싱에서 먼저 터짐)
        val signature = "00" // valid HEX

        // when
        val ex = Assertions.assertThrows(UnauthorizedException::class.java) {
            verifier.verify(
                sourceSystemRaw = sourceSystemRaw,
                eventId = eventId,
                timestampSec = timestampSec,
                signatureRaw = signature,
                rawBody = rawBody,
                nowEpochSec = nowEpochSec
            )
        }

        // then
        Assertions.assertEquals("INVALID_SOURCE_SYSTEM", ex.message)
    }

    private fun propsOf(encoding: WebhookSecurityProperties.SignatureEncoding): WebhookSecurityProperties {
        return WebhookSecurityProperties(
            security = WebhookSecurityProperties.Security(
                allowedSkewSeconds = 300,
                signatureEncoding = encoding,
                canonicalFormat = WebhookSecurityProperties.CanonicalFormat.SOURCE_EVENT_TS_BODY
            ),
            secrets = WebhookSecurityProperties.Secrets(
                apple = "test-apple-secret",
                partner = "test-partner-secret",
                internal = "test-internal-secret"
            )
        )
    }

    private fun sign(secret: String, canonical: String, encoding: WebhookSecurityProperties.SignatureEncoding): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(canonical.toByteArray(StandardCharsets.UTF_8))
        return when (encoding) {
            WebhookSecurityProperties.SignatureEncoding.HEX -> Hex.encodeHexString(bytes)
            WebhookSecurityProperties.SignatureEncoding.BASE64 -> Base64.getEncoder().encodeToString(bytes)
        }
    }

    companion object {
        @JvmStatic
        fun validCases(): Stream<Arguments> {
            return Stream.of(
                // raw input normalization: trim + uppercase
                Arguments.of(WebhookSecurityProperties.SourceSystem.INTERNAL, WebhookSecurityProperties.SignatureEncoding.HEX, "INTERNAL"),
                Arguments.of(WebhookSecurityProperties.SourceSystem.INTERNAL, WebhookSecurityProperties.SignatureEncoding.HEX, " internal "),
                Arguments.of(WebhookSecurityProperties.SourceSystem.PARTNER, WebhookSecurityProperties.SignatureEncoding.HEX, "partner"),
                Arguments.of(WebhookSecurityProperties.SourceSystem.APPLE, WebhookSecurityProperties.SignatureEncoding.HEX, " APPLE "),

                Arguments.of(WebhookSecurityProperties.SourceSystem.INTERNAL, WebhookSecurityProperties.SignatureEncoding.BASE64, "INTERNAL"),
                Arguments.of(WebhookSecurityProperties.SourceSystem.PARTNER, WebhookSecurityProperties.SignatureEncoding.BASE64, " partner "),
                Arguments.of(WebhookSecurityProperties.SourceSystem.APPLE, WebhookSecurityProperties.SignatureEncoding.BASE64, "apple")
            )
        }

        @JvmStatic
        fun mismatchCases(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(WebhookSecurityProperties.SourceSystem.INTERNAL, WebhookSecurityProperties.SignatureEncoding.HEX),
                Arguments.of(WebhookSecurityProperties.SourceSystem.PARTNER, WebhookSecurityProperties.SignatureEncoding.HEX),
                Arguments.of(WebhookSecurityProperties.SourceSystem.APPLE, WebhookSecurityProperties.SignatureEncoding.HEX),

                Arguments.of(WebhookSecurityProperties.SourceSystem.INTERNAL, WebhookSecurityProperties.SignatureEncoding.BASE64),
                Arguments.of(WebhookSecurityProperties.SourceSystem.PARTNER, WebhookSecurityProperties.SignatureEncoding.BASE64),
                Arguments.of(WebhookSecurityProperties.SourceSystem.APPLE, WebhookSecurityProperties.SignatureEncoding.BASE64)
            )
        }
    }
}
