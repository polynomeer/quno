package com.quno.qunobackend.domain.organization

/** Thrown when the claimed email's domain is a public webmail/free-mail provider (gmail.com,
 * naver.com, ...) — see [PublicEmailDomains]. Verifying such a domain wouldn't prove anything
 * about a real organization, since anyone can register any name there. */
class PublicEmailDomainException(domain: String) : RuntimeException("$domain is a public email provider, not an organization domain")

/** No pending verification exists for this user (never requested, or already confirmed). */
class EmailDomainVerificationNotFoundException(userId: Long) : RuntimeException("No pending email verification for user: $userId")

class EmailDomainVerificationExpiredException(userId: Long) : RuntimeException("Verification code expired for user: $userId")

class InvalidVerificationCodeException(userId: Long) : RuntimeException("Incorrect verification code for user: $userId")
