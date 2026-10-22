package task

import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.Serializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.ProducerFactory
import java.util.concurrent.CopyOnWriteArrayList

private val textSerializer = Serializer<Any> { _, data -> data.toString().toByteArray() }

/** 브로커 없이 테스트하기 위한 프로듀서 팩토리. 만들어 준 MockProducer를 기억해 둔다. */
class RecordingProducerFactory : ProducerFactory<Any, Any> {
	val producers = CopyOnWriteArrayList<MockProducer<Any, Any>>()

	override fun transactionCapable() = true

	override fun createProducer(): Producer<Any, Any> = newProducer()

	override fun createProducer(txIdPrefix: String?): Producer<Any, Any> = newProducer()

	override fun getConfigurationProperties(): Map<String, Any> = emptyMap()

	private fun newProducer(): Producer<Any, Any> =
		MockProducer<Any, Any>(true, null, textSerializer, textSerializer)
			.also {
				it.initTransactions()
				producers += it
			}
}

@TestConfiguration
class TestKafkaConfig {
	@Bean
	fun producerFactory(): ProducerFactory<Any, Any> = RecordingProducerFactory()
}

@SpringBootTest
@Import(TestKafkaConfig::class)
class OrderEventPublisherTests {
	@Autowired
	lateinit var publisher: OrderEventPublisher

	@Autowired
	lateinit var producerFactory: ProducerFactory<Any, Any>

	private val factory get() = producerFactory as RecordingProducerFactory

	@BeforeEach
	fun reset() {
		factory.producers.clear()
	}

	@Test
	fun `발행한 이벤트는 커밋되어 모두 전송된다`() {
		publisher.publishAll(listOf("o-1", "o-2"))

		val sent = factory.producers.flatMap { it.history() }.map { it.value() }
		assertThat(sent).containsExactly("created:o-1", "created:o-2")
	}

	@Test
	fun `중간에 실패하면 보낸 이벤트도 취소된다`() {
		assertThatThrownBy { publisher.publishThenFail(listOf("o-3", "o-4")) }
			.isInstanceOf(IllegalStateException::class.java)
			.hasMessage("재고 확인 실패")

		val sent = factory.producers.flatMap { it.history() }
		assertThat(sent).isEmpty()
	}
}
