package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.directask.dto.CreateDirectAskRequestCommand
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionVersionRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryQuestionTagRepository
import com.quno.qunobackend.application.tag.usecase.InMemoryTagRepository
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.InMemoryUserRepository
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.application.user.usecase.UpdateDirectAskSettingsUseCase
import com.quno.qunobackend.domain.directask.DirectAskNotAcceptedException
import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.domain.directask.DuplicateDirectAskException
import com.quno.qunobackend.domain.directask.SelfDirectAskException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateDirectAskRequestUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val directAskRequestRepository = InMemoryDirectAskRequestRepository()
    private val directAskPaymentRepository = InMemoryDirectAskPaymentRepository()

    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val updateDirectAskSettingsUseCase = UpdateDirectAskSettingsUseCase(userRepository)
    private val useCase = CreateDirectAskRequestUseCase(
        questionRepository, userRepository, directAskRequestRepository, directAskPaymentRepository,
        feeAmount = 1000L, tossClientKey = "test_ck_fake",
    )

    private fun signUp(email: String, nickname: String): Long = signUpUseCase.execute(SignUpCommand(email, nickname, "password123")).userId

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(CreateQuestionCommand(authorId, "t", "body", null, null)).id

    @Test
    fun `creates a request awaiting payment when the target accepts Direct Ask`() {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)
        val questionId = questionAskedBy(requesterId)

        val result = useCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, "please help"))

        assertEquals(questionId, result.request.questionId)
        assertEquals(targetId, result.request.targetUserId)
        assertEquals(DirectAskRequestStatus.AWAITING_PAYMENT, result.request.status)
        assertEquals(1000L, result.payment.amount)
        assertEquals(DirectAskPaymentStatus.PENDING, result.payment.status)
        assertEquals("test_ck_fake", result.payment.clientKey)

        val storedPayment = directAskPaymentRepository.findByOrderId(result.payment.orderId)
        assertEquals(1000L, storedPayment!!.amount)
    }

    @Test
    fun `rejects a target who has not opted into Direct Ask`() {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        val questionId = questionAskedBy(requesterId)

        assertFailsWith<DirectAskNotAcceptedException> {
            useCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, null))
        }
    }

    @Test
    fun `rejects asking yourself`() {
        val userId = signUp("a@b.com", "alice")
        updateDirectAskSettingsUseCase.execute(userId, accepts = true)
        val questionId = questionAskedBy(userId)

        assertFailsWith<SelfDirectAskException> {
            useCase.execute(CreateDirectAskRequestCommand(questionId, userId, userId, null))
        }
    }

    @Test
    fun `rejects a question that does not exist`() {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)

        assertFailsWith<QuestionNotFoundException> {
            useCase.execute(CreateDirectAskRequestCommand(999L, requesterId, targetId, null))
        }
    }

    @Test
    fun `rejects a second open request for the same question and target`() {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)
        val questionId = questionAskedBy(requesterId)
        useCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, null))

        assertFailsWith<DuplicateDirectAskException> {
            useCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, null))
        }
    }
}
