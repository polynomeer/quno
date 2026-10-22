package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal

@SpringBootApplication
class Application

data class PaymentResult(val approved: Boolean, val channel: String)

interface PaymentGateway {
	fun pay(orderNo: String, amount: BigDecimal): PaymentResult
}

/** 카드 결제 대행사 연동. */
@Component
class CardPaymentGateway : PaymentGateway {
	override fun pay(orderNo: String, amount: BigDecimal) = PaymentResult(true, "CARD")
}

/** 계좌이체 결제 연동. */
@Component
class BankTransferGateway : PaymentGateway {
	override fun pay(orderNo: String, amount: BigDecimal) = PaymentResult(true, "BANK")
}

/** 주문 결제를 처리한다. 기본 결제 수단은 카드다. */
@Service
class CheckoutService(
	private val gateway: PaymentGateway,
) {
	fun checkout(orderNo: String, amount: BigDecimal): PaymentResult =
		gateway.pay(orderNo, amount)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
