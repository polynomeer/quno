package com.quno.qunobackend.integration

import com.quno.qunobackend.domain.stack.AnswerApplicabilityRepository
import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
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

/**
 * Phase 63.3 (ADR-0077) through real HTTP and Postgres: an answer's applicability ranges are
 * stored with it, listed with it, kept by a revision that omits them, cleared by `[]`, and readable
 * per question for impact detection. Bad ranges are a 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnswerApplicabilityLifecycleE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var answerApplicabilityRepository: AnswerApplicabilityRepository

    private val questionIds = mutableListOf<Long>()
    private val userIds = mutableListOf<Long>()

    @AfterEach
    fun cleanUp() {
        questionIds.forEach { id ->
            jdbcTemplate.update("DELETE FROM notifications WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL, accepted_answer_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("UPDATE answers SET latest_version_id = NULL WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            jdbcTemplate.update("DELETE FROM answers WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", id) }
        userIds.forEach { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `ranges follow the answer through write, list and revise`() {
        val (askerId, askerToken) = signUpAndLogin("rangeask")
        val (answererId, token) = signUpAndLogin("rangeans")
        userIds += listOf(askerId, answererId)
        val questionId = (send("/api/v1/questions", askerToken, mapOf("title" to "Pool exhausted", "body" to "b"), status().isCreated)["id"] as Number).toLong()
        questionIds += questionId

        val boot = mapOf("ecosystem" to "GRADLE_PLUGIN", "name" to "org.springframework.boot", "fromVersion" to "3.2.0", "untilVersion" to "4.0.0")
        val written = send("/api/v1/questions/$questionId/answers", token, mapOf("body" to "Raise the pool size", "applicability" to listOf(boot)), status().isCreated)
        val answerId = (written["id"] as Number).toLong()
        assertEquals(listOf(boot), written["applicability"])
        assertEquals(listOf(boot), applicabilityOf(questionId))

        send("/api/v1/answers/$answerId/versions", token, mapOf("body" to "Raise the pool size, then restart"))
        assertEquals(listOf(boot), applicabilityOf(questionId))
        assertEquals(
            mapOf(questionId to listOf(ApplicabilityRange(Ecosystem.GRADLE_PLUGIN, "org.springframework.boot", "3.2.0", "4.0.0"))),
            answerApplicabilityRepository.findByQuestionIds(listOf(questionId)),
        )

        send("/api/v1/answers/$answerId/versions", token, mapOf("body" to "v3", "applicability" to emptyList<Any>()))
        assertEquals(emptyList<Any>(), applicabilityOf(questionId))
    }

    @Test
    fun `bad ranges are a 400, not a 500`() {
        val (userId, token) = signUpAndLogin("rangebad")
        userIds += userId
        val questionId = (send("/api/v1/questions", token, mapOf("title" to "t", "body" to "b"), status().isCreated)["id"] as Number).toLong()
        questionIds += questionId
        fun answerWith(vararg range: Map<String, Any?>) = send("/api/v1/questions/$questionId/answers", token, mapOf("body" to "b", "applicability" to range.toList()), status().isBadRequest)

        answerWith(mapOf("ecosystem" to "NPM", "name" to "react"))
        answerWith(mapOf("ecosystem" to "NPM", "name" to "react", "fromVersion" to "^18"))
        answerWith(mapOf("ecosystem" to "NPM", "name" to "react", "fromVersion" to "19.0", "untilVersion" to "18.0"))
        answerWith(mapOf("ecosystem" to "NPM", "name" to "react", "fromVersion" to "18.0"), mapOf("ecosystem" to "NPM", "name" to "react", "fromVersion" to "17.0"))
        answerWith(mapOf("ecosystem" to "PYPI", "name" to "django", "fromVersion" to "5.0"))
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM answers WHERE question_id = ?", Int::class.java, questionId))
    }

    private fun applicabilityOf(questionId: Long): List<*> {
        val json = mockMvc.perform(get("/api/v1/questions/$questionId/answers")).andExpect(status().isOk).andReturn().response.contentAsString

        @Suppress("UNCHECKED_CAST")
        val answers = objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>
        return answers.single()["applicability"] as List<*>
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
