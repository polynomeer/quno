package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
class Application

data class OrderLine(val orderNo: String, val customer: String, val quantity: Int)

/** 외부 채널에서 받은 주문 묶음을 가져와 저장한다. 묶음은 전부 저장되거나 하나도 저장되지 않아야 한다. */
@Service
class OrderImportService(
	private val jdbcTemplate: JdbcTemplate,
) {
	fun importBatch(lines: List<OrderLine>): Int {
		saveAll(lines)
		return lines.size
	}

	@Transactional
	fun saveAll(lines: List<OrderLine>) {
		lines.forEach { line ->
			jdbcTemplate.update(
				"INSERT INTO orders (order_no, customer, quantity) VALUES (?, ?, ?)",
				line.orderNo,
				line.customer,
				line.quantity,
			)
			require(line.quantity > 0) { "수량은 1 이상이어야 합니다: ${line.orderNo}" }
		}
	}

	fun count(): Int =
		jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Int::class.java)!!

	fun clear() {
		jdbcTemplate.update("DELETE FROM orders")
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
