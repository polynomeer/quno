package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ShipmentApiTests {
	@Value("\${local.server.port}")
	var port: Int = 0

	@Test
	fun `배송 상태를 HTTP로 조회한다`() {
		val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/shipments/ORD-3001")).build()

		val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

		assertEquals(200, response.statusCode())
		assertTrue(response.body().contains("IN_TRANSIT"))
	}
}
