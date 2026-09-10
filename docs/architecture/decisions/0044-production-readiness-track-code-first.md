# ADR-0044: 상용 전환을 "사람이 할 일"과 "코드로 할 일"로 나누고 코드 작업부터 진행한다

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

MVP 백로그(Phase 1~31)가 모두 완료되어 기능 격차는 남아있지 않다([roadmap.md 7절](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)). 사용자가 다음 목표로 "MVP가 아니라 상용 제품 수준으로 완성"을 요청했다(2026-09-10). 그러나 상용화에는 사업자 등록, PG 가맹점 계약, 법률 문서 검토, 클라우드 계정 개설처럼 Claude Code가 실행할 수 없는 작업이 다수 섞여 있어, 이를 구분하지 않으면 "코드 관점에서는 끝났는데 실제로는 서비스를 열 수 없는" 상태와 "계약 없이는 못 하는 일" 사이에서 진행이 막힐 위험이 있었다.

## 결정 (Decision)

1. **문서를 목적별로 분리한다.** [docs/product/production-readiness.md](../../product/production-readiness.md)는 "상용 서비스를 열기 위한 필수 조건"(배포 파이프라인·보안·관측 가능성·신뢰성·테스트·문서화)을, [docs/product/quality-improvement-plan.md](../../product/quality-improvement-plan.md)는 "이미 배포 가능한 제품의 완성도"(성능·접근성·UX·코드 품질)를 다룬다. 전자가 없으면 후자는 의미가 없어 순서를 명확히 선후 관계로 둔다.
2. **각 항목을 "사람이 해야 할 일"과 "코드로 구현 가능한 일"로 나눈다.** 사업자 등록/PG 계약/법률 검토/클라우드 계정 개설/DNS 레코드 등록처럼 브라우저 조작이나 법적 책임이 필요한 항목은 사람이 해야 할 일로 명시하고, Claude Code는 그 목록을 만드는 데까지만 관여한다.
3. **Claude Code는 코드로 구현 가능한 일부터 우선 진행한다.** production-readiness.md의 B-1(배포 파이프라인)을 최우선으로 하고, 이후 B-2(보안)~B-6(문서화) 순으로, 그다음 quality-improvement-plan.md를 진행한다. 각 항목은 기존 Phase 리듬(필요 시 ADR → PLAN.md 체크리스트 → 구현 → 검증 → 커밋)을 그대로 따른다.
4. **선행 조건이 없는 작업은 안전한 기본값을 유지한 채 코드만 준비한다.** 예를 들어 실제 PG 연동은 가맹점 계약(사람의 일) 없이는 테스트 키 이상으로 진행할 수 없으므로, prod 프로필에서 필수 환경변수가 비어 있으면 기동을 실패시키는 안전장치까지만 코드로 구현하고 실제 키 주입은 계약 이후로 남긴다.

## 결과 (Consequences)

- PLAN.md의 "Phase 32+" 이후 백로그는 이제 mvp-scope.md 로드맵이 아니라 production-readiness.md/quality-improvement-plan.md의 항목을 순서대로 반영한다.
- 사람이 해야 할 일(사업자 등록, PG 계약 등)은 진행 상황을 Claude Code가 추적하지 않는다 — 사용자가 별도로 관리한다.
- 코드 작업이 실제 계약/인프라 없이 검증 가능한 한도(로컬 Docker, 목/테스트 키)까지만 검증되고, 실환경 검증은 추후 실제 배포 대상이 정해진 뒤로 미뤄진다.

## 관련 문서

- [production-readiness.md](../../product/production-readiness.md)
- [quality-improvement-plan.md](../../product/quality-improvement-plan.md)
- [PLAN.md](../../../PLAN.md) Phase 32+
