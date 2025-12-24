package aladin.webhook.presentation

import aladin.webhook.application.AccountQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountQueryController(
    private val accountQueryService: AccountQueryService
) {
    data class AccountResponse(
        val accountKey: String,
        val email: String?,
        val status: String,
        val createdAt: String,
        val updatedAt: String
    )

    @GetMapping("/accounts/{accountKey}")
    fun get(@PathVariable accountKey: String): ResponseEntity<AccountResponse> {
        val v = accountQueryService.getAccount(accountKey) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            AccountResponse(
                accountKey = v.accountKey,
                email = v.email,
                status = v.status,
                createdAt = v.createdAt,
                updatedAt = v.updatedAt
            )
        )
    }
}
