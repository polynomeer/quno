package com.quno.qunobackend.domain.reputation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UserReputationTest {

    @Test
    fun `score weighs accepted answers and super answers most heavily`() {
        val reputation = UserReputation(
            userId = 1L,
            questionCount = 2,
            answerCount = 3,
            acceptedAnswerCount = 1,
            superAnswerCount = 1,
        )

        // 2*1 + 3*2 + 1*15 + 1*10 = 2 + 6 + 15 + 10 = 33
        assertEquals(33, reputation.score)
    }

    @Test
    fun `score is zero for a user with no activity`() {
        val reputation = UserReputation(userId = 1L, questionCount = 0, answerCount = 0, acceptedAnswerCount = 0, superAnswerCount = 0)

        assertEquals(0, reputation.score)
    }
}
