package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@SpringBootApplication
class Application

/** 채널 이름(예: `#general`)으로 알림 웹훅 주소를 찾는다. 매핑은 routes.properties 에 둔다. */
@Component
class ChannelRouter(
	private val environment: Environment,
) {
	fun webhookFor(channel: String): String? = environment.getProperty(channel)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
