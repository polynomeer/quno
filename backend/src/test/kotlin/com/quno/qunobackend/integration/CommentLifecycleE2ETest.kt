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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Real HTTP walk through Comment (PLAN.md Phase 12, ADR-0024): commenting on a question and on
 * an answer notifies the right people via the existing outbox fan-out, and deleting a comment
 * tombstones it (body goes null, but it stays in the list) rather than removing it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommentLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var dispatchOutboxEventsUseCase: DispatchOutboxEventsUseCase

    private var questionId: Long? = null
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionId?.let { id ->
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
    fun `commenting on a question notifies the author, and deleting it tombstones the body`() {
        val (authorId, authorToken) = signUpAndLogin("comment-author")
        val (commenterId, commenterToken) = signUpAndLogin("comment-commenter")
        userIds += authorId
        userIds += commenterId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Comment target question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        val commentJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/comments")
                .header("Authorization", "Bearer $commenterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"Can you share the stack trace?"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.body").value("Can you share the stack trace?"))
            .andExpect(jsonPath("$.isDeleted").value(false))
            .andReturn().response.contentAsString
        val commentId = (readMap(commentJson)["id"] as Number).toLong()

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(authorToken, "NEW_COMMENT")

        mockMvc.perform(get("/api/v1/questions/$questionId/comments").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].body").value("Can you share the stack trace?"))

        mockMvc.perform(
            delete("/api/v1/comments/$commentId").header("Authorization", "Bearer $commenterToken"),
        ).andExpect(status().isNoContent)

        val afterDeleteJson = mockMvc.perform(get("/api/v1/questions/$questionId/comments").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        val comments = objectMapper.readValue(afterDeleteJson, List::class.java) as List<Map<*, *>>
        assert(comments.size == 1 && comments[0]["body"] == null && comments[0]["isDeleted"] == true) {
            "expected a single tombstoned comment, got: $comments"
        }
    }

    @Test
    fun `commenting on an answer notifies both the question author and the answer author`() {
        val (authorId, authorToken) = signUpAndLogin("comment-q-author")
        val (answererId, answererToken) = signUpAndLogin("comment-answerer")
        val (commenterId, commenterToken) = signUpAndLogin("comment-on-answer")
        userIds += authorId
        userIds += answererId
        userIds += commenterId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Answer comment question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        val answerJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/answers")
                .header("Authorization", "Bearer $answererToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"try this"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val answerId = (readMap(answerJson)["id"] as Number).toLong()

        mockMvc.perform(
            post("/api/v1/answers/$answerId/comments")
                .header("Authorization", "Bearer $commenterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"did you test this?"}"""),
        ).andExpect(status().isCreated)

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(authorToken, "NEW_COMMENT")
        assertHasNotification(answererToken, "NEW_COMMENT")
    }

    @Test
    fun `only the comment's author can delete it`() {
        val (authorId, authorToken) = signUpAndLogin("comment-forbidden-author")
        val (commenterId, commenterToken) = signUpAndLogin("comment-forbidden-commenter")
        val (strangerId, strangerToken) = signUpAndLogin("comment-forbidden-stranger")
        userIds += authorId
        userIds += commenterId
        userIds += strangerId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Forbidden delete question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        val commentJson = mockMvc.perform(
            post("/api/v1/questions/$questionId/comments")
                .header("Authorization", "Bearer $commenterToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"a comment"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val commentId = (readMap(commentJson)["id"] as Number).toLong()

        mockMvc.perform(
            delete("/api/v1/comments/$commentId").header("Authorization", "Bearer $strangerToken"),
        ).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("FORBIDDEN"))
    }

    @Test
    fun `a comment over 600 characters is rejected`() {
        val (authorId, authorToken) = signUpAndLogin("comment-toolong")
        userIds += authorId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Too long comment question","body":"body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        val tooLong = "a".repeat(601)
        mockMvc.perform(
            post("/api/v1/questions/$questionId/comments")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"$tooLong"}"""),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    private fun assertHasNotification(bearerToken: String, expectedType: String) {
        val notificationsJson = mockMvc.perform(
            get("/api/v1/me/notifications").header("Authorization", "Bearer $bearerToken"),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        @Suppress("UNCHECKED_CAST")
        val notifications = objectMapper.readValue(notificationsJson, List::class.java) as List<Map<*, *>>
        assert(notifications.any { it["type"] == expectedType }) {
            "expected a $expectedType notification, got: $notifications"
        }
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
