package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service

@SpringBootApplication
class Application

data class Order(val id: Long, val sku: String, val quantity: Int)

/** 주문을 접수한다. 접수 전에 재고를 예약한다. */
@Service
class OrderService(
	private val inventoryService: InventoryService,
) {
	private val orders = mutableMapOf<Long, Order>()

	fun place(order: Order): Order {
		inventoryService.reserve(order.sku, order.quantity)
		orders[order.id] = order
		return order
	}

	fun find(id: Long): Order? = orders[id]

	fun orderedQuantity(sku: String): Int =
		orders.values.filter { it.sku == sku }.sumOf { it.quantity }
}

/** SKU별 재고를 관리한다. 가용 재고는 이미 접수된 주문 수량을 반영해 계산한다. */
@Service
class InventoryService(
	private val orderService: OrderService,
) {
	private val stock = mutableMapOf("SKU-100" to 10, "SKU-200" to 3)

	fun available(sku: String): Int =
		(stock[sku] ?: 0) - orderService.orderedQuantity(sku)

	fun reserve(sku: String, quantity: Int) {
		check(available(sku) >= quantity) { "재고가 부족합니다: $sku" }
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
