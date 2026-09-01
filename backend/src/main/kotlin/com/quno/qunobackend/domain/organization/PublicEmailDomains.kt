package com.quno.qunobackend.domain.organization

/**
 * Curated, hand-maintained blocklist (Phase 23, ADR-0035) — same "explicit curation over
 * auto-detection" philosophy as `domain/qunobot/TrackedTechnologies`. A public webmail domain
 * proves nothing about organizational affiliation, since anyone can register any address there.
 */
object PublicEmailDomains {
    val BLOCKED: Set<String> = setOf(
        "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.kr", "outlook.com", "hotmail.com",
        "live.com", "icloud.com", "me.com", "aol.com", "protonmail.com", "proton.me",
        "naver.com", "daum.net", "hanmail.net", "kakao.com", "nate.com",
    )

    fun isBlocked(domain: String): Boolean = domain.lowercase() in BLOCKED
}
