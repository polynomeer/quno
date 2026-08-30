package com.quno.qunobackend.domain.follow

class SelfFollowException(userId: Long) : RuntimeException("Cannot follow yourself: $userId")
