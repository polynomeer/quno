# Quno 프론트엔드 로드맵

> 원본: [docs/archive/Quno_프론트엔드_상세_설계서.docx](../archive/Quno_프론트엔드_상세_설계서.docx). UX/화면 설계는 [design.md](design.md), 기술 아키텍처는 [architecture.md](architecture.md) 참고.

## 1. 최종 구현 방향

Quno의 프론트엔드는 "Stack Overflow를 예쁘게 다시 그리는 것"이 목표가 아니다. 검색 효율, 질문 품질, 답변 신뢰도라는 Q&A 서비스의 본질을 보존하면서 질문의 상태 변화와 지식의 축적 과정을 더 잘 보이게 만드는 것이 핵심이다.

초기 개발에서는 홈·검색·질문 상세·질문 작성·답변이라는 5개 화면의 완성도를 최우선으로 한다. 알림, 배지, 프로필 통계, 개인화는 핵심 루프가 안정화된 뒤 추가한다. React 애플리케이션도 백엔드와 마찬가지로 하나의 배포 단위를 유지하고 feature 단위 경계만 명확히 나누는 편이 좋다.

## 2. 전체 화면 관계

```text
┌─────────────┐
│    Home     │
└──────┬──────┘
       │
┌──────────────────┼───────────────────┐
v                   v                   v
┌───────────┐ ┌───────────┐ ┌───────────┐
│ Questions │ │   Tags    │ │ Watching  │
└─────┬─────┘ └─────┬─────┘ └─────┬─────┘
      │             │             │
      └────────────┬┴─────────────┘
                    v
         ┌──────────────────┐
         │  Question Detail │
         │ revision/activity│
         └───────┬──────────┘
                  │
      ┌───────────┼──────────────┐
      v           v              v
   Answer      Comment        Related
      │
      v
   Accept

Search ─────────────→ Question Detail
Ask ────────────────→ Question Detail
Notification ───────→ Question/Answer anchor
Profile ────────────→ Contributions ─→ Question Detail
```

## 3. 단계별 구현 로드맵

| Phase | 프론트엔드 범위 | 핵심 산출물 |
|---|---|---|
| Phase 0 | Design foundation | 토큰, Button/Input/Tag/Badge, AppShell |
| Phase 1 | Read experience | Home, Questions, Question Detail, Tag Detail, SEO |
| Phase 2 | Write experience | Login, Ask, Answer Composer, Comments |
| Phase 3 | Community actions | Vote, Accept, Watch, Save, Reputation |
| Phase 4 | Discovery | 고급 검색, 필터, 관련 질문, 태그 탐색 |
| Phase 5 | Retention | Notifications, Watching, Saved, Profile |
| Phase 6 | Quality | Revision/Diff, Moderation, accessibility/performance hardening |
| Phase 7 | Advanced | 실시간, 개인화 피드, 고급 추천 |

> **원본 로드맵과 백엔드 현황의 차이(2026-09-01 갱신)**: 이 표를 처음 작성할 당시 Phase 2의 Comments, Phase 3의 Vote/Save, Phase 6의 Moderation은 대응하는 백엔드 기능이 없었다. 이후 Phase 11~20(백엔드)과 Frontend Phase 6~11에서 Vote/Comment/Save/Follow User/Badge/Moderation/Answer Revision/검색 Score 정렬을 모두 구현했다 — 실제로는 원래 로드맵의 Phase 순서·묶음과 다르게(예: Comments가 Phase 2가 아니라 훨씬 뒤에), 그리고 여러 항목이 한 Phase에 안 묶이고 각각 별도 Phase로 진행됐다. 상세 이력은 [PLAN.md](../../PLAN.md) Phase 11 이후, 각 항목의 현재 상태는 [design.md](design.md)의 화면별 "백엔드 연동 메모"와 아래 7절 참고.

### 3.1 가장 먼저 구현할 Vertical Slice

1. QuestionCard + 질문 목록
2. QuestionDetail + AnswerCard
3. 검색 query + 태그 필터
4. Ask form + MarkdownEditor
5. Answer form
6. Vote/Accept *(둘 다 구현됨 — Vote는 Phase 11/F6)*
7. 로그인/권한 처리

## 4. 화면별 완료 조건 (Definition of Done)

| 화면 | Definition of Done |
|---|---|
| Home | 피드 로딩/빈상태/오류/페이지네이션, 모바일 대응 |
| Questions/Search | URL 기반 query/filter/sort, 뒤로가기 상태 보존 |
| Question Detail | 질문/답변/댓글/투표/수락/Watch 기본 흐름 완성 |
| Ask | 유사 질문, validation, preview, 중복 submit 방지, draft |
| Tag Detail | 태그 정보, 질문 탭, Follow 상태 |
| Profile | 기여 목록, 태그별 전문성, 배지 |
| Notifications | 읽음 처리, anchor 이동, 빈 상태 |
| Moderation | 필터, review action, confirmation, audit 정보 |

## 5. MVP 페이지 우선순위

| Priority | 페이지/기능 | 이유 |
|---|---|---|
| P0 | Question Detail | 서비스의 검색 유입·지식 소비·답변이 모두 만나는 핵심 화면 |
| P0 | Ask | 콘텐츠 공급과 질문 품질을 결정 |
| P0 | Questions/Search | 기존 지식 재사용과 중복 방지 |
| P0 | Answer Composer + Vote + Accept | Q&A 해결 루프 완성 (모두 구현됨) |
| P1 | Home Feed | 재방문과 발견 |
| P1 | Tag Detail | 관심 분야 탐색 |
| P1 | Notifications / Watching | 사용자 재참여 |
| P2 | Profile / Badges / Moderation advanced | 커뮤니티 성장 이후 가치 증가 |

## 6. 첫 구현 체크리스트

- [ ] 디자인 토큰과 Typography 확정
- [ ] AppHeader / SideNavigation / Responsive container
- [ ] QuestionCard
- [ ] QuestionDetail shell
- [ ] MarkdownRenderer + CodeBlock
- [ ] VoteControl / WatchButton / StatusBadge (모두 구현됨)
- [ ] AnswerCard
- [ ] SearchInput + FilterBar
- [ ] QuestionEditor + TagPicker
- [ ] API Client + Error model + Query keys
- [ ] Auth guard와 redirectTo
- [ ] Playwright 핵심 플로우 테스트

## 7. 백엔드 격차 요약과 착수 전 확인 사항

> 2026-09-01 갱신: 이 절은 원래 "투표/댓글/배지/모더레이션 등이 백엔드에 없다"는 격차를 기록했으나, Phase 11~20에서 아래 표의 항목 대부분이 구현됐다. 실제 프론트엔드 작업을 시작하기 전에 여전히 남은 격차만 확인하면 된다.

이 설계서는 Stack Overflow형 서비스를 전제로 투표(Vote)·댓글(Comment)·배지(Badge)·모더레이션을 포함했고, Quno 백엔드([PLAN.md](../../PLAN.md) 참고)는 처음엔 다른 방향(리비전·Ward·QPR·Cluster/Super Answer·평판 점수·Quno Flow)으로 "살아있는 질문" 철학을 구현해왔다.

| 이 설계서가 전제하는 기능 | 현재 백엔드 상태 |
|---|---|
| Upvote/Downvote, score 정렬 | **구현됨**(Phase 11, [ADR-0023](../architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)) — `GET /search?sort=score`(Phase 20, [ADR-0032](../architecture/decisions/0032-vote-score-search-sort-dashboard-reputation.md))도 있음. 다만 **답변 목록 정렬에는 Score 옵션이 없음**(Best/Newest/Oldest만) |
| Comment(질문/답변 댓글) | **구현됨**(Phase 12/19, [ADR-0024](../architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md)/[ADR-0031](../architecture/decisions/0031-comment-thread-mention-edit-history.md)) — 1단계 답글, 수정 이력, `@mention` 알림(자동완성 없음) 포함. QPR ReviewRequest는 여전히 별개 워크플로 |
| Save(북마크, Watch와 별개) | **구현됨**(Phase 13, [ADR-0025](../architecture/decisions/0025-save-as-separate-side-aggregate-from-watch.md)) |
| Follow User | **구현됨**(Phase 14, [ADR-0026](../architecture/decisions/0026-follow-user-relationship-only-no-activity-feed.md)) — 관계 기록·조회만, 활동 피드/알림은 없음 |
| Badge(배지) | **구현됨**(Phase 15, [ADR-0027](../architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md)) — 영속화 없는 계산형 읽기 모델, 획득 알림 없음. 평판 점수(`UserReputation`)에도 Phase 20에서 투표 항이 추가됨 |
| 태그별 세분화 Expertise | 여전히 없음 — 태그 자체에 설명/문서 링크/상위 기여자 개념 없음([ADR-0021](../architecture/decisions/0021-tag-detail-via-search-approximation.md)) |
| 모더레이션(신고, 리뷰 큐, 역할 기반 권한) | **구현됨**(Phase 16, [ADR-0028](../architecture/decisions/0028-moderation-mvp-report-dismiss-hide-only.md)) — 단, Keep/Hide 두 액션뿐, Close-as-duplicate/Edit/사유별 필터/역할 부여 API는 없음 |
| 답변(Answer) 수정 이력 | **구현됨**(Phase 17, [ADR-0029](../architecture/decisions/0029-answer-revision-mirrors-question-version-no-locking.md)) — Question과 동일한 revision UI 패턴 재사용 |
| 질문/프로필 비로그인 공개 열람 | 여전히 없음(모두 인증 필요) — [ADR-0013](../architecture/decisions/0013-defer-public-read-access.md)에서 보류 중 |
| 실시간 질문방(Live Chat) | 여전히 없음 — [ADR-0019](../architecture/decisions/0019-quno-flow-and-dashboard-only-no-live-chat.md)로 범위 밖, 착수 시점 WebSocket 인프라 투자 결정 필요 |
| Cluster Merge, Question Fork, 지식 그래프 | **구현됨**(Phase 18, [ADR-0030](../architecture/decisions/0030-cluster-merge-question-fork-graph-data-only.md)) — 그래프는 데이터 API까지만, 시각화 UI는 없음 |
| Organization, Direct Ask | 여전히 없음 — 조직 인증 방식·결제 범위 등 핵심 설계 자체가 없어 착수 시점에 재설계 필요 |

반대로 이 설계서에 없지만 백엔드에는 이미 있는 기능도 있다 — QPR Review(정보 요청/재요청), Cluster/Super Answer/Merge, Question Fork, 지식 그래프 데이터 API, Outdated 표시, Spike Detection, Quno Flow 활동 스트림. 프론트엔드 설계 시 이들을 어느 화면에 어떻게 노출할지도 함께 정해야 한다.
