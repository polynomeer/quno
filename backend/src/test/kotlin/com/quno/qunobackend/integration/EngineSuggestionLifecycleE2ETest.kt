package com.quno.qunobackend.integration

import com.quno.qunobackend.application.engine.QuestionEngine
import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
import com.quno.qunobackend.domain.engine.EngineRunRepository
import com.quno.qunobackend.domain.engine.QuestionContent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 78 (ADR-0081, ADR-0082) through real HTTP and Postgres: secrets are masked on read for
 * everyone but the author (latest, old versions, diff, forks), the author alone sees and decides on
 * QunoBot suggestions, accepting becomes a revision under the author's name, dismissing a redaction
 * lifts the masking for good and dismissing a cleanup kind keeps it from returning, and only the
 * author is notified about secrets.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EngineSuggestionLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase

    @Autowired
    lateinit var engineRunRepository: EngineRunRepository

    @Autowired
    lateinit var questionEngine: QuestionEngine

    private val questionIds = mutableListOf<Long>()
    private val userIds = mutableListOf<Long>()

    // The AWS documentation's example key, split so this file never holds a scannable token.
    private val key = "AKIA" + "IOSFODNN7EXAMPLE"
    private val logs = "java.lang.IllegalStateException: boom\n\tat demo.Main.main(Main.kt:3)\naws.accessKeyId=$key"

    @AfterEach
    fun cleanUp() {
        questionIds.reversed().forEach { id ->
            jdbcTemplate.update("DELETE FROM watches WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, origin_question_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
        }
        questionIds.reversed().forEach { id -> jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `masked for others on every read, accepted into a redacted revision by the author`() {
        val (authorId, author) = signUpAndLogin("engauthor")
        val (readerId, reader) = signUpAndLogin("engreader")
        userIds += listOf(authorId, readerId)
        val questionId = ask(author, "Secret in logs")
        send("/api/v1/questions/$questionId/watch", reader, emptyMap(), status().isNoContent)

        val raw = read("/api/v1/questions/$questionId", author)
        assertEquals(logs, raw["logs"])
        assertEquals(0, raw["maskedSecretCount"])
        for (token in listOf(reader, null)) {
            val masked = read("/api/v1/questions/$questionId", token)
            assertFalse((masked["logs"] as String).contains(key))
            assertTrue((masked["logs"] as String).contains("[가려짐: AWS 액세스 키]"))
            assertEquals(1, masked["maskedSecretCount"])
        }

        val suggestions = readList("/api/v1/questions/$questionId/engine-suggestions", author)
        val suggestion = suggestions.single()
        assertEquals("SECRET_REDACTION", suggestion["kind"])
        assertEquals("OPEN", suggestion["status"])
        assertEquals("secret-scan", suggestion["engine"])
        assertEquals(listOf("logs"), suggestion["changedFields"])
        mockMvc.perform(get("/api/v1/questions/$questionId/engine-suggestions").header("Authorization", "Bearer $reader")).andExpect(status().isForbidden)

        dispatchOutboxEventsUseCase.execute(500)
        assertEquals(1, notificationCount(authorId, questionId))
        assertEquals(0, notificationCount(readerId, questionId), "watchers must not learn where a secret sits")

        val suggestionId = (suggestion["id"] as Number).toLong()
        send("/api/v1/engine-suggestions/$suggestionId/accept", reader, emptyMap(), status().isForbidden)
        assertEquals("ACCEPTED", send("/api/v1/engine-suggestions/$suggestionId/accept", author, emptyMap())["status"])
        send("/api/v1/engine-suggestions/$suggestionId/accept", author, emptyMap(), status().isConflict)

        val revised = read("/api/v1/questions/$questionId", reader)
        assertEquals(2, revised["versionNumber"])
        assertTrue((revised["logs"] as String).endsWith("aws.accessKeyId=[REDACTED]"))
        assertEquals(0, revised["maskedSecretCount"])
        assertEquals(authorId, jdbcTemplate.queryForObject("SELECT created_by FROM question_versions WHERE question_id = ? AND version_number = 2", Long::class.java, questionId))

        // The first version still holds the key, so it stays masked for others.
        assertFalse((read("/api/v1/questions/$questionId/versions/1", reader)["logs"] as String).contains(key))
        assertTrue((read("/api/v1/questions/$questionId/versions/1", author)["logs"] as String).contains(key))
    }

    @Test
    fun `dismissing means not a secret, stale suggestions are refused, forks copy the masked text`() {
        val (authorId, author) = signUpAndLogin("engdismiss")
        val (otherId, other) = signUpAndLogin("engfork")
        userIds += listOf(authorId, otherId)
        val questionId = ask(author, "Dismiss me")
        val first = readList("/api/v1/questions/$questionId/engine-suggestions", author).single()

        // Revising keeps the key: the old suggestion is superseded and a new one appears.
        send("/api/v1/questions/$questionId/versions", author, mapOf("title" to "Dismiss me", "body" to "more detail", "logs" to logs))
        val afterRevision = readList("/api/v1/questions/$questionId/engine-suggestions", author)
        assertEquals(listOf("OPEN", "SUPERSEDED"), afterRevision.map { it["status"] })
        send("/api/v1/engine-suggestions/${(first["id"] as Number).toLong()}/accept", author, emptyMap(), status().isConflict)

        val forkId = (send("/api/v1/questions/$questionId/fork", other, emptyMap(), status().isCreated)["id"] as Number).toLong()
        questionIds += forkId
        assertFalse((read("/api/v1/questions/$forkId", other)["logs"] as String).contains(key), "the forker never saw the key")

        send("/api/v1/engine-suggestions/${(afterRevision.first()["id"] as Number).toLong()}/dismiss", author, emptyMap())
        assertEquals(logs, read("/api/v1/questions/$questionId", other)["logs"])

        send("/api/v1/questions/$questionId/versions", author, mapOf("title" to "Dismiss me", "body" to "even more", "logs" to logs))
        assertEquals(0, readList("/api/v1/questions/$questionId/engine-suggestions", author).count { it["status"] == "OPEN" }, "an allowlisted value is not suggested again")
    }

    @Test
    fun `the sweep marker lists questions the engine has not read and forgets them once read`() {
        val (authorId, author) = signUpAndLogin("engsweep")
        userIds += authorId
        val questionId = ask(author, "Sweep me")
        jdbcTemplate.update("DELETE FROM engine_runs WHERE question_id = ?", questionId)
        val needing = { engineRunRepository.findQuestionIdsNeedingRun(QuestionEngine.SECRET_SCAN, QuestionEngine.SECRET_SCAN_VERSION, 1_000_000) }
        assertTrue(questionId in needing())

        questionEngine.analyze(questionId, authorId, 1, QuestionContent("Sweep me", "b", null, logs))
        questionEngine.analyze(questionId, authorId, 1, QuestionContent("Sweep me", "b", null, logs))

        assertFalse(questionId in needing())
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM engine_runs WHERE question_id = ? AND engine = 'secret-scan'", Int::class.java, questionId))
    }

    @Test
    fun `cleanups are offered one by one, a dismissed kind never returns, and moving a log fills the logs field`() {
        val (authorId, author) = signUpAndLogin("engtidy")
        userIds += authorId
        val fence = "```"
        val body = "It fails:\njava.lang.IllegalStateException: boom\n\tat a.B.c(B.kt:1)\n\tat a.B.d(B.kt:2)\n\tat a.B.e(B.kt:3)\n\nConfig:\n$fence\n{\"port\": 8080}\n$fence"
        val questionId = (send("/api/v1/questions", author, mapOf("title" to "Tidy me", "body" to body), status().isCreated)["id"] as Number).toLong()
        questionIds += questionId

        val offered = readList("/api/v1/questions/$questionId/engine-suggestions", author)
        assertEquals(setOf("FENCE_STACK_TRACE", "ADD_CODE_FENCE_LANGUAGE"), offered.map { it["kind"] }.toSet())
        assertTrue(offered.all { it["engine"] == "format-tidy" && it["changedFields"] == listOf("body") })

        val fenceTrace = offered.single { it["kind"] == "FENCE_STACK_TRACE" }
        val label = offered.single { it["kind"] == "ADD_CODE_FENCE_LANGUAGE" }
        send("/api/v1/engine-suggestions/${(fenceTrace["id"] as Number).toLong()}/dismiss", author, emptyMap())
        send("/api/v1/engine-suggestions/${(label["id"] as Number).toLong()}/accept", author, emptyMap())

        assertTrue((read("/api/v1/questions/$questionId", author)["body"] as String).contains("${fence}json\n{"))
        val open = readList("/api/v1/questions/$questionId/engine-suggestions", author).filter { it["status"] == "OPEN" }
        assertEquals(emptyList(), open, "the trace fence was dismissed and the language is in place")

        val log = (1..35).joinToString("\n") { "2026-11-01 10:00:${"%02d".format(it % 60)} INFO line $it" }
        send("/api/v1/questions/$questionId/versions", author, mapOf("title" to "Tidy me", "body" to "Startup hangs.\n\n$fence\n$log\n$fence"))
        val move = readList("/api/v1/questions/$questionId/engine-suggestions", author).single { it["status"] == "OPEN" }
        assertEquals("MOVE_LONG_LOG", move["kind"])
        send("/api/v1/engine-suggestions/${(move["id"] as Number).toLong()}/accept", author, emptyMap())
        val moved = read("/api/v1/questions/$questionId", author)
        assertEquals("Startup hangs.", moved["body"])
        assertEquals(log, moved["logs"])
    }

    private fun ask(token: String, title: String): Long {
        val id = (send("/api/v1/questions", token, mapOf("title" to title, "body" to "b", "logs" to logs), status().isCreated)["id"] as Number).toLong()
        questionIds += id
        return id
    }

    private fun notificationCount(userId: Long, questionId: Long): Int? = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND question_id = ? AND type = 'ENGINE_SECRET_DETECTED'",
        Int::class.java,
        userId,
        questionId,
    )

    private fun read(path: String, token: String?): Map<String, Any?> {
        val request = get(path)
        token?.let { request.header("Authorization", "Bearer $it") }
        return readMap(mockMvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString)
    }

    private fun readList(path: String, token: String): List<Map<String, Any?>> {
        val json = mockMvc.perform(get(path).header("Authorization", "Bearer $token")).andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>
    }

    private fun send(path: String, token: String?, body: Map<String, Any?>, expected: ResultMatcher = status().isOk): Map<String, Any?> {
        val request = post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))
        token?.let { request.header("Authorization", "Bearer $it") }
        val json = mockMvc.perform(request).andExpect(expected).andReturn().response.contentAsString
        return if (json.isBlank()) emptyMap() else readMap(json)
    }

    private fun signUpAndLogin(prefix: String): Pair<Long, String> {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val nickname = "$prefix${System.nanoTime() % 1_000_000}"
        val signUp = send("/api/v1/auth/signup", null, mapOf("email" to email, "nickname" to nickname, "password" to "password123"), status().isCreated)
        val login = send("/api/v1/auth/login", null, mapOf("email" to email, "password" to "password123"))
        return (signUp["id"] as Number).toLong() to (login["accessToken"] as String)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
}
