package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
@EnableCaching
class Application

/** 항목을 일괄 저장하고, 저장하면 목록 캐시를 비운다. */
@Service
class ItemService {
	val savedCount = AtomicInteger()

	@CacheEvict(cacheNames = ["items"], allEntries = true)
	fun saveAll(names: List<String>): Flux<String> =
		Flux.fromIterable(names).doOnNext { savedCount.incrementAndGet() }
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
