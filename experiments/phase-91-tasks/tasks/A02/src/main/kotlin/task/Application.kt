package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class Application

@RestController
class HelloController {
	@GetMapping("/api/hello")
	fun hello() = "hello"
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
