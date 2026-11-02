package com.quno.qunobackend.integration

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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 93 (ADR-0083) through real HTTP and Postgres: a snapshot sent with a question is scrubbed
 * again and fingerprinted by the server, kept by a revision that omits it, replaced by one that
 * sends it, not copied into forks, and refused when its schema is unknown, it is too large or it carries Tier 2 without opting in.
 * A failure snapshot's symptom lands in its own column (Phase 93.3, ADR-0084).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnvironmentSnapshotLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val questionIds = mutableListOf<Long>()
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionIds.reversed().forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, origin_question_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
        }
        questionIds.reversed().forEach { id -> jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    private fun snapshot(jdk: String, jvmArgs: List<String> = emptyList()) = mapOf(
        "schemaVersion" to 1,
        "source" to "COLLECTOR",
        "collector" to mapOf("name" to "quno-cli", "version" to "0.2.0"),
        "capturedAt" to "2026-11-01T10:00:00Z",
        "fingerprint" to "client-says-so",
        "tiers" to listOf(0),
        "tier0" to mapOf(
            "os" to mapOf("name" to "macOS", "version" to "15.1", "arch" to "arm64"),
            "runtimes" to listOf(mapOf("name" to "java", "vendor" to "Eclipse Adoptium", "version" to jdk)),
            "container" to mapOf("docker" to false, "wsl" to false, "rosetta" to false),
            "locale" to mapOf("lang" to "ko_KR", "encoding" to "UTF-8", "timezone" to "Asia/Seoul"),
            "jvmArgs" to jvmArgs,
            "unknownFutureField" to mapOf("kept" to true),
        ),
    )

    @Test
    fun `the server re-scrubs, fingerprints and keeps the latest snapshot`() {
        val (userId, token) = signUpAndLogin("snap")
        userIds += userId
        val leaked = "-Ddb.url=jdbc:postgresql://quno:S3cr3t-Pw@db.internal/quno"
        val questionId = ask(token, "With snapshot", snapshot("21.0.5", listOf("-Xmx2g", leaked)))

        val stored = snapshotOf(questionId, token)!!
        assertEquals("COLLECTOR", stored["source"])
        assertEquals("quno-cli 0.2.0", stored["collector"])
        assertEquals(1, stored["serverRedactions"])
        assertNull(stored["symptom"], "Tier 0 only")
        val fingerprint = stored["fingerprint"] as String
        assertEquals(16, fingerprint.length)
        @Suppress("UNCHECKED_CAST")
        val document = stored["document"] as Map<String, Any?>
        assertEquals(fingerprint, document["fingerprint"], "the client's fingerprint is replaced")
        val serialized = objectMapper.writeValueAsString(document)
        assertFalse(serialized.contains("S3cr3t-Pw"))
        assertTrue(serialized.contains("unknownFutureField"), "unknown fields are kept")

        // Same major JDK on another question: same fingerprint. A revision without a snapshot keeps it.
        val other = ask(token, "Same machine, newer patch", snapshot("21.0.9"))
        assertEquals(fingerprint, snapshotOf(other, token)!!["fingerprint"])
        send("/api/v1/questions/$questionId/versions", token, mapOf("title" to "With snapshot", "body" to "b2"))
        assertEquals(fingerprint, snapshotOf(questionId, token)!!["fingerprint"])

        send("/api/v1/questions/$questionId/versions", token, mapOf("title" to "With snapshot", "body" to "b3", "environmentSnapshot" to snapshot("17.0.12")))
        val replaced = snapshotOf(questionId, token)!!
        assertNotEquals(fingerprint, replaced["fingerprint"])
        assertEquals(3, replaced["versionNumber"])

        val forkId = (send("/api/v1/questions/$questionId/fork", token, emptyMap(), status().isCreated)["id"] as Number).toLong()
        questionIds += forkId
        assertNull(snapshotOf(forkId, token), "a fork is someone else's machine")
    }

    @Test
    fun `a failure snapshot stores its symptom in its own column`() {
        val (userId, token) = signUpAndLogin("snapfail")
        userIds += userId
        val failure = snapshot("21.0.5").plus("tiers" to listOf(0, 1)).plus(
            "tier1" to mapOf(
                "command" to mapOf("argv" to listOf("./gradlew", "test"), "exitCode" to 1, "durationMs" to 4200),
                "symptom" to "OUT_OF_MEMORY",
            ),
        )
        val questionId = ask(token, "OOM in tests", failure)

        assertEquals("OUT_OF_MEMORY", snapshotOf(questionId, token)!!["symptom"])
        assertEquals("OUT_OF_MEMORY", jdbcTemplate.queryForObject("SELECT symptom FROM question_environment_snapshots WHERE question_id = ?", String::class.java, questionId))
    }

    @Test
    fun `unknown schemas, oversized documents and non-objects are a 400`() {
        val (userId, token) = signUpAndLogin("snapbad")
        userIds += userId
        fun post(body: Any?) = send("/api/v1/questions", token, mapOf("title" to "t", "body" to "b", "environmentSnapshot" to body), status().isBadRequest)

        post(snapshot("21").plus("schemaVersion" to 2))
        post(snapshot("21").plus("padding" to "x".repeat(70 * 1024)))
        post(listOf(1, 2))
        post(snapshot("21").plus("source" to "ROBOT"))
        post(snapshot("21").plus("tier2" to mapOf("cpus" to 8)))
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM questions WHERE author_id = ?", Int::class.java, userId))
    }

    private fun ask(token: String, title: String, snapshot: Map<String, Any?>): Long {
        val id = (send("/api/v1/questions", token, mapOf("title" to title, "body" to "b", "environmentSnapshot" to snapshot), status().isCreated)["id"] as Number).toLong()
        questionIds += id
        return id
    }

    @Suppress("UNCHECKED_CAST")
    private fun snapshotOf(questionId: Long, token: String): Map<String, Any?>? {
        val json = mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $token")).andExpect(status().isOk).andReturn().response.contentAsString
        return readMap(json)["environmentSnapshot"] as Map<String, Any?>?
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
