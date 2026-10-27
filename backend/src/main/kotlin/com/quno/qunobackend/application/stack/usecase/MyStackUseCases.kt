package com.quno.qunobackend.application.stack.usecase

import com.quno.qunobackend.application.question.usecase.checkedDependencies
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionStatus
import com.quno.qunobackend.domain.stack.QuestionDependencyRepository
import com.quno.qunobackend.domain.stack.StackDependency
import com.quno.qunobackend.domain.stack.StackMatchRepository
import com.quno.qunobackend.domain.stack.UserStackRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** "내 스택" (Phase 63.5, ADR-0078): private to its owner, replaced as a whole. */
@Service
class MyStackUseCase(
    private val userStackRepository: UserStackRepository,
) {
    fun get(userId: Long): List<StackDependency> = userStackRepository.findByUserId(userId)

    @Transactional
    fun replace(userId: Long, dependencies: List<StackDependency>): List<StackDependency> {
        userStackRepository.replaceForUser(userId, checkedDependencies(dependencies))
        return userStackRepository.findByUserId(userId)
    }
}

data class StackWardSuggestionResult(
    val questionId: Long,
    val title: String,
    val status: QuestionStatus,
    val createdAt: Instant,
    /** The question's entries whose package is also in the user's stack — versions are the question's. */
    val sharedDependencies: List<StackDependency>,
)

/**
 * Questions worth a Ward because they share packages with the user's stack (ADR-0078). Only a
 * suggestion: watching stays the user's click on the existing watch endpoint, and nothing is
 * subscribed automatically as new questions arrive.
 */
@Service
class SuggestStackWardsUseCase(
    private val userStackRepository: UserStackRepository,
    private val stackMatchRepository: StackMatchRepository,
    private val questionRepository: QuestionRepository,
    private val questionDependencyRepository: QuestionDependencyRepository,
) {
    fun execute(userId: Long, limit: Int): List<StackWardSuggestionResult> {
        val myPackages = userStackRepository.findByUserId(userId).map { it.ecosystem to it.name }.toSet()
        if (myPackages.isEmpty()) return emptyList()

        val matches = stackMatchRepository.findQuestionsSharingStack(userId, limit.coerceIn(1, MAX_LIMIT))
        val ids = matches.map { it.questionId }
        val questions = questionRepository.findAllByIds(ids).associateBy { it.id }
        val dependencies = questionDependencyRepository.findByQuestionIds(ids)
        return matches.mapNotNull { match ->
            val question = questions[match.questionId] ?: return@mapNotNull null
            StackWardSuggestionResult(
                questionId = match.questionId,
                title = question.title,
                status = question.status,
                createdAt = question.createdAt,
                sharedDependencies = dependencies[match.questionId].orEmpty().filter { (it.ecosystem to it.name) in myPackages },
            )
        }
    }

    companion object {
        const val MAX_LIMIT = 50
    }
}
