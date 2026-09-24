package com.quno.qunobackend.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

/**
 * docs/architecture/system-architecture.md의 "계층 규칙" 표(domain/application/infrastructure/
 * interfaces)를 문서로만 남기지 않고 빌드가 강제하도록 만든다. 도입 시점(2026-09-24) 기준 규칙
 * 위반은 없었다 — 이 테스트는 새 위반을 *만들지 못하게* 막는 회귀 방지 장치다(ADR-0059).
 *
 * infrastructure가 application을 참조하는 지점(예: [com.quno.qunobackend.infrastructure.messaging.OutboxDispatchScheduler],
 * WebSocket 핸들러, JWT 필터)이 실제로 존재한다 — 이건 위반이 아니라 의도된 구조다. HTTP가 아닌
 * 진입점(스케줄러, WebSocket, 시큐리티 필터)은 `interfaces/api`의 컨트롤러와 같은 역할("driving
 * adapter")을 infrastructure 안에서 수행하며 유스케이스를 직접 호출한다. 그래서 Infrastructure는
 * Domain과 Application 둘 다에 접근을 허용한다 — Application이 Infrastructure에 접근하는
 * 반대 방향(포트가 아니라 구현체를 직접 참조하는 것)만 막는다.
 */
class LayeringArchitectureTest {

    private val classes: JavaClasses = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.quno.qunobackend")

    @Test
    fun `layers only depend in the documented direction`() {
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Interfaces").definedBy("..interfaces..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Interfaces").mayOnlyAccessLayers("Application", "Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
            .check(classes)
    }

    @Test
    fun `domain stays free of Spring and JPA`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
            .because("domain은 Spring/JPA 없이도 컴파일되는 순수 Kotlin이어야 한다(system-architecture.md)")
            .check(classes)
    }

    @Test
    fun `domain repositories are ports, not implementations`() {
        classes()
            .that().resideInAPackage("..domain..")
            .and().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .because("domain은 Repository의 포트(interface)만 정의하고, 구현은 infrastructure의 어댑터가 담당한다")
            .check(classes)
    }
}
