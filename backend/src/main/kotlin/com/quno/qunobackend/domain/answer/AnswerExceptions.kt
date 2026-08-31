package com.quno.qunobackend.domain.answer

class AnswerNotFoundException(id: Long) : RuntimeException("Answer not found: $id")

class AnswerAccessDeniedException(id: Long) : RuntimeException("Not authorized to modify answer: $id")

class AnswerVersionNotFoundException(answerId: Long, versionNumber: Int) :
    RuntimeException("Answer $answerId has no version $versionNumber")
