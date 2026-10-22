package task

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@SpringBootTest
@Transactional
class PaymentRepositoryTests {
	@Autowired
	lateinit var paymentRepository: PaymentRepository

	@Autowired
	lateinit var entityManager: EntityManager

	@Test
	fun `저장한 결제 내역을 주문번호로 다시 조회할 수 있다`() {
		paymentRepository.save(Payment("ORD-1001", BigDecimal("12900"), "CARD"))
		entityManager.flush()
		entityManager.clear()

		val found = paymentRepository.findByOrderNo("ORD-1001")

		assertEquals(1, found.size)
		assertEquals("CARD", found[0].method)
	}
}
