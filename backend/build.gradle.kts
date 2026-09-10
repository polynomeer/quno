plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.4.20"
	id("org.springframework.boot") version "4.0.8"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.quno"
version = "0.0.1-SNAPSHOT"
description = "Quno backend application"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	// 로그인/회원가입/결제 확인 같은 민감 엔드포인트의 무차별 대입·남용 방지용 rate limiting.
	// 인메모리 토큰 버킷이라 단일 인스턴스 기준이다 — 다중 인스턴스로 스케일아웃하면 Redis 백엔드로
	// 바꿔야 한다(production-readiness.md B-2 참고).
	implementation("com.bucket4j:bucket4j_jdk17-core:8.19.0")
	// /actuator/prometheus 노출용. 버전은 spring-boot-starter-actuator가 이미 끌어온
	// micrometer-core와 호환되도록 Spring Boot의 의존성 관리 BOM에 맡긴다(버전 명시 안 함).
	implementation("io.micrometer:micrometer-registry-prometheus")
	// 에러 트래킹 SDK. `sentry-spring-boot-starter-jakarta`는 아직 Spring Boot 4의 재구성된
	// 패키지(`RestClientCustomizer` 등)와 호환되지 않아(ApplicationContext 로드 실패로 확인)
	// Spring 통합 없는 코어 SDK만 쓰고 초기화는 SentryConfig가 직접 한다. DSN이 비어 있으면
	// (기본값) SDK가 스스로 비활성화되므로 로컬/테스트에는 영향이 없다(production-readiness.md B-3).
	implementation("io.sentry:sentry:8.56.0")
	// OpenAPI 문서를 코드에서 자동 생성한다 — 지금까지는 api-design.md를 수기로 갱신해 코드와
	// 어긋날 위험이 있었다(production-readiness.md B-6). 3.x는 Spring Boot 4/Framework 7 지원.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-gson:0.13.0")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Spring context tests (e.g. QunoBackendApplicationTests) need a real datasource/mongo/redis;
	// the local profile points at the docker-compose services (see CLAUDE.md).
	systemProperty("spring.profiles.active", "local")
}
