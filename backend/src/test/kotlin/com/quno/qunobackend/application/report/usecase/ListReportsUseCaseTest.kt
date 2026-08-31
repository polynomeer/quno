package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.report.dto.FileReportCommand
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.domain.report.ModeratorAccessDeniedException
import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListReportsUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val reportRepository = InMemoryReportRepository()
    private val fileReportUseCase = FileReportUseCase(questionRepository, InMemoryAnswerRepository(), reportRepository)
    private val listReportsUseCase = ListReportsUseCase(userRepository, reportRepository)

    private fun aUser(nickname: String): Long =
        signUpUseCase.execute(SignUpCommand("$nickname@example.com", nickname, "password123")).userId

    private fun aQuestion(authorId: Long): Long = createQuestionUseCase.execute(
        CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
    ).id

    @Test
    fun `a moderator sees pending reports`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val questionId = aQuestion(authorId = aUser("author"))
        fileReportUseCase.execute(FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null))

        val result = listReportsUseCase.execute(moderatorId, ReportStatus.PENDING)

        assertEquals(1, result.size)
    }

    @Test
    fun `a non-moderator cannot list reports`() {
        val userId = aUser("regular")

        assertFailsWith<ModeratorAccessDeniedException> { listReportsUseCase.execute(userId, ReportStatus.PENDING) }
    }

    @Test
    fun `filters by status`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val questionId = aQuestion(authorId = aUser("author"))
        val reportId = fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null),
        ).id
        val dismissUseCase = DismissReportUseCase(userRepository, reportRepository)
        dismissUseCase.execute(moderatorId, reportId)

        assertEquals(0, listReportsUseCase.execute(moderatorId, ReportStatus.PENDING).size)
        assertEquals(1, listReportsUseCase.execute(moderatorId, ReportStatus.DISMISSED).size)
    }
}
