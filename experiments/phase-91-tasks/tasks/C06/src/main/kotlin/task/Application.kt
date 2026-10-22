package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@SpringBootApplication
class Application

data class Refund(val paymentNo: String, val amount: BigDecimal, val reason: String?)

/** 환불 요청을 저장하고 조회한다. */
@Repository
class RefundRepository(
	private val jdbcTemplate: JdbcTemplate,
) {
	fun save(refund: Refund) {
		jdbcTemplate.update(
			"INSERT INTO refunds (payment_no, amount, reason) VALUES (?, ?, ?)",
			refund.paymentNo,
			refund.amount,
			refund.reason,
		)
	}

	fun findByPaymentNo(paymentNo: String): List<Refund> =
		jdbcTemplate.query(
			"SELECT payment_no, amount, reason FROM refunds WHERE payment_no = ?",
			{ rs, _ -> Refund(rs.getString("payment_no"), rs.getBigDecimal("amount"), rs.getString("reason")) },
			paymentNo,
		)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
