package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.errorsignature.ErrorSignature
import com.quno.qunobackend.domain.errorsignature.QuestionErrorSignatureRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionErrorSignatureJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionErrorSignatureJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QuestionErrorSignatureRepositoryAdapter(
    private val jpaRepository: QuestionErrorSignatureJpaRepository,
) : QuestionErrorSignatureRepository {

    override fun replaceForQuestion(questionId: Long, versionNumber: Int, signatures: List<ErrorSignature>) {
        jpaRepository.deleteAllByQuestionId(questionId)
        val now = Instant.now()
        jpaRepository.saveAll(
            signatures.map {
                QuestionErrorSignatureJpaEntity(
                    questionId = questionId,
                    versionNumber = versionNumber,
                    exceptionClass = it.exceptionClass.take(300),
                    signatureHash = it.signatureHash,
                    messageKey = it.messageKey,
                    createdAt = now,
                )
            },
        )
    }

    override fun findMatchingQuestionIds(
        signatureHashes: Collection<String>,
        messageKeys: Collection<String>,
        excludeQuestionId: Long?,
        limit: Int,
    ): List<Long> {
        if (signatureHashes.isEmpty() && messageKeys.isEmpty()) return emptyList()
        return jpaRepository.findMatchingQuestionIds(
            hashes = signatureHashes.ifEmpty { listOf(NO_MATCH) },
            keys = messageKeys.ifEmpty { listOf(NO_MATCH) },
            excludeQuestionId = excludeQuestionId ?: -1L,
            limit = limit,
        )
    }

    private companion object {
        /** Never a real key (keys are 16 hex chars), so it stands in for an empty IN list. */
        const val NO_MATCH = "-"
    }
}
