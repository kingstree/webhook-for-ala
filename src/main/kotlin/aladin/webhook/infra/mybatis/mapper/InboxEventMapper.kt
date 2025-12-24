package aladin.webhook.infra.mybatis.mapper


import aladin.webhook.infra.mybatis.model.InboxEventRow
import org.apache.ibatis.annotations.Mapper


@Mapper
interface InboxEventMapper {
    fun insertReceived(p: InsertInboxEventParam): Int
    fun selectByEventId(eventId: String): InboxEventRow?
    fun updateToProcessingIfReceived(eventId: String): Int
    fun updateDone(eventId: String): Int
    fun updateFailed(param: UpdateFailedParam): Int
    fun selectReceived(limit: Int): List<InboxEventRow>
}
