package task

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootApplication
class Application

/** 첨부 파일을 업로드한다. 빈 파일이면 업로드를 건너뛰고 파일명만 기록한다. */
@RestController
class UploadController {
	private val logger = LoggerFactory.getLogger(javaClass)

	@PostMapping("/uploads", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun upload(
		@RequestPart("file", required = false) file: FilePart?,
		@RequestPart("action", required = false) action: String?,
		@RequestPart("reason", required = false) reason: String?,
	): Mono<ResponseEntity<String>> {
		logger.info("action={}, reason={}, filename={}", action, reason, file?.filename())
		val body = file?.let { "received: ${it.filename()}" } ?: "no file received"
		return Mono.just(ResponseEntity.ok(body))
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
