package com.quno.qunobackend.integration

import com.quno.qunobackend.application.notification.usecase.DispatchOutboxEventsUseCase
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
import kotlin.test.assertTrue

/**
 * Real HTTP walk through the QPR Review flow (PLAN.md Phase 5, ADR-0012/0015): a reviewer
 * opens a review request, the question moves to NEEDS_INFO, the author revises (which alone
 * moves it back to UPDATED — see ADR-0015), and the author re-requests review on that specific
 * thread, notifying the original reviewer. Mirrors QuestionLifecycleE2ETest's approach: real
 * HTTP + JWT via MockMvc, outbox dispatched explicitly for determinism, manual cleanup since
 * each call commits in its own transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionReviewLifecycleE2ETest {

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
            jdbcTemplate.update("DELETE FROM review_requests WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM watches WHERE question_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, accepted_answer_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
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
    fun `review request through revision and re-request notifies the original reviewer`() {
        val (authorId, authorToken) = signUpAndLogin("qpr-author")
        val (reviewerId, reviewerToken) = signUpAndLogin("qpr-reviewer")
        userIds += authorId
        userIds += reviewerId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"QPR question","body":"v1 body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        // the author can't request a review on their own question
        mockMvc.perform(
            post("/api/v1/questions/$questionId/review-requests")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"self"}"""),
        ).andExpect(status().isForbidden)

        val reviewRequestJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/review-requests")
                .header("Authorization", "Bearer $reviewerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"please add logs"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val reviewRequestId = (readMap(reviewRequestJson)["id"] as Number).toLong()

        mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NEEDS_INFO"))

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(authorToken, "REVIEW_REQUESTED")

        // re-requesting before any revision is not allowed yet
        mockMvc.perform(
            post("/api/v1/questions/$questionId/review-requests/$reviewRequestId/re-request")
                .header("Authorization", "Bearer $authorToken"),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            post("/api/v1/questions/$questionId/versions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"QPR question","body":"v2 body with logs"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("UPDATED"))

        mockMvc.perform(
            post("/api/v1/questions/$questionId/review-requests/$reviewRequestId/re-request")
                .header("Authorization", "Bearer $authorToken"),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("ADDRESSED"))

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(reviewerToken, "REVIEW_RE_REQUESTED")

        mockMvc.perform(get("/api/v1/questions/$questionId/review-requests").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("ADDRESSED"))
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
