package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest

@DataJpaTest
class MemberRepositoryTests {
	@Autowired
	lateinit var members: MemberRepository

	@Test
	fun `표시 이름을 페이지 단위로 조회하고 전체 개수도 센다`() {
		members.saveAll(
			listOf(
				Member("Alice Kim", "ally"),
				Member("Bob Lee", null),
				Member("Carol Park", "caz"),
				Member("Dave Choi", null),
				Member("Eve Han", "ally"),
				Member("Frank Yoo", null, active = false),
			),
		)

		val page = members.findDisplayNames(PageRequest.of(0, 2))

		assertThat(page.content).hasSize(2)
		assertThat(page.totalElements).isEqualTo(4)
		assertThat(page.totalPages).isEqualTo(2)
	}
}
