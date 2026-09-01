package com.quno.qunobackend.domain.directask

class DirectAskRequestNotFoundException(id: Long) : RuntimeException("Direct ask request not found: $id")

class SelfDirectAskException(userId: Long) : RuntimeException("Cannot Direct Ask yourself: $userId")

/** Thrown when the target has not opted into Direct Ask (User.acceptsDirectAsk == false) —
 * the request is refused outright rather than silently created and ignored. */
class DirectAskNotAcceptedException(targetUserId: Long) : RuntimeException("User does not accept Direct Ask requests: $targetUserId")

/** One open (AWAITING_PAYMENT or PENDING) request per (question, target) — see V21's partial
 * unique index. */
class DuplicateDirectAskException(questionId: Long, targetUserId: Long) :
    RuntimeException("An open Direct Ask request already exists for question $questionId and user $targetUserId")

class DirectAskRequestAlreadyRespondedException(id: Long) : RuntimeException("Direct ask request already responded to: $id")

/** Only the target of the request may accept/decline it. */
class DirectAskAccessDeniedException(id: Long) : RuntimeException("Not the target of direct ask request: $id")

class DirectAskPaymentNotFoundException(orderId: String) : RuntimeException("Direct ask payment not found: $orderId")

/** The client-supplied amount doesn't match what was recorded when the payment was opened —
 * either tampering or a stale client. Never trust the client's amount for the actual Toss confirm
 * call. */
class PaymentAmountMismatchException(expected: Long, actual: Long) :
    RuntimeException("Payment amount mismatch: expected $expected, got $actual")

class PaymentAlreadyProcessedException(orderId: String) : RuntimeException("Payment already processed: $orderId")

/** Toss rejected a confirm or cancel call. */
class PaymentConfirmationFailedException(message: String) : RuntimeException(message)
