package task

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
class ItemServiceTests {
	@Autowired
	lateinit var itemService: ItemService

	@Test
	fun `일괄 저장은 저장한 항목을 모두 방출한다`() {
		StepVerifier
			.create(itemService.saveAll(listOf("one", "two", "three")))
			.expectNext("one", "two", "three")
			.verifyComplete()
	}
}
