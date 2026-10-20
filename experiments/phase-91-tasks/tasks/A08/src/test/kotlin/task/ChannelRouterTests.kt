package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ChannelRouterTests {
	@Autowired
	lateinit var router: ChannelRouter

	@Test
	fun `routes_properties 에 적은 모든 채널의 웹훅을 찾는다`() {
		assertThat(router.webhookFor("dm")).isEqualTo("https://hooks.example.com/T000/dm")
		assertThat(router.webhookFor("#general")).isEqualTo("https://hooks.example.com/T000/general")
		assertThat(router.webhookFor("#alerts")).isEqualTo("https://hooks.example.com/T000/alerts")
		assertThat(router.webhookFor("!oncall")).isEqualTo("https://hooks.example.com/T000/oncall")
	}
}
