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
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.directask.DirectAskAccessDeniedException
import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestAlreadyRespondedException
import com.quno.qunobackend.domain.directask.DirectAskRequestNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RespondToDirectAskRequestUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val questionRepository = InMemoryQuestionRepository()
    private val questionVersionRepository = InMemoryQuestionVersionRepository()
    private val tagRepository = InMemoryTagRepository()
    private val directAskRequestRepository = InMemoryDirectAskRequestRepository()
    private val directAskPaymentRepository = InMemoryDirectAskPaymentRepository()
    private val paymentGateway = FakePaymentGateway()
    private val outboxEventRepository = InMemoryOutboxEventRepository()

    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val createQuestionUseCase = CreateQuestionUseCase(
        questionRepository, questionVersionRepository, tagRepository, InMemoryQuestionTagRepository(tagRepository),
    )
    private val updateDirectAskSettingsUseCase = UpdateDirectAskSettingsUseCase(userRepository)
    private val createDirectAskRequestUseCase = CreateDirectAskRequestUseCase(
        questionRepository, userRepository, directAskRequestRepository, directAskPaymentRepository,
        feeAmount = 1000L, tossClientKey = "test_ck_fake",
    )
    private val confirmPaymentUseCase =
        ConfirmDirectAskPaymentUseCase(directAskPaymentRepository, directAskRequestRepository, paymentGateway, outboxEventRepository)
    private val useCase = RespondToDirectAskRequestUseCase(directAskRequestRepository, directAskPaymentRepository, paymentGateway, outboxEventRepository)

    private fun signUp(email: String, nickname: String): Long = signUpUseCase.execute(SignUpCommand(email, nickname, "password123")).userId

    /** Creates a request and immediately confirms its payment, landing it in PENDING (open,
     * visible to the target) — the state most response tests need to start from. */
    private fun pendingRequest(): Triple<Long, Long, Long> {
        val requesterId = signUp("a@b.com", "alice")
        val targetId = signUp("c@d.com", "bob")
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)
        val questionId = createQuestionUseCase.execute(CreateQuestionCommand(requesterId, "t", "body", null, null)).id
        val created = createDirectAskRequestUseCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, null))
        confirmPaymentUseCase.execute(ConfirmDirectAskPaymentCommand(created.payment.orderId, "toss-key-1", created.payment.amount))
        return Triple(created.request.id, requesterId, targetId)
    }

    @Test
    fun `the target can accept, notifying the requester and keeping the payment`() {
        val (requestId, requesterId, targetId) = pendingRequest()

        useCase.execute(requestId, targetId, accept = true)

        assertEquals(DirectAskRequestStatus.ACCEPTED, directAskRequestRepository.findById(requestId)!!.status)
        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.DIRECT_ASK_ACCEPTED }
        assertTrue(event.payload.contains("\"requesterId\":$requesterId"))
        assertEquals(DirectAskPaymentStatus.PAID, directAskPaymentRepository.findByDirectAskRequestId(requestId)!!.status)
        assertTrue(paymentGateway.cancelledPaymentKeys.isEmpty())
    }

    @Test
    fun `the target can decline, notifying the requester and refunding the payment`() {
        val (requestId, requesterId, targetId) = pendingRequest()

        useCase.execute(requestId, targetId, accept = false)

        assertEquals(DirectAskRequestStatus.DECLINED, directAskRequestRepository.findById(requestId)!!.status)
        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.DIRECT_ASK_DECLINED }
        assertTrue(event.payload.contains("\"requesterId\":$requesterId"))
        assertEquals(DirectAskPaymentStatus.CANCELLED, directAskPaymentRepository.findByDirectAskRequestId(requestId)!!.status)
        assertEquals(listOf("toss-key-1"), paymentGateway.cancelledPaymentKeys)
    }

    @Test
    fun `someone other than the target cannot respond`() {
        val (requestId, requesterId, _) = pendingRequest()

        assertFailsWith<DirectAskAccessDeniedException> { useCase.execute(requestId, requesterId, accept = true) }
    }

    @Test
    fun `responding twice fails the second time`() {
        val (requestId, _, targetId) = pendingRequest()
        useCase.execute(requestId, targetId, accept = true)

        assertFailsWith<DirectAskRequestAlreadyRespondedException> { useCase.execute(requestId, targetId, accept = false) }
    }

    @Test
    fun `responding to a request that does not exist fails`() {
        assertFailsWith<DirectAskRequestNotFoundException> { useCase.execute(999L, 1L, accept = true) }
    }
}
