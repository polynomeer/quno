# Archive — 원본 기획 문서

이 디렉터리는 초기 브레인스토밍 단계에서 작성된 원본 기획 문서(.docx/.md/.png)를 보관한다. 여기 있는 문서는 더 이상 직접 참조하는 기준 문서가 아니며, 정리된 내용은 [docs/product](../product/)와 [docs/architecture](../architecture/)에 재작성되어 있다. 원본은 논의 과정과 세부 근거를 확인하고 싶을 때만 참고한다.

## 상태별 분류

### 핵심 계열 — Living Question 철학 (재작성의 근거 자료)

아래 4개 문서는 "질문카드는 죽어있어서는 안 된다"는 철학과 Kotlin/Spring Boot 4/PostgreSQL+MongoDB+Redis 기술 스택으로 수렴한다. [docs/product/vision.md](../product/vision.md), [docs/product/mvp-scope.md](../product/mvp-scope.md), [docs/architecture/](../architecture/)의 원본 소스다.

- `Quno 서비스 통합 기획서 — Living Question Knowledge Platform.md` — 가장 포괄적인 제품 철학·컨셉 문서 (Living Question Card, QPR, Ward, Cluster, Super Answer, QunoBot 등 전체 개념)
- `Quno_통합_기획_도메인_설계서_v1.docx` — DDD Bounded Context, Aggregate, Event Storming까지 포함한 통합 설계서
- `Quno_MVP_기획_및_시스템_설계서.docx` — Spring Boot 4.0.0 · Kotlin · Gradle 스택을 명시한 MVP+시스템 설계서 (기술 스택 확정 근거)
- `Quno_MVP_Product_System_Design_v1.docx`, `quno_mvp_product_system_design.docx` — 위 문서들과 내용이 겹치는 초안/버전들

### 대안 탐색안 — 참고용, 현재 방향 아님

- `Quno_서비스_기획_및_백엔드_설계서.docx` — Living Question/Ward/Revision 철학이 반영되지 않은 범용 Stack Overflow형 Q&A 설계안. **MySQL + Kafka** 스택을 제안하지만, 확정된 스택은 **PostgreSQL + MongoDB + Redis**다. 평판/투표/모더레이션 등 일부 아이디어는 후속 단계에서 재검토할 수 있다.
- `StackNext_통합_서비스_기획서.docx` — Quno와 다른 프로덕트(기술 블로그 + Q&A + AI 학습 + 커리어 네트워크) 기획. 별도 브레인스토밍 산출물이며 Quno의 현재 방향이 아니다.

### 기타

- `quno-event-storming.png` — 이벤트 스토밍 다이어그램 이미지. 텍스트 요약은 [docs/architecture/domain-model.md](../architecture/domain-model.md)의 Event Storming 절 참고.

## 확정된 사항 (2026-08-24)

- 기술 스택: **Kotlin, Spring Boot 4.0.0, Java 21, Gradle Kotlin DSL, 단일 모듈 DDD, PostgreSQL + MongoDB + Redis**. Kafka는 MVP 단계에서 도입하지 않고 Redis 기반 Outbox/Worker로 시작하며, 이벤트 내구성이 실제 병목이 되는 시점에 재검토한다.
- `StackNext`와 `Quno_서비스_기획_및_백엔드_설계서`(MySQL+Kafka)는 참고 아카이브로만 보관하고 현재 개발 방향에서 제외한다.
