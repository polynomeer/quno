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
 * Real HTTP walk through Cluster + Super Answer (PLAN.md Phase 6, ADR-0016): two independently
 * authored questions are marked as the same problem, forming a Cluster, and the accepted answer
 * on one of them is designated the cluster's Super Answer. Mirrors the other lifecycle E2E
 * tests: MockMvc + real JWT, manual cleanup since each call commits in its own transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionClusterLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val questionIds = mutableListOf<Long>()
    private var clusterId: Long? = null
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionIds.forEach { id -> jdbcTemplate.update("UPDATE questions SET accepted_answer_id = NULL WHERE id = ?", id) }
        clusterId?.let { jdbcTemplate.update("UPDATE question_clusters SET representative_answer_id = NULL WHERE id = ?", it) }
        questionIds.forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, cluster_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        clusterId?.let { jdbcTemplate.update("DELETE FROM question_clusters WHERE id = ?", it) }
        // The outbox dispatch scheduler polls independently of this test's transactions, so a
        // notification for one of these users can still land after the question_id-scoped
        // deletes above ran. Catch it by user_id right before deleting the users themselves.
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `marking two questions as the same problem and designating a Super Answer`() {
        val (authorAId, authorAToken) = signUpAndLogin("cluster-a")
        val (authorBId, authorBToken) = signUpAndLogin("cluster-b")
        userIds += authorAId
        userIds += authorBId

        val q1 = createQuestion(authorAToken, "Redis timeout on connect")
        val q2 = createQuestion(authorBToken, "Redis connection times out")
        questionIds += q1
        questionIds += q2

        mockMvc.perform(
            post("/api/v1/questions/$q1/cluster")
                .header("Authorization", "Bearer $authorAToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"relatedQuestionId":$q1}"""),
        ).andExpect(status().isBadRequest)

        val markJson = mockMvc.perform(
            post("/api/v1/questions/$q1/cluster")
                .header("Authorization", "Bearer $authorAToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"relatedQuestionId":$q2}"""),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val clusterId = (readMap(markJson)["clusterId"] as Number).toLong()
        this.clusterId = clusterId

        mockMvc.perform(get("/api/v1/questions/$q2/cluster").header("Authorization", "Bearer $authorBToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.clusterId").value(clusterId))
            .andExpect(jsonPath("$.members.length()").value(2))

        val answerJson = mockMvc.perform(
            post("/api/v1/questions/$q1/answers")
                .header("Authorization", "Bearer $authorBToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"increase the connection timeout"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val answerId = (readMap(answerJson)["id"] as Number).toLong()

        mockMvc.perform(
            post("/api/v1/clusters/$clusterId/super-answer")
                .header("Authorization", "Bearer $authorAToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"answerId":$answerId}"""),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/answers/$answerId/accept").header("Authorization", "Bearer $authorAToken"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/clusters/$clusterId/super-answer")
                .header("Authorization", "Bearer $authorAToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"answerId":$answerId}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.representativeAnswerId").value(answerId))

        mockMvc.perform(get("/api/v1/clusters/$clusterId").header("Authorization", "Bearer $authorBToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.representativeAnswerId").value(answerId))
    }

    private fun createQuestion(bearerToken: String, title: String): Long {
        val json = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $bearerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"$title","body":"v1 body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        return (readMap(json)["id"] as Number).toLong()
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
