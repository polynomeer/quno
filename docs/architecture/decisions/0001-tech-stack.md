# ADR-0001: 기술 스택을 Kotlin/Spring Boot 4 + PostgreSQL/MongoDB/Redis 단일 모듈 DDD로 확정

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

`docs/archive/`의 초기 브레인스토밍에는 두 가지 갈래의 백엔드 안이 섞여 있었다: (1) StackNext 스타일의 대안 기획, (2) MySQL + Kafka 조합. 프로젝트를 실제로 시작하려면 이 중 하나를 확정해야 새 코드베이스의 패키지 구조·마이그레이션 도구·인프라 구성을 결정할 수 있었다. 또한 MSA로 시작할지 모듈형 모놀리스로 시작할지도 정해야 했다 — Quno는 아직 트래픽/조직 규모가 없는 MVP 단계다.

## 결정 (Decision)

다음 스택으로 확정한다: **Kotlin · Spring Boot 4.0.8 · Java 21 · Gradle Kotlin DSL · 단일 모듈 + DDD + 모듈형 모놀리스 · PostgreSQL(Source of Truth) + MongoDB(형태가 자주 바뀌는 문서) + Redis(캐시/비동기)**. Flyway로 PostgreSQL 스키마를 관리하고 `ddl-auto`는 쓰지 않는다.

MSA 대신 모듈형 모놀리스를 선택한 이유: 배포 복잡도를 낮추면서도 `domain/application/infrastructure/interfaces` 레이어와 `package-by-domain` 하위 패키지로 도메인 경계를 코드 레벨에서 명확히 유지하면, 이후 트래픽이나 조직 규모가 커졌을 때 Notification/Search처럼 비동기·읽기 중심인 영역부터 독립 서비스로 분리할 수 있다.

## 결과 (Consequences)

- 코어 도메인의 Source of Truth는 항상 PostgreSQL이며, MongoDB로 옮기지 않는다. PostgreSQL 트랜잭션과 MongoDB를 분산 트랜잭션으로 묶지 않고 도메인 이벤트로만 동기화한다(ADR-0006).
- MVP 동안은 서비스 분리 없이 단일 배포 단위로 개발 속도를 우선한다. 분리는 "Notification/Search 부하가 API와 다른 스케일 특성을 보일 때", "이벤트 유실 방지가 중요해질 때", "여러 팀으로 늘어날 때" 중 하나가 실제로 나타나면 재검토한다.
- 모든 후속 ADR과 코드 구조는 이 스택을 전제로 한다. 스택을 바꾸려면 이 ADR을 대체하는 새 ADR과 함께 `system-architecture.md`, `docs/archive/README.md`의 "확정된 사항"을 갱신해야 한다.

## 관련 문서

- [system-architecture.md](../system-architecture.md#확정-기술-스택), [system-architecture.md](../system-architecture.md#확장-시점과-분리-기준)
- [PLAN.md](../../../PLAN.md) Phase 1.1
- 커밋 `a40f0d7` (Spring Boot Kotlin 프로젝트 스캐폴딩)
