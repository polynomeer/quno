package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.qunobot.AffectedQuestion
import com.quno.qunobackend.domain.qunobot.VersionImpact
import com.quno.qunobackend.domain.qunobot.VersionImpactRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.VersionImpactJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class VersionImpactRepositoryAdapter(
    private val jpaRepository: VersionImpactJpaRepository,
) : VersionImpactRepository {

    override fun findAffectedQuestions(tagSlug: String, sinceDate: LocalDate, limit: Int): List<AffectedQuestion> =
        jpaRepository.findAffectedQuestions(tagSlug, sinceDate, limit).map {
            AffectedQuestion(questionId = it.getQuestionId(), questionAuthorId = it.getQuestionAuthorId())
        }

    override fun findVersionImpacts(limit: Int): List<VersionImpact> =
        jpaRepository.findVersionImpacts(limit).map {
            VersionImpact(
                questionId = it.getQuestionId(),
                questionTitle = it.getQuestionTitle(),
                tagSlug = it.getTagSlug(),
                productSlug = it.getProductSlug(),
                latestVersion = it.getLatestVersion(),
                latestReleaseDate = it.getLatestReleaseDate(),
            )
        }
}
