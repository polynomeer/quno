package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

@SpringBootApplication
class Application

/**
 * 시작 시점에 IdP 와 통신하지 않도록, 등록 정보는 처음 사용할 때 만든다.
 * 등록 정보 자체는 spring.security.oauth2.client.* 설정을 그대로 쓴다.
 */
class LazyClientRegistrationRepository(
	private val delegate: Lazy<ClientRegistrationRepository>,
) : ClientRegistrationRepository, Iterable<ClientRegistration> {
	override fun findByRegistrationId(registrationId: String): ClientRegistration? =
		delegate.value.findByRegistrationId(registrationId)

	override fun iterator(): Iterator<ClientRegistration> =
		(delegate.value as InMemoryClientRegistrationRepository).iterator()
}

@Configuration
class OAuth2Config {
	@Bean
	fun clientRegistrationRepository(properties: OAuth2ClientProperties): ClientRegistrationRepository =
		LazyClientRegistrationRepository(
			lazy {
				InMemoryClientRegistrationRepository(
					OAuth2ClientPropertiesMapper(properties).asClientRegistrations(),
				)
			},
		)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
