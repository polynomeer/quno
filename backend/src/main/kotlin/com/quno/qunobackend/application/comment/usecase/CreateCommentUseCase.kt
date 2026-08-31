package com.quno.qunobackend.application.comment.usecase

import com.quno.qunobackend.application.comment.dto.CommentResult
import com.quno.qunobackend.application.comment.dto.CreateCommentCommand
import com.quno.qunobackend.domain.answer.AnswerNotFoundException
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.comment.Comment
import com.quno.qunobackend.domain.comment.CommentMentionParser
import com.quno.qunobackend.domain.comment.CommentNotFoundException
import com.quno.qunobackend.domain.comment.CommentReplyDepthExceededException
import com.quno.qunobackend.domain.comment.CommentRepository
import com.quno.qunobackend.domain.comment.CommentTargetType
import com.quno.qunobackend.domain.common.OutboxEvent
import com.quno.qunobackend.domain.common.OutboxEventRepository
import com.quno.qunobackend.domain.common.OutboxEventTypes
import com.quno.qunobackend.domain.question.QuestionNotFoundException
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Resolved recipients for the fan-out payload — [questionId] is always the *question's* id
 * (an answer comment still notifies via the question's Ward subscribers, same as ANSWER_ACCEPTED). */
private data class CommentTarget(val questionId: Long, val questionAuthorId: Long, val answerAuthorId: Long?)

@Service
class CreateCommentUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val outboxEventRepository: OutboxEventRepository,
) {
    @Transactional
    fun execute(command: CreateCommentCommand): CommentResult {
        val target = resolveTarget(command.targetType, command.targetId)
        val parent = command.parentCommentId?.let {
            commentRepository.findById(it) ?: throw CommentNotFoundException(it)
        }
        if (parent != null && parent.parentCommentId != null) {
            throw CommentReplyDepthExceededException(parent.id!!)
        }

        val saved = commentRepository.save(
            Comment.write(command.targetType, command.targetId, command.authorId, command.body, parent?.id),
        )

        outboxEventRepository.save(
            OutboxEvent.create(
                eventType = OutboxEventTypes.NEW_COMMENT,
                aggregateType = "QUESTION",
                aggregateId = target.questionId,
                payload = """{"commentId":${saved.id},"actorId":${command.authorId},"questionAuthorId":${target.questionAuthorId},"answerAuthorId":${target.answerAuthorId ?: "null"},"parentCommentAuthorId":${parent?.authorId ?: "null"}}""",
            ),
        )

        val mentionedUserIds = CommentMentionParser.parseNicknames(command.body)
            .mapNotNull { userRepository.findByNickname(it)?.id }
            .filter { it != command.authorId }
            .toSet()
        if (mentionedUserIds.isNotEmpty()) {
            outboxEventRepository.save(
                OutboxEvent.create(
                    eventType = OutboxEventTypes.MENTIONED_IN_COMMENT,
                    aggregateType = "QUESTION",
                    aggregateId = target.questionId,
                    payload = """{"commentId":${saved.id},"actorId":${command.authorId},"mentionedUserIds":[${mentionedUserIds.joinToString(",")}]}""",
                ),
            )
        }

        return saved.toResult()
    }

    private fun resolveTarget(targetType: CommentTargetType, targetId: Long): CommentTarget = when (targetType) {
        CommentTargetType.QUESTION -> {
            val question = questionRepository.findById(targetId) ?: throw QuestionNotFoundException(targetId)
            CommentTarget(questionId = targetId, questionAuthorId = question.authorId, answerAuthorId = null)
        }
        CommentTargetType.ANSWER -> {
            val answer = answerRepository.findById(targetId) ?: throw AnswerNotFoundException(targetId)
            val question = questionRepository.findById(answer.questionId) ?: throw QuestionNotFoundException(answer.questionId)
            CommentTarget(questionId = answer.questionId, questionAuthorId = question.authorId, answerAuthorId = answer.authorId)
        }
    }
}

internal fun Comment.toResult() = CommentResult(
    id = requireNotNull(id),
    targetType = targetType,
    targetId = targetId,
    authorId = authorId,
    parentCommentId = parentCommentId,
    body = if (deletedAt != null) null else body,
    versionNumber = versionNumber,
    isDeleted = deletedAt != null,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
