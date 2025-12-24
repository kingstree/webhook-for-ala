package aladin.webhook.infra.mybatis.mapper


import aladin.webhook.infra.mybatis.model.UserRow
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param


@Mapper
interface UserMapper {
    fun selectByAccountKey(@Param("accountKey") accountKey: String): UserRow?
    fun updateEmail(@Param("accountKey") accountKey: String, @Param("email") email: String): Int
    fun updateStatus(@Param("accountKey") accountKey: String, @Param("status") status: String): Int
}
