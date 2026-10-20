package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest

@DataJpaTest
class CallRepositoryTests {
	@Autowired
	lateinit var calls: CallRepository

	@Test
	fun `통화 시간이 짝수인 기록만 조회한다`() {
		calls.saveAll(
			listOf(
				Call("alice", 30),
				Call("bob", 45),
				Call("carol", 60),
				Call("dave", 7),
			),
		)

		val even = calls.findEvenDurations()

		assertThat(even.map { it.caller }).containsExactly("alice", "carol")
	}
}
