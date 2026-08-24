package com.quno.qunobackend.domain.user

class DuplicateEmailException(email: String) : RuntimeException("Email already in use: $email")

class DuplicateNicknameException(nickname: String) : RuntimeException("Nickname already in use: $nickname")

class InvalidCredentialsException : RuntimeException("Invalid email or password")

class InvalidTokenException : RuntimeException("Invalid or expired token")

class UserNotFoundException(id: Long) : RuntimeException("User not found: $id")
