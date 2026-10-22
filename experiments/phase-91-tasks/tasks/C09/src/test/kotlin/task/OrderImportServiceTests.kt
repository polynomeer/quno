package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderImportServiceTests {
	@Autowired
	lateinit var importService: OrderImportService

	@BeforeEach
	fun setUp() {
		importService.clear()
	}

	@Test
	fun `정상 묶음은 모두 저장된다`() {
		val saved = importService.importBatch(
			listOf(OrderLine("ORD-1", "kim", 2), OrderLine("ORD-2", "lee", 1)),
		)

		assertEquals(2, saved)
		assertEquals(2, importService.count())
	}

	@Test
	fun `묶음 중 하나가 잘못되면 앞서 처리한 주문도 저장되지 않는다`() {
		assertThrows(IllegalArgumentException::class.java) {
			importService.importBatch(
				listOf(OrderLine("ORD-3", "kim", 2), OrderLine("ORD-4", "lee", 0)),
			)
		}

		assertEquals(0, importService.count(), "실패한 묶음은 롤백되어야 한다")
	}
}
