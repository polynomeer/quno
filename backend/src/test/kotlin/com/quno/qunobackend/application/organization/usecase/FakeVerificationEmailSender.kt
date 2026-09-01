package com.quno.qunobackend.application.organization.usecase

import com.quno.qunobackend.domain.organization.VerificationEmailSender

class FakeVerificationEmailSender : VerificationEmailSender {
    data class SentEmail(val toEmail: String, val code: String)

    val sent = mutableListOf<SentEmail>()

    override fun sendVerificationCode(toEmail: String, code: String) {
        sent += SentEmail(toEmail, code)
    }
}
