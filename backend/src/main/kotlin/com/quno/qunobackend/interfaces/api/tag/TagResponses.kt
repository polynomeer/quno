package com.quno.qunobackend.interfaces.api.tag

import com.quno.qunobackend.application.tag.dto.TagResult

data class TagResponse(val id: Long, val name: String, val slug: String)

fun TagResult.toResponse() = TagResponse(id = id, name = name, slug = slug)
