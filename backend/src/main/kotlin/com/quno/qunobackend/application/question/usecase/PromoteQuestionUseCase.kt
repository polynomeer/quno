package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.application.answer.dto.AcceptAnswerCommand
import com.quno.qunobackend.application.answer.dto.WriteAnswerCommand
import com.quno.qunobackend.application.answer.usecase.AcceptAnswerUseCase
import com.quno.qunobackend.application.answer.usecase.WriteAnswerUseCase
import com.quno.qunobackend.application.question.dto.CreateQuestionCommand
import com.quno.qunobackend.application.question.dto.PromotionDraft
import com.quno.qunobackend.application.question.dto.QuestionMutationResult
import com.quno.qunobackend.domain.organization.OrganizationRepository
import com.quno.qunobackend.domain.question.PromotionBlockedException
import com.quno.qunobackend.domain.question.PromotionFinding
import com.quno.qunobackend.domain.question.PromotionFindingSeverity
import com.quno.qunobackend.domain.question.PromotionSanitizer
import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionAccessDeniedException
import com.quno.qunobackend.domain.question.QuestionAlreadyPromotedException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionPromotionNotAllowedException
import com.quno.qunobackend.domain.question.QuestionPromotionRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVisibility
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * "정제 후 공개" (Phase 79.3, ADR-0086): turns a resolved private question into a *new* public card
 * the author has edited — never by flipping the private question's visibility, because its earlier
 * revisions, answers and comments would all go public with it.
 *
 * The card is the author's question plus the author's own write-up of the solution, accepted at
 * once so it reads as solved. The private question keeps a link to the card; the card has no link
 * back.
 */
@Service
class PromoteQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val organizationRepository: OrganizationRepository,
    private val promotionRepository: QuestionPromotionRepository,
    private val createQuestionUseCase: CreateQuestionUseCase,
    private val writeAnswerUseCase: WriteAnswerUseCase,
    private val acceptAnswerUseCase: AcceptAnswerUseCase,
) {
    /** The same findings [execute] would stop on, for the editor's preview. */
    fun check(questionId: Long, actorId: Long, draft: PromotionDraft): List<PromotionFinding> = findings(promotable(questionId, actorId), draft)

    @Transactional
    fun execute(questionId: Long, actorId: Long, draft: PromotionDraft): QuestionMutationResult {
        val question = promotable(questionId, actorId)
        val blocking = findings(question, draft).filter { it.severity == PromotionFindingSeverity.BLOCK }
        if (blocking.isNotEmpty()) throw PromotionBlockedException(blocking)

        val card = createQuestionUseCase.execute(
            CreateQuestionCommand(
                authorId = actorId,
                title = draft.title,
                body = draft.body,
                environment = draft.environment,
                logs = draft.logs,
                tagNames = draft.tags,
            ),
        )
        val solution = writeAnswerUseCase.execute(WriteAnswerCommand(questionId = card.id, authorId = actorId, body = draft.solution))
        val accepted = acceptAnswerUseCase.execute(AcceptAnswerCommand(answerId = solution.id, actorId = actorId))
        promotionRepository.record(privateQuestionId = questionId, publicQuestionId = card.id, promotedBy = actorId)
        return card.copy(status = accepted.questionStatus)
    }

    private fun promotable(questionId: Long, actorId: Long): Question {
        val question = questionRepository.findById(questionId) ?: throw QuestionNotFoundException(questionId)
        if (question.authorId != actorId) throw QuestionAccessDeniedException(questionId)
        if (question.visibility != QuestionVisibility.ORG) throw QuestionPromotionNotAllowedException("question $questionId is already public")
        if (question.acceptedAnswerId == null) throw QuestionPromotionNotAllowedException("question $questionId is not resolved yet; promote it once an answer is accepted")
        if (promotionRepository.findPublicQuestionId(questionId) != null) throw QuestionAlreadyPromotedException(questionId)
        return question
    }

    private fun findings(question: Question, draft: PromotionDraft): List<PromotionFinding> {
        val organization = organizationRepository.findById(requireNotNull(question.organizationId))
        val terms = organization?.let { PromotionSanitizer.organizationTermsOf(it.name, it.slug, it.emailDomain) }.orEmpty()
        return PromotionSanitizer.check(
            linkedMapOf(
                "title" to draft.title,
                "body" to draft.body,
                "environment" to draft.environment,
                "logs" to draft.logs,
                "tags" to draft.tags.joinToString(" "),
                "solution" to draft.solution,
            ),
            terms,
        )
    }
}
