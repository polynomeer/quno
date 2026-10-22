package task

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class NoteRepositoryTests {
	@Autowired
	lateinit var notes: NoteRepository

	@Test
	fun `노트를 저장하고 조회한다`() {
		val saved = notes.save(Note(title = "hello"))

		assertThat(notes.findById(saved.id!!).get().title).isEqualTo("hello")
	}
}
