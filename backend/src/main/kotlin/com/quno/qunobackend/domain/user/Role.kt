package com.quno.qunobackend.domain.user

/** No self-service promotion API (Phase 16, ADR-0028) — the first MODERATOR is set directly in the DB. */
enum class Role { USER, MODERATOR }
