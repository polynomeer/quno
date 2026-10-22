package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootApplication
class Application

/** 정산 금액을 계산한다. */
@Service
class SettlementService {
	fun settle(sales: BigDecimal, feeRate: BigDecimal): BigDecimal =
		(sales - sales * feeRate).setScale(0, RoundingMode.DOWN)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
