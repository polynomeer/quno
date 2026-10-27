package com.quno.qunobackend.domain.errorsignature

/** Port implemented by infrastructure/persistence/jpa/adapter/QuestionErrorSignatureRepositoryAdapter. */
interface QuestionErrorSignatureRepository {
    /** Drops whatever was indexed for the question and stores [signatures] for [versionNumber]. */
    fun replaceForQuestion(questionId: Long, versionNumber: Int, signatures: List<ErrorSignature>)

    /**
     * Questions sharing any of the given keys, strong matches ([signatureHashes]) before
     * message-only ones, most recently indexed first within each group.
     */
    fun findMatchingQuestionIds(
        signatureHashes: Collection<String>,
        messageKeys: Collection<String>,
        excludeQuestionId: Long?,
        limit: Int,
    ): List<Long>

    /** [findMatchingQuestionIds] keyed by whole signatures — what every caller actually has. */
    fun findQuestionIdsMatching(signatures: List<ErrorSignature>, excludeQuestionId: Long?, limit: Int): List<Long> = if (signatures.isEmpty()) {
        emptyList()
    } else {
        findMatchingQuestionIds(
            signatureHashes = signatures.mapNotNull { it.signatureHash }.distinct(),
            messageKeys = signatures.map { it.messageKey }.distinct(),
            excludeQuestionId = excludeQuestionId,
            limit = limit,
        )
    }
}
