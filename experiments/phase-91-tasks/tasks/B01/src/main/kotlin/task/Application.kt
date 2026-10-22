package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@SpringBootApplication
class Application

@Repository
class MemberRepository(
	private val jdbc: JdbcTemplate,
) {
	fun count(): Int = jdbc.queryForObject("select count(*) from member", Int::class.java) ?: 0

	fun emails(): List<String> = jdbc.queryForList("select email from member order by id", String::class.java)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
