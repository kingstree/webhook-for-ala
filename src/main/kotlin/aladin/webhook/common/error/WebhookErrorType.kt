package aladin.webhook.common.error

import org.springframework.http.HttpStatus

enum class WebhookErrorType(
    override val code: String,
    override val message: String,
    override val httpStatus: HttpStatus
) : ErrorType {

    RAW_BODY_MISSING(
        code = "WEBHOOK_RAW_BODY_MISSING",
        message = "요청 본문(rawBody)을 읽을 수 없습니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    INVALID_SOURCE_SYSTEM(
        code = "WEBHOOK_INVALID_SOURCE_SYSTEM",
        message = "X-Source-System 값이 올바르지 않습니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    TIMESTAMP_SKEW_EXCEEDED(
        code = "WEBHOOK_TIMESTAMP_SKEW_EXCEEDED",
        message = "요청 시간이 허용 범위를 초과했습니다.",
        httpStatus = HttpStatus.FORBIDDEN
    ),

    SIGNATURE_DECODE_FAILED(
        code = "WEBHOOK_SIGNATURE_DECODE_FAILED",
        message = "서명(Signature) 디코딩에 실패했습니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    SIGNATURE_MISMATCH(
        code = "WEBHOOK_SIGNATURE_MISMATCH",
        message = "서명(Signature)이 일치하지 않습니다.",
        httpStatus = HttpStatus.FORBIDDEN
    )
}
