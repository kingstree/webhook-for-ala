package aladin.webhook.presentation.dto

import java.time.Instant

data class ErrorResponse(
    val timestamp: Long = Instant.now().epochSecond,
    val code: String,
    val message: String,
    val path: String,
    val details: Map<String, Any?>? = null
)
