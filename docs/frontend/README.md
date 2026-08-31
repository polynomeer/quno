# Quno 프론트엔드 설계

React/Next.js 기반 프론트엔드 설계 문서. 원본은 2026-08-26에 전달된 [docs/archive/Quno_프론트엔드_상세_설계서.docx](../archive/Quno_프론트엔드_상세_설계서.docx)이며, 이 디렉터리는 그 내용을 Claude Code가 참조하기 쉬운 형태로 재구성한 것이다.

- [design.md](design.md): 제품 경험 원칙, "살아있는 질문" UI 개념, 사용자 여정, 정보 구조(IA), 디자인 시스템, 화면별 UX(홈/검색/작성/상세/답변/댓글/태그/프로필/알림/Watch/평판/모더레이션 등). **화면마다 현재 백엔드로 구현 가능한지 "백엔드 연동 메모"를 달아뒀다.**
- [architecture.md](architecture.md): 권장 스택(React+Next.js+TypeScript, TanStack Query, Zustand, RHF+Zod 등), 디렉터리 구조, 상태 관리, API 통신 계층, 인증 UX, 실시간 업데이트, 성능/SEO/분석/테스트 전략.
- [roadmap.md](roadmap.md): 단계별 구현 로드맵(Phase 0~7), 화면별 완료 조건, MVP 우선순위, **백엔드 격차 요약**(이 설계서가 전제하는 기능 중 현재 백엔드 상태 — 투표·댓글·배지·모더레이션·저장·사용자 팔로우·답변 리비전은 Phase 11~20에서 모두 구현됐고, 태그 상세/Organization/Direct Ask/실시간 질문방만 남음).
- [quno-design-sample.png](quno-design-sample.png): 실제 화면 목업(홈, 질문 상세, 질문 작성, 태그, 알림, 프로필, 모바일).

## 착수 전 반드시 확인할 것

이 설계서는 Stack Overflow형 서비스(투표, 댓글, 배지, 모더레이션)를 전제로 2026-08-26에 작성됐고, 당시 Quno 백엔드([PLAN.md](../../PLAN.md))는 다른 방향(리비전, Ward, QPR, Cluster/Super Answer, 평판 점수, Quno Flow)으로 먼저 발전해 있었다. 이후 Phase 11~20에서 이 설계서가 전제한 기능 대부분을 구현했지만, 실제 구현은 목업과 세부가 다른 지점이 많다(예: 모더레이션은 Keep/Hide 두 액션뿐, 댓글 자동완성 없음, 답글은 1단계까지만). 실제 화면/컴포넌트 작업을 시작하기 전에 [roadmap.md의 격차 표](roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항-2026-09-01-갱신)와 [design.md](design.md)의 해당 화면 "백엔드 연동 메모"를 먼저 읽고 실제 동작과 목업의 차이를 확인한다.
