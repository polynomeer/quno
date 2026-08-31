package com.quno.qunobackend.application.report.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
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
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.report.ModeratorAccessDeniedException
import com.quno.qunobackend.domain.report.ReportAlreadyResolvedException
import com.quno.qunobackend.domain.report.ReportReason
import com.quno.qunobackend.domain.report.ReportTargetType
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HideReportedContentUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val questionRepository = InMemoryQuestionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, InMemoryQuestionVersionRepository(), tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val answerRepository = InMemoryAnswerRepository()
    private val reportRepository = InMemoryReportRepository()
    private val fileReportUseCase = FileReportUseCase(questionRepository, answerRepository, reportRepository)
    private val outboxEventRepository = InMemoryOutboxEventRepository()
    private val hideReportedContentUseCase = HideReportedContentUseCase(
        userRepository, questionRepository, answerRepository, reportRepository, outboxEventRepository,
    )

    private fun aUser(nickname: String): Long =
        signUpUseCase.execute(SignUpCommand("$nickname@example.com", nickname, "password123")).userId

    @Test
    fun `hiding a reported question soft-deletes it and notifies only its author`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val authorId = aUser("author")
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = authorId, title = "t", body = "body", environment = null, logs = null),
        ).id
        val reportId = fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null),
        ).id

        hideReportedContentUseCase.execute(moderatorId, reportId)

        assertNotNull(requireNotNull(questionRepository.findById(questionId)).deletedAt)
        val event = outboxEventRepository.events.single()
        assertEquals(OutboxEventTypes.CONTENT_HIDDEN, event.eventType)
        assertTrue(event.payload.contains(""""contentAuthorId":$authorId"""))
    }

    @Test
    fun `hiding a reported answer soft-deletes it without touching the question`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val questionAuthorId = aUser("qauthor")
        val answerAuthorId = aUser("aauthor")
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = questionAuthorId, title = "t", body = "body", environment = null, logs = null),
        ).id
        val answerId = requireNotNull(answerRepository.save(Answer.write(questionId, answerAuthorId, "answer body", 1)).id)
        val reportId = fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.ANSWER, answerId, ReportReason.LOW_QUALITY, null),
        ).id

        hideReportedContentUseCase.execute(moderatorId, reportId)

        assertNull(answerRepository.findById(answerId))
        assertNotNull(questionRepository.findById(questionId))
        val event = outboxEventRepository.events.single()
        assertTrue(event.payload.contains(""""contentAuthorId":$answerAuthorId"""))
        assertTrue(event.payload.contains(""""answerId":$answerId"""))
    }

    @Test
    fun `re-hiding an already resolved report fails with the report's own conflict, not a 404 from the now-deleted target`() {
        val moderatorId = aUser("mod")
        userRepository.promoteToModerator(moderatorId)
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = aUser("author"), title = "t", body = "body", environment = null, logs = null),
        ).id
        val reportId = fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null),
        ).id
        hideReportedContentUseCase.execute(moderatorId, reportId)

        assertFailsWith<ReportAlreadyResolvedException> { hideReportedContentUseCase.execute(moderatorId, reportId) }
    }

    @Test
    fun `a non-moderator cannot hide reported content`() {
        val userId = aUser("regular")
        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(authorId = aUser("author"), title = "t", body = "body", environment = null, logs = null),
        ).id
        val reportId = fileReportUseCase.execute(
            FileReportCommand(aUser("reporter"), ReportTargetType.QUESTION, questionId, ReportReason.SPAM, null),
        ).id

        assertFailsWith<ModeratorAccessDeniedException> { hideReportedContentUseCase.execute(userId, reportId) }
    }
}
