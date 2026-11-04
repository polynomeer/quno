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
            run("UPDATE questions SET accepted_answer_id = NULL WHERE id = ?", id)
            run("DELETE FROM answer_applicability_ranges WHERE question_id = ?", id)
            run("DELETE FROM answer_versions WHERE answer_id IN (SELECT id FROM answers WHERE question_id = ?)", id)
            run("DELETE FROM answers WHERE question_id = ?", id)
            run("DELETE FROM question_dependencies WHERE question_id = ?", id)
            run("DELETE FROM outbox_events WHERE aggregate_id = ?", id)
            run("DELETE FROM notifications WHERE question_id = ?", id)
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
        userIds.forEach { id ->
            run("DELETE FROM notifications WHERE user_id = ?", id)
            run("DELETE FROM users WHERE id = ?", id)
        }
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
        val latest = (panel["latestByCell"] as List<Map<String, Any?>>).associateBy { it["environment"] }
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
    fun `a matrix walks the question's versions, and the scheduled workflow asks before it runs`() {
        val author = signUp("rpmatrix")
        val questionId = send(
            HttpMethod.POST,
            "/api/v1/questions",
            author,
            mapOf(
                "title" to "Pool leaks after upgrading HikariCP",
                "body" to "connections never return",
                "dependencies" to listOf(mapOf("ecosystem" to "MAVEN", "name" to "com.zaxxer:HikariCP", "version" to "^5.1.0")),
            ),
            201,
        ).long("id").also { questionIds += it }
        // An answer says the fix holds until 6.0.0 — an edge worth running.
        val helper = signUp("rphelper")
        send(
            HttpMethod.POST,
            "/api/v1/questions/$questionId/answers",
            helper,
            mapOf("body" to "Set leakDetectionThreshold.", "applicability" to listOf(mapOf("ecosystem" to "MAVEN", "name" to "com.zaxxer:HikariCP", "fromVersion" to "5.0.0", "untilVersion" to "6.0.0"))),
            201,
        )
        val repository = "octo-${UUID.randomUUID().toString().take(6)}/hikari-matrix"
        val spec = mapOf("repository" to repository, "ref" to "main", "kind" to "GRADLE", "command" to "test", "matrixPackage" to "com.zaxxer:HikariCP", "matrixVersions" to listOf("4.0.3"))
        assertEquals(400, status(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, spec + ("matrixVersions" to listOf("latest"))))
        val capsuleId = send(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, spec, 200).long("id")

        // First scheduled run: due, and the matrix is the author's, the question's and the answer's versions in order.
        val plan = plan(oidc(repository), capsuleId, 200)
        assertEquals(true, plan["due"])
        assertEquals("first-run", plan["reason"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("4.0.3", "5.0.0", "5.1.0", "6.0.0"), (plan["matrix"] as List<Map<String, Any?>>).map { it["version"] })
        assertEquals(409, plan(oidc("someone/else"), capsuleId, null)["_status"])
        assertEquals(401, plan("not-a-token", capsuleId, null)["_status"])

        val base = mapOf("capsuleId" to capsuleId, "kind" to "GRADLE", "command" to "test", "environment" to "ubuntu24 · java 21.0.5")
        listOf("4.0.3" to 0, "5.1.0" to 1, "6.0.0" to 1).forEachIndexed { i, (version, exit) ->
            report(oidc(repository, runId = 2000L + i), base + mapOf("exitCode" to exit, "dependencyVersion" to version), 201)
        }
        assertEquals(409, report(oidc(repository, runId = 2100), base + mapOf("exitCode" to 1, "dependencyVersion" to "not-a-version")))

        // Just ran: the next scheduled run is skipped until the interval passes; a manual run or a dispute goes at once.
        val skipped = plan(oidc(repository), capsuleId, 200)
        assertEquals(false, skipped["due"])
        assertEquals(emptyList<Any>(), skipped["matrix"])
        assertNotNull(skipped["nextDueAt"])
        assertEquals("manual", plan(oidc(repository, eventName = "workflow_dispatch"), capsuleId, 200)["reason"])
        send(HttpMethod.POST, "/api/v1/questions/$questionId/reproduction/rerun-requests", helper, mapOf("reason" to "6.0.1에서도 확인해 주세요"), 200)
        assertEquals("rerun-requested", plan(oidc(repository), capsuleId, 200)["reason"])

        val panel = send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200)

        @Suppress("UNCHECKED_CAST")
        val cells = (panel["latestByCell"] as List<Map<String, Any?>>).associate { it["dependencyVersion"] to it["result"] }
        assertEquals<Map<Any?, Any?>>(mapOf("4.0.3" to "NOT_REPRODUCED", "5.1.0" to "REPRODUCED", "6.0.0" to "REPRODUCED"), cells)
        assertEquals(listOf("4.0.3", "5.0.0", "5.1.0", "6.0.0"), panel["plannedVersions"])

        val metrics = send(HttpMethod.GET, "/api/v1/metrics/reproduction", author, null, 200)
        assertTrue((metrics["runs"] as Number).toLong() >= 3)
        assertTrue((metrics["disputes"] as Number).toLong() >= 1)
        assertEquals(401, status(HttpMethod.GET, "/api/v1/metrics/reproduction", null, null))
    }

    @Test
    fun `a question climbs to L2 only when a trusted run fails with the reported error`() {
        val token = "Zqx${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val author = signUp("rvauthor")
        val reported = "java.lang.IllegalStateException: pool $token exhausted\n\tat com.zaxxer.hikari.Pool.get(Pool.java:42)\n\tat demo.App.main(App.java:7)"
        val snapshot = mapOf(
            "schemaVersion" to 1,
            "source" to "COLLECTOR",
            "tier0" to mapOf(
                "os" to mapOf("name" to "Linux$token", "arch" to "x86_64"),
                "runtimes" to listOf(mapOf("name" to "java", "vendor" to "Eclipse Adoptium", "version" to "21.0.5")),
            ),
        )
        val questionId = createQuestion(author, mapOf("body" to "the pool runs dry $token"))
        fun verification() = send(HttpMethod.GET, "/api/v1/questions/$questionId/verification", null, null, 200)

        // L0: nothing structured yet; the checks say what's missing.
        assertEquals("L0", verification()["level"])
        assertEquals(false, verification().map("checks")["ERROR_SIGNATURE"])

        // L1: signature, snapshot, expected/actual, steps.
        val content = mapOf("title" to "Pool $token exhausted", "body" to "Expected: connections return. Actual: they do not.\n\n1. Run the load test", "logs" to reported, "environmentSnapshot" to snapshot)
        send(HttpMethod.POST, "/api/v1/questions/$questionId/versions", author, content, 200)
        assertEquals("L1", verification()["level"])

        val repository = "octo-${UUID.randomUUID().toString().take(6)}/verify"
        val capsuleId = send(HttpMethod.PUT, "/api/v1/questions/$questionId/reproduction", author, mapOf("repository" to repository, "ref" to "main", "kind" to "SCRIPT", "command" to "./repro.sh"), 200).long("id")
        val run = mapOf("capsuleId" to capsuleId, "kind" to "SCRIPT", "command" to "./repro.sh", "environment" to "ubuntu24")

        // Failing with some other error is not this bug: no L2, and the flag says why.
        report(oidc(repository, runId = 3001), run + mapOf("exitCode" to 1, "logTail" to "java.lang.NullPointerException: other\n\tat demo.Other.x(Other.java:1)\n"), 201)
        assertEquals("L1", verification()["level"])
        assertEquals(listOf("SIGNATURE_MISMATCH"), verification()["flags"])

        // The reported error, but through a hand-written workflow: the exit code could say anything.
        report(oidc(repository, runId = 3002, workflowRef = "$repository/.github/workflows/own.yml@refs/heads/main"), run + mapOf("exitCode" to 1, "logTail" to reported), 201)
        assertEquals("L1", verification()["level"])

        // The reported error through Quno's workflow: L2, and search and the card both say so.
        report(oidc(repository, runId = 3003), run + mapOf("exitCode" to 1, "logTail" to reported), 201)
        assertEquals("L2", verification()["level"])
        assertEquals(emptyList<Any>(), verification()["flags"])
        val older = send(HttpMethod.POST, "/api/v1/questions", author, mapOf("title" to "Pool $token newer but unverified", "body" to "nothing yet"), 201).long("id").also { questionIds += it }
        val hits = readList(perform(HttpMethod.GET, "/api/v1/search?q=$token", null, null).contentAsString)
        assertEquals(listOf(questionId, older), hits.map { (it["id"] as Number).toLong() }, "the reproduced one first, though it is older")
        assertEquals("L2", hits.first()["verifiedLevel"])
        assertNull(hits.last()["verifiedLevel"])

        val card = send(HttpMethod.GET, "/api/v1/questions/$questionId/evidence-card", null, null, 200)
        assertEquals("quno.evidence/1", card["schema"])
        assertEquals("L2", card["level"])
        @Suppress("UNCHECKED_CAST")
        assertEquals("https://github.com/$repository/actions/runs/3003/attempts/1", (card["reproducedIn"] as List<Map<String, Any?>>).single()["runUrl"])
        assertEquals("Linux$token x86_64 · java 21", card.map("environment")["label"])
        assertTrue((card.map("untrusted")["title"] as String).contains(token), "free text only under untrusted")
        assertFalse((card - "untrusted").toString().contains("exhausted"), "the message itself stays out of the structured part")

        // The recipe follows the snapshot; the aggregate counts this environment once per public question.
        val recipe = send(HttpMethod.GET, "/api/v1/questions/$questionId/reproduction", null, null, 200).map("recipe")
        assertEquals("ubuntu-latest", recipe["runsOn"])
        assertEquals("21", recipe["javaVersion"])
        val other = createQuestion(author, mapOf("environmentSnapshot" to snapshot))
        val groups = readList(perform(HttpMethod.GET, "/api/v1/environments?limit=50", null, null).contentAsString)
        assertEquals(2, (groups.single { it["label"] == "Linux$token x86_64 · java 21" }["questionCount"] as Number).toInt())
        assertTrue(other > 0)

        // Rewriting the question to another error loses the level: the runs proved the old one.
        send(HttpMethod.POST, "/api/v1/questions/$questionId/versions", author, content + ("logs" to "java.io.IOException: disk full\n\tat demo.Disk.write(Disk.java:3)"), 200)
        assertEquals("L1", verification()["level"])
        assertEquals(listOf("SIGNATURE_MISMATCH"), verification()["flags"])
        assertNull(readList(perform(HttpMethod.GET, "/api/v1/search?q=$token", null, null).contentAsString).first { (it["id"] as Number).toLong() == questionId }["verifiedLevel"])
        send(HttpMethod.POST, "/api/v1/questions/$questionId/versions", author, content, 200)
        assertEquals("L2", verification()["level"])

        // Now it passes: the latest run of the cell decides, so the level drops and says why.
        report(oidc(repository, runId = 3004), run + mapOf("exitCode" to 0, "logTail" to "ok"), 201)
        assertEquals("L1", verification()["level"])
        assertEquals(listOf("NOT_REPRODUCING"), verification()["flags"])

        // The author's classification keeps the question findable and shows on the card.
        val classify = "/api/v1/questions/$questionId/verification/classification"
        assertEquals(403, status(HttpMethod.PUT, classify, signUp("rvother"), mapOf("classification" to "MISUSE")))
        assertEquals("MISUSE", send(HttpMethod.PUT, classify, author, mapOf("classification" to "MISUSE"), 200)["classification"])
        assertEquals("MISUSE", send(HttpMethod.GET, "/api/v1/questions/$questionId/evidence-card", null, null, 200)["classification"])
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

    private fun createQuestion(author: User, extra: Map<String, Any?>): Long = send(HttpMethod.POST, "/api/v1/questions", author, mapOf("title" to "Pool leaks under load", "body" to "connections never return") + extra, 201)
        .long("id").also { questionIds += it }

    private fun oidc(
        repository: String,
        runId: Long = 77,
        sha: String = "c".repeat(40),
        audience: String = "quno",
        expiresInSeconds: Long = 300,
        workflowRef: String = "polynomeer/quno/.github/workflows/reproduce.yml@refs/heads/main",
        signer: KeyPair = KEYS,
        eventName: String = "schedule",
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
        .claim("event_name", eventName)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expiresInSeconds * 1000))
        .signWith(signer.private, Jwts.SIG.RS256)
        .compact()

    /** The plan's body, or `_status` alone when [expected] is null. */
    private fun plan(token: String, capsuleId: Long, expected: Int?): Map<String, Any?> {
        val response = perform(HttpMethod.POST, "/api/v1/reproduction-runs/plan", null, mapOf("capsuleId" to capsuleId), token)
        if (expected == null) return mapOf("_status" to response.status)
        assertEquals(expected, response.status, response.contentAsString)
        return readMap(response.contentAsString)
    }

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
    private fun readList(json: String): List<Map<String, Any?>> = objectMapper.readValue(json, List::class.java) as List<Map<String, Any?>>

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
