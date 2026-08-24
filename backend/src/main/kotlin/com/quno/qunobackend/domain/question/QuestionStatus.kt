package com.quno.qunobackend.domain.question

/** MVP P0 subset only; see docs/product/vision.md#질문-상태-모델 for the full target model. */
enum class QuestionStatus {
    OPEN,
    NEEDS_INFO,
    UPDATED,
    RESOLVED,
}
