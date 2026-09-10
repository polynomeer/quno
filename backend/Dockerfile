# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 의존성 레이어를 소스보다 먼저 복사해 캐시 적중률을 높인다
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --stacktrace || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar --stacktrace -x test

# --- Runtime stage -------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd -r quno && useradd -r -g quno quno
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown quno:quno app.jar
USER quno

# 8081은 로컬 개발 시 다른 프로젝트와의 포트 충돌을 피하려고 application-local.yml에서만 쓰는
# 값이다(run.sh 참고) — 컨테이너 안에서는 그럴 이유가 없으므로 Spring Boot 기본값 8080을 쓴다.
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
