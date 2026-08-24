package com.quno.qunobackend.domain.question

class QuestionNotFoundException(id: Long) : RuntimeException("Question not found: $id")
