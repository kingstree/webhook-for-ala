package aladin.webhook.presentation

import aladin.webhook.common.error.BusinessException
import aladin.webhook.presentation.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val errorType = e.errorType

        return ResponseEntity
            .status(errorType.httpStatus)
            .body(
                ErrorResponse(
                    timestamp = Instant.now().epochSecond,
                    code = errorType.code,
                    message = e.message,
                    path = request.requestURI
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        return ResponseEntity
            .status(500)
            .body(
                ErrorResponse(
                    timestamp = Instant.now().epochSecond,
                    code = "UNEXPECTED_ERROR",
                    message = "처리 중 알 수 없는 오류가 발생했습니다.",
                    path = request.requestURI
                )
            )
    }
}
