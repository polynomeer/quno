package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.math.BigDecimal

@SpringBootApplication(proxyBeanMethods = false)
class Application

/** 결제 수수료율. */
data class FeePolicy(val rate: BigDecimal)

@Configuration
class PaymentConfig {
	@Bean
	fun feePolicy(): FeePolicy = FeePolicy(BigDecimal("0.03"))
}

/** 결제 금액에 수수료를 더해 청구 금액을 계산한다. */
@Service
class PaymentService(
	private val feePolicy: FeePolicy,
) {
	fun chargeAmount(amount: BigDecimal): BigDecimal =
		amount + amount * feePolicy.rate
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
