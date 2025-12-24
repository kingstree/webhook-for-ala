package aladin.webhook.unit.application

import aladin.webhook.application.AccountQueryService
import aladin.webhook.application.dto.AccountView
import aladin.webhook.infra.mybatis.mapper.UserMapper
import aladin.webhook.infra.mybatis.model.UserRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AccountQueryServiceUnitTest {

    private val userMapper: UserMapper = mock()
    private val service = AccountQueryService(userMapper)

    @Test
    fun `accountKey로 조회 시 존재하면 AccountView 반환`() {
        // given
        val accountKey = "u-100"
        val row = UserRow(
            userId = 100L,
            externalUserKey = accountKey,
            email = "new@x.com",
            status = "ACTIVE",
            createdAt = "2025-12-23 12:00:00",
            updatedAt = "2025-12-23 12:30:00"
        )
        whenever(userMapper.selectByAccountKey(accountKey)).thenReturn(row)

        // when
        val result: AccountView? = service.getAccount(accountKey)

        // then
        assertEquals(
            AccountView(
                accountKey = accountKey,
                email = "new@x.com",
                status = "ACTIVE",
                createdAt = "2025-12-23 12:00:00",
                updatedAt = "2025-12-23 12:30:00"
            ),
            result
        )
        verify(userMapper).selectByAccountKey(accountKey)
        verifyNoMoreInteractions(userMapper)
    }

    @Test
    fun `accountKey로 조회 시 없으면 null 반환`() {
        // given
        val accountKey = "u-404"
        whenever(userMapper.selectByAccountKey(accountKey)).thenReturn(null)

        // when
        val result = service.getAccount(accountKey)

        // then
        assertNull(result)
        verify(userMapper).selectByAccountKey(accountKey)
        verifyNoMoreInteractions(userMapper)
    }
}
