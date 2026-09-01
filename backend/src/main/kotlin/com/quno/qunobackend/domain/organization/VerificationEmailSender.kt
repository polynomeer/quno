package com.quno.qunobackend.domain.organization

/**
 * Outbound port to send the verification code (Phase 23, ADR-0035) — implemented by
 * infrastructure/external/SmtpVerificationEmailSender via Spring's JavaMailSender. Local dev
 * points at a Mailpit catcher (docker-compose); production SMTP credentials are not configured
 * by this codebase and must be supplied via environment/secrets at deploy time.
 */
interface VerificationEmailSender {
    fun sendVerificationCode(toEmail: String, code: String)
}
