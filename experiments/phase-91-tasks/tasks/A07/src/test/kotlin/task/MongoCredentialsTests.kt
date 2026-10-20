package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails
import org.springframework.boot.test.context.SpringBootTest

/**
 * application.properties 에 설정한 계정 정보가 드라이버에 그대로 전달되는지 확인한다.
 * 운영 DB 비밀번호가 "my secret" 이라고 가정한다(실제 서버에는 연결하지 않는다).
 */
@SpringBootTest
class MongoCredentialsTests {
	@Autowired
	lateinit var connectionDetails: MongoConnectionDetails

	@Test
	fun `설정한 사용자 이름과 비밀번호가 드라이버에 그대로 전달된다`() {
		val credential = connectionDetails.connectionString.credential

		assertThat(credential).isNotNull
		assertThat(credential!!.userName).isEqualTo("audit-writer")
		assertThat(String(credential.password!!)).isEqualTo("my secret")
		assertThat(credential.source).isEqualTo("admin")
	}
}
