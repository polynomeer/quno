package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.PublicEmailDomainException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RequestEmailDomainVerificationUseCaseTest {
    private val emailDomainVerificationRepository = InMemoryEmailDomainVerificationRepository()
    private val emailSender = FakeVerificationEmailSender()
    private val useCase = RequestEmailDomainVerificationUseCase(emailDomainVerificationRepository, emailSender)

    @Test
    fun `sends a 6-digit code to a work email and stores a pending verification`() {
        val result = useCase.execute(1L, "alice@quno.dev")

        assertEquals("alice@quno.dev", result.email)
        val sent = emailSender.sent.single()
        assertEquals("alice@quno.dev", sent.toEmail)
        assertEquals(6, sent.code.length)
        assertTrue(sent.code.all { it.isDigit() })

        val stored = emailDomainVerificationRepository.findLatestByUserId(1L)
        assertEquals("quno.dev", stored!!.domain)
        assertEquals(sent.code, stored.code)
    }

    @Test
    fun `rejects a public webmail domain`() {
        assertFailsWith<PublicEmailDomainException> { useCase.execute(1L, "alice@gmail.com") }
    }

    @Test
    fun `a second request supersedes the first — only the latest is findable`() {
        useCase.execute(1L, "alice@quno.dev")
        useCase.execute(1L, "alice@quno.dev")

        assertEquals(2, emailSender.sent.size)
        val latest = emailDomainVerificationRepository.findLatestByUserId(1L)
        assertEquals(emailSender.sent.last().code, latest!!.code)
    }
}
