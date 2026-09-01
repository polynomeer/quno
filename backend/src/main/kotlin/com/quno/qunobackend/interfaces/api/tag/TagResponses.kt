package com.quno.qunobackend.interfaces.api.tag

import com.quno.qunobackend.application.tag.dto.TagContributorResult
import com.quno.qunobackend.application.tag.dto.TagResult

data class TagResponse(val id: Long, val name: String, val slug: String, val description: String?, val docsUrl: String?)

fun TagResult.toResponse() = TagResponse(id = id, name = name, slug = slug, description = description, docsUrl = docsUrl)

data class TagContributorResponse(val userId: Long, val nickname: String, val answerCount: Long)

fun TagContributorResult.toResponse() = TagContributorResponse(userId = userId, nickname = nickname, answerCount = answerCount)
