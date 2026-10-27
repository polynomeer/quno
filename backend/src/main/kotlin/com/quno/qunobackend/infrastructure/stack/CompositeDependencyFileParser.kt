package com.quno.qunobackend.infrastructure.stack

import com.quno.qunobackend.domain.stack.DependencyFileFormat
import com.quno.qunobackend.domain.stack.DependencyFileParser
import com.quno.qunobackend.domain.stack.Ecosystem
import com.quno.qunobackend.domain.stack.ParsedDependencyFile
import com.quno.qunobackend.domain.stack.StackDependency
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.xml.sax.InputSource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Gradle, Maven and npm dependency declarations → [StackDependency] (Phase 63, ADR-0076).
 * Nothing here stores the input; a malformed file yields an empty list, never an exception, so the
 * preview can simply show "nothing found".
 */
@Component
class CompositeDependencyFileParser(
    private val objectMapper: ObjectMapper,
) : DependencyFileParser {

    override fun parse(fileName: String?, content: String): ParsedDependencyFile {
        val text = content.take(MAX_INPUT_LENGTH)
        val format = detect(fileName, text)
        val dependencies = when (format) {
            DependencyFileFormat.MAVEN -> parseMaven(text)
            DependencyFileFormat.NPM -> parseNpm(text)
            DependencyFileFormat.GRADLE -> parseGradle(text)
        }
        return ParsedDependencyFile(format, dependencies.distinct().take(StackDependency.MAX_PER_QUESTION))
    }

    private fun detect(fileName: String?, text: String): DependencyFileFormat {
        val name = fileName?.substringAfterLast('/')?.lowercase()
        return when {
            name == "pom.xml" || name?.endsWith(".pom") == true -> DependencyFileFormat.MAVEN
            name == "package.json" -> DependencyFileFormat.NPM
            name?.endsWith(".gradle") == true || name?.endsWith(".gradle.kts") == true -> DependencyFileFormat.GRADLE
            text.trimStart().startsWith("<") -> DependencyFileFormat.MAVEN
            text.trimStart().startsWith("{") -> DependencyFileFormat.NPM
            else -> DependencyFileFormat.GRADLE
        }
    }

    private fun parseGradle(text: String): List<StackDependency> {
        val coordinates = GRADLE_DEPENDENCY.findAll(text).mapNotNull { match ->
            val parts = match.groupValues[2].split(':')
            if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank() || '$' in parts[0] || '$' in parts[1]) return@mapNotNull null
            dependency(Ecosystem.MAVEN, "${parts[0]}:${parts[1]}", parts.getOrNull(2)?.substringBefore('@'))
        }
        val plugins = GRADLE_PLUGIN.findAll(text).mapNotNull { dependency(Ecosystem.GRADLE_PLUGIN, it.groupValues[1], it.groupValues[2]) }
        val kotlinPlugins = KOTLIN_PLUGIN.findAll(text).mapNotNull {
            dependency(Ecosystem.GRADLE_PLUGIN, "org.jetbrains.kotlin.${it.groupValues[1]}", it.groupValues[2])
        }
        return (plugins + kotlinPlugins + coordinates).toList()
    }

    private fun parseMaven(text: String): List<StackDependency> {
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                // XXE: no DTDs, no external entities or stylesheets.
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(text)))
        }.getOrNull() ?: return emptyList()

        val project = document.documentElement
        val properties = mutableMapOf<String, String>()
        project.child("properties")?.let { props ->
            props.childElements().forEach { properties[it.tagName] = it.textContent.trim() }
        }
        project.child("version")?.textContent?.trim()?.let { properties["project.version"] = it }

        fun resolve(value: String?): String? = value?.trim()?.let { raw ->
            PROPERTY.replace(raw) { properties[it.groupValues[1]] ?: it.value }
        }?.takeIf { it.isNotBlank() && '$' !in it }

        fun coordinate(element: Element): StackDependency? {
            val group = resolve(element.child("groupId")?.textContent) ?: return null
            val artifact = resolve(element.child("artifactId")?.textContent) ?: return null
            return dependency(Ecosystem.MAVEN, "$group:$artifact", resolve(element.child("version")?.textContent))
        }

        val parent = project.child("parent")?.let { coordinate(it) }
        val direct = project.child("dependencies")?.childElements("dependency").orEmpty().mapNotNull { coordinate(it) }
        val managed = project.child("dependencyManagement")?.child("dependencies")?.childElements("dependency").orEmpty().mapNotNull { coordinate(it) }
        return listOfNotNull(parent) + direct + managed
    }

    private fun parseNpm(text: String): List<StackDependency> {
        val root = runCatching { objectMapper.readTree(text) }.getOrNull() ?: return emptyList()
        if (!root.isObject) return emptyList()
        return NPM_SECTIONS.flatMap { section ->
            val node: JsonNode = root.get(section) ?: return@flatMap emptyList()
            node.properties().mapNotNull { (name, version) ->
                dependency(Ecosystem.NPM, name, version.asString().takeIf { version.isString })
            }
        }
    }

    /** Skips entries the domain would reject (overlong names or versions) instead of failing the whole file. */
    private fun dependency(ecosystem: Ecosystem, name: String, version: String?): StackDependency? = runCatching {
        StackDependency(ecosystem, name.trim(), version?.trim()?.takeIf { it.isNotEmpty() })
    }.getOrNull()

    private fun Element.child(name: String): Element? = childElements(name).firstOrNull()

    private fun Element.childElements(name: String? = null): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.filter { name == null || it.tagName == name }
    }

    companion object {
        const val MAX_INPUT_LENGTH = 100_000

        private val GRADLE_CONFIGURATIONS = listOf(
            "implementation", "api", "compileOnly", "runtimeOnly", "testImplementation", "testRuntimeOnly",
            "testCompileOnly", "annotationProcessor", "kapt", "ksp", "developmentOnly", "compile", "testCompile",
        ).joinToString("|")

        // implementation("g:a:v"), implementation 'g:a:v', implementation(platform("g:a:v"))
        private val GRADLE_DEPENDENCY = Regex("""\b($GRADLE_CONFIGURATIONS)\s*\(?\s*(?:(?:enforcedPlatform|platform)\s*\(\s*)?["']([^"'\s]+)["']""")
        private val GRADLE_PLUGIN = Regex("""\bid\s*\(?\s*["']([\w.\-]+)["']\s*\)?\s+version\s+["']([^"'\s]+)["']""")
        private val KOTLIN_PLUGIN = Regex("""\bkotlin\s*\(\s*["']([\w.\-]+)["']\s*\)\s+version\s+["']([^"'\s]+)["']""")
        private val PROPERTY = Regex("""\$\{([\w.\-]+)}""")
        private val NPM_SECTIONS = listOf("dependencies", "devDependencies", "peerDependencies")
    }
}
