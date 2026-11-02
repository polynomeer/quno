package com.quno.qunobackend.domain.question

import com.quno.qunobackend.domain.engine.SecretDetector

/** BLOCK stops the promotion; WARN is shown to the author, who decides (Phase 79.3, ADR-0086). */
enum class PromotionFindingSeverity { BLOCK, WARN }

/** One span in a draft field that may carry internal information. [excerpt] never shows a secret in full. */
data class PromotionFinding(
    val field: String,
    val label: String,
    val start: Int,
    val end: Int,
    val excerpt: String,
    val severity: PromotionFindingSeverity,
)

/**
 * Looks for internal information in a draft of the public card a private question is promoted to
 * (Phase 79.3, ADR-0086). Pure and deterministic, so the preview and the final check agree.
 *
 * - BLOCK: secrets ([SecretDetector]) and the organization's own identifiers (name, slug, email
 *   domain). Either one in a public card is a leak no matter the context.
 * - WARN: private IPs, internal-looking hostnames, email addresses and home-directory paths. They
 *   are often internal but just as often sample values, so the author decides.
 */
object PromotionSanitizer {
    private data class Rule(val label: String, val regex: Regex)

    private val WARN_RULES = listOf(
        Rule("사설 IP 주소", Regex("""(?<![\w.])(?:10\.\d{1,3}|172\.(?:1[6-9]|2\d|3[01])|192\.168)\.\d{1,3}\.\d{1,3}(?![\w\-]|\.\d)""")),
        Rule("내부 호스트 이름", Regex("""(?i)\b[a-z0-9][a-z0-9\-]*(?:\.[a-z0-9\-]+)*\.(?:internal|corp|local|lan|intra|intranet)\b""")),
        Rule("이메일 주소", Regex("""\b[\w.+\-]+@[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)+\b""")),
        Rule("사용자 경로", Regex("""(?:/Users/|/home/|[A-Za-z]:\\Users\\)[^/\\\s]+""")),
    )

    /** [fields] maps a field name to its text; [organizationTerms] come from [organizationTermsOf]. */
    fun check(fields: Map<String, String?>, organizationTerms: Collection<String>): List<PromotionFinding> = fields.flatMap { (field, text) -> checkField(field, text.orEmpty(), organizationTerms) }

    /** Name, slug, email domain and the domain's first label (`acme` of `acme.co.kr`) when it is long enough to mean something. */
    fun organizationTermsOf(name: String, slug: String, emailDomain: String?): List<String> {
        val domainLabel = emailDomain?.substringBefore('.')
        return listOfNotNull(name, slug, emailDomain, domainLabel)
            .map { it.trim() }
            .filter { it.length >= 3 }
            .distinctBy { it.lowercase() }
    }

    private fun checkField(field: String, text: String, organizationTerms: Collection<String>): List<PromotionFinding> {
        if (text.isEmpty()) return emptyList()
        val findings = mutableListOf<PromotionFinding>()
        fun add(label: String, range: IntRange, severity: PromotionFindingSeverity, secret: Boolean = false) {
            if (findings.any { it.start <= range.last && range.first < it.end }) return
            val value = text.substring(range)
            val excerpt = if (secret) value.take(4) + "…" else value.take(80)
            findings += PromotionFinding(field, label, range.first, range.last + 1, excerpt, severity)
        }

        SecretDetector.find(text)
            // A value the author already replaced is not a new finding.
            .filter { text.substring(it.start, it.end) != SecretDetector.REDACTED }
            .forEach { add(it.label, it.start until it.end, PromotionFindingSeverity.BLOCK, secret = true) }
        organizationTerms.forEach { term ->
            Regex("""(?i)(?<![\p{L}\p{N}])${Regex.escape(term)}(?![\p{L}\p{N}])""").findAll(text).forEach {
                add("조직 식별자", it.range, PromotionFindingSeverity.BLOCK)
            }
        }
        WARN_RULES.forEach { rule ->
            rule.regex.findAll(text).forEach { add(rule.label, it.range, PromotionFindingSeverity.WARN) }
        }
        return findings.sortedBy { it.start }
    }
}

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionPromotionRepositoryAdapter. */
interface QuestionPromotionRepository {
    /** The public card a private question was promoted to, or null. Only read from the private side. */
    fun findPublicQuestionId(privateQuestionId: Long): Long?

    fun record(privateQuestionId: Long, publicQuestionId: Long, promotedBy: Long)
}

/** Only a resolved private question, by its author, can be promoted. */
class QuestionPromotionNotAllowedException(message: String) : RuntimeException(message)

class QuestionAlreadyPromotedException(questionId: Long) : RuntimeException("question $questionId has already been promoted to a public card")

/** The draft still carries blocking findings; the labels say what, never the values. */
class PromotionBlockedException(findings: List<PromotionFinding>) : RuntimeException("공개 카드에 남은 사내 정보: " + findings.joinToString(", ") { "${it.field} ${it.label}" })
