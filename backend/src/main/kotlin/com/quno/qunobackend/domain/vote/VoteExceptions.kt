package com.quno.qunobackend.domain.vote

class InvalidVoteValueException(value: Int) :
    RuntimeException("Vote value must be 1 or -1, got: $value")

class SelfVoteException(targetType: VoteTargetType, targetId: Long) :
    RuntimeException("Cannot vote on your own $targetType: $targetId")
