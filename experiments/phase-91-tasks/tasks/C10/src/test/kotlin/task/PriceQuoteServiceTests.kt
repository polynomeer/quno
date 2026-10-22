package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class PriceQuoteServiceTests {
	@Autowired
	lateinit var priceQuoteService: PriceQuoteService

	@Test
	fun `VIP 회원은 10퍼센트 할인된 견적을 받는다`() {
		assertEquals(BigDecimal("45000"), priceQuoteService.quote("VIP", BigDecimal("50000")))
	}

	@Test
	fun `일반 회원은 할인이 없다`() {
		assertEquals(BigDecimal("50000"), priceQuoteService.quote("BASIC", BigDecimal("50000")))
	}
}
