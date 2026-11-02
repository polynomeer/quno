package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshot
import com.quno.qunobackend.domain.snapshot.EnvironmentSnapshotRepository
import com.quno.qunobackend.domain.snapshot.SnapshotSource
import com.quno.qunobackend.domain.snapshot.SnapshotSymptom
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.QuestionEnvironmentSnapshotJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.QuestionEnvironmentSnapshotJpaRepository
import org.springframework.stereotype.Component

@Component
class EnvironmentSnapshotRepositoryAdapter(
    private val jpaRepository: QuestionEnvironmentSnapshotJpaRepository,
) : EnvironmentSnapshotRepository {

    override fun replaceForQuestion(snapshot: EnvironmentSnapshot) {
        jpaRepository.deleteByQuestionId(snapshot.questionId)
        jpaRepository.flush()
        jpaRepository.save(
            QuestionEnvironmentSnapshotJpaEntity(
                questionId = snapshot.questionId,
                versionNumber = snapshot.versionNumber,
                schemaVersion = snapshot.schemaVersion,
                source = snapshot.source.name,
                collector = snapshot.collector,
                fingerprint = snapshot.fingerprint,
                serverRedactions = snapshot.serverRedactions,
                symptom = snapshot.symptom?.name,
                document = snapshot.document,
                createdAt = snapshot.createdAt,
            ),
        )
    }

    override fun findByQuestionId(questionId: Long): EnvironmentSnapshot? = jpaRepository.findByQuestionId(questionId)?.let {
        EnvironmentSnapshot(
            questionId = it.questionId,
            versionNumber = it.versionNumber,
            schemaVersion = it.schemaVersion,
            source = SnapshotSource.valueOf(it.source),
            collector = it.collector,
            fingerprint = it.fingerprint,
            serverRedactions = it.serverRedactions,
            symptom = it.symptom?.let(SnapshotSymptom::valueOf),
            document = it.document,
            createdAt = it.createdAt,
        )
    }
}
