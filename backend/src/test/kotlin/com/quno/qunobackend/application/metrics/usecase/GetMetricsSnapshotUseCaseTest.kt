package com.quno.qunobackend.application.metrics.usecase

import com.quno.qunobackend.domain.metrics.MetricsRepository
import com.quno.qunobackend.domain.metrics.MetricsSnapshot
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetMetricsSnapshotUseCaseTest {

    @Test
    fun `delegates to the repository and exposes derived rates`() {
        val snapshot = MetricsSnapshot(
            totalQuestions = 10,
            revisedQuestions = 4,
            answeredQuestions = 6,
            resolvedQuestions = 2,
            watchedQuestions = 5,
            livingQuestions = 8,
        )
        val useCase = GetMetricsSnapshotUseCase(object : MetricsRepository {
            override fun snapshot(): MetricsSnapshot = snapshot
        })

        val result = useCase.execute()

        assertEquals(0.4, result.revisionRate)
        assertEquals(0.6, result.answerRate)
        assertEquals(0.2, result.acceptRate)
        assertEquals(0.5, result.wardCoverageRate)
        assertEquals(0.8, result.livingQuestionRate)
    }

    @Test
    fun `rates are zero when there are no questions`() {
        val useCase = GetMetricsSnapshotUseCase(object : MetricsRepository {
            override fun snapshot(): MetricsSnapshot = MetricsSnapshot(0, 0, 0, 0, 0, 0)
        })

        val result = useCase.execute()

        assertEquals(0.0, result.revisionRate)
        assertEquals(0.0, result.livingQuestionRate)
    }
}
