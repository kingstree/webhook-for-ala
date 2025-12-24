package aladin.webhook.presentation

import WebhookRequestDto
import aladin.webhook.application.WebhookIngestService
import aladin.webhook.presentation.dto.WebhookIngestResponse
import aladin.webhook.security.UnauthorizedException
import aladin.webhook.security.WebhookSignatureVerifier
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.ContentCachingRequestWrapper
import java.time.Instant

@RestController
class WebhookController(
    private val verifier: WebhookSignatureVerifier,
    private val ingestService: WebhookIngestService
) {
    @Operation(
        summary = "계정 변경 Webhook 수신",
        description = """
외부 시스템(APPLE / PARTNER / INTERNAL)에서 전달되는 계정 변경 이벤트를 수신합니다.

- HMAC 서명 검증(raw body 기준)
- eventId 기반 멱등성 보장
- payload 원문(JSON)을 그대로 저장
"""
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Webhook 수신 성공",
                content = [Content(schema = Schema(implementation = WebhookIngestResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청 형식 오류 (raw body 누락 등)"
            ),
            ApiResponse(
                responseCode = "403",
                description = "서명 검증 실패"
            )
        ]
    )
    @PostMapping("/webhooks/account-changes")
    fun receive(
        request: HttpServletRequest,
        @RequestHeader("X-Source-System") sourceSystem: String,
        @RequestHeader("X-Event-Id") eventId: String,
        @RequestHeader("X-Timestamp") timestamp: Long,
        @RequestHeader("X-Signature") signature: String,
        @Valid @RequestBody body: WebhookRequestDto
    ): ResponseEntity<WebhookIngestResponse> {

        val rawBody = (request as? ContentCachingRequestWrapper)
            ?.contentAsByteArray
            ?.takeIf { it.isNotEmpty() }
            ?.let { String(it, Charsets.UTF_8) }
            ?: return ResponseEntity.badRequest().build()

        val now = Instant.now().epochSecond

        try {
            verifier.verify(
                sourceSystemRaw = sourceSystem,
                eventId = eventId,
                timestampSec = timestamp,
                signatureRaw = signature,
                rawBody = rawBody,     // ⭐ 진짜 원문
                nowEpochSec = now
            )
        } catch (_: UnauthorizedException) {
            return ResponseEntity.status(403).build()
        }
        var webhookIngestResponse = ingestService.ingest(
            eventId = eventId,
            sourceSystem = sourceSystem.trim().uppercase(),
            eventType = body.eventType.trim().uppercase(),
            accountKey = body.accountKey.trim(),
            signature = signature,
            requestTimestamp = timestamp,
            payloadJson = rawBody
        )// ⭐ DB에도 원문 저장
        return ResponseEntity.ok(webhookIngestResponse)

    }
}
