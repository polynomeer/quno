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
 * Real HTTP walk through Outdated marking (PLAN.md 8.1, ADR-0017): anyone — including a
 * non-author watcher — can flag a question outdated, which notifies the author and any other
 * watchers, and a later revision naturally brings the question out of OUTDATED again.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QuestionOutdatedLifecycleE2ETest {

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
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_tags WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `marking a question outdated notifies the author, and a later revision brings it back`() {
        val (authorId, authorToken) = signUpAndLogin("outdated-author")
        val (watcherId, watcherToken) = signUpAndLogin("outdated-watcher")
        userIds += authorId
        userIds += watcherId

        val createJson = mockMvc.perform(
            post("/api/v1/questions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Outdated question","body":"v1 body"}"""),
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val questionId = (readMap(createJson)["id"] as Number).toLong()
        this.questionId = questionId

        mockMvc.perform(
            post("/api/v1/questions/$questionId/watch").header("Authorization", "Bearer $watcherToken"),
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/v1/questions/$questionId/outdated")
                .header("Authorization", "Bearer $watcherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"Spring Boot 4 removed this API"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("OUTDATED"))

        dispatchOutboxEventsUseCase.execute()
        assertHasNotification(authorToken, "QUESTION_OUTDATED")

        mockMvc.perform(
            post("/api/v1/questions/$questionId/versions")
                .header("Authorization", "Bearer $authorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Outdated question","body":"v2 body using the new API"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("UPDATED"))

        mockMvc.perform(get("/api/v1/questions/$questionId").header("Authorization", "Bearer $authorToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UPDATED"))
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
