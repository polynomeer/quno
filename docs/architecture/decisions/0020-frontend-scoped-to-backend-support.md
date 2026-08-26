# ADR-0020: 프론트엔드는 백엔드가 이미 지원하는 화면부터 구현하고, Vote/Comment/Badge/Moderation은 후속으로 미룬다

- 날짜: 2026-08-26
- 상태: 승인됨

## 배경 (Context)

[docs/frontend/](../../frontend/README.md)에 정리한 프론트엔드 설계서(원본: [docs/archive/Quno_프론트엔드_상세_설계서.docx](../../archive/Quno_프론트엔드_상세_설계서.docx))는 Stack Overflow형 Q&A 서비스를 전제로 Upvote/Downvote, 범용 댓글(Comment), 배지(Badge), 모더레이션(신고/리뷰 큐)을 포함한다. 반면 지금까지 구축한 Quno 백엔드([PLAN.md](../../../PLAN.md) Phase 0~10)는 다른 방향 — 리비전, Ward(Watch), QPR(정보 요청/재요청), Cluster/Super Answer, 활동 기반 평판 점수, Quno Flow 활동 스트림 — 으로 "살아있는 질문" 철학을 구현해왔다. 두 문서를 대조한 결과([docs/frontend/roadmap.md](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항) 참고) Vote/Comment/Save(북마크)/Follow User/Badge/모더레이션/답변 리비전/비로그인 공개 열람이 백엔드에 없다는 것을 확인했다. 프론트엔드 구현을 시작하기 전에 이 격차를 어떻게 다룰지 정해야 했다.

## 결정 (Decision)

프론트엔드는 **백엔드가 이미 지원하는 화면부터** 구현한다. 질문 목록/상세/작성, 검색, 태그, 답변 작성/채택, 알림, Watch, 평판, QPR Review, Cluster/Super Answer, Quno Flow처럼 대응하는 API가 있는 화면을 우선 만든다. Vote/Comment/Save/Follow User/Badge/모더레이션/답변 리비전은 이번 착수 범위에서 제외하고, 해당 UI는 아예 만들지 않거나 자리표시(placeholder)만 남긴다.

백엔드 확장을 먼저 하지 않기로 한 이유: 지금까지 백엔드가 검증해 온 "살아있는 질문"의 핵심 루프(리비전, Ward, QPR, Cluster)를 실제 화면으로 먼저 보여주는 것이 이 세션의 지금까지 방향과 일관되고, Vote/Comment 같은 범용 Q&A 기능을 추가하는 것은 별도의 설계 결정(예: 투표 점수가 정렬/신뢰도에 어떻게 반영되는지, 댓글이 QPR ReviewRequest와 어떻게 공존하는지)이 필요해 이번 스코프에 포함하면 착수가 늦어진다.

## 결과 (Consequences)

- 실제로 동작하는 화면을 빠르게 볼 수 있다 — 새 백엔드 기능 없이 이미 검증된 API로 프론트엔드를 시작한다.
- 질문/답변 상세 화면의 Action Rail에서 투표 UI, 답변 카드의 Score 정렬, 프로필의 Badge 영역 등은 만들지 않는다 — 화면이 설계서보다 단순해 보이는 것은 의도된 것이다.
- Comment(댓글) 섹션은 통째로 비운다 — QPR ReviewRequest UI로 대체하지 않는다(둘은 성격이 다르므로 섣불리 매핑하면 나중에 되돌리기 어렵다).
- Vote/Comment/Badge/모더레이션이 실제로 필요해지는 시점에는 백엔드부터 새로 설계해야 한다 — 이 결정은 "안 만든다"가 아니라 "지금은 순서를 뒤로 미룬다"는 뜻이다.

## 관련 문서

- [docs/frontend/roadmap.md](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)
- [PLAN.md](../../../PLAN.md) Frontend Phase 0+
