package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.EmailDomainVerification
import com.quno.qunobackend.domain.organization.EmailDomainVerificationExpiredException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationNotFoundException
import com.quno.qunobackend.domain.organization.EmailDomainVerificationStatus
import com.quno.qunobackend.domain.organization.InvalidVerificationCodeException
import com.quno.qunobackend.domain.organization.VerifiedOrganizationJoinRequiresEmailException
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfirmEmailDomainVerificationUseCaseTest {
    private val emailDomainVerificationRepository = InMemoryEmailDomainVerificationRepository()
    private val organizationRepository = InMemoryOrganizationRepository()
    private val organizationMembershipRepository = InMemoryOrganizationMembershipRepository()
    private val emailSender = FakeVerificationEmailSender()
    private val requestUseCase = RequestEmailDomainVerificationUseCase(emailDomainVerificationRepository, emailSender)
    private val useCase = ConfirmEmailDomainVerificationUseCase(
        emailDomainVerificationRepository, organizationRepository, organizationMembershipRepository,
    )
    private val joinUseCase = JoinOrganizationUseCase(organizationRepository, organizationMembershipRepository)

    private fun codeFor(userId: Long, email: String): String {
        requestUseCase.execute(userId, email)
        return emailSender.sent.last().code
    }

    @Test
    fun `confirming the right code creates a verified organization and joins the user`() {
        val code = codeFor(1L, "alice@quno.dev")

        val result = useCase.execute(1L, code)

        assertEquals("quno.dev", result.emailDomain)
        assertTrue(result.verified)
        assertEquals(1L, result.memberCount)
        assertTrue(organizationMembershipRepository.isMember(result.id, 1L))
    }

    @Test
    fun `a second user verifying the same domain joins the same organization`() {
        val code1 = codeFor(1L, "alice@quno.dev")
        val org1 = useCase.execute(1L, code1)

        val code2 = codeFor(2L, "bob@quno.dev")
        val org2 = useCase.execute(2L, code2)

        assertEquals(org1.id, org2.id)
        assertEquals(2L, org2.memberCount)
    }

    @Test
    fun `upgrades a pre-existing Virtual organization that shares the domain's exact name`() {
        val preExisting = CreateOrganizationUseCase(organizationRepository, organizationMembershipRepository)
            .execute("quno.dev", null, createdBy = 99L)
        val code = codeFor(1L, "alice@quno.dev")

        val result = useCase.execute(1L, code)

        assertEquals(preExisting.id, result.id)
        assertTrue(result.verified)
    }

    @Test
    fun `wrong code is rejected`() {
        codeFor(1L, "alice@quno.dev")

        assertFailsWith<InvalidVerificationCodeException> { useCase.execute(1L, "000000") }
    }

    @Test
    fun `no pending verification is rejected`() {
        assertFailsWith<EmailDomainVerificationNotFoundException> { useCase.execute(1L, "123456") }
    }

    @Test
    fun `confirming twice fails the second time`() {
        val code = codeFor(1L, "alice@quno.dev")
        useCase.execute(1L, code)

        assertFailsWith<EmailDomainVerificationNotFoundException> { useCase.execute(1L, code) }
    }

    @Test
    fun `an expired code is rejected`() {
        emailDomainVerificationRepository.save(
            EmailDomainVerification.reconstitute(
                id = 1L,
                userId = 1L,
                email = "alice@quno.dev",
                domain = "quno.dev",
                code = "123456",
                status = EmailDomainVerificationStatus.PENDING,
                createdAt = Instant.now().minusSeconds(3600),
                expiresAt = Instant.now().minusSeconds(1),
                verifiedAt = null,
            ),
        )

        assertFailsWith<EmailDomainVerificationExpiredException> { useCase.execute(1L, "123456") }
    }

    @Test
    fun `a verified organization cannot be joined directly`() {
        val code = codeFor(1L, "alice@quno.dev")
        val organization = useCase.execute(1L, code)

        assertFailsWith<VerifiedOrganizationJoinRequiresEmailException> { joinUseCase.execute(2L, organization.id) }
    }
}
