package aladin.webhook.common.error

import org.springframework.http.HttpStatus

enum class CommonErrorType(
    override val code: String,
    override val message: String,
    override val httpStatus: HttpStatus
) : ErrorType {

    INVALID_REQUEST(
        code = "INVALID_REQUEST",
        message = "요청이 올바르지 않습니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    UNAUTHORIZED(
        code = "UNAUTHORIZED",
        message = "인증에 실패했습니다.",
        httpStatus = HttpStatus.FORBIDDEN
    ),

    NOT_FOUND(
        code = "NOT_FOUND",
        message = "대상을 찾을 수 없습니다.",
        httpStatus = HttpStatus.NOT_FOUND
    ),

    INTERNAL_ERROR(
        code = "INTERNAL_ERROR",
        message = "서버 내부 오류가 발생했습니다.",
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
    )
}
