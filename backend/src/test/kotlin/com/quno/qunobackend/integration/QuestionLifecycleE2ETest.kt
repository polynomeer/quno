package com.quno.qunobackend.integration

import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertTrue

/**
 * Real HTTP walk through the MVP core hypothesis (docs/product/vision.md): a question is not
 * a dead post — revising it and answering it produces Ward notifications for people who
 * watched it. Unlike the other integration tests (which call use cases directly), this one
 * goes through MockMvc so the controllers, request/response DTOs, and the real JWT security
 * filter chain are exercised too — see PLAN.md Phase 4.2.
 *
 * Not @Transactional: each HTTP call commits in its own transaction, and outbox dispatch is
 * triggered explicitly (rather than waiting on the live @Scheduled poller) to keep the test
 * fast and deterministic. Cleanup is manual for the same reason as the other tests here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private var questionId: Long? = null
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionId?.let { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("DELETE FROM watches WHERE question_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, accepted_answer_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `question creation through revision, answer, accept, and ward notification`() {
        val (authorId, authorToken) = signUpAndLogin("author")
        val (watcherId, watcherToken) = signUpAndLogin("watcher")
        userIds += authorId
        userIds += watcherId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"E2E question","body":"v1 body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        mockMvc.perform(
            post("/api/v1/questions/$questionId/watch").header("Authorization", "Bearer $watcherToken"),
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/questions/$questionId/versions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"E2E question","body":"v2 body with more detail"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("UPDATED"))

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(watcherToken, "QUESTION_REVISION")

        val answerJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/answers")
                .header("Authorization", "Bearer $watcherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"Here is my answer."}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val answerId = (readMap(answerJson)["id"] as Number).toLong()

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(authorToken, "NEW_ANSWER")

        mockMvc.perform(
            post("/api/v1/answers/$answerId/accept").header("Authorization", "Bearer $authorToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.questionStatus").value("RESOLVED"))

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(watcherToken, "ANSWER_ACCEPTED")

        mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.versionNumber").value(2))
    }

    private fun assertHasNotification(bearerToken: String, expectedType: String) {
        val notificationsJson = mockMvc.perform(
            get("/api/v1/me/notifications").header("Authorization", "Bearer $bearerToken"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        val notifications = objectMapper.readValue(notificationsJson, List::class.java) as List<Map<*, *>>
        assertTrue(
            notifications.any { it["type"] == expectedType },
            "expected a $expectedType notification, got: $notifications",
        )
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
