package task

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal

@SpringBootApplication
class Application

/** 결제 내역. */
@Entity
@Table(name = "payments")
class Payment(
	@Column(nullable = false)
	val orderNo: String,

	@Column(nullable = false)
	val amount: BigDecimal,

	@Column(nullable = false)
	val method: String,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null
}

interface PaymentRepository : JpaRepository<Payment, Long> {
	fun findByOrderNo(orderNo: String): List<Payment>
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
