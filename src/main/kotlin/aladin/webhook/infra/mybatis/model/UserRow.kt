package aladin.webhook.infra.mybatis.model

data class UserRow(
    val userId: Long? = null,
    val externalUserKey: String,
    val email: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
