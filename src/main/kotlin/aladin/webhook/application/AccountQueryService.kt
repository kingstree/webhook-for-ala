package aladin.webhook.application


import aladin.webhook.application.dto.AccountView
import aladin.webhook.infra.mybatis.mapper.UserMapper
import org.springframework.stereotype.Service

@Service
class AccountQueryService(
    private val userMapper: UserMapper
) {
    fun getAccount(accountKey: String): AccountView? {
        val u = userMapper.selectByAccountKey(accountKey) ?: return null
        return AccountView(
            accountKey = u.externalUserKey,
            email = u.email,
            status = u.status,
            createdAt = u.createdAt,
            updatedAt = u.updatedAt
        )
    }
}
