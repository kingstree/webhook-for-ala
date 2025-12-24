package aladin.webhook.common.error

open class BusinessException(
    val errorType: ErrorType,
    override val message: String = errorType.message,
    cause: Throwable? = null
) : RuntimeException(message, cause)
