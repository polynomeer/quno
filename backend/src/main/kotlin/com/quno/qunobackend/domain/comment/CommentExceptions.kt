package com.quno.qunobackend.domain.comment

class CommentNotFoundException(id: Long) :
    RuntimeException("Comment not found: $id")

class CommentAccessDeniedException(id: Long) :
    RuntimeException("Not authorized to modify comment: $id")

/** A reply's parent is itself a reply — ADR-0031 #1 limits nesting to one level. */
class CommentReplyDepthExceededException(parentCommentId: Long) :
    RuntimeException("Comment $parentCommentId is already a reply and cannot be replied to")

/** Editing a tombstoned comment — see ADR-0031 #2. */
class CommentAlreadyDeletedException(id: Long) :
    RuntimeException("Comment $id has already been deleted and cannot be edited")
