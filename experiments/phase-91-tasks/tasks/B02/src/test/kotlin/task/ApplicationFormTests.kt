package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationFormTests {
	@LocalServerPort
	var port: Int = 0

	@Test
	fun `항목이 많은 가입 신청서도 접수된다`() {
		val form = LinkedMultiValueMap<String, String>()
		listOf(
			"name", "email", "phone", "company", "title", "country",
			"city", "address", "zip", "referrer", "newsletter", "memo",
		).forEach { form.add(it, "value-of-$it") }

		val response =
			RestClient
				.create("http://localhost:$port")
				.post()
				.uri("/applications")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(form)
				.exchange { _, res -> res.statusCode.value() to res.bodyTo(String::class.java) }

		assertThat(response.first).isEqualTo(200)
		assertThat(response.second).isEqualTo("""{"received":12}""")
	}
}
