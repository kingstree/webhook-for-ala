package aladin.webhook.common.error

import org.springframework.http.HttpStatus

interface ErrorType {
    val code: String
    val message: String
    val httpStatus: HttpStatus
}
