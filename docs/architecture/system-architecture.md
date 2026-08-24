# Quno 시스템 아키텍처

> 제품 범위는 [../product/mvp-scope.md](../product/mvp-scope.md), 도메인 모델은 [domain-model.md](domain-model.md), API는 [api-design.md](api-design.md) 참고.
>
> **기술 스택은 확정 사항이다** (2026-08-24). 변경 시 이 문서와 [../archive/README.md](../archive/README.md)의 "확정된 사항"을 함께 갱신한다.

## 확정 기술 스택

| 항목 | 선택 |
|---|---|
| Language | Kotlin |
| Framework | Spring Boot 4.0.0 |
| Runtime | Java 21 |
| Build | Gradle Kotlin DSL |
| Architecture | Single Module + DDD + Modular Monolith |
| ORM | Spring Data JPA / Hibernate |
| Primary DB (Source of Truth) | PostgreSQL |
| Document DB | MongoDB |
| Cache / Async | Redis (MVP). 이벤트 내구성이 실제 병목이 될 때 Kafka/RabbitMQ/SQS 등으로 확장 검토 |
| Migration | Flyway |
| Search | 초기 lexical search, 이후 OpenSearch/Elasticsearch 계열 검토 |
| Object Storage | 스크린샷/재현 영상 등 첨부파일 (MVP 이후) |

MVP는 MSA보다 **모듈형 모놀리스**를 선택한다. 배포 복잡도를 낮추면서도 도메인 경계를 패키지 레벨에서 명확히 하여, 트래픽이나 조직 규모가 커질 때 Search·Notification·Insight 등을 독립 서비스로 분리할 수 있도록 한다.

## 전체 아키텍처

```text
┌───────────────────────────────┐
│        React Web Client       │
│ Ask / Question / Diff / Ward  │
│ Search / Dashboard / Profile  │
└───────────────┬───────────────┘
                │ HTTPS / REST
                ▼
┌─────────────────────────────────────────────┐
│          Quno Backend Application            │
│      Single Module / DDD / Modular Monolith  │
│                                               │
│ User │ Question │ Answer │ Tag │ Watch        │
│ Notification │ Search │ Dashboard             │
└───────┬────────────┬──────────────┬──────────┘
        │             │              │
        ▼             ▼              ▼
┌──────────────┐ ┌────────────┐ ┌─────────────┐
│ PostgreSQL   │ │ MongoDB    │ │ Search      │
│ Core Domain  │ │ Documents  │ │ Full-text / │
│ & Relations  │ │ & Context  │ │ Similarity  │
└──────┬───────┘ └────────────┘ └──────▲──────┘
       │                                │
       │ domain events (Outbox)         │ indexing
       ▼                                │
┌───────────────────┐          ┌────────┴────────┐
│ Redis             │◀────────▶│ Background      │
│ Cache / Queue     │          │ Worker          │
└───────────────────┘          │ Notify / Index  │
                                └─────────────────┘
```

## 저장소별 책임

| 저장소 | 책임 | 대표 데이터 |
|---|---|---|
| PostgreSQL | 정합성이 중요한 Source of Truth | users, questions, question_versions, answers, tags, watches, notifications |
| MongoDB | 형태가 자주 바뀌는 문서·맥락·스냅샷 | question timeline events, architecture snapshot, personalization profile |
| Search | 전문 검색 · 유사 질문 · 향후 vector search | 최신 질문 콘텐츠, 태그, 에러 메타데이터 |
| Redis | 짧은 수명 캐시, 비동기 이벤트 전달 | 인기 질문/대시보드 캐시, 이벤트 큐 |
| Object Storage | 대용량 바이너리 (MVP 이후) | 스크린샷, 재현 영상, 첨부 파일 |

**원칙**: 코어 도메인의 Source of Truth를 MongoDB로 옮기지 않는다. PostgreSQL 트랜잭션과 MongoDB를 분산 트랜잭션으로 묶지 않고, 도메인 이벤트/Outbox를 통한 eventual consistency로 동기화한다.

## 주요 실행 흐름

- **질문 생성**: PostgreSQL 트랜잭션으로 Question + Qv1 생성 → 이벤트 발행 → Worker가 검색 인덱스 갱신
- **리비전**: Qv2 append → `latest_version_id` 갱신 → `QUESTION_REVISION` 이벤트 → 검색 재색인 + Ward 알림 생성
- **답변**: Answer 생성 → `NEW_ANSWER` 이벤트 → 질문 작성자와 Ward 사용자에게 알림
- **채택**: 질문자 권한 검증 → Answer accepted → Question `RESOLVED` → 답변자에게 채택 알림
- **대시보드**: Redis 캐시 우선 사용, miss 시 PostgreSQL 집계/추천 조회 후 캐시

## 비동기 이벤트 처리 — Transactional Outbox

질문 리비전 저장과 Redis publish를 단순 연속 호출하면 "DB commit 후 메시지 발행 실패" 같은 dual-write 문제가 발생한다. Transactional Outbox를 사용한다.

```text
[DB Transaction]
  Question Revision 저장 + OutboxEvent(QUESTION_REVISION_CREATED) 저장
  │
  COMMIT
  │
  ▼
Outbox Publisher → Redis (MVP) / Kafka (확장 시)
  │
  ├──▶ Notification Worker
  └──▶ Search Index Worker
```

- Consumer는 `event_id` 기준으로 멱등하게 처리한다.
- Search는 eventual consistency를 허용한다.
- Notification fan-out은 재처리 가능하게 설계한다.

## 패키지 구조 (Kotlin, 단일 모듈)

```text
com.quno.qunobackend
├── QunoBackendApplication.kt
├── domain
│   ├── common
│   ├── user
│   ├── question
│   ├── answer
│   ├── tag
│   └── watch
├── application
│   ├── question
│   │   ├── usecase
│   │   └── dto
│   ├── answer
│   ├── watch
│   └── notification
├── interfaces
│   └── api
│       ├── common
│       ├── question
│       ├── answer
│       ├── tag
│       ├── watch
│       └── notification
└── infrastructure
    ├── persistence
    │   ├── jpa
    │   │   ├── entity
    │   │   ├── repository
    │   │   └── adapter
    │   └── mongo
    ├── search
    ├── messaging
    └── config
```

### 계층 규칙

| 계층 | 역할 |
|---|---|
| `domain` | 엔티티·값 객체·도메인 정책. Spring/JPA 의존성 최소화. Repository는 포트(interface)만 정의 |
| `application` | Use Case, 트랜잭션 경계, 도메인 객체 조합, 이벤트 발행 의도 |
| `infrastructure` | JPA/Mongo/Redis/Search 등 기술 구현체 (domain 포트의 adapter) |
| `interfaces/api` | Controller, Request/Response DTO, HTTP 검증 |

단일 모듈이어도 **package-by-domain**을 우선한다. 모든 클래스를 `common`이나 `service`에 몰아넣지 않는다.

## Question Aggregate 설계 원칙

| 규칙 | 설명 |
|---|---|
| Revision append-only | 기존 버전을 UPDATE하지 않고 새 `version_number`로 추가 |
| Latest pointer | `questions.latest_version_id`로 최신 리비전을 빠르게 조회 |
| Version uniqueness | `(question_id, version_number)` unique 제약 |
| Soft delete | 질문/답변은 물리 삭제 대신 `deleted_at` |
| Accept invariant | 채택 답변은 반드시 해당 질문에 속한 활성 답변이어야 함 |
| Concurrency | 동시 revision 생성 시 `SELECT ... FOR UPDATE` 또는 optimistic lock + unique constraint 재시도로 방어 (단순 `MAX(version_number)+1`는 경쟁 조건 발생) |

`QuestionVersion` 전체를 JPA 컬렉션으로 항상 로딩하는 방식은 피한다. 버전은 append-only 성격의 별도 저장 모델로 다뤄야 대량 리비전에서도 유리하다. `Answer`도 독립 Aggregate로 두어 질문 하나에 답변이 많아져도 Question Aggregate가 비대해지지 않게 한다.

## 로컬 개발 환경

Docker Compose로 PostgreSQL, MongoDB, Redis를 실행하고 애플리케이션은 IDE/CLI에서 직접 실행한다.

```yaml
services:
  postgres:
    image: postgres:16
    ports: ["5442:5432"]   # non-default host port; see docker-compose.yml
  mongo:
    image: mongo:7
    ports: ["27017:27017"]
  redis:
    image: redis:7
    ports: ["6390:6379"]   # non-default host port; see docker-compose.yml
```

운영 배포 구성은 로컬 Compose와 분리한다. DDL 자동 생성(`ddl-auto`)은 로컬 실험 외에는 사용하지 않고 Flyway로 PostgreSQL 스키마를 관리한다.

## 확장 시점과 분리 기준

현재는 단일 모듈·단일 배포 단위가 맞다. 다음 조건이 실제로 나타날 때만 분리를 검토한다.

- Notification/Search 인덱싱 부하가 API 부하와 완전히 다른 스케일 특성을 보일 때
- 이벤트 유실 방지가 중요해져 Kafka 등 내구성 있는 메시징이 필요할 때
- 팀이 여러 개로 늘어나 배포 독립성이 실질적 이점을 줄 때

분리 후보 1순위는 Notification과 Search/Recommend 같은 비동기·읽기 중심 영역이다. Question/Answer 쓰기 흐름은 충분히 안정화되기 전까지 단일 애플리케이션 안에서 관리한다.
