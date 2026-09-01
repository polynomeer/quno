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
import com.quno.qunobackend.domain.directask.DirectAskPaymentNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskPaymentStatus
import com.quno.qunobackend.domain.directask.DirectAskRequestStatus
import com.quno.qunobackend.domain.directask.PaymentAlreadyProcessedException
import com.quno.qunobackend.domain.directask.PaymentAmountMismatchException
import com.quno.qunobackend.domain.directask.PaymentConfirmationFailedException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfirmDirectAskPaymentUseCaseTest {
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
    private val useCase =
        ConfirmDirectAskPaymentUseCase(directAskPaymentRepository, directAskRequestRepository, paymentGateway, outboxEventRepository)

    private fun openRequest(): Triple<String, Long, Long> {
        val requesterId = signUpUseCase.execute(SignUpCommand("a@b.com", "alice", "password123")).userId
        val targetId = signUpUseCase.execute(SignUpCommand("c@d.com", "bob", "password123")).userId
        updateDirectAskSettingsUseCase.execute(targetId, accepts = true)
        val questionId = createQuestionUseCase.execute(CreateQuestionCommand(requesterId, "t", "body", null, null)).id
        val created = createDirectAskRequestUseCase.execute(CreateDirectAskRequestCommand(questionId, requesterId, targetId, null))
        return Triple(created.payment.orderId, targetId, requesterId)
    }

    @Test
    fun `confirming activates the request and notifies the target`() {
        val (orderId, targetId, requesterId) = openRequest()

        val result = useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 1000L, requesterId))

        assertEquals(DirectAskRequestStatus.PENDING, result.status)
        assertEquals(DirectAskPaymentStatus.PAID, directAskPaymentRepository.findByOrderId(orderId)!!.status)
        assertEquals(listOf("toss-key-1"), paymentGateway.confirmedPaymentKeys)
        val event = outboxEventRepository.events.single { it.eventType == OutboxEventTypes.DIRECT_ASK_REQUESTED }
        assertTrue(event.payload.contains("\"targetUserId\":$targetId"))
    }

    @Test
    fun `an amount that does not match the recorded fee is rejected before calling Toss`() {
        val (orderId, _, requesterId) = openRequest()

        assertFailsWith<PaymentAmountMismatchException> {
            useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 999_999L, requesterId))
        }
        assertTrue(paymentGateway.confirmedPaymentKeys.isEmpty())
    }

    @Test
    fun `an unknown order id is rejected`() {
        assertFailsWith<DirectAskPaymentNotFoundException> {
            useCase.execute(ConfirmDirectAskPaymentCommand("no-such-order", "toss-key-1", 1000L, 1L))
        }
    }

    @Test
    fun `confirming twice fails the second time`() {
        val (orderId, _, requesterId) = openRequest()
        useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 1000L, requesterId))

        assertFailsWith<PaymentAlreadyProcessedException> {
            useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 1000L, requesterId))
        }
    }

    @Test
    fun `a rejection from Toss propagates and leaves the request unactivated`() {
        val (orderId, _, requesterId) = openRequest()
        paymentGateway.shouldFailConfirm = true

        assertFailsWith<PaymentConfirmationFailedException> {
            useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 1000L, requesterId))
        }
        assertEquals(DirectAskPaymentStatus.PENDING, directAskPaymentRepository.findByOrderId(orderId)!!.status)
    }

    @Test
    fun `someone other than the requester cannot confirm the payment`() {
        val (orderId, targetId, _) = openRequest()

        assertFailsWith<DirectAskAccessDeniedException> {
            useCase.execute(ConfirmDirectAskPaymentCommand(orderId, "toss-key-1", 1000L, targetId))
        }
        assertTrue(paymentGateway.confirmedPaymentKeys.isEmpty())
        assertEquals(DirectAskPaymentStatus.PENDING, directAskPaymentRepository.findByOrderId(orderId)!!.status)
    }
}
