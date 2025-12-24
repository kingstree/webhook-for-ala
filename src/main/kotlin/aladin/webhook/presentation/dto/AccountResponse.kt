package aladin.webhook.presentation.dto

data class AccountResponse(
    val accountKey: String,
    val email: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
