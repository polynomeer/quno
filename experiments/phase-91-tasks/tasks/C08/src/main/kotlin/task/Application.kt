package task

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
class Application

@Entity
@Table(name = "orders")
class Order(
	@Column(nullable = false)
	val customerName: String,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null

	@OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
	val items: MutableList<OrderItem> = mutableListOf()

	fun addItem(sku: String, quantity: Int) {
		items.add(OrderItem(this, sku, quantity))
	}
}

@Entity
@Table(name = "order_items")
class OrderItem(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	val order: Order,

	@Column(nullable = false)
	val sku: String,

	@Column(nullable = false)
	val quantity: Int,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null
}

interface OrderRepository : JpaRepository<Order, Long>

@Service
class OrderService(
	private val orderRepository: OrderRepository,
) {
	@Transactional
	fun create(customerName: String, lines: Map<String, Int>): Long {
		val order = Order(customerName)
		lines.forEach { (sku, quantity) -> order.addItem(sku, quantity) }
		return orderRepository.save(order).id!!
	}

	/** 주문 상세 화면에 쓸 주문과 품목을 돌려준다. */
	fun findDetail(id: Long): Order =
		orderRepository.findById(id).orElseThrow { NoSuchElementException("주문이 없습니다: $id") }
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
