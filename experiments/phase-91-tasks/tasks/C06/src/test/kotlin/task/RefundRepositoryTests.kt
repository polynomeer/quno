package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class RefundRepositoryTests {
	@Autowired
	lateinit var refundRepository: RefundRepository

	@Test
	fun `저장한 환불 요청을 결제번호로 조회한다`() {
		refundRepository.save(Refund("PAY-7001", BigDecimal("5000.00"), "단순 변심"))

		val found = refundRepository.findByPaymentNo("PAY-7001")

		assertEquals(1, found.size)
		assertEquals("단순 변심", found[0].reason)
	}
}
