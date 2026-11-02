package com.quno.qunobackend.domain.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecretDetectorTest {
    private fun labels(text: String) = SecretDetector.find(text).map { it.label }

    @Test
    fun `finds well-formed keys and tokens`() {
        // Built from parts so this file itself never holds a scannable token.
        val github = "ghp_" + "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8"
        assertEquals(listOf("AWS 액세스 키"), labels("aws_access_key_id = AKIA" + "IOSFODNN7EXAMPLE"))
        assertEquals(listOf("GitHub 토큰"), labels("export GH=$github"))
        assertEquals(listOf("Slack 토큰"), labels("xox" + "b-1234567890-abcdefghij"))
        assertEquals(listOf("Quno 액세스 토큰"), labels("QUNO_TOKEN=quno_pat_" + "Zx9_aB3dE5fG7hJ9kL1mN3pQ5"))
        assertEquals(listOf("JWT"), labels("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"))
        assertEquals(listOf("개인 키"), labels("-----BEGIN RSA PRIVATE KEY-----\nMIIEow\n-----END RSA PRIVATE KEY-----"))
    }

    @Test
    fun `finds the password in a connection URL and high-entropy assignments only`() {
        val url = SecretDetector.find("jdbc:postgresql://quno:S3cr3t-Pw@db.internal:5432/quno").single()
        assertEquals("URL 속 비밀번호", url.label)
        assertEquals("S3cr3t-Pw", "jdbc:postgresql://quno:S3cr3t-Pw@db.internal:5432/quno".substring(url.start, url.end))

        assertEquals(listOf("비밀값"), labels("spring.datasource.password=Xq7#pL2vR9mK"))
        assertEquals(listOf("비밀값"), labels("""{"client_secret": "9fG2kQ8wZ1xV7bN3"}"""))
        assertTrue(labels("password=changeme").isEmpty(), "low-entropy sample values stay readable")
        assertTrue(labels("password=\${DB_PASSWORD}").isEmpty(), "placeholders are not secrets")
        assertTrue(labels("token: <your-token-here>").isEmpty())
        assertTrue(labels("jdbc:postgresql://quno:[REDACTED]@db.internal/quno").isEmpty(), "already redacted by the collector")
    }

    @Test
    fun `flags public addresses only in network context`() {
        assertEquals(listOf("공인 IP 주소"), labels("Connection refused to 52.79.120.11:5432"))
        assertEquals(listOf("공인 IP 주소"), labels("host: 52.79.120.11"))
        assertEquals(listOf("공인 IP 주소"), labels("curl http://52.79.120.11/health"))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "connect to 10.0.3.15:5432",
            "host 192.168.0.10",
            "listening on 127.0.0.1:8080",
            "from 172.20.1.4",
            "jackson-databind 2.13.4.2 breaks",
            "com.fasterxml.jackson.core:jackson-databind:2.13.4.2",
            "upgrade to 4.1.2.10 fixed it",
            "server 203.0.113.7",
        ],
    )
    fun `leaves private addresses, versions and documentation ranges alone`(text: String) {
        assertTrue(labels(text).isEmpty(), text)
    }

    @Test
    fun `masks for readers, redacts for revisions, and honours the allowlist`() {
        val key = "AKIA" + "IOSFODNN7EXAMPLE"
        val text = "key=$key and host: 52.79.120.11"

        assertEquals("key=[가려짐: AWS 액세스 키] and host: [가려짐: 공인 IP 주소]", SecretDetector.mask(text))
        assertEquals("key=[REDACTED] and host: [REDACTED]", SecretDetector.redact(text))
        assertEquals("key=[가려짐: AWS 액세스 키] and host: 52.79.120.11", SecretDetector.mask(text, setOf(SecretDetector.hash("52.79.120.11"))))
        assertEquals(null, SecretDetector.mask(null))
        assertEquals("nothing here", SecretDetector.mask("nothing here"))
    }
}
