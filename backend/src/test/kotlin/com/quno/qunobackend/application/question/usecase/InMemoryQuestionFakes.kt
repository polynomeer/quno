package com.quno.qunobackend.application.question.usecase

import com.quno.qunobackend.domain.question.Question
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.question.QuestionVersion
import com.quno.qunobackend.domain.question.QuestionVersionRepository
import com.quno.qunobackend.domain.question.QuestionViewCountRepository
import com.quno.qunobackend.domain.question.QuestionViewDeduplicator

class InMemoryQuestionRepository : QuestionRepository {
    private val byId = mutableMapOf<Long, Question>()
    private var nextId = 1L

    override fun save(question: Question): Question {
        val saved = if (question.id == null) {
            Question.reconstitute(
                id = nextId++,
                authorId = question.authorId,
                title = question.title,
                status = question.status,
                latestVersionId = question.latestVersionId,
                acceptedAnswerId = question.acceptedAnswerId,
                clusterId = question.clusterId,
                originQuestionId = question.originQuestionId,
                deletedAt = question.deletedAt,
                createdAt = question.createdAt,
                updatedAt = question.updatedAt,
            )
        } else {
            question
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): Question? = byId[id]

    override fun findAllByIds(ids: List<Long>): List<Question> = ids.mapNotNull { byId[it] }

    // No real locking needed for a single-threaded in-memory fake.
    override fun findByIdForUpdate(id: Long): Question? = findById(id)

    override fun findAllByAuthorId(authorId: Long): List<Question> = byId.values.filter { it.authorId == authorId }.sortedByDescending { it.createdAt }

    override fun findAllByClusterId(clusterId: Long): List<Question> = byId.values.filter { it.clusterId == clusterId }

    override fun findAllByOriginQuestionId(originQuestionId: Long): List<Question> = byId.values.filter { it.originQuestionId == originQuestionId }
}

class InMemoryQuestionVersionRepository : QuestionVersionRepository {
    private val byId = mutableMapOf<Long, QuestionVersion>()
    private var nextId = 1L

    override fun save(version: QuestionVersion): QuestionVersion {
        val saved = if (version.id == null) {
            QuestionVersion.reconstitute(
                id = nextId++,
                questionId = version.questionId,
                versionNumber = version.versionNumber,
                title = version.title,
                bodyMarkdown = version.bodyMarkdown,
                environment = version.environment,
                logs = version.logs,
                createdBy = version.createdBy,
                createdAt = version.createdAt,
            )
        } else {
            version
        }
        byId[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: Long): QuestionVersion? = byId[id]

    override fun findLatestVersionNumbersByQuestionIds(questionIds: List<Long>): Map<Long, Int> = byId.values
        .filter { it.questionId in questionIds }
        .groupBy { it.questionId }
        .mapValues { (_, versions) -> versions.maxOf { it.versionNumber } }

    override fun findByQuestionIdAndVersionNumber(questionId: Long, versionNumber: Int): QuestionVersion? = byId.values.find { it.questionId == questionId && it.versionNumber == versionNumber }

    override fun findAllByQuestionIdOrderByVersionNumberAsc(questionId: Long): List<QuestionVersion> = byId.values.filter { it.questionId == questionId }.sortedBy { it.versionNumber }
}

class InMemoryQuestionViewCountRepository : QuestionViewCountRepository {
    private val counts = mutableMapOf<Long, Long>()

    override fun increment(questionId: Long) {
        counts[questionId] = (counts[questionId] ?: 0L) + 1
    }

    override fun countByQuestionId(questionId: Long): Long = counts[questionId] ?: 0L

    override fun countsByQuestionIds(questionIds: List<Long>): Map<Long, Long> = counts.filterKeys { it in questionIds }
}

/** Same contract as the Redis one minus expiry — a (question, viewer) pair counts once. */
class InMemoryQuestionViewDeduplicator : QuestionViewDeduplicator {
    private val seen = mutableSetOf<Pair<Long, String>>()

    override fun isFirstView(questionId: Long, viewerKey: String): Boolean = seen.add(questionId to viewerKey)
}
