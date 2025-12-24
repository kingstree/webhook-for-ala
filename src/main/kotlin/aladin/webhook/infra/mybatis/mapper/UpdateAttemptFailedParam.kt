package aladin.webhook.infra.mybatis.mapper

data class UpdateAttemptFailedParam(val eventId: String, val attemptNo: Int, val errorMessage: String)
