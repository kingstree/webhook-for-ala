package aladin.webhook.security

import aladin.webhook.config.WebhookSecurityProperties
import aladin.webhook.config.WebhookSecurityProperties.SourceSystem
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

@Component
class WebhookSignatureVerifier(
    private val props: WebhookSecurityProperties
) {
    fun verify(
        sourceSystemRaw: String,
        eventId: String,
        timestampSec: Long,
        signatureRaw: String,
        rawBody: String,
        nowEpochSec: Long
    ) {
        val sourceSystem = parseSourceSystem(sourceSystemRaw)

        // replay guard
        val skew = abs(nowEpochSec - timestampSec)
        if (skew > props.security.allowedSkewSeconds) {
            throw UnauthorizedException("TIMESTAMP_SKEW_EXCEEDED")
        }

        val canonical = "${sourceSystem.name}.${eventId}.${timestampSec}.${rawBody}"
        val secret = props.secretOf(sourceSystem)

        val computed = hmacSha256(secret, canonical)
        val providedBytes = decodeSignature(signatureRaw)

        if (!constantTimeEquals(computed, providedBytes)) {
            throw UnauthorizedException("SIGNATURE_MISMATCH")
        }
    }

    private fun parseSourceSystem(v: String): SourceSystem =
        runCatching { SourceSystem.valueOf(v.trim().uppercase()) }
            .getOrElse { throw UnauthorizedException("INVALID_SOURCE_SYSTEM") }

    private fun hmacSha256(secret: String, msg: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(msg.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeSignature(sig: String): ByteArray =
        when (props.security.signatureEncoding) {
            WebhookSecurityProperties.SignatureEncoding.HEX -> Hex.decodeHex(sig.trim())
            WebhookSecurityProperties.SignatureEncoding.BASE64 -> Base64.decodeBase64(sig.trim())
        }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}

class UnauthorizedException(message: String) : RuntimeException(message)
