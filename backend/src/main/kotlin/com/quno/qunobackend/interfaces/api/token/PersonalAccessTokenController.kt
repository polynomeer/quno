package com.quno.qunobackend.interfaces.api.token

import com.quno.qunobackend.application.token.usecase.IssuePersonalAccessTokenUseCase
import com.quno.qunobackend.application.token.usecase.ManagePersonalAccessTokensUseCase
import com.quno.qunobackend.domain.token.PersonalAccessToken
import com.quno.qunobackend.domain.token.TokenScope
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class IssueTokenRequest(
    @field:NotBlank @field:Size(max = PersonalAccessToken.MAX_NAME_LENGTH)
    val name: String,
    @field:NotEmpty
    val scopes: Set<TokenScope>,
    /** 30, 90 or 365. */
    val lifetimeDays: Long = 90,
)

data class TokenResponse(
    val id: Long,
    val name: String,
    val displayPrefix: String,
    val scopes: List<TokenScope>,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastUsedAt: Instant?,
    val revokedAt: Instant?,
)

/** The secret appears here and nowhere else, ever. */
data class IssuedTokenResponse(val secret: String, val token: TokenResponse)

private fun PersonalAccessToken.toResponse() = TokenResponse(
    id = requireNotNull(id),
    name = name,
    displayPrefix = displayPrefix,
    scopes = scopes.sorted(),
    createdAt = createdAt,
    expiresAt = expiresAt,
    lastUsedAt = lastUsedAt,
    revokedAt = revokedAt,
)

/** Personal access tokens for the CLI and MCP server (Phase 65.1, ADR-0079). Unreachable with a token itself. */
@RestController
@RequestMapping("/api/v1/me/tokens")
class PersonalAccessTokenController(
    private val issueUseCase: IssuePersonalAccessTokenUseCase,
    private val manageUseCase: ManagePersonalAccessTokensUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun issue(@AuthenticationPrincipal userId: Long, @Valid @RequestBody request: IssueTokenRequest): IssuedTokenResponse {
        val issued = issueUseCase.execute(userId, request.name, request.scopes, request.lifetimeDays)
        return IssuedTokenResponse(issued.secret, issued.token.toResponse())
    }

    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long): List<TokenResponse> = manageUseCase.list(userId).map { it.toResponse() }

    @DeleteMapping("/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@AuthenticationPrincipal userId: Long, @PathVariable tokenId: Long) = manageUseCase.revoke(userId, tokenId)
}
