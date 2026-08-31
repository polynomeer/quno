package com.quno.qunobackend.infrastructure.persistence.jpa.adapter

import com.quno.qunobackend.domain.qunobot.TechnologyRelease
import com.quno.qunobackend.domain.qunobot.TechnologyReleaseRepository
import com.quno.qunobackend.infrastructure.persistence.jpa.entity.TechnologyReleaseJpaEntity
import com.quno.qunobackend.infrastructure.persistence.jpa.repository.TechnologyReleaseJpaRepository
import org.springframework.stereotype.Component

@Component
class TechnologyReleaseRepositoryAdapter(
    private val jpaRepository: TechnologyReleaseJpaRepository,
) : TechnologyReleaseRepository {

    override fun findByTagSlug(tagSlug: String): TechnologyRelease? =
        jpaRepository.findByTagSlug(tagSlug)?.toDomain()

    override fun save(release: TechnologyRelease): TechnologyRelease {
        val entity = TechnologyReleaseJpaEntity(
            id = release.id,
            tagSlug = release.tagSlug,
            productSlug = release.productSlug,
            latestVersion = release.latestVersion,
            latestReleaseDate = release.latestReleaseDate,
            checkedAt = release.checkedAt,
            updatedAt = release.updatedAt,
        )
        return jpaRepository.save(entity).toDomain()
    }

    private fun TechnologyReleaseJpaEntity.toDomain(): TechnologyRelease = TechnologyRelease(
        id = id,
        tagSlug = tagSlug,
        productSlug = productSlug,
        latestVersion = latestVersion,
        latestReleaseDate = latestReleaseDate,
        checkedAt = checkedAt,
        updatedAt = updatedAt,
    )
}
