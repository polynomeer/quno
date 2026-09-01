# Quno

Quno는 개발자를 위한 Q&A 플랫폼으로, 질문을 "죽은 게시물"이 아니라 리비전·클러스터링·재활성화가 가능한 **Living Question Card**로 다루는 것을 핵심 철학으로 한다.

## 문서 구조

- [PLAN.md](PLAN.md): **Claude Code가 순서대로 진행할 작업계획서.** 새 세션을 시작할 때 가장 먼저 확인한다.
- [docs/product/vision.md](docs/product/vision.md): 제품 철학, 핵심 개념(Living Question Card), 차별화
- [docs/product/mvp-scope.md](docs/product/mvp-scope.md): MVP 범위(P0/P1), 로드맵, 성공 지표
- [docs/architecture/system-architecture.md](docs/architecture/system-architecture.md): 확정 기술 스택, 시스템 아키텍처, 패키지 구조
- [docs/architecture/domain-model.md](docs/architecture/domain-model.md): DDD Bounded Context, Aggregate, ERD, SQL 흐름
- [docs/architecture/api-design.md](docs/architecture/api-design.md): REST API 설계
- [docs/architecture/decisions/](docs/architecture/decisions/README.md): ADR(Architecture Decision Record). "현재 상태가 무엇인가"는 위 문서들이, "왜 지금 이 상태인가"는 여기가 답한다.
- [docs/frontend/](docs/frontend/README.md): 프론트엔드 UX/디자인 시스템/기술 아키텍처 설계(React+Next.js). **화면별로 현재 백엔드가 지원하는지 격차가 정리돼 있으니 프론트엔드 작업 전 반드시 확인한다** — 이 설계서가 전제한 투표/댓글/배지/모더레이션/저장/사용자 팔로우/답변 리비전은 Phase 11~20에서, Organization/Direct Ask는 Phase 26에서, 실시간 질문방은 Phase 27에서, 태그 상세 정보는 Phase 28에서, 질문 비로그인 공개 열람은 Phase 29에서 모두 구현됐다([roadmap.md 7절](docs/frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항) 참고) — 더 이상 남은 프론트엔드 격차가 없다. 태그/조직/프로필 공개 확대와 SEO는 ADR-0041이 남겨둔 후속 후보다.
- [docs/archive/](docs/archive/README.md): 초기 브레인스토밍 원본(.docx 등). 현재 방향과 다른 대안 탐색안(StackNext, MySQL+Kafka 백엔드안)도 여기 보관되어 있으며 활성 기준 문서가 아니다.

## ADR (Architecture Decision Record)

**Claude Code는 아키텍처/기술적으로 유의미한 결정을 내리거나 발견할 때마다, 사용자가 요청하지 않아도 [docs/architecture/decisions/](docs/architecture/decisions/README.md)에 ADR을 스스로 작성한다.** 어떤 결정이 해당하는지, 어떻게 쓰는지는 그 디렉터리의 README를 따른다. 요약:

- 대상: 기술 스택/라이브러리 선택, 여러 대안 중 하나를 골라 트레이드오프를 감수하는 설계 결정, 정책으로 남을 버그 수정, 범위를 의도적으로 보류/축소하는 결정, `AskUserQuestion`으로 사용자에게 확인받은 스코프 결정.
- 제외: 단순 리팩터링, 오타 수정, 이미 확정된 패턴을 반복 적용하는 구현.
- ADR을 새로 쓴 뒤에는 `docs/architecture/decisions/README.md`의 목록도 함께 갱신하고, 관련 작업 커밋과 같은 단위로 커밋한다(또는 `docs(adr): ...` 커밋으로 분리).

## 확정 기술 스택

Kotlin · Spring Boot 4.0.8 · Java 21 · Gradle Kotlin DSL · 단일 모듈 DDD · PostgreSQL + MongoDB + Redis. 상세는 [system-architecture.md](docs/architecture/system-architecture.md) 참고. 백엔드 프로젝트는 [backend/](backend/)에 있다.

## 빌드/테스트/실행

가장 빠른 방법: 저장소 루트에서 [run.sh](run.sh) 하나로 Docker 인프라 기동부터 서버 실행까지 한 번에 처리한다.

```bash
./run.sh
```

Docker 데몬이 꺼져 있으면(macOS) 자동으로 켜고 기동을 기다린 뒤, PostgreSQL/MongoDB/Redis 컨테이너를 올리고, `local` 프로필로 서버를 포그라운드 실행한다(`http://localhost:8081/actuator/health`에서 상태 확인, Ctrl+C로 종료).

개별 단계가 필요하면:

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

## 커밋 규칙

이 저장소는 [Conventional Commits](https://www.conventionalcommits.org/) 규칙을 따른다. 상세 내용은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고한다. 요약:

- 형식: `<type>(<scope>): <subject>` — 예: `docs(planning): Quno 통합 기획서 추가`
- 하나의 커밋은 하나의 논리적 작업 단위만 포함한다 (기능 하나, 문서 하나, 설정 변경 하나 등을 섞지 않는다).
- 제목은 명령형·현재형으로, 마침표 없이 작성한다.

**작업 진행 방식**: 작업을 완료할 때마다 (기능 단위, 문서 단위, 설정 변경 단위 등) 바로 커밋한다. 여러 작업을 모아서 한 번에 커밋하지 않는다.
