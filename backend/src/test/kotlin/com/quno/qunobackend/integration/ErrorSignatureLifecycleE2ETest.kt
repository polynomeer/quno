package com.quno.qunobackend.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 64 (ADR-0075) through real HTTP and Postgres: a question's logs are indexed on create, the
 * same error pasted elsewhere — different paths and line numbers — finds it in the ask-form preview,
 * in search and in related questions, and revising the trace away drops the match.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorSignatureLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val questionIds = mutableListOf<Long>()
    private val userIds = mutableListOf<Long>()

    // Unique per run so rows left by other tests in a shared local database never match.
    private val run = System.nanoTime() % 1_000_000_000
    private val exception = "com.e2e.Sig${run}Exception"

    private fun trace(home: String, line: Int) = """
        2026-10-27 10:00:00 ERROR [main] Application run failed
        $exception: could not open /home/$home/app/config-$line.yml
        	at com.e2e.Loader.load(Loader.java:$line)
        	at com.e2e.Boot${'$'}${'$'}SpringCGLIB${'$'}${'$'}0.start(<generated>)
        	at com.e2e.Main.main(Main.java:${line + 3})
    """.trimIndent()

    @AfterEach
    fun cleanUp() {
        questionIds.forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `the same error is found from the ask form, search and related, until it is revised away`() {
        val (authorId, token) = signUpAndLogin("sigauthor")
        userIds += authorId

        val first = createQuestion(token, "Config load fails $run", trace(home = "alice", line = 42))

        // Different home directory and line numbers — same signature.
        val preview = postJson("/api/v1/error-signatures/preview", token, mapOf("text" to trace(home = "bob", line = 7)))

        @Suppress("UNCHECKED_CAST")
        val signatures = preview["signatures"] as List<Map<String, Any?>>
        assertEquals(exception, signatures.single()["exceptionClass"])
        assertNotNull(signatures.single()["signatureHash"])
        assertEquals(listOf(first), questionIdsOf(preview["questions"]))

        val search = getList("/api/v1/search?q={q}", token, trace(home = "carol", line = 99))
        assertEquals(first, (search.first()["id"] as Number).toLong())

        val second = createQuestion(token, "Same failure on CI $run", trace(home = "runner", line = 13))
        val related = getList("/api/v1/questions/$second/related", token)
        assertEquals(first, (related.first()["id"] as Number).toLong())

        mockMvc.perform(
            post("/api/v1/questions/$first/versions")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("title" to "Config load fails $run", "body" to "Turned out to be a typo.", "logs" to null))),
        ).andExpect(status().isOk)

        val afterRevision = postJson("/api/v1/error-signatures/preview", token, mapOf("text" to trace(home = "bob", line = 7)))
        val stillMatching = questionIdsOf(afterRevision["questions"])
        assertFalse(first in stillMatching)
        assertTrue(second in stillMatching)
    }

    @Test
    fun `text without an exception previews nothing`() {
        val (authorId, token) = signUpAndLogin("signone")
        userIds += authorId

        val preview = postJson("/api/v1/error-signatures/preview", token, mapOf("text" to "just a sentence"))

        assertEquals(emptyList<Any?>(), preview["signatures"])
        assertEquals(emptyList<Any?>(), preview["questions"])
    }

    private fun createQuestion(token: String, title: String, logs: String): Long {
        val json = postJson("/api/v1/questions", token, mapOf("title" to title, "body" to "See logs.", "logs" to logs), expectCreated = true)
        return (json["id"] as Number).toLong().also { questionIds += it }
    }

    private fun questionIdsOf(questions: Any?): List<Long> = (questions as List<*>).map { ((it as Map<*, *>)["id"] as Number).toLong() }

    private fun postJson(path: String, token: String, body: Map<String, Any?>, expectCreated: Boolean = false): Map<String, Any?> {
        val result = mockMvc.perform(
            post(path)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        ).andExpect(if (expectCreated) status().isCreated else status().isOk).andReturn()
        return readMap(result.response.contentAsString)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getList(path: String, token: String, vararg uriVars: Any): List<Map<String, Any?>> {
        val json = mockMvc.perform(get(path, *uriVars).header("Authorization", "Bearer $token"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>
    }

    private fun signUpAndLogin(prefix: String): Pair<Long, String> {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val nickname = "$prefix${System.nanoTime() % 1_000_000}"
        val signUp = postJsonAnonymous("/api/v1/auth/signup", mapOf("email" to email, "nickname" to nickname, "password" to "password123"), created = true)
        val login = postJsonAnonymous("/api/v1/auth/login", mapOf("email" to email, "password" to "password123"), created = false)
        return (signUp["id"] as Number).toLong() to (login["accessToken"] as String)
    }

    private fun postJsonAnonymous(path: String, body: Map<String, Any?>, created: Boolean): Map<String, Any?> {
        val json = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
            .andExpect(if (created) status().isCreated else status().isOk).andReturn().response.contentAsString
        return readMap(json)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
}
