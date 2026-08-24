package com.quno.qunobackend.integration

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.ReviseQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.question.usecase.ReviseQuestionUseCase
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real-DB test for the pessimistic lock in QuestionRepository#findByIdForUpdate — an
 * in-memory fake can't exercise actual row locking, only a real Postgres transaction can.
 * See docs/architecture/domain-model.md "Revision 생성의 동시성 주의".
 *
 * Not wrapped in @Transactional: each ReviseQuestionUseCase call must run in its own top-level
 * transaction/connection for the lock to mean anything, so cleanup is manual (see [cleanUp]).
 */
@SpringBootTest
class ReviseQuestionConcurrencyIntegrationTest {

    @Autowired
    lateinit var signUpUseCase: SignUpUseCase

    @Autowired
    lateinit var createQuestionUseCase: CreateQuestionUseCase

    @Autowired
    lateinit var reviseQuestionUseCase: ReviseQuestionUseCase

    @Autowired
    lateinit var questionVersionRepository: QuestionVersionRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private var createdQuestionId: Long? = null
    private var createdUserId: Long? = null

    @AfterEach
    fun cleanUp() {
        createdQuestionId?.let { id ->
            jdbcTemplate.update("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            // questions.latest_version_id references question_versions.id, so null it out first.
            jdbcTemplate.update("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM question_versions WHERE question_id = ?", id)
            jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id)
        }
        createdUserId?.let { id -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `concurrent revisions never produce duplicate or skipped version numbers`() {
        val userId = signUpUseCase.execute(
            SignUpCommand(
                email = "concurrency-${UUID.randomUUID()}@example.com",
                nickname = "conc${System.nanoTime() % 1_000_000}",
                rawPassword = "password123",
            ),
        ).userId
        createdUserId = userId

        val questionId = createQuestionUseCase.execute(
            CreateQuestionCommand(userId, "concurrency test", "v1", null, null),
        ).id
        createdQuestionId = questionId

        val threadCount = 8
        val pool = Executors.newFixedThreadPool(threadCount)
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()

        val futures = (1..threadCount).map { i ->
            pool.submit {
                start.await()
                try {
                    reviseQuestionUseCase.execute(ReviseQuestionCommand(questionId, userId, "t$i", "b$i", null, null))
                } catch (ex: Throwable) {
                    failures += ex
                }
            }
        }
        start.countDown()
        futures.forEach { it.get(15, TimeUnit.SECONDS) }
        pool.shutdown()

        assertTrue(failures.isEmpty(), "revision(s) failed: ${failures.toList()}")
        val versionNumbers = questionVersionRepository.findAllByQuestionIdOrderByVersionNumberAsc(questionId)
            .map { it.versionNumber }
        assertEquals(versionNumbers.distinct().size, versionNumbers.size, "duplicate version numbers: $versionNumbers")
        assertEquals((1..threadCount + 1).toList(), versionNumbers.sorted())
    }
}
