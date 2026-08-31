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
import com.quno.qunobackend.domain.report.ReportAlreadyResolvedException
import com.quno.qunobackend.domain.report.ReportNotFoundException
import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportStatus
import com.quno.qunobackend.domain.report.ReportTargetType
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class DismissReportUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val reportRepository = InMemoryReportRepository()
    private val fileReportUseCase = FileReportUseCase(questionRepository, InMemoryAnswerRepository(), reportRepository)
    private val dismissReportUseCase = DismissReportUseCase(userRepository, reportRepository)

    private fun aUser(nickname: String): Long =
        signUpUseCase.execute(SignUpCommand("$nickname@example.com", nickname, "password123")).userId

    private fun aPendingReport(): Long {
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = aUser("author"), title = "t", body = "body", environment = null, logs = null),
        ).id
        return fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.LOW_QUALITY, null),
        ).id
    }

    @Test
    fun `a moderator can dismiss a pending report, leaving the content untouched`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val reportId = aPendingReport()

        dismissReportUseCase.execute(moderatorId, reportId)

        val report = requireNotNull(reportRepository.findById(reportId))
        assertEquals(ReportStatus.DISMISSED, report.status)
        assertEquals(moderatorId, report.resolvedBy)
        assertNotNull(report.resolvedAt)
    }

    @Test
    fun `a non-moderator cannot dismiss a report`() {
        val userId = aUser("regular")
        val reportId = aPendingReport()

        assertFailsWith<ModeratorAccessDeniedException> { dismissReportUseCase.execute(userId, reportId) }
    }

    @Test
    fun `rejects dismissing a report that does not exist`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)

        assertFailsWith<ReportNotFoundException> { dismissReportUseCase.execute(moderatorId, 999L) }
    }

    @Test
    fun `rejects dismissing an already resolved report`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val reportId = aPendingReport()
        dismissReportUseCase.execute(moderatorId, reportId)

        assertFailsWith<ReportAlreadyResolvedException> { dismissReportUseCase.execute(moderatorId, reportId) }
    }
}
