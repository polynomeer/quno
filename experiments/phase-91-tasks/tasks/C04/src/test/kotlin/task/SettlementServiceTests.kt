package task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class SettlementServiceTests {
	@Autowired
	lateinit var settlementService: SettlementService

	@Test
	fun `정산 금액은 매출에서 수수료를 뺀 값이다`() {
		assertEquals(BigDecimal("97000"), settlementService.settle(BigDecimal("100000"), BigDecimal("0.03")))
	}
}
