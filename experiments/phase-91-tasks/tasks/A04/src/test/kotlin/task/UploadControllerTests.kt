package task

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadControllerTests {
	@LocalServerPort
	var port: Int = 0

	private val client: WebTestClient by lazy {
		WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
	}

	private val boundary = "------------------------d74496d66958873e"

	private fun filePart(): String =
		"--$boundary\r\n" +
			"Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
			"Content-Type: text/plain\r\n" +
			"\r\n" +
			"\r\n"

	private fun nonEmptyFilePart(): String =
		"--$boundary\r\n" +
			"Content-Disposition: form-data; name=\"file\"; filename=\"report.txt\"\r\n" +
			"Content-Type: text/plain\r\n" +
			"\r\n" +
			"hello\r\n"

	private fun actionPart(): String =
		"--$boundary\r\n" +
			"Content-Disposition: form-data; name=\"action\"\r\n" +
			"\r\n" +
			"asd\r\n"

	private fun upload(body: String) =
		client
			.post()
			.uri("/uploads")
			.contentType(MediaType.parseMediaType("multipart/form-data; boundary=$boundary"))
			.bodyValue(body.toByteArray())
			.exchange()

	@Test
	fun `빈 파일이 마지막 파트여도 수신된다`() {
		upload(actionPart() + filePart() + "--$boundary--\r\n")
			.expectStatus()
			.isOk
			.expectBody(String::class.java)
			.isEqualTo("received: test.txt")
	}

	@Test
	fun `빈 파일이 다른 파트보다 앞에 있어도 수신된다`() {
		upload(filePart() + actionPart() + "--$boundary--\r\n")
			.expectStatus()
			.isOk
			.expectBody(String::class.java)
			.isEqualTo("received: test.txt")
	}

	@Test
	fun `내용이 있는 파일은 순서와 상관없이 수신된다`() {
		upload(nonEmptyFilePart() + actionPart() + "--$boundary--\r\n")
			.expectStatus()
			.isOk
			.expectBody(String::class.java)
			.isEqualTo("received: report.txt")
	}
}
