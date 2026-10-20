package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CatalogApplicationTests {
	@LocalServerPort
	var port: Int = 0

	@Value("\${local.management.port}")
	var managementPort: Int = 0

	@Test
	fun `카탈로그 API가 응답한다`() {
		val body =
			RestClient
				.create("http://localhost:$port")
				.get()
				.uri("/catalog/ping")
				.retrieve()
				.body(String::class.java)

		assertThat(body).isEqualTo("pong")
	}

	@Test
	fun `관리 포트에서 헬스 체크가 UP이다`() {
		val body =
			RestClient
				.create("http://localhost:$managementPort")
				.get()
				.uri("/actuator/health")
				.retrieve()
				.body(String::class.java)

		assertThat(body).contains("\"status\":\"UP\"")
	}
}
