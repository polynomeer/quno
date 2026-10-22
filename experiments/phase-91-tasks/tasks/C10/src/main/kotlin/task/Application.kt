package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@SpringBootApplication
class Application

/** 회원 등급별 할인율. */
@Component
class DiscountPolicy {
	fun rateFor(grade: String): BigDecimal = when (grade) {
		"VIP" -> BigDecimal("0.10")
		"GOLD" -> BigDecimal("0.05")
		else -> BigDecimal.ZERO
	}
}

/** 주문 금액에 회원 등급 할인을 적용한 견적을 낸다. */
@Service
class PriceQuoteService {
	lateinit var discountPolicy: DiscountPolicy

	fun quote(grade: String, amount: BigDecimal): BigDecimal {
		val discount = amount * discountPolicy.rateFor(grade)
		return (amount - discount).setScale(0, RoundingMode.HALF_UP)
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
