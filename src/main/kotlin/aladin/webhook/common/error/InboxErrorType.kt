package aladin.webhook.common.error

import org.springframework.http.HttpStatus

enum class InboxErrorType(
    override val code: String,
    override val message: String,
    override val httpStatus: HttpStatus
) : ErrorType {

    EVENT_NOT_FOUND(
        code = "INBOX_EVENT_NOT_FOUND",
        message = "Inbox 이벤트를 찾을 수 없습니다.",
        httpStatus = HttpStatus.NOT_FOUND
    ),

    PAYLOAD_JSON_MISSING(
        code = "INBOX_PAYLOAD_JSON_MISSING",
        message = "payload_json이 비어있습니다.",
        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR
    ),

    INVALID_PAYLOAD_JSON(
        code = "INBOX_INVALID_PAYLOAD_JSON",
        message = "payload_json 파싱에 실패했습니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    UNSUPPORTED_EVENT_TYPE(
        code = "INBOX_UNSUPPORTED_EVENT_TYPE",
        message = "지원하지 않는 eventType 입니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    EMAIL_REQUIRED_FOR_EVENT(
        code = "INBOX_EMAIL_REQUIRED_FOR_EVENT",
        message = "이 이벤트에는 email이 필요합니다.",
        httpStatus = HttpStatus.BAD_REQUEST
    ),

    USER_NOT_FOUND(
        code = "INBOX_USER_NOT_FOUND",
        message = "대상 사용자를 찾을 수 없습니다.",
        httpStatus = HttpStatus.NOT_FOUND
    )
}
