package task

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.net.InetSocketAddress
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthTests {
	@Autowired
	lateinit var mockMvc: MockMvc

	@Test
	fun `서명이 유효한 JWT로 보호된 엔드포인트에 접근할 수 있다`() {
		val token = signedToken(issuer = "https://issuer.example.com")

		mockMvc
			.get("/api/hello") { header("Authorization", "Bearer $token") }
			.andDo { print() }
			.andExpect { status { isOk() } }
	}

	companion object {
		private val key: RSAKey = RSAKeyGenerator(2048).keyID("test-key").generate()

		private val jwksServer: HttpServer =
			HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
				val body = JWKSet(key.toPublicJWK()).toString().toByteArray()
				createContext("/jwks") { exchange ->
					exchange.responseHeaders.add("Content-Type", "application/json")
					exchange.sendResponseHeaders(200, body.size.toLong())
					exchange.responseBody.use { it.write(body) }
				}
				start()
			}

		@JvmStatic
		@DynamicPropertySource
		fun properties(registry: DynamicPropertyRegistry) {
			registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") {
				"http://127.0.0.1:${jwksServer.address.port}/jwks"
			}
		}

		private fun signedToken(issuer: String): String {
			val jwt =
				SignedJWT(
					JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(),
					JWTClaimsSet
						.Builder()
						.issuer(issuer)
						.subject("user-1")
						.expirationTime(Date(System.currentTimeMillis() + 60_000))
						.build(),
				)
			jwt.sign(RSASSASigner(key))
			return jwt.serialize()
		}
	}
}
