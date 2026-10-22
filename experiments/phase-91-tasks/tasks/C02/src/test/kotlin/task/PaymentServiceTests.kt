package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class PaymentServiceTests {
	@Autowired
	lateinit var paymentService: PaymentService

	@Test
	fun `청구 금액은 결제 금액에 수수료를 더한 값이다`() {
		val charged = paymentService.chargeAmount(BigDecimal("10000"))

		assertEquals(0, BigDecimal("10300").compareTo(charged))
	}
}
