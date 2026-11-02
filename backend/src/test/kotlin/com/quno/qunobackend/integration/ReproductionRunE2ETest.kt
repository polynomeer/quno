package com.quno.qunobackend.integration

import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import tools.jackson.databind.ObjectMapper
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 80 (ADR-0087) through real HTTP, Postgres and the real OIDC verifier: GitHub's JWKS is
 * replaced by a local file holding a test key, so signatures, issuer, audience and expiry are all
 * checked exactly as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReproductionRunE2ETest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val userIds = mutableListOf<Long>()
    private val questionIds = mutableListOf<Long>()
    private val organizationIds = mutableListOf<Long>()

    private data class User(val id: Long, val token: String)

    @AfterEach
    fun cleanUp() {
        fun run(sql: String, vararg args: Any) = runCatching { jdbcTemplate.update(sql, *args) }
        questionIds.reversed().forEach { id ->
            run("DELETE FROM reproduction_runs WHERE capsule_id IN (SELECT id FROM reproduction_capsules WHERE question_id = ?)", id)
            run("DELETE FROM reproduction_capsules WHERE question_id = ?", id)
            run("UPDATE questions SET latest_version_id = NULL WHERE id = ?", id)
            run("DELETE FROM question_tags WHERE question_id = ?", id)
            run("DELETE FROM question_error_signatures WHERE question_id = ?", id)
            run("DELETE FROM engine_runs WHERE question_id = ?", id)
            run("DELETE FROM engine_suggestions WHERE question_id = ?", id)
            run("DELETE FROM question_versions WHERE question_id = ?", id)
            run("DELETE FROM questions WHERE id = ?", id)
        }
        organizationIds.forEach { id ->
            run("DELETE FROM organization_memberships WHERE organization_id = ?", id)
            run("DELETE FROM organizations WHERE id = ?", id)
        }
        userIds.forEach { id -> run("DELETE FROM users WHERE id = ?", id) }
    }

    @Test
    fun `runs proven by a GitHub OIDC token land on the capsule, and nothing else does`() {
        val author = signUp("rpauthor")
        val other = signUp("rpother")
        val questionId = createQuestion(author, emptyMap())
        val repository = "octo-${UUID.randomUUID().toString().take(6)}/repro-demo"
        val capsule = mapOf("repository" to "https://github.com/$repository", "ref" to "main", "kind" to "GRADLE", "command" to "test --tests 'demo.PoolTest'")

        // Only the author attaches it, and only a sane spec.
        assertEquals(403, status(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", other, capsule))
        assertEquals(400, status(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, capsule + ("repository" to "not a repo")))
        val capsuleId = send(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, capsule, 200).long("id")
        assertEquals(repository, send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200).map("capsule")["repository"])

        val ghToken = "ghp_" + "Z9y8".repeat(9)
        val report = mapOf(
            "capsuleId" to capsuleId,
            "kind" to "GRADLE",
            "command" to "test --tests 'demo.PoolTest'",
            "exitCode" to 1,
            "environment" to "ubuntu24 · java 21.0.5",
            "durationMs" to 4200,
            "logTail" to "PoolTest > leaks FAILED\nGITHUB_TOKEN=$ghToken\n",
        )

        // No token, a token from an unknown key, the wrong audience, an expired one: 401.
        assertEquals(401, status(HttpMethod.POST, "/api/v1/reproduction-runs", null, report))
        assertEquals(401, report(oidc(repository, signer = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()), report))
        assertEquals(401, report(oidc(repository, audience = "someone-else"), report))
        assertEquals(401, report(oidc(repository, expiresInSeconds = -120), report))
        // Valid tokens that don't belong to this capsule: 409.
        assertEquals(409, report(oidc("someone/else"), report))
        assertEquals(409, report(oidc(repository), report + ("command" to "test")))

        val first = report(oidc(repository, runId = 1001), report, 201)
        assertEquals("REPRODUCED", first["result"])
        assertEquals(409, report(oidc(repository, runId = 1001), report), "the same run attempt counts once")
        report(oidc(repository, runId = 1002, workflowRef = "$repository/.github/workflows/own.yml@refs/heads/main"), report + mapOf("exitCode" to 0, "environment" to "macos15 · java 21.0.5"), 201)

        val panel = send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200)

        @Suppress("UNCHECKED_CAST")
        val latest = (panel["latestByEnvironment"] as List<Map<String, Any?>>).associateBy { it["environment"] }
        assertEquals("REPRODUCED", latest["ubuntu24 · java 21.0.5"]?.get("result"))
        assertEquals(true, latest["ubuntu24 · java 21.0.5"]?.get("trustedWorkflow"))
        assertEquals("NOT_REPRODUCED", latest["macos15 · java 21.0.5"]?.get("result"))
        assertEquals(false, latest["macos15 · java 21.0.5"]?.get("trustedWorkflow"), "a hand-written workflow is labeled as such")
        assertEquals("https://github.com/$repository/actions/runs/1001/attempts/1", latest["ubuntu24 · java 21.0.5"]?.get("runUrl"))

        val log = send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction/runs/${first.long("runId")}/log", null, null, 200)
        assertFalse((log["logTail"] as String).contains(ghToken), "secrets are masked before the log is stored")
        assertEquals(1, (log["maskedSecretCount"] as Number).toInt())
        assertEquals(404, status(HttpMethod.GET, "/api/v1/questions/${createQuestion(author, emptyMap())}/reproduction/runs/${first.long("runId")}/log", null, null))

        // 이의 제기: one outstanding request, answered by the next run.
        assertEquals(401, status(HttpMethod.POST, "/api/v1/questions/$questionId/reproduction/rerun-requests", null, mapOf("reason" to "x")))
        assertNotNull(send(HttpMethod.POST, "/api/v1/questions/$questionId/reproduction/rerun-requests", other, mapOf("reason" to "JDK 21.0.6에서는 재현 안 돼요"), 200)["rerunRequestedAt"])
        assertEquals(409, status(HttpMethod.POST, "/api/v1/questions/$questionId/reproduction/rerun-requests", author, mapOf("reason" to "again")))
        report(oidc(repository, runId = 1003), report, 201)
        assertNull(send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200).map("capsule")["rerunRequestedAt"])

        // Replacing the capsule: runs of the old spec stop counting, and a pinned sha must match.
        val sha = "a".repeat(40)
        val pinnedId = send(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, capsule + ("ref" to sha), 200).long("id")
        assertEquals(409, report(oidc(repository, runId = 1004), report), "the replaced capsule takes no more runs")
        assertEquals(409, report(oidc(repository, runId = 1005, sha = "b".repeat(40)), report + ("capsuleId" to pinnedId)))
        report(oidc(repository, runId = 1006, sha = sha), report + ("capsuleId" to pinnedId), 201)
        @Suppress("UNCHECKED_CAST")
        assertEquals(1, (send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200)["recentRuns"] as List<Any>).size)
    }

    @Test
    fun `a private question's reproduction stays behind the visibility gate`() {
        val author = signUp("rpprivate")
        val outsider = signUp("rpoutsider")
        val org = jdbcTemplate.queryForObject(
            "INSERT INTO organizations (name, slug, created_by, email_domain) VALUES (?, ?, ?, ?) RETURNING id",
            Long::class.java,
            "rp-${UUID.randomUUID().toString().take(8)}",
            "rp-${UUID.randomUUID().toString().take(8)}",
            author.id,
            "rp-${UUID.randomUUID().toString().take(8)}.example",
        ) as Long
        organizationIds += org
        jdbcTemplate.update("INSERT INTO organization_memberships (organization_id, user_id) VALUES (?, ?)", org, author.id)
        val questionId = createQuestion(author, mapOf("organizationId" to org))
        send(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, mapOf("repository" to "octo/private-repro", "ref" to "main", "kind" to "SCRIPT", "command" to "./repro.sh"), 200)

        assertEquals(404, status(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null))
        assertEquals(404, status(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", outsider, null))
        assertEquals(404, status(HttpMethod.POST, "/api/v1/questions/$questionId/reproduction/rerun-requests", outsider, mapOf("reason" to "x")))
        assertTrue(send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", author, null, 200)["capsule"] != null)
    }

    private fun createQuestion(author: User, extra: Map<String, Any>): Long = send(HttpMethod.POST, "/api/v1/questions", author, mapOf("title" to "Pool leaks under load", "body" to "connections never return") + extra, 201)
        .long("id").also { questionIds += it }

    private fun oidc(
        repository: String,
        runId: Long = 77,
        sha: String = "c".repeat(40),
        audience: String = "quno",
        expiresInSeconds: Long = 300,
        workflowRef: String = "polynomeer/quno/.github/workflows/reproduce.yml@refs/heads/main",
        signer: KeyPair = KEYS,
    ): String = Jwts.builder()
        .header().keyId(if (signer === KEYS) KID else "other-kid").and()
        .issuer(ISSUER)
        .audience().add(audience).and()
        .subject("repo:$repository:ref:refs/heads/main")
        .claim("repository", repository)
        .claim("sha", sha)
        .claim("run_id", runId.toString())
        .claim("run_attempt", "1")
        .claim("job_workflow_ref", workflowRef)
        .claim("runner_environment", "github-hosted")
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .signWith(signer.private, Jwts.SIG.RS256)
        .compact()

    private fun report(token: String, body: Map<String, Any?>): Int = perform(HttpMethod.POST, "/api/v1/reproduction-runs", null, body, token).status

    private fun report(token: String, body: Map<String, Any?>, expected: Int): Map<String, Any?> {
        val response = perform(HttpMethod.POST, "/api/v1/reproduction-runs", null, body, token)
        assertEquals(expected, response.status, response.contentAsString)
        return readMap(response.contentAsString)
    }

    private fun signUp(prefix: String): User {
        val email = "$prefix-${UUID.randomUUID()}@example.com"
        val id = send(HttpMethod.POST, "/api/v1/auth/signup", null, mapOf("email" to email, "nickname" to "$prefix${System.nanoTime() % 1_000_000}", "password" to "password123"), 201).long("id")
        userIds += id
        val login = readMap(perform(HttpMethod.POST, "/api/v1/auth/login", null, mapOf("email" to email, "password" to "password123")).contentAsString)
        return User(id, login["accessToken"] as String)
    }

    private fun perform(method: HttpMethod, path: String, user: User?, body: Any?, attestation: String? = null) = mockMvc.perform(
        request(method, path).contentType(MediaType.APPLICATION_JSON).apply {
            body?.let { content(objectMapper.writeValueAsString(it)) }
            user?.let { header("Authorization", "Bearer ${it.token}") }
            attestation?.let { header("X-Quno-Attestation", it) }
        },
    ).andReturn().response

    private fun status(method: HttpMethod, path: String, user: User?, body: Any?): Int = perform(method, path, user, body).status

    private fun send(method: HttpMethod, path: String, user: User?, body: Any?, expected: Int): Map<String, Any?> {
        val response = perform(method, path, user, body)
        assertEquals(expected, response.status, "$method $path: ${response.contentAsString}")
        return if (response.contentAsString.isBlank()) emptyMap() else readMap(response.contentAsString)
    }

    private fun Map<String, Any?>.long(key: String): Long = (this[key] as Number).toLong()

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.map(key: String): Map<String, Any?> = this[key] as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun readMap(json: String): Map<String, Any?> = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>

    companion object {
        private const val ISSUER = "https://token.actions.githubusercontent.com"
        private const val KID = "quno-test-key"
        private val KEYS: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        @JvmStatic
        @DynamicPropertySource
        fun runnerProperties(registry: DynamicPropertyRegistry) {
            val publicKey = KEYS.public as RSAPublicKey
            fun b64(value: BigInteger) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray().dropWhile { it == 0.toByte() }.toByteArray())
            val jwks = Files.createTempFile("quno-jwks", ".json")
            Files.writeString(jwks, """{"keys":[{"kty":"RSA","kid":"$KID","alg":"RS256","use":"sig","n":"${b64(publicKey.modulus)}","e":"${b64(publicKey.publicExponent)}"}]}""")
            registry.add("quno.runner.oidc.jwks-uri") { jwks.toUri().toString() }
        }
    }
}
