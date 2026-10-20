package task

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

@SpringBootApplication
class Application

@Entity
class Member(
	val name: String,
	val nickname: String?,
	val active: Boolean = true,
) {
	@Id
	@GeneratedValue
	var id: Long? = null
}

/** 회원 목록 화면에 보여 줄 표시 이름. */
data class DisplayName(
	val value: String,
)

interface MemberRepository : JpaRepository<Member, Long> {
	/** 활성 회원의 표시 이름(닉네임이 있으면 닉네임, 없으면 이름) 목록. 중복은 제거한다. */
	@Query(
		"select distinct new task.DisplayName(coalesce(nickname, name)) " +
			"from Member where active = true",
	)
	fun findDisplayNames(pageable: Pageable): Page<DisplayName>
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
