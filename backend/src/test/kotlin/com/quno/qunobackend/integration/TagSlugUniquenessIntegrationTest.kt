package com.quno.qunobackend.integration

import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.usecase.CreateQuestionUseCase
import com.quno.qunobackend.application.tag.usecase.SearchTagsUseCase
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.application.user.usecase.SignUpUseCase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Regression test for the bug found in Phase 2.6: "Kotlin" and "kotlin" have different
 * `name` but the same `slug`, and the DB enforces slug uniqueness (uq_tags_slug_active).
 * Only a real Postgres-backed test can catch this — the InMemoryTagRepository fake used in
 * the rest of the suite has no such constraint, so it can't reproduce what actually broke.
 */
@SpringBootTest
@Transactional
class TagSlugUniquenessIntegrationTest {

    @Autowired
    lateinit var signUpUseCase: SignUpUseCase

    @Autowired
    lateinit var createQuestionUseCase: CreateQuestionUseCase

    @Autowired
    lateinit var searchTagsUseCase: SearchTagsUseCase

    @Test
    fun `tag names differing only by case reuse the same row instead of violating slug uniqueness`() {
        val userId = signUpUseCase.execute(
            SignUpCommand(
                email = "tag-slug-${UUID.randomUUID()}@example.com",
                nickname = "tagslug${System.nanoTime() % 1_000_000}",
                rawPassword = "password123",
            ),
        ).userId

        createQuestionUseCase.execute(
            CreateQuestionCommand(userId, "q1", "body", null, null, tagNames = listOf("IntegrationKotlinXyz")),
        )
        createQuestionUseCase.execute(
            CreateQuestionCommand(userId, "q2", "body", null, null, tagNames = listOf("integrationkotlinxyz")),
        )

        val matches = searchTagsUseCase.execute(query = "integrationkotlinxyz", limit = 10)
        assertEquals(1, matches.size, "expected exactly one tag row, got: $matches")
    }
}
