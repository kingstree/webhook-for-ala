package aladin.webhook.presentation

import aladin.webhook.application.InboxQueryService
import aladin.webhook.presentation.dto.AttemptDto
import aladin.webhook.presentation.dto.InboxEventDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InboxQueryController(
    private val inboxQueryService: InboxQueryService
) {

    @GetMapping("/inbox/events/{eventId}")
    fun get(@PathVariable eventId: String): ResponseEntity<InboxEventDto> {
        val v = inboxQueryService.getEvent(eventId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            InboxEventDto(
                eventId = v.eventId,
                sourceSystem = v.sourceSystem,
                eventType = v.eventType,
                accountKey = v.accountKey,
                status = v.status,
                errorMessage = v.errorMessage,
                receivedAt = v.receivedAt,
                processedAt = v.processedAt,
                attempts = v.attempts.map {
                    AttemptDto(it.attemptNo, it.attemptStatus, it.startedAt, it.endedAt, it.errorMessage)
                }
            )
        )
    }
}
