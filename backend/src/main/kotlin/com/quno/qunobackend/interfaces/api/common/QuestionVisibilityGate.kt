package com.quno.qunobackend.interfaces.api.common

import com.quno.qunobackend.application.common.QuestionAccess
import com.quno.qunobackend.application.common.QuestionScopedResource
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.directask.DirectAskRequestNotFoundException
import com.quno.qunobackend.domain.engine.EngineSuggestionNotFoundException
import com.quno.qunobackend.domain.livechat.LiveChatRoomNotFoundException
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * The request gate for private questions (Phase 79, ADR-0085). Any URL that addresses a question,
 * or something living under one — an answer, a comment, a live-chat room, a direct ask, an engine
 * suggestion — is checked against the question's visibility before the controller runs. Hidden
 * resources fail with the same not-found a missing one would, so the gate can't be used to probe.
 * Matching on the URL rather than per controller means a new endpoint under these paths is
 * covered without anyone remembering to add a check.
 */
@Component
class QuestionVisibilityGate(private val access: QuestionAccess) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        val (resource, id) = match(path) ?: return true
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val viewer = access.viewer(principal as? Long)
        if (access.isHidden(resource, id, viewer)) throw notFound(resource, id)
        return true
    }

    private fun notFound(resource: QuestionScopedResource, id: Long): RuntimeException = when (resource) {
        QuestionScopedResource.QUESTION -> QuestionNotFoundException(id)
        QuestionScopedResource.ANSWER -> AnswerNotFoundException(id)
        QuestionScopedResource.COMMENT -> CommentNotFoundException(id)
        QuestionScopedResource.LIVE_CHAT_ROOM -> LiveChatRoomNotFoundException(id)
        QuestionScopedResource.DIRECT_ASK -> DirectAskRequestNotFoundException(id)
        QuestionScopedResource.ENGINE_SUGGESTION -> EngineSuggestionNotFoundException(id)
    }

    companion object {
        private val PATTERNS = listOf(
            Regex("""^/api/v1/questions/(\d+)(/.*)?$""") to QuestionScopedResource.QUESTION,
            Regex("""^/api/v1/answers/(\d+)(/.*)?$""") to QuestionScopedResource.ANSWER,
            Regex("""^/api/v1/comments/(\d+)(/.*)?$""") to QuestionScopedResource.COMMENT,
            Regex("""^/api/v1/live-chat/(\d+)(/.*)?$""") to QuestionScopedResource.LIVE_CHAT_ROOM,
            Regex("""^/api/v1/direct-asks/(\d+)(/.*)?$""") to QuestionScopedResource.DIRECT_ASK,
            Regex("""^/api/v1/engine-suggestions/(\d+)(/.*)?$""") to QuestionScopedResource.ENGINE_SUGGESTION,
        )

        fun match(path: String): Pair<QuestionScopedResource, Long>? = PATTERNS.firstNotNullOfOrNull { (regex, resource) ->
            regex.matchEntire(path)?.groupValues?.get(1)?.toLongOrNull()?.let { resource to it }
        }
    }
}

@Configuration
class QuestionVisibilityGateConfig(private val gate: QuestionVisibilityGate) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(gate).addPathPatterns("/api/v1/**")
    }
}
