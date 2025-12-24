package aladin.webhook.presentation

import aladin.webhook.application.InboxProcessorService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class InboxProcessController(
    private val processor: InboxProcessorService
) {
    data class ProcessResponse(val picked: Int)

    @PostMapping("/inbox/process")
    fun process(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<ProcessResponse> {
        val picked = processor.processReceived(limit)
        return ResponseEntity.ok(ProcessResponse(picked))
    }
}
