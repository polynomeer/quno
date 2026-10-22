package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberRepositoryTests {
	@Autowired
	lateinit var members: MemberRepository

	@Test
	fun `마이그레이션이 적용된 뒤 회원을 조회할 수 있다`() {
		assertThat(members.count()).isEqualTo(2)
		assertThat(members.emails()).containsExactly("first@example.com", "second@example.com")
	}
}
