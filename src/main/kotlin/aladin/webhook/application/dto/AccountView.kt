package aladin.webhook.application.dto

data class AccountView(
    val accountKey: String,
    val email: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
