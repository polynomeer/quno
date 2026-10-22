package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class CheckoutServiceTests {
	@Autowired
	lateinit var checkoutService: CheckoutService

	@Test
	fun `체크아웃은 기본 결제 수단인 카드로 결제한다`() {
		val result = checkoutService.checkout("ORD-2001", BigDecimal("32000"))

		assertTrue(result.approved)
		assertEquals("CARD", result.channel)
	}
}
