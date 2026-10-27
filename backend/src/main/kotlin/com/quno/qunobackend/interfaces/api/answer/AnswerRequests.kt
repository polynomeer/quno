package com.quno.qunobackend.interfaces.api.answer

import com.quno.qunobackend.domain.stack.ApplicabilityRange
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.StackDependency
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class WriteAnswerRequest(
    @field:NotBlank
    val body: String,
    /** Cite the live chat this answer was summarised from (Phase 71, ADR-0073). */
    val liveChatRoomId: Long? = null,
    @field:Valid @field:Size(max = ApplicabilityRange.MAX_PER_ANSWER)
    val applicability: List<ApplicabilityRangeRequest> = emptyList(),
)

/** Revising an answer touches its body and, optionally, its applicability ranges (null keeps them, ADR-0077). */
data class AnswerContentRequest(
    @field:NotBlank
    val body: String,
    @field:Valid @field:Size(max = ApplicabilityRange.MAX_PER_ANSWER)
    val applicability: List<ApplicabilityRangeRequest>? = null,
)

/** `[fromVersion, untilVersion)` for one package (Phase 63.3); version parsing and order are checked by [ApplicabilityRange]. */
data class ApplicabilityRangeRequest(
    @field:NotNull
    val ecosystem: Ecosystem,
    @field:NotBlank @field:Size(max = StackDependency.MAX_NAME_LENGTH)
    val name: String,
    @field:Size(max = StackDependency.MAX_VERSION_LENGTH)
    val fromVersion: String? = null,
    @field:Size(max = StackDependency.MAX_VERSION_LENGTH)
    val untilVersion: String? = null,
) {
    fun toDomain() = ApplicabilityRange(
        ecosystem = ecosystem,
        name = name.trim(),
        fromVersion = fromVersion?.trim()?.takeIf { it.isNotEmpty() },
        untilVersion = untilVersion?.trim()?.takeIf { it.isNotEmpty() },
    )
}
