package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class Application

/** 가입 신청서(multipart/form-data) 폼을 받는다. 폼 항목 하나가 파트 하나로 전송된다. */
@RestController
class ApplicationFormController {
	@PostMapping("/applications", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun submit(
		@RequestParam fields: Map<String, String>,
	): Map<String, Int> = mapOf("received" to fields.size)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
