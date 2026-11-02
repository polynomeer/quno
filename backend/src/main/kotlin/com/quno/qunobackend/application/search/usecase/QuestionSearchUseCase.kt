package com.quno.qunobackend.application.search.usecase

import com.quno.qunobackend.application.common.QuestionSummaryHydrator
import com.quno.qunobackend.application.search.dto.QuestionSearchResult
import com.quno.qunobackend.domain.errorsignature.ErrorSignature
import com.quno.qunobackend.domain.errorsignature.ErrorSignatureExtractor
import com.quno.qunobackend.domain.errorsignature.QuestionErrorSignatureRepository
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.question.QuestionViewer
import com.quno.qunobackend.domain.search.SearchRepository
import com.quno.qunobackend.domain.search.SearchSort
import org.springframework.stereotype.Service

/**
 * Both operations here rank candidate question ids (lexical match or tag overlap) and then
 * hydrate them into summaries — kept as one class since they share every dependency and the
 * hydration step, and are really two ranking strategies over the same "related questions"
 * concept (see docs/product/mvp-scope.md P1 "유사 질문 추천").
 */
@Service
class QuestionSearchUseCase(
    private val searchRepository: SearchRepository,
    private val questionRepository: QuestionRepository,
    private val hydrator: QuestionSummaryHydrator,
    /** Both null only in unit tests that don't care about signatures; Spring always injects them. */
    private val questionVersionRepository: QuestionVersionRepository? = null,
    private val errorSignatureRepository: QuestionErrorSignatureRepository? = null,
) {
    /** A pasted stack trace finds the same error first, then ordinary full-text matches (ADR-0075). */
    fun search(query: String, limit: Int = 20, sort: SearchSort = SearchSort.RELEVANCE, viewer: QuestionViewer): List<QuestionSearchResult> {
        val bySignature = signatureMatches(ErrorSignatureExtractor.extractAll(query), excludeQuestionId = null, limit, viewer)
        return hydrator.hydrate((bySignature + searchRepository.searchQuestionIds(query, limit, sort, viewer)).distinct().take(limit), viewer)
    }

    /** Questions with the same error come before tag-overlap neighbours (ADR-0075). */
    fun related(questionId: Long, limit: Int = 5, viewer: QuestionViewer): List<QuestionSearchResult> {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        val bySignature = question.latestVersionId
            ?.let { questionVersionRepository?.findById(it) }
            ?.let { signatureMatches(ErrorSignatureExtractor.extractAll(listOfNotNull(it.logs, it.bodyMarkdown).joinToString("\n")), questionId, limit, viewer) }
            .orEmpty()
        return hydrator.hydrate((bySignature + searchRepository.findRelatedQuestionIds(questionId, limit, viewer)).distinct().take(limit), viewer)
    }

    private fun signatureMatches(signatures: List<ErrorSignature>, excludeQuestionId: Long?, limit: Int, viewer: QuestionViewer): List<Long> = errorSignatureRepository?.findQuestionIdsMatching(signatures, excludeQuestionId, limit, viewer).orEmpty()
}
