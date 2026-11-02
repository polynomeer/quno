package com.quno.qunobackend.infrastructure.runner

import com.quno.qunobackend.domain.reproduction.InvalidRunAttestationException
import com.quno.qunobackend.domain.reproduction.RunAttestation
import com.quno.qunobackend.domain.reproduction.RunAttestationVerifier
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import io.jsonwebtoken.ProtectedHeader
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.math.BigInteger
import java.net.URI
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

/** Phase 80 (ADR-0087): who may report reproduction runs, and how to check them. */
@ConfigurationProperties(prefix = "quno.runner.oidc")
data class RunnerOidcProperties(
    val issuer: String = "https://token.actions.githubusercontent.com",
    val audience: String = "quno",
    val jwksUri: String = "https://token.actions.githubusercontent.com/.well-known/jwks",
    /** `job_workflow_ref` prefix of Quno's reusable workflow; runs through any other workflow are labeled untrusted. */
    val trustedWorkflowPrefix: String = "polynomeer/quno/.github/workflows/reproduce.yml@",
)

@Configuration
@EnableConfigurationProperties(RunnerOidcProperties::class)
class RunnerConfig

/**
 * Verifies a GitHub Actions OIDC token (ADR-0087). GitHub signs it for the job that requested it,
 * so it proves which repository, commit, run and workflow file produced the report — without a
 * GitHub App, a stored secret or any call to the GitHub API. Signing keys come from GitHub's JWKS,
 * cached by `kid` and re-fetched (at most once a minute) only when an unknown `kid` shows up.
 */
@Component
class GithubOidcRunAttestationVerifier(
    private val properties: RunnerOidcProperties,
    private val objectMapper: ObjectMapper,
) : RunAttestationVerifier {
    private val clock: Clock = Clock.systemUTC()
    private var keys: Map<String, PublicKey> = emptyMap()
    private var fetchedAt: Instant = Instant.EPOCH

    override fun verify(token: String): RunAttestation {
        val claims = try {
            Jwts.parser()
                .keyLocator(
                    object : LocatorAdapter<Key>() {
                        override fun locate(header: ProtectedHeader): Key = keyFor(header.keyId ?: throw InvalidRunAttestationException("token has no kid"))
                    },
                )
                .requireIssuer(properties.issuer)
                .requireAudience(properties.audience)
                .clockSkewSeconds(30)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: JwtException) {
            throw InvalidRunAttestationException("not a valid GitHub Actions token for Quno: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw InvalidRunAttestationException("not a valid GitHub Actions token for Quno: ${e.message}")
        }
        val workflowRef = claims.required("job_workflow_ref")
        return RunAttestation(
            repository = claims.required("repository"),
            sha = claims.required("sha").lowercase(),
            runId = claims.required("run_id").toLongOrNull() ?: throw InvalidRunAttestationException("run_id is not a number"),
            runAttempt = claims.required("run_attempt").toIntOrNull() ?: throw InvalidRunAttestationException("run_attempt is not a number"),
            workflowRef = workflowRef,
            runnerEnvironment = claims["runner_environment"] as? String ?: "unknown",
            trustedWorkflow = workflowRef.startsWith(properties.trustedWorkflowPrefix),
            eventName = (claims["event_name"] as? String)?.take(40),
        )
    }

    private fun Claims.required(name: String): String = (this[name] as? String)?.takeIf { it.isNotBlank() } ?: throw InvalidRunAttestationException("token has no $name")

    @Synchronized
    private fun keyFor(kid: String): Key {
        keys[kid]?.let { return it }
        if (Duration.between(fetchedAt, clock.instant()) >= REFETCH_INTERVAL) {
            keys = fetchKeys()
            fetchedAt = clock.instant()
        }
        return keys[kid] ?: throw InvalidRunAttestationException("unknown signing key $kid")
    }

    private fun fetchKeys(): Map<String, PublicKey> {
        val connection = URI.create(properties.jwksUri).toURL().openConnection().apply {
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        val document = connection.getInputStream().use { objectMapper.readTree(it) }
        val factory = KeyFactory.getInstance("RSA")
        return document.path("keys").filter { it.path("kty").asString() == "RSA" && it.hasNonNull("kid") }.associate { jwk ->
            val modulus = BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("n").asString()))
            val exponent = BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("e").asString()))
            jwk.path("kid").asString() to factory.generatePublic(RSAPublicKeySpec(modulus, exponent))
        }
    }

    companion object {
        private val REFETCH_INTERVAL: Duration = Duration.ofMinutes(1)
    }
}
