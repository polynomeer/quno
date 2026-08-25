package com.quno.qunobackend.domain.question

class QuestionNotFoundException(id: Long) : RuntimeException("Question not found: $id")

class QuestionVersionNotFoundException(questionId: Long, versionNumber: Int) :
    RuntimeException("Question $questionId has no version $versionNumber")

class QuestionAccessDeniedException(questionId: Long) :
    RuntimeException("Not authorized to modify question: $questionId")

class QuestionAlreadyResolvedException(questionId: Long) :
    RuntimeException("Question is already resolved: $questionId")
