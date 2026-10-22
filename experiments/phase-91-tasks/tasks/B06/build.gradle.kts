plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "task"
version = "0.0.1"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// WAR로 외부 Tomcat에 배포하므로 내장 Tomcat은 포함하지 않는다(서블릿 API는 컨테이너가 제공).
	implementation("org.springframework.boot:spring-boot-starter-webmvc") {
		exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
	}
	compileOnly("jakarta.servlet:jakarta.servlet-api")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("jakarta.servlet:jakarta.servlet-api")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showStandardStreams = true
	}
}
