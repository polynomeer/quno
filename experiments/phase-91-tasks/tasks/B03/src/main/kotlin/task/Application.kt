package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
class Application

/**
 * 주문 이벤트를 `orders` 토픽으로 발행한다.
 * 한 번의 호출에서 보낸 이벤트는 모두 함께 커밋되거나 함께 취소되어야 한다.
 */
@Service
class OrderEventPublisher(
	private val kafkaTemplate: KafkaTemplate<String, String>,
) {
	@Transactional
	fun publishAll(orderIds: List<String>) {
		orderIds.forEach { kafkaTemplate.send("orders", it, "created:$it") }
	}

	@Transactional
	fun publishThenFail(orderIds: List<String>) {
		orderIds.forEach { kafkaTemplate.send("orders", it, "created:$it") }
		throw IllegalStateException("재고 확인 실패")
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
