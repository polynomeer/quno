package com.quno.qunobackend.infrastructure.stack

import com.quno.qunobackend.domain.stack.DependencyFileFormat
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.StackDependency
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositeDependencyFileParserTest {
    private val parser = CompositeDependencyFileParser(JsonMapper.builder().build())

    @Test
    fun `reads Gradle Kotlin DSL coordinates, platforms and plugins`() {
        val gradle = """
            plugins {
                kotlin("jvm") version "2.2.21"
                id("org.springframework.boot") version "4.0.8"
            }
            dependencies {
                implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("io.r2dbc:r2dbc-pool:1.0.2")
                testImplementation("io.mockk:mockk:1.14.2")
                implementation("${'$'}group:${'$'}name:1.0")
            }
        """.trimIndent()

        val parsed = parser.parse("build.gradle.kts", gradle)

        assertEquals(DependencyFileFormat.GRADLE, parsed.format)
        assertEquals(
            setOf(
                StackDependency(Ecosystem.GRADLE_PLUGIN, "org.jetbrains.kotlin.jvm", "2.2.21"),
                StackDependency(Ecosystem.GRADLE_PLUGIN, "org.springframework.boot", "4.0.8"),
                StackDependency(Ecosystem.MAVEN, "org.springframework.cloud:spring-cloud-dependencies", "2025.0.0"),
                StackDependency(Ecosystem.MAVEN, "org.springframework.boot:spring-boot-starter-web", null),
                StackDependency(Ecosystem.MAVEN, "io.r2dbc:r2dbc-pool", "1.0.2"),
                StackDependency(Ecosystem.MAVEN, "io.mockk:mockk", "1.14.2"),
            ),
            parsed.dependencies.toSet(),
        )
    }

    @Test
    fun `reads Groovy DSL single-quoted coordinates`() {
        val parsed = parser.parse("build.gradle", "dependencies {\n  implementation 'com.google.guava:guava:33.0.0-jre'\n}")

        assertEquals(listOf(StackDependency(Ecosystem.MAVEN, "com.google.guava:guava", "33.0.0-jre")), parsed.dependencies)
    }

    @Test
    fun `reads Maven parent, dependencies and managed versions with property substitution`() {
        val pom = """
            <project>
              <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.4.1</version></parent>
              <version>0.1.0</version>
              <properties><r2dbc.version>1.0.7</r2dbc.version></properties>
              <dependencies>
                <dependency><groupId>org.postgresql</groupId><artifactId>r2dbc-postgresql</artifactId><version>${'$'}{r2dbc.version}</version></dependency>
                <dependency><groupId>com.acme</groupId><artifactId>billing-common</artifactId><version>${'$'}{project.version}</version></dependency>
                <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webflux</artifactId></dependency>
              </dependencies>
            </project>
        """.trimIndent()

        val parsed = parser.parse("pom.xml", pom)

        assertEquals(DependencyFileFormat.MAVEN, parsed.format)
        assertEquals(
            listOf(
                StackDependency(Ecosystem.MAVEN, "org.springframework.boot:spring-boot-starter-parent", "3.4.1"),
                StackDependency(Ecosystem.MAVEN, "org.postgresql:r2dbc-postgresql", "1.0.7"),
                StackDependency(Ecosystem.MAVEN, "com.acme:billing-common", "0.1.0"),
                StackDependency(Ecosystem.MAVEN, "org.springframework.boot:spring-boot-starter-webflux", null),
            ),
            parsed.dependencies,
        )
    }

    @Test
    fun `refuses XML with a DTD so external entities can never be resolved`() {
        val xxe = """
            <?xml version="1.0"?>
            <!DOCTYPE project [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
            <project><dependencies><dependency><groupId>&secret;</groupId><artifactId>x</artifactId></dependency></dependencies></project>
        """.trimIndent()

        assertEquals(emptyList(), parser.parse("pom.xml", xxe).dependencies)
    }

    @Test
    fun `reads npm dependencies, devDependencies and peerDependencies keeping declared ranges`() {
        val json = """{"name":"web","dependencies":{"next":"16.0.1","react":"^19.2.0"},"devDependencies":{"vitest":"~3.2.7"},"peerDependencies":{"react-dom":">=19"}}"""

        val parsed = parser.parse("package.json", json)

        assertEquals(DependencyFileFormat.NPM, parsed.format)
        assertEquals(
            listOf(
                StackDependency(Ecosystem.NPM, "next", "16.0.1"),
                StackDependency(Ecosystem.NPM, "react", "^19.2.0"),
                StackDependency(Ecosystem.NPM, "vitest", "~3.2.7"),
                StackDependency(Ecosystem.NPM, "react-dom", ">=19"),
            ),
            parsed.dependencies,
        )
    }

    @Test
    fun `sniffs the format when no file name is given`() {
        assertEquals(DependencyFileFormat.NPM, parser.parse(null, """{"dependencies":{"zod":"4.6.5"}}""").format)
        assertEquals(DependencyFileFormat.MAVEN, parser.parse(null, "<project></project>").format)
        assertEquals(DependencyFileFormat.GRADLE, parser.parse(null, "implementation(\"a:b:1\")").format)
    }

    @Test
    fun `malformed input yields nothing instead of an error`() {
        assertTrue(parser.parse("package.json", "{not json").dependencies.isEmpty())
        assertTrue(parser.parse("pom.xml", "<project><unclosed>").dependencies.isEmpty())
        assertTrue(parser.parse("build.gradle.kts", "").dependencies.isEmpty())
    }

    @Test
    fun `caps the number of entries per question`() {
        val many = (1..300).joinToString("\n") { "implementation(\"g:a$it:1.0\")" }

        assertEquals(StackDependency.MAX_PER_QUESTION, parser.parse("build.gradle.kts", many).dependencies.size)
    }
}
