package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderServiceTests {
	@Autowired
	lateinit var orderService: OrderService

	@Test
	fun `주문 상세는 주문 품목을 함께 돌려준다`() {
		val id = orderService.create("kim", mapOf("SKU-100" to 2, "SKU-200" to 1))

		val detail = orderService.findDetail(id)

		assertEquals("kim", detail.customerName)
		assertEquals(2, detail.items.size)
		assertEquals(setOf("SKU-100", "SKU-200"), detail.items.map { it.sku }.toSet())
	}
}
