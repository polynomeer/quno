package com.quno.qunobackend.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Real HTTP walk through Vote (PLAN.md Phase 11, ADR-0023): casting, changing, and retracting a
 * vote is reflected in the question's own `score` and in `GET /me/votes`, and self-voting is
 * rejected — mirrors the other `*LifecycleE2ETest`s (Outdated/Cluster/Review).
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoteLifecycleE2ETest {

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
            jdbcTemplate.update("DELETE FROM votes WHERE target_id = ?", id)
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        // The outbox dispatch scheduler polls independently of this test's transactions, so a
        // notification for one of these users can still land after the question_id-scoped
        // deletes above ran. Catch it by user_id right before deleting the users themselves.
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `casting, changing, and retracting a vote updates the question's score`() {
        val (authorId, authorToken) = signUpAndLogin("vote-author")
        val (voterId, voterToken) = signUpAndLogin("vote-voter")
        userIds += authorId
        userIds += voterId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Vote target question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        assertScore(questionId, 0, authorToken)

        mockMvc.perform(
            post("/api/v1/questions/$questionId/vote")
                .header("Authorization", "Bearer $voterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":1}"""),
        ).andExpect(status().isNoContent)

        assertScore(questionId, 1, authorToken)

        val votesJson = mockMvc.perform(
            get("/api/v1/me/votes").header("Authorization", "Bearer $voterToken"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        val votes = objectMapper.readValue(votesJson, List::class.java) as List<Map<*, *>>
        assert(votes.any { (it["targetId"] as Number).toLong() == questionId && (it["value"] as Number).toInt() == 1 }) {
            "expected a vote on question $questionId, got: $votes"
        }

        mockMvc.perform(
            post("/api/v1/questions/$questionId/vote")
                .header("Authorization", "Bearer $voterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":-1}"""),
        ).andExpect(status().isNoContent)

        assertScore(questionId, -1, authorToken)

        mockMvc.perform(
            delete("/api/v1/questions/$questionId/vote").header("Authorization", "Bearer $voterToken"),
        ).andExpect(status().isNoContent)

        assertScore(questionId, 0, authorToken)
    }

    private fun assertScore(questionId: Long, expected: Long, bearerToken: String) {
        val json = mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $bearerToken"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val actual = (readMap(json)["score"] as Number).toLong()
        assert(actual == expected) { "expected score $expected for question $questionId, got: $actual" }
    }

    @Test
    fun `voting on your own question is forbidden`() {
        val (authorId, authorToken) = signUpAndLogin("vote-self")
        userIds += authorId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Self vote question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        mockMvc.perform(
            post("/api/v1/questions/$questionId/vote")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":1}"""),
        ).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("FORBIDDEN"))
    }

    @Test
    fun `voting with a value other than 1 or -1 is rejected`() {
        val (authorId, authorToken) = signUpAndLogin("vote-invalid-author")
        val (voterId, voterToken) = signUpAndLogin("vote-invalid-voter")
        userIds += authorId
        userIds += voterId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Invalid value question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        mockMvc.perform(
            post("/api/v1/questions/$questionId/vote")
                .header("Authorization", "Bearer $voterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":5}"""),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
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
