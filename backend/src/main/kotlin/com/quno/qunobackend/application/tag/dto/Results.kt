package com.quno.qunobackend.application.tag.dto

import com.quno.qunobackend.domain.tag.TagContributor

data class TagResult(val id: Long, val name: String, val slug: String, val description: String?, val docsUrl: String?)

data class TagContributorResult(val userId: Long, val nickname: String, val answerCount: Long)

internal fun TagContributor.toResult() = TagContributorResult(userId = userId, nickname = nickname, answerCount = answerCount)
