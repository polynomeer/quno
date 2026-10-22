package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ReactorClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@SpringBootApplication
class Application

@ConfigurationProperties("partner-api")
data class PartnerApiProperties(
	val baseUrl: String,
	/** 연결 타임아웃. 사실상 제한을 두지 않으려는 환경에서는 아주 길게 설정한다. */
	val connectTimeout: Duration,
	val readTimeout: Duration,
)

@Configuration
@EnableConfigurationProperties(PartnerApiProperties::class)
class PartnerApiConfig {
	@Bean
	fun partnerRestClient(properties: PartnerApiProperties): RestClient {
		val requestFactory = ReactorClientHttpRequestFactory()
		requestFactory.setConnectTimeout(properties.connectTimeout)
		requestFactory.setReadTimeout(properties.readTimeout)
		return RestClient
			.builder()
			.baseUrl(properties.baseUrl)
			.requestFactory(requestFactory)
			.build()
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
