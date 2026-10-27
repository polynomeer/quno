package com.quno.qunobackend.domain.errorsignature

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression suite for the Phase 64 normaliser (ADR-0075). The cases and fixtures are the Phase 91
 * ones (experiments/phase-91-mcp/test/signature.test.js); the expected hashes were produced by that
 * JavaScript implementation, so a drift between the two shows up here.
 */
class ErrorSignatureExtractorTest {
    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/error-signatures/$name")).readText()

    @Test
    fun `matches the JavaScript implementation's hashes`() {
        val a = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-a.txt")))
        assertEquals("3a69aa74c0ab148d", a.signatureHash)
        assertEquals("e19d86a5a7fca541", a.messageKey)
        assertEquals(
            listOf(
                "org.springframework.beans.factory.support.DefaultListableBeanFactory.raiseNoMatchingBeanFound",
                "org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency",
                "com.acme.Boot.lambda\$run",
                "com.acme.Boot\$proxy.invoke",
            ),
            a.frames,
        )

        val b = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-b.txt")))
        assertNull(b.signatureHash)
        assertEquals("7fd089f4421d1f8a", b.messageKey)

        val node = assertNotNull(
            ErrorSignatureExtractor.extract("TypeError: Cannot read properties of undefined (reading 'x')\n    at foo (/app/a.js:10:5)\n    at bar (/app/b.js:2:1)"),
        )
        assertEquals("32ebfcc3cfd71816", node.signatureHash)
        assertEquals("5407002c4e21e165", node.messageKey)
    }

    @Test
    fun `uses the root cause, the last Caused by`() {
        val signature = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-a.txt")))

        assertEquals("org.springframework.beans.factory.NoSuchBeanDefinitionException", signature.exceptionClass)
        assertEquals(2, signature.chain.size)
    }

    @Test
    fun `paths, line numbers, proxy hashes, lambda numbers and quoted names don't split one error`() {
        val a = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-a.txt")))
        val b = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-a-variant.txt")))

        assertEquals(a.signatureHash, b.signatureHash)
        assertEquals(a.messageKey, b.messageKey)
    }

    @Test
    fun `different errors get different signatures`() {
        val a = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-a.txt")))
        val c = assertNotNull(ErrorSignatureExtractor.extract(fixture("trace-b.txt")))

        assertNotEquals(a.messageKey, c.messageKey)
        assertEquals("java.net.ConnectException", c.exceptionClass)
    }

    @Test
    fun `message normalisation erases ip, port, path, uuid, quoted text and numbers`() {
        assertEquals(
            "failed at <path> <ip> id=<uuid> <str> <n>",
            ErrorSignatureExtractor.normalizeMessage("Failed at /var/app/x/y.txt 10.0.0.5:8080 id=3f2b8a52-aaaa-bbbb-cccc-1234567890ab 'name' 42"),
        )
        assertEquals("cannot read <path>", ErrorSignatureExtractor.normalizeMessage("cannot read /home/alice/app/config.yml"))
    }

    @Test
    fun `frame normalisation drops proxy suffixes and lambda numbers`() {
        assertEquals("a.B\$proxy.invoke", ErrorSignatureExtractor.normalizeFrame("a.B\$\$FastClassBySpringCGLIB\$\$9fa31c2e.invoke"))
        assertEquals("a.B.lambda\$run", ErrorSignatureExtractor.normalizeFrame("a.B.lambda\$run\$12"))
    }

    @Test
    fun `text without an exception has no signature`() {
        assertNull(ErrorSignatureExtractor.extract("그냥 문장"))
        assertNull(ErrorSignatureExtractor.extract(""))
        assertNull(ErrorSignatureExtractor.extract(null))
    }

    @Test
    fun `helpful NPE messages keep their quoted code names apart`() {
        val a = assertNotNull(ErrorSignatureExtractor.extract("java.lang.NullPointerException: Cannot invoke \"java.util.List.size()\" because \"items\" is null"))
        val b = assertNotNull(ErrorSignatureExtractor.extract("java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"name\" is null"))

        assertNotEquals(a.messageKey, b.messageKey)
    }

    @Test
    fun `NoClassDefFoundError keeps the class path instead of treating it as a file path`() {
        val a = assertNotNull(ErrorSignatureExtractor.extract("java.lang.NoClassDefFoundError: org/springframework/boot/jdbc/autoconfigure/DataSourceProperties"))
        val b = assertNotNull(ErrorSignatureExtractor.extract("java.lang.NoClassDefFoundError: org/springframework/util/StringUtils"))

        assertNotEquals(a.messageKey, b.messageKey)
        assertTrue(a.normalizedMessage.contains("datasourceproperties"))
    }

    @Test
    fun `gradle task names starting lower-case are not mistaken for exceptions`() {
        val log = "> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED\n> Task :test FAILED\n" +
            "JwtAuthTests > x() FAILED\n    java.lang.AssertionError: Status expected:<200> but was:<401>\n" +
            "        at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:62)"

        assertEquals("java.lang.AssertionError", ErrorSignatureExtractor.extract(log)?.exceptionClass)
    }

    @Test
    fun `nested and simple class names parse`() {
        assertEquals("org.x.Outer\$Inner\$BoomException", ErrorSignatureExtractor.extract("org.x.Outer\$Inner\$BoomException: bad")?.exceptionClass)
        assertEquals("TypeError", ErrorSignatureExtractor.extract("TypeError: x is not a function")?.exceptionClass)
    }

    @Test
    fun `a log with several chains yields one signature per chain`() {
        val log = "WARN x : failed\njava.lang.UnsatisfiedLinkError: no foo in java.library.path\n\tat a.B.c(B.java:1)\n" +
            "> Task :test FAILED\njava.lang.AssertionError: expected 1 but was 2\n\tat a.D.e(D.java:2)\n" +
            "Caused by: java.io.IOException: boom\n\tat a.F.g(F.java:3)"

        assertEquals(
            listOf("java.lang.UnsatisfiedLinkError", "java.io.IOException"),
            ErrorSignatureExtractor.extractAll(log).map { it.exceptionClass },
        )
    }

    @Test
    fun `input beyond the size cap is ignored`() {
        val padding = "x".repeat(ErrorSignatureExtractor.MAX_INPUT_LENGTH)

        assertNull(ErrorSignatureExtractor.extract(padding + "\njava.lang.IllegalStateException: late"))
    }
}
