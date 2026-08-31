package com.quno.qunobackend.domain.directask

class DirectAskRequestNotFoundException(id: Long) : RuntimeException("Direct ask request not found: $id")

class SelfDirectAskException(userId: Long) : RuntimeException("Cannot Direct Ask yourself: $userId")

/** Thrown when the target has not opted into Direct Ask (User.acceptsDirectAsk == false) —
 * the request is refused outright rather than silently created and ignored. */
class DirectAskNotAcceptedException(targetUserId: Long) : RuntimeException("User does not accept Direct Ask requests: $targetUserId")

/** One open (PENDING) request per (question, target) — see V18's partial unique index. */
class DuplicateDirectAskException(questionId: Long, targetUserId: Long) :
    RuntimeException("A pending Direct Ask request already exists for question $questionId and user $targetUserId")

class DirectAskRequestAlreadyRespondedException(id: Long) : RuntimeException("Direct ask request already responded to: $id")

/** Only the target of the request may accept/decline it. */
class DirectAskAccessDeniedException(id: Long) : RuntimeException("Not the target of direct ask request: $id")
