package com.quno.qunobackend.domain.review

class SelfReviewRequestException(questionId: Long) :
    RuntimeException("Cannot request a review on your own question: $questionId")
