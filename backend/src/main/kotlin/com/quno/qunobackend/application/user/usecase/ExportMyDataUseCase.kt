package com.quno.qunobackend.application.user.usecase

import com.quno.qunobackend.application.user.dto.MyDataExportAnswer
import com.quno.qunobackend.application.user.dto.MyDataExportQuestion
import com.quno.qunobackend.application.user.dto.MyDataExportResult
import com.quno.qunobackend.domain.answer.AnswerRepository
import com.quno.qunobackend.domain.question.QuestionRepository
import com.quno.qunobackend.domain.user.UserNotFoundException
import com.quno.qunobackend.domain.user.UserRepository
import org.springframework.stereotype.Service

/** 개인정보 다운로드 요청(ADR-0046) — 자기 자신의 프로필과 직접 작성한 질문/답변만 내려준다. */
@Service
class ExportMyDataUseCase(
    private val userRepository: UserRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
) {
    fun execute(userId: Long): MyDataExportResult {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return MyDataExportResult(
            userId = userId,
            email = user.email,
            nickname = user.nickname,
            createdAt = user.createdAt,
            questions = questionRepository.findAllByAuthorId(userId)
                .map { MyDataExportQuestion(id = requireNotNull(it.id), title = it.title, createdAt = it.createdAt) },
            answers = answerRepository.findAllByAuthorId(userId)
                .map { MyDataExportAnswer(id = requireNotNull(it.id), questionId = it.questionId, createdAt = it.createdAt) },
        )
    }
}
