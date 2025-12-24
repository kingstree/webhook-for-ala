package aladin.webhook

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication


@MapperScan("aladin.webhook.infra.mybatis.mapper")
@ConfigurationPropertiesScan
@SpringBootApplication
class WebhookApplication

fun main(args: Array<String>) {
    runApplication<WebhookApplication>(*args)
}
