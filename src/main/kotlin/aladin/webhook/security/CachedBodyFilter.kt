package aladin.webhook.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

@Component
class CachedBodyFilter : OncePerRequestFilter() {

    companion object {
        const val RAW_BODY_ATTR = "RAW_BODY"
        private const val CACHE_LIMIT = 1024 * 1024 // 1MB
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrapped = ContentCachingRequestWrapper(request, CACHE_LIMIT)
        filterChain.doFilter(wrapped, response)
    }
}
