package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

@SpringBootTest
class OAuth2ConfigTests {
	@Autowired
	lateinit var repository: ClientRegistrationRepository

	@Test
	fun `설정에 적은 github 등록 정보를 조회할 수 있다`() {
		val registration = repository.findByRegistrationId("github")

		assertThat(registration).isNotNull
		assertThat(registration!!.clientId).isEqualTo("my-client-id")
	}
}
