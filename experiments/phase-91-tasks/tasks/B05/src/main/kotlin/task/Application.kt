package task

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository

@SpringBootApplication
class Application

@Table("note")
data class Note(
	@Id val id: Long? = null,
	val title: String,
)

interface NoteRepository : CrudRepository<Note, Long>

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
