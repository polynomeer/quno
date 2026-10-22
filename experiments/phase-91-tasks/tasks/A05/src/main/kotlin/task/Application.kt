package task

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@SpringBootApplication
class Application

/** 통화 기록. [duration]은 통화 시간(초)이다. */
@Entity
class Call(
	val caller: String,
	val duration: Int,
) {
	@Id
	@GeneratedValue
	var id: Long? = null
}

interface CallRepository : JpaRepository<Call, Long> {
	/** 통화 시간이 짝수 초인 기록. */
	@Query("select c from Call c where c.duration % 2 = 0 order by c.id")
	fun findEvenDurations(): List<Call>
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
