package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.answer.usecase.InMemoryAnswerRepository
import com.quno.qunobackend.application.question.usecase.InMemoryQuestionRepository
import com.quno.qunobackend.application.user.dto.SignUpCommand
import com.quno.qunobackend.domain.answer.Answer
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.user.UserNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExportMyDataUseCaseTest {
    private val userRepository = InMemoryUserRepository()
    private val questionRepository = InMemoryQuestionRepository()
    private val answerRepository = InMemoryAnswerRepository()
    private val signUpUseCase = SignUpUseCase(userRepository, BCryptPasswordEncoder())
    private val useCase = ExportMyDataUseCase(userRepository, questionRepository, answerRepository)

    @Test
    fun `exports the profile and authored content only`() {
        val userId = signUpUseCase.execute(SignUpCommand("a@b.com", "alice", "password123")).userId
        val otherUserId = signUpUseCase.execute(SignUpCommand("c@d.com", "bob", "password123")).userId

        val ownQuestion = questionRepository.save(Question.open(userId, "My question"))
        questionRepository.save(Question.open(otherUserId, "Someone else's question"))
        val ownAnswer = answerRepository.save(Answer.write(requireNotNull(ownQuestion.id), userId, "My answer", 1))

        val result = useCase.execute(userId)

        assertEquals("a@b.com", result.email)
        assertEquals("alice", result.nickname)
        assertEquals(listOf(ownQuestion.id), result.questions.map { it.id })
        assertEquals(listOf(ownAnswer.id), result.answers.map { it.id })
    }

    @Test
    fun `throws when the user does not exist`() {
        assertFailsWith<UserNotFoundException> { useCase.execute(999L) }
    }
}
