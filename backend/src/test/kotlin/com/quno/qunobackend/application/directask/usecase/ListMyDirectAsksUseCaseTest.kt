package com.quno.qunobackend.application.directask.usecase

import com.quno.qunobackend.application.common.InMemoryOutboxEventRepository
import com.quno.qunobackend.application.directask.dto.ConfirmDirectAskPaymentCommand
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
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListMyDirectAsksUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val directAskRequestRepository = InMemoryDirectAskRequestRepository()
    private val directAskPaymentRepository = InMemoryDirectAskPaymentRepository()
    private val paymentGateway = FakePaymentGateway()

    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val updateDirectAskSettingsUseCase = UpdateDirectAskSettingsUseCase(userRepository)
    private val createDirectAskRequestUseCase = CreateDirectAskRequestUseCase(
        questionRepository, userRepository, directAskRequestRepository, directAskPaymentRepository,
        feeAmount = 1000L, tossClientKey = "test_ck_fake",
    )
    private val confirmDirectAskPaymentUseCase = ConfirmDirectAskPaymentUseCase(
        directAskPaymentRepository, directAskRequestRepository, paymentGateway, InMemoryOutboxEventRepository(),
    )
    private val useCase = ListMyDirectAsksUseCase(directAskRequestRepository, questionRepository, userRepository)

    private fun signUp(email: String, nickname: String): Long = signUpUseCase.execute(SignUpCommand(email, nickname, "password123")).userId

    private fun questionAskedBy(authorId: Long): Long =
        createQuestionUseCase.execute(CreateQuestionCommand(authorId, "How does X work?", "body", null, null)).id

    @Test
    fun `enriches sent and received lists with question title and nicknames, and hides unpaid requests from the target`() {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)
        val questionId = questionAskedBy(requesterId)

        val created = createDirectAskRequestUseCase.execute(
            CreateDirectAskRequestCommand(questionId, requesterId, targetId, "please help"),
        )

        val sentBeforePayment = useCase.executeSent(requesterId)
        assertEquals(1, sentBeforePayment.size)
        assertEquals(DirectAskRequestStatus.AWAITING_PAYMENT, sentBeforePayment[0].status)
        assertEquals("How does X work?", sentBeforePayment[0].questionTitle)
        assertEquals("alice", sentBeforePayment[0].requesterNickname)
        assertEquals("bob", sentBeforePayment[0].targetUserNickname)

        assertTrue(useCase.executeReceived(targetId).isEmpty())

        confirmDirectAskPaymentUseCase.execute(
            ConfirmDirectAskPaymentCommand(created.payment.orderId, "mock-payment-key", created.payment.amount, requesterId),
        )

        val received = useCase.executeReceived(targetId)
        assertEquals(1, received.size)
        assertEquals(DirectAskRequestStatus.PENDING, received[0].status)
        assertEquals("How does X work?", received[0].questionTitle)
        assertEquals("alice", received[0].requesterNickname)
    }
}
