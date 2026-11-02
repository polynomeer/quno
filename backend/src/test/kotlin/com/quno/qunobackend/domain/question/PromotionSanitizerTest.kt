package com.quno.qunobackend.domain.question

import com.quno.qunobackend.domain.engine.SecretDetector
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromotionSanitizerTest {
    private val terms = PromotionSanitizer.organizationTermsOf("Acme Pay", "acme-pay", "acmepay.co.kr")

    private fun check(text: String) = PromotionSanitizer.check(mapOf("body" to text), terms)

    @Test
    fun `organization terms are the name, slug, domain and its first label`() {
        assertEquals(listOf("Acme Pay", "acme-pay", "acmepay.co.kr", "acmepay"), terms)
        assertEquals(listOf("Acme", "ab.com"), PromotionSanitizer.organizationTermsOf("Acme", "acme", "ab.com"), "short labels and duplicates drop out")
    }

    @Test
    fun `the organization's own identifiers block, case-insensitively and only as whole words`() {
        val findings = check("Deploy at ACMEPAY broke; see wiki.acmepay.co.kr. acmepayments is someone else.")
        assertEquals(listOf("ACMEPAY", "acmepay.co.kr"), findings.map { it.excerpt })
        assertTrue(findings.all { it.severity == PromotionFindingSeverity.BLOCK && it.label == "조직 식별자" })
        assertEquals(1, check("Acme Pay 결제 모듈").size, "works next to Hangul too")
    }

    @Test
    fun `secrets block and are never echoed in full`() {
        val token = "ghp_" + "a1B2".repeat(9)
        val finding = check("export GH=$token").single()
        assertEquals(PromotionFindingSeverity.BLOCK, finding.severity)
        assertEquals("ghp_…", finding.excerpt)
        assertEquals(token, "export GH=$token".substring(finding.start, finding.end))
        assertEquals(emptyList(), check("password: ${SecretDetector.REDACTED}"), "an already redacted value is fine")
    }

    @Test
    fun `internal-looking values only warn`() {
        val findings = check("db at 10.1.2.3 and 192.168.0.10, host build-01.corp, mail me@example.org, log /Users/kim/app.log; jackson 2.13.4.2 is fine")
        assertEquals(
            listOf("사설 IP 주소" to "10.1.2.3", "사설 IP 주소" to "192.168.0.10", "내부 호스트 이름" to "build-01.corp", "이메일 주소" to "me@example.org", "사용자 경로" to "/Users/kim"),
            findings.map { it.label to it.excerpt },
        )
        assertTrue(findings.all { it.severity == PromotionFindingSeverity.WARN })
    }

    @Test
    fun `findings carry their field, and a block wins over an overlapping warning`() {
        val findings = PromotionSanitizer.check(mapOf("title" to "fine", "solution" to "ask ops@acmepay.co.kr"), terms)
        assertEquals(listOf("solution"), findings.map { it.field }.distinct())
        assertEquals(PromotionFindingSeverity.BLOCK, findings.first().severity)
    }
}
