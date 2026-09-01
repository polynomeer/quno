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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Real HTTP proof that Phase 29's public read access (ADR-0041) is scoped exactly as decided:
 * question/answer/comment reads and search work with no Authorization header at all, while every
 * write on those same paths — and every other resource (tags, organizations, direct-asks,
 * live-chat, dashboard, profiles) — still 401s without one. Goes through MockMvc + the real
 * JwtAuthenticationFilter/SecurityConfig chain, same reasoning as QuestionLifecycleE2ETest: a
 * unit test with fakes can't catch a SecurityConfig pattern mistake, only the real filter chain can.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PublicReadAccessE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private var questionId: Long? = null
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionId?.let { id ->
            jdbcTemplate.update(
                "DELETE FROM comment_versions WHERE comment_id IN (SELECT id FROM comments WHERE target_id = ? OR target_id IN (SELECT id FROM answers WHERE question_id = ?))",
                id,
                id,
            )
            jdbcTemplate.update("DELETE FROM comments WHERE target_id = ? OR target_id IN (SELECT id FROM answers WHERE question_id = ?)", id, id)
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `an anonymous request can read a question, its answer, and its comments, but cannot write`() {
        val (authorId, authorToken) = signUpAndLogin("public-read-author")
        userIds += authorId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Public read E2E question","body":"v1 body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        val answerJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/answers")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"An answer for the public to read."}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val answerId = (readMap(answerJson)["id"] as Number).toLong()

        mockMvc.perform(
            post("/api/v1/questions/$questionId/comments")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"A comment for the public to read."}"""),
        ).andExpect(status().isCreated)

        // No Authorization header on any of these — an anonymous visitor.
        mockMvc.perform(get("/api/v1/questions/$questionId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Public read E2E question"))
        mockMvc.perform(get("/api/v1/questions/$questionId/versions")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/questions/$questionId/versions/1")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/questions/$questionId/versions/1/diff")).andExpect(status().isNotFound) // no earlier version to diff against — proves the route itself isn't blocked by auth
        mockMvc.perform(get("/api/v1/questions/$questionId/answers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].body").value("An answer for the public to read."))
        mockMvc.perform(get("/api/v1/questions/$questionId/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].body").value("A comment for the public to read."))
        mockMvc.perform(get("/api/v1/answers/$answerId/versions")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/answers/$answerId/comments")).andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/search?q=public+read")).andExpect(status().isOk)

        // Writes on those same paths still require auth.
        mockMvc.perform(
            post("/api/v1/questions/$questionId/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"anonymous answer attempt"}"""),
        ).andExpect(status().isUnauthorized)
        mockMvc.perform(
            post("/api/v1/questions/$questionId/versions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"hijacked","body":"hijacked"}"""),
        ).andExpect(status().isUnauthorized)
        mockMvc.perform(
            post("/api/v1/questions/$questionId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"anonymous comment attempt"}"""),
        ).andExpect(status().isUnauthorized)

        // Resources outside this Phase's scope still require auth even for reads.
        mockMvc.perform(get("/api/v1/tags")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized)
    }

    private fun signUpAndLogin(prefix: String): Pair<Long, String> {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val nickname = "$prefix${System.nanoTime() % 1_000_000}"

        val signUpJson = mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","nickname":"$nickname","password":"password123"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val userId = (readMap(signUpJson)["id"] as Number).toLong()

        val loginJson = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"password123"}"""),
        ).andExpect(status().isOk).andReturn().response.contentAsString

        return userId to (readMap(loginJson)["accessToken"] as String)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
}
