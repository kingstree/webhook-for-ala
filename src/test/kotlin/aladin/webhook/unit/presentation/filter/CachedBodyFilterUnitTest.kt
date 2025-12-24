package aladin.webhook.unit.presentation.filter

import aladin.webhook.security.CachedBodyFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper

class CachedBodyFilterUnitTest {

    @Test
    fun `요청이 들어오면 ContentCachingRequestWrapper 로 감싸서 체인에 전달한다`() {
        val filter = CachedBodyFilter()

        val req = MockHttpServletRequest("POST", "/webhooks/account-changes").apply {
            contentType = "application/json;charset=UTF-8"
            characterEncoding = "UTF-8"
            setContent("{\"hello\":\"world\"}".toByteArray(Charsets.UTF_8))
            // or: contentAsByteArray = ... (but setContent is the official setter)
        }
        val res = MockHttpServletResponse()

        var wrapped: ServletRequest? = null

        val chain = FilterChain { request: ServletRequest, _: ServletResponse ->
            wrapped = request
            // NOTE: ContentCachingRequestWrapper 는 body를 읽은 후에 contentAsByteArray 가 채워진다.
            request.inputStream.readBytes()
        }

        filter.doFilter(req, res, chain)

        Assertions.assertTrue(wrapped is ContentCachingRequestWrapper)
    }

    @Test
    fun `체인 내부에서 body를 읽으면 wrapper에 원문이 캐싱된다`() {
        val filter = CachedBodyFilter()

        val body = "{\"eventType\":\"ACCOUNT_DELETED\",\"accountKey\":\"u-1\"}".toByteArray(Charsets.UTF_8)

        val req = MockHttpServletRequest("POST", "/webhooks/account-changes").apply {
            contentType = "application/json;charset=UTF-8"
            characterEncoding = "UTF-8"
            setContent(body)
        }
        val res = MockHttpServletResponse()

        var cachedBytes: ByteArray? = null

        val chain = FilterChain { request: ServletRequest, _: ServletResponse ->
            val wrapper = request as ContentCachingRequestWrapper

            // request body 소비 (컨트롤러의 @RequestBody 읽기와 동일한 효과)
            wrapper.inputStream.readBytes()

            cachedBytes = wrapper.contentAsByteArray
        }

        filter.doFilter(req, res, chain)

        Assertions.assertArrayEquals(body, cachedBytes)
    }
}
