package com.quno.qunobackend.domain.answer

class AnswerNotFoundException(id: Long) : RuntimeException("Answer not found: $id")
