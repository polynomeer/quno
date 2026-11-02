package com.quno.qunobackend.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 79.3 (ADR-0086) "정제 후 공개" through real HTTP and Postgres: only the author of a resolved
 * private question can promote it; the organization's own identifiers and secrets block it; the
 * result is a new, solved public card with no link back, and the private question points to it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionPromotionE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val userIds = mutableListOf<Long>()
    private val questionIds = mutableListOf<Long>()
    private val organizationIds = mutableListOf<Long>()

    private data class User(val id: Long, val token: String)

    @AfterEach
    fun cleanUp() {
        fun run(sql: String, vararg args: Any) = runCatching { jdbcTemplate.update(sql, *args) }
        questionIds.forEach { id -> run("DELETE FROM question_promotions WHERE private_question_id = ? OR public_question_id = ?", id, id) }
        questionIds.reversed().forEach { id ->
            run("DELETE FROM notifications WHERE question_id = ?", id)
            run("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            run("UPDATE questions SET latest_version_id = NULL, accepted_answer_id = NULL WHERE id = ?", id)
            run("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            run("DELETE FROM answers WHERE question_id = ?", id)
            run("DELETE FROM question_tags WHERE question_id = ?", id)
            run("DELETE FROM question_error_signatures WHERE question_id = ?", id)
            run("DELETE FROM engine_runs WHERE question_id = ?", id)
            run("DELETE FROM engine_suggestions WHERE question_id = ?", id)
            run("DELETE FROM question_versions WHERE question_id = ?", id)
            run("DELETE FROM questions WHERE id = ?", id)
        }
        organizationIds.forEach { id ->
            run("DELETE FROM organization_memberships WHERE organization_id = ?", id)
            run("DELETE FROM organizations WHERE id = ?", id)
        }
        userIds.forEach { id ->
            run("DELETE FROM notifications WHERE user_id = ?", id)
            run("DELETE FROM users WHERE id = ?", id)
        }
    }

    @Test
    fun `a resolved private question becomes a sanitized public card, and only that`() {
        val orgName = "zqx${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val author = signUp("prauthor")
        val member = signUp("prmember")
        val outsider = signUp("proutsider")
        val org = verifiedOrganization(orgName, author, member)

        val questionId = send(
            HttpMethod.POST,
            "/api/v1/questions",
            author,
            mapOf("title" to "$orgName billing times out", "body" to "our $orgName.internal gateway at 10.0.3.7 hangs", "organizationId" to org),
            201,
        ).long("id").also { questionIds += it }
        val clean = mapOf(
            "title" to "Gateway call times out after 30s behind a proxy",
            "body" to "A payment gateway call hangs behind an HTTP proxy.",
            "tags" to listOf("http-client"),
            "solution" to "Set the proxy's idle timeout above the client's read timeout.",
        )

        // Not resolved yet; then only the author may promote, and outsiders still see nothing.
        assertEquals(400, status(HttpMethod.POST, "/api/v1/questions/$questionId/promotion", author, clean))
        val answerId = send(HttpMethod.POST, "/api/v1/questions/$questionId/answers", member, mapOf("body" to "raise the idle timeout on $orgName's proxy"), 201).long("id")
        send(HttpMethod.POST, "/api/v1/answers/$answerId/accept", author, null, 200)
        assertEquals(403, status(HttpMethod.POST, "/api/v1/questions/$questionId/promotion", member, clean))
        assertEquals(404, status(HttpMethod.POST, "/api/v1/questions/$questionId/promotion/check", outsider, clean))

        // The private question's own wording is caught: the org name blocks, the private IP warns.
        val leaky = clean + mapOf(
            "body" to "Our ${orgName.uppercase()} gateway at 10.0.3.7 hangs; token ghp_${"a1B2".repeat(9)}",
            "solution" to "ask ops@$orgName.example",
        )

        @Suppress("UNCHECKED_CAST")
        val findings = send(HttpMethod.POST, "/api/v1/questions/$questionId/promotion/check", author, leaky, 200)["findings"] as List<Map<String, Any?>>
        val labels = findings.map { "${it["field"]}:${it["label"]}:${it["severity"]}" }.toSet()
        assertTrue("body:조직 식별자:BLOCK" in labels, labels.toString())
        assertTrue("body:GitHub 토큰:BLOCK" in labels, labels.toString())
        assertTrue("body:사설 IP 주소:WARN" in labels, labels.toString())
        assertTrue(findings.any { it["field"] == "solution" && it["severity"] == "BLOCK" }, labels.toString())
        assertFalse(findings.any { (it["excerpt"] as String).contains("a1B2a1B2") }, "a secret is never echoed in full")
        val blocked = perform(HttpMethod.POST, "/api/v1/questions/$questionId/promotion", author, leaky)
        assertEquals(400, blocked.status)
        assertFalse(blocked.contentAsString.contains("a1B2a1B2"))
        assertNull(send(HttpMethod.GET, "/api/v1/questions/$questionId", author, null, 200)["promotedQuestionId"])

        assertEquals(emptyList<Any>(), send(HttpMethod.POST, "/api/v1/questions/$questionId/promotion/check", author, clean, 200)["findings"])
        val card = send(HttpMethod.POST, "/api/v1/questions/$questionId/promotion", author, clean, 201)
        val cardId = card.long("id").also { questionIds += it }
        assertEquals("RESOLVED", card["status"])

        // A public, solved card anyone can read — with nothing pointing back.
        val publicCard = send(HttpMethod.GET, "/api/v1/questions/$cardId", null, null, 200)
        assertEquals("PUBLIC", publicCard["visibility"])
        assertNull(publicCard["organizationId"])
        assertNull(publicCard["promotedQuestionId"])
        val cardJson = body(HttpMethod.GET, "/api/v1/questions/$cardId", null, null) + body(HttpMethod.GET, "/api/v1/questions/$cardId/answers", null, null)
        assertFalse(cardJson.contains(orgName, ignoreCase = true))
        assertTrue(cardJson.contains("idle timeout above"))
        assertEquals(cardId, send(HttpMethod.GET, "/api/v1/questions/$questionId", author, null, 200).long("promotedQuestionId"))

        // Once only; and the self-accepted write-up earns no "accepted answer" reputation.
        assertEquals(409, status(HttpMethod.POST, "/api/v1/questions/$questionId/promotion", author, clean))
        assertEquals(0L, send(HttpMethod.GET, "/api/v1/users/${author.id}/reputation", author, null, 200).long("acceptedAnswerCount"))
        assertEquals(1L, send(HttpMethod.GET, "/api/v1/users/${member.id}/reputation", member, null, 200).long("acceptedAnswerCount"))
    }

    private fun verifiedOrganization(name: String, vararg members: User): Long {
        val id = jdbcTemplate.queryForObject(
            "INSERT INTO organizations (name, slug, created_by, email_domain) VALUES (?, ?, ?, ?) RETURNING id",
            Long::class.java,
            name,
            name,
            members.first().id,
            "$name.example",
        ) as Long
        organizationIds += id
        members.forEach { jdbcTemplate.update("INSERT INTO organization_memberships (organization_id, user_id) VALUES (?, ?)", id, it.id) }
        return id
    }

    private fun signUp(prefix: String): User {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val nickname = "$prefix${System.nanoTime() % 1_000_000}"
        val id = send(HttpMethod.POST, "/api/v1/auth/signup", null, mapOf("email" to email, "nickname" to nickname, "password" to "password123"), 201).long("id")
        userIds += id
        val login = readMap(body(HttpMethod.POST, "/api/v1/auth/login", null, mapOf("email" to email, "password" to "password123")))
        return User(id, login["accessToken"] as String)
    }

    private fun perform(method: HttpMethod, path: String, user: User?, body: Any?) = mockMvc.perform(
        request(method, path).contentType(MediaType.APPLICATION_JSON).apply {
            body?.let { content(objectMapper.writeValueAsString(it)) }
            user?.let { header("Authorization", "Bearer ${it.token}") }
        },
    ).andReturn().response

    private fun status(method: HttpMethod, path: String, user: User?, body: Any?): Int = perform(method, path, user, body).status

    private fun body(method: HttpMethod, path: String, user: User?, body: Any?): String {
        val response = perform(method, path, user, body)
        assertTrue(response.status in 200..299, "$method $path → ${response.status}: ${response.contentAsString}")
        return response.contentAsString
    }

    private fun send(method: HttpMethod, path: String, user: User?, body: Any?, expected: Int?): Map<String, Any?> {
        val response = perform(method, path, user, body)
        expected?.let { assertEquals(it, response.status, "$method $path: ${response.contentAsString}") }
        return if (response.contentAsString.isBlank()) emptyMap() else readMap(response.contentAsString)
    }

    private fun Map<String, Any?>.long(key: String): Long = (this[key] as Number).toLong()

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
}
