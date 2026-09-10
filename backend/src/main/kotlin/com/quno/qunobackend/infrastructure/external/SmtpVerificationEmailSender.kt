package com.quno.qunobackend.infrastructure.external

import com.quno.qunobackend.domain.organization.VerificationEmailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/** Spring Boot auto-configures [JavaMailSender] from `spring.mail.*` (application-local.yml
 * points it at the Mailpit catcher; see VerificationEmailSender's kdoc for the production gap).
 * `application-{local,prod}.yml`이 SMTP 연결/응답 타임아웃을 명시한다 — 기본값이 없어서 서버가
 * 느리거나 응답이 없으면 이 호출이 사실상 무한정 블로킹될 수 있었다(production-readiness.md B-4). */
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
        sendWithRetry(message)
    }

    // 인증 메일 발송은 재시도해도 안전하다(최악의 경우 같은 안내를 두 번 받는 정도) — 결제 확인처럼
    // 중복 실행이 위험한 작업과는 다르다. 짧은 지수 백오프로 일시적인 네트워크 문제만 흡수한다.
    private fun sendWithRetry(message: SimpleMailMessage, attempt: Int = 1) {
        try {
            mailSender.send(message)
        } catch (e: MailException) {
            if (attempt >= MAX_ATTEMPTS) throw e
            Thread.sleep(RETRY_DELAY_MS * attempt)
            sendWithRetry(message, attempt + 1)
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L
    }
}
