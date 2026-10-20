package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderServiceTests {
	@Autowired
	lateinit var orderService: OrderService

	@Autowired
	lateinit var inventoryService: InventoryService

	@Test
	fun `주문을 접수하면 가용 재고가 줄어든다`() {
		orderService.place(Order(1, "SKU-100", 4))

		assertEquals(6, inventoryService.available("SKU-100"))
		assertEquals(Order(1, "SKU-100", 4), orderService.find(1))
	}

	@Test
	fun `재고보다 많이 주문하면 거절된다`() {
		assertThrows(IllegalStateException::class.java) {
			orderService.place(Order(2, "SKU-200", 5))
		}
	}
}
