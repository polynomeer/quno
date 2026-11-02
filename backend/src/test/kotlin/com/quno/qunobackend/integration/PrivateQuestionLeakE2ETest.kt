package com.quno.qunobackend.integration

import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
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
import kotlin.test.assertTrue

/**
 * Phase 79 (ADR-0085) leak test through real HTTP and Postgres. One private question carries a
 * unique token in its title, body, logs and tag; then every read path the leak inventory found is
 * walked as an outsider, an anonymous reader and a member. Outsiders must get exactly what a
 * missing question looks like (404) on every per-question path, and the token must appear in no
 * list, search, profile, feed or notification. Members must still see everything.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrivateQuestionLeakE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase

    private val userIds = mutableListOf<Long>()
    private val questionIds = mutableListOf<Long>()
    private val organizationIds = mutableListOf<Long>()

    private data class User(val id: Long, val token: String, val nickname: String)

    @AfterEach
    fun cleanUp() {
        // Best effort, newest first: many tables point at questions/answers without cascading.
        fun run(sql: String, vararg args: Any) = runCatching { jdbcTemplate.update(sql, *args) }
        questionIds.reversed().forEach { id ->
            run("DELETE FROM notifications WHERE question_id = ?", id)
            run("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            run("UPDATE questions SET latest_version_id = NULL, accepted_answer_id = NULL, origin_question_id = NULL, cluster_id = NULL WHERE id = ?", id)
            run("DELETE FROM comment_versions WHERE comment_id IN (SELECT id FROM comments WHERE (target_type = 'QUESTION' AND target_id = ?) OR (target_type = 'ANSWER' AND target_id IN (SELECT id FROM answers WHERE question_id = ?)))", id, id)
            run("DELETE FROM comments WHERE (target_type = 'QUESTION' AND target_id = ?) OR (target_type = 'ANSWER' AND target_id IN (SELECT id FROM answers WHERE question_id = ?))", id, id)
            run("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            run("DELETE FROM answers WHERE question_id = ?", id)
            run("DELETE FROM watches WHERE question_id = ?", id)
            run("DELETE FROM saves WHERE question_id = ?", id)
            run("DELETE FROM votes WHERE target_type = 'QUESTION' AND target_id = ?", id)
            run("DELETE FROM question_tags WHERE question_id = ?", id)
            run("DELETE FROM question_view_counts WHERE question_id = ?", id)
            run("DELETE FROM question_error_signatures WHERE question_id = ?", id)
            run("DELETE FROM engine_runs WHERE question_id = ?", id)
            run("DELETE FROM engine_suggestions WHERE question_id = ?", id)
            run("DELETE FROM question_versions WHERE question_id = ?", id)
        }
        questionIds.reversed().forEach { id -> run("DELETE FROM questions WHERE id = ?", id) }
        organizationIds.forEach { id ->
            run("DELETE FROM organization_memberships WHERE organization_id = ?", id)
            run("DELETE FROM organizations WHERE id = ?", id)
        }
        userIds.forEach { id ->
            run("DELETE FROM notifications WHERE user_id = ?", id)
            run("DELETE FROM user_tag_follows WHERE user_id = ?", id)
            run("DELETE FROM users WHERE id = ?", id)
        }
    }

    @Test
    fun `a private question leaks through no path, and members still see all of it`() {
        val token = "zqx${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val author = signUp("pqauthor")
        val member = signUp("pqmember")
        val outsider = signUp("pqoutsider")
        val org = verifiedOrganization(token, author, member)

        val stackTrace = "com.zqx.Billing${token}Exception: boom $token\n\tat com.zqx.Billing.charge(Billing.java:42)\n\tat com.zqx.Api.handle(Api.java:7)"
        val questionId = send(
            HttpMethod.POST,
            "/api/v1/questions",
            author,
            mapOf("title" to "Private $token", "body" to "internal $token details", "logs" to stackTrace, "tags" to listOf(token), "organizationId" to org),
            201,
        ).long("id").also { questionIds += it }
        val publicId = send(HttpMethod.POST, "/api/v1/questions", author, mapOf("title" to "Public control question", "body" to "nothing secret", "tags" to listOf(token)), 201)
            .long("id").also { questionIds += it }
        val answerId = send(HttpMethod.POST, "/api/v1/questions/$questionId/answers", member, mapOf("body" to "answer $token"), 201).long("id")
        val commentId = send(HttpMethod.POST, "/api/v1/questions/$questionId/comments", member, mapOf("body" to "ping @${outsider.nickname} about $token"), 201).long("id")
        val tagId = jdbcTemplate.queryForObject("SELECT id FROM tags WHERE slug = ?", Long::class.java, token.lowercase())
        send(HttpMethod.POST, "/api/v1/tags/$tagId/follow", outsider, null, null)
        dispatchOutboxEventsUseCase.execute(500)

        // Every per-question path: exactly what a missing question looks like, for outsiders and anonymous readers.
        val perQuestion = listOf(
            HttpMethod.GET to "/api/v1/questions/$questionId",
            HttpMethod.GET to "/api/v1/questions/$questionId/versions",
            HttpMethod.GET to "/api/v1/questions/$questionId/versions/1",
            HttpMethod.GET to "/api/v1/questions/$questionId/timeline",
            HttpMethod.GET to "/api/v1/questions/$questionId/related",
            HttpMethod.GET to "/api/v1/questions/$questionId/answers",
            HttpMethod.GET to "/api/v1/questions/$questionId/comments",
            HttpMethod.GET to "/api/v1/questions/$questionId/forks",
            HttpMethod.GET to "/api/v1/questions/$questionId/graph",
            HttpMethod.GET to "/api/v1/questions/$questionId/live-chat",
            HttpMethod.GET to "/api/v1/questions/$questionId/review-requests",
            HttpMethod.GET to "/api/v1/answers/$answerId/versions",
            HttpMethod.GET to "/api/v1/answers/$answerId/comments",
            HttpMethod.GET to "/api/v1/comments/$commentId/versions",
            HttpMethod.POST to "/api/v1/questions/$questionId/views",
        )
        perQuestion.forEach { (method, path) ->
            assertEquals(404, status(method, path, outsider, null), "outsider: $method $path")
            if (path.contains("/forks") || path.contains("/graph") || path.contains("/live-chat") || path.contains("/review-requests")) return@forEach
            assertEquals(404, status(method, path, null, null), "anonymous: $method $path")
        }
        val writes = listOf(
            "/api/v1/questions/$questionId/fork" to null,
            "/api/v1/questions/$questionId/vote" to mapOf("value" to 1),
            "/api/v1/questions/$questionId/save" to null,
            "/api/v1/questions/$questionId/watch" to null,
            "/api/v1/questions/$questionId/outdated" to mapOf("reason" to "x"),
            "/api/v1/questions/$questionId/answers" to mapOf("body" to "x"),
            "/api/v1/questions/$questionId/comments" to mapOf("body" to "x"),
            "/api/v1/answers/$answerId/vote" to mapOf("value" to 1),
            "/api/v1/answers/$answerId/comments" to mapOf("body" to "x"),
        )
        writes.forEach { (path, body) -> assertEquals(404, status(HttpMethod.POST, path, outsider, body), "outsider: POST $path") }

        // No list, search, feed, profile or notification mentions it.
        val lists = listOf(
            HttpMethod.GET to "/api/v1/search?q=$token",
            HttpMethod.GET to "/api/v1/tags/$tagId/questions",
            HttpMethod.GET to "/api/v1/tags/$tagId/questions?sort=TOP",
            HttpMethod.GET to "/api/v1/tags/$tagId/questions?sort=UNANSWERED",
            HttpMethod.GET to "/api/v1/users/${author.id}/profile",
            HttpMethod.GET to "/api/v1/users/${member.id}/profile",
            HttpMethod.GET to "/api/v1/questions/$publicId/related",
            HttpMethod.GET to "/api/v1/flow",
            HttpMethod.GET to "/api/v1/dashboard",
            HttpMethod.GET to "/api/v1/recommendations/questions",
            HttpMethod.GET to "/api/v1/qunobot/version-impacts",
            HttpMethod.GET to "/api/v1/me/notifications",
            HttpMethod.GET to "/api/v1/me/saves",
            HttpMethod.GET to "/api/v1/me/watches",
        )
        lists.forEach { (method, path) ->
            val body = body(method, path, outsider, null)
            assertFalse(body.contains(token.drop(3)) && body.contains("\"id\":$questionId"), "outsider sees it in $path")
            assertFalse(body.contains("Private $token") || body.contains("internal $token") || body.contains("answer $token"), "outsider sees its content in $path")
        }
        listOf("/api/v1/search?q=$token", "/api/v1/tags/$tagId/questions", "/api/v1/users/${member.id}/profile").forEach { path ->
            assertFalse(body(HttpMethod.GET, path, null, null).contains("Private $token"), "anonymous sees it in $path")
        }
        val preview = body(HttpMethod.POST, "/api/v1/error-signatures/preview", outsider, mapOf("text" to stackTrace))
        assertTrue(preview.contains("Billing${token}Exception"), "the trace must yield a signature, or this check proves nothing: $preview")
        assertFalse(preview.contains("\"id\":$questionId"), "outsider finds it by error signature")
        assertTrue(jdbcTemplate.queryForList("SELECT question_id FROM notifications WHERE user_id = ?", Long::class.java, outsider.id).none { it == questionId }, "the @mention reached an outsider")

        // Members see everything; the author sees their own question in lists.
        assertEquals(200, status(HttpMethod.GET, "/api/v1/questions/$questionId", member, null))
        val detail = readMap(body(HttpMethod.GET, "/api/v1/questions/$questionId", member, null))
        assertEquals("ORG", detail["visibility"])
        assertEquals(org, (detail["organizationId"] as Number).toLong())
        assertTrue(body(HttpMethod.GET, "/api/v1/search?q=$token", member, null).contains("Private $token"))
        assertTrue(body(HttpMethod.GET, "/api/v1/users/${member.id}/profile", member, null).contains("answer $token"))
        val memberPreview = body(HttpMethod.POST, "/api/v1/error-signatures/preview", member, mapOf("text" to stackTrace))
        assertTrue(memberPreview.contains("\"id\":$questionId"), memberPreview)

        // A member's fork stays private; private questions only cluster with their own kind.
        val forkId = send(HttpMethod.POST, "/api/v1/questions/$questionId/fork", member, null, 201).long("id").also { questionIds += it }
        assertEquals(404, status(HttpMethod.GET, "/api/v1/questions/$forkId", outsider, null))
        assertEquals(400, status(HttpMethod.POST, "/api/v1/questions/$questionId/cluster", member, mapOf("relatedQuestionId" to publicId)))
        assertEquals(404, status(HttpMethod.POST, "/api/v1/questions/$publicId/cluster", outsider, mapOf("relatedQuestionId" to questionId)))
        assertEquals(400, status(HttpMethod.POST, "/api/v1/questions/$questionId/direct-asks", author, mapOf("targetUserId" to outsider.id)))
    }

    @Test
    fun `only members of a verified organization can ask it privately`() {
        val token = "zqy${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val member = signUp("pqm")
        val outsider = signUp("pqo")
        val verified = verifiedOrganization(token, member)
        val community = (jdbcTemplate.queryForObject("INSERT INTO organizations (name, slug, created_by) VALUES (?, ?, ?) RETURNING id", Long::class.java, "c-$token", "c-$token", member.id) as Long)
            .also { organizationIds += it }
        jdbcTemplate.update("INSERT INTO organization_memberships (organization_id, user_id) VALUES (?, ?)", community, member.id)

        fun ask(user: User, organizationId: Long) = status(HttpMethod.POST, "/api/v1/questions", user, mapOf("title" to "t $token", "body" to "b", "organizationId" to organizationId))
        assertEquals(400, ask(outsider, verified), "not a member")
        assertEquals(400, ask(member, community), "community organizations are open to anyone")
        assertEquals(400, ask(member, 999_999_999L), "no such organization")
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions WHERE title = ?", Int::class.java, "t $token"))
    }

    private fun verifiedOrganization(token: String, vararg members: User): Long {
        val id = jdbcTemplate.queryForObject(
            "INSERT INTO organizations (name, slug, created_by, email_domain) VALUES (?, ?, ?, ?) RETURNING id",
            Long::class.java,
            "$token.example",
            "$token.example",
            members.first().id,
            "$token.example",
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
        return User(id, login["accessToken"] as String, nickname)
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
