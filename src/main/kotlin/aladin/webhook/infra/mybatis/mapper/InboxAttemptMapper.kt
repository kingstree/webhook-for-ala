package aladin.webhook.infra.mybatis.mapper

import aladin.webhook.infra.mybatis.model.InboxAttemptRow
import org.apache.ibatis.annotations.Mapper

@Mapper
interface InboxAttemptMapper {
    fun selectNextAttemptNo(eventId: String): Int
    fun insertStarted(param: InsertAttemptParam): Int
    fun updateSucceeded(param: UpdateAttemptParam): Int
    fun updateFailed(param: UpdateAttemptFailedParam): Int
    fun selectByEventId(eventId: String): List<InboxAttemptRow>
}
