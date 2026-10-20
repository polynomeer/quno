package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class Application

data class ShipmentStatus(val orderNo: String, val status: String)

/** 주문번호로 배송 상태를 조회한다. */
@RestController
class ShipmentController {
	@GetMapping("/shipments/{orderNo}")
	fun status(@PathVariable orderNo: String) = ShipmentStatus(orderNo, "IN_TRANSIT")
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
