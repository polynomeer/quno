# Quno

Quno는 개발자를 위한 Q&A 플랫폼으로, 질문을 "죽은 게시물"이 아니라 리비전·클러스터링·재활성화가 가능한 **Living Question Card**로 다루는 것을 핵심 철학으로 한다.

## 문서 구조

- [PLAN.md](PLAN.md): **Claude Code가 순서대로 진행할 작업계획서.** 새 세션을 시작할 때 가장 먼저 확인한다.
- [docs/product/vision.md](docs/product/vision.md): 제품 철학, 핵심 개념(Living Question Card), 차별화
- [docs/product/mvp-scope.md](docs/product/mvp-scope.md): MVP 범위(P0/P1), 로드맵, 성공 지표
- [docs/architecture/system-architecture.md](docs/architecture/system-architecture.md): 확정 기술 스택, 시스템 아키텍처, 패키지 구조
- [docs/architecture/domain-model.md](docs/architecture/domain-model.md): DDD Bounded Context, Aggregate, ERD, SQL 흐름
- [docs/architecture/api-design.md](docs/architecture/api-design.md): REST API 설계
- [docs/archive/](docs/archive/README.md): 초기 브레인스토밍 원본(.docx 등). 현재 방향과 다른 대안 탐색안(StackNext, MySQL+Kafka 백엔드안)도 여기 보관되어 있으며 활성 기준 문서가 아니다.

## 확정 기술 스택

Kotlin · Spring Boot 4.0.8 · Java 21 · Gradle Kotlin DSL · 단일 모듈 DDD · PostgreSQL + MongoDB + Redis. 상세는 [system-architecture.md](docs/architecture/system-architecture.md) 참고. 백엔드 프로젝트는 [backend/](backend/)에 있다.

## 빌드/테스트/실행

로컬 인프라(PostgreSQL/MongoDB/Redis, 포트 5442/6390/27017)를 먼저 띄운다.

```bash
docker compose up -d
```

```bash
cd backend && ./gradlew build
```

```bash
cd backend && ./gradlew test
```

```bash
cd backend && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`local` 프로필로 기동하면 `http://localhost:8081/actuator/health`에서 DB/Mongo/Redis 연결 상태를 확인할 수 있다.

## 커밋 규칙

이 저장소는 [Conventional Commits](https://www.conventionalcommits.org/) 규칙을 따른다. 상세 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고한다. 요약:

- 형식: `<type>(<scope>): <subject>` — 예: `docs(planning): Quno 통합 기획서 추가`
- 하나의 커밋은 하나의 논리적 작업 단위만 포함한다 (기능 하나, 문서 하나, 설정 변경 하나 등을 섞지 않는다).
- 제목은 명령형·현재형으로, 마침표 없이 작성한다.

**작업 진행 방식**: 작업을 완료할 때마다 (기능 단위, 문서 단위, 설정 변경 단위 등) 바로 커밋한다. 여러 작업을 모아서 한 번에 커밋하지 않는다.
