package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestClient
import java.time.Duration

@SpringBootTest
class PartnerApiConfigTests {
	@Autowired
	lateinit var properties: PartnerApiProperties

	@Autowired
	lateinit var partnerRestClient: RestClient

	@Test
	fun `설정한 타임아웃으로 파트너 API 클라이언트가 만들어진다`() {
		assertThat(properties.connectTimeout).isEqualTo(Duration.ofDays(30))
		assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(60))
		assertThat(partnerRestClient).isNotNull
	}
}
