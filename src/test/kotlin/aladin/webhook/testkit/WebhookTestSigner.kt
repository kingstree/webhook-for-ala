package aladin.webhook.testkit

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookTestSigner {

    fun hmacSha256Hex(secret: String, canonical: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun canonical(sourceSystem: String, eventId: String, timestamp: Long, rawBody: String): String {
        return "${sourceSystem}.${eventId}.${timestamp}.${rawBody}"
    }
}
