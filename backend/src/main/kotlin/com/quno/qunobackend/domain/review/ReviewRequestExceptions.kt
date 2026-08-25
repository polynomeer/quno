package com.quno.qunobackend.domain.review

class SelfReviewRequestException(questionId: Long) :
    RuntimeException("Cannot request a review on your own question: $questionId")

class ReviewRequestNotFoundException(id: Long) :
    RuntimeException("Review request not found: $id")

class ReviewRequestAlreadyAddressedException(id: Long) :
    RuntimeException("Review request is already addressed: $id")

class QuestionNotRevisedSinceRequestException(reviewRequestId: Long) :
    RuntimeException("Question has not been revised since review request $reviewRequestId was opened")
