package com.quno.qunobackend.domain.comment

class CommentNotFoundException(id: Long) :
    RuntimeException("Comment not found: $id")

class CommentAccessDeniedException(id: Long) :
    RuntimeException("Not authorized to modify comment: $id")
