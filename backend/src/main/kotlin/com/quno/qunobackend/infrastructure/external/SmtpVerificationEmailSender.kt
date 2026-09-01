package com.quno.qunobackend.infrastructure.external

import com.quno.qunobackend.domain.organization.VerificationEmailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/** Spring Boot auto-configures [JavaMailSender] from `spring.mail.*` (application-local.yml
 * points it at the Mailpit catcher; see VerificationEmailSender's kdoc for the production gap). */
@Component
class SmtpVerificationEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${quno.mail.from-address}") private val fromAddress: String,
) : VerificationEmailSender {

    override fun sendVerificationCode(toEmail: String, code: String) {
        val message = SimpleMailMessage().apply {
            setFrom(fromAddress)
            setTo(toEmail)
            setSubject("Quno 조직 인증 코드")
            setText("인증 코드: $code\n\n15분 이내에 입력해주세요.")
        }
        mailSender.send(message)
    }
}
