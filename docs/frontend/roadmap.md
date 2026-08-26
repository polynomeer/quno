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

> **원본 로드맵과 백엔드 현황의 차이**: Phase 2의 Comments, Phase 3의 Vote/Save, Phase 6의 Moderation은 대응하는 백엔드 기능이 없다([design.md](design.md)의 각 화면 "백엔드 연동 메모" 참고). 실제 착수 시점에 이 Phase들의 범위를 백엔드 확장과 함께 다시 정하거나, 해당 기능 없이 진행 가능한 부분만 먼저 구현해야 한다.

### 3.1 가장 먼저 구현할 Vertical Slice

1. QuestionCard + 질문 목록
2. QuestionDetail + AnswerCard
3. 검색 query + 태그 필터
4. Ask form + MarkdownEditor
5. Answer form
6. Vote/Accept *(Vote는 백엔드 미구현 — Accept만 우선 가능)*
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
| P0 | Answer Composer + Vote + Accept | Q&A 해결 루프 완성 *(Vote 제외 — 미구현)* |
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
- [ ] VoteControl / WatchButton / StatusBadge *(VoteControl은 백엔드 확정 전까지 UI 스텁)*
- [ ] AnswerCard
- [ ] SearchInput + FilterBar
- [ ] QuestionEditor + TagPicker
- [ ] API Client + Error model + Query keys
- [ ] Auth guard와 redirectTo
- [ ] Playwright 핵심 플로우 테스트

## 7. 백엔드 격차 요약과 착수 전 확인 사항

이 설계서는 Stack Overflow형 서비스를 전제로 투표(Vote)·댓글(Comment)·배지(Badge)·모더레이션을 포함하지만, 지금까지 구축한 Quno 백엔드([PLAN.md](../../PLAN.md) 참고)는 다른 방향(리비전·Ward·QPR·Cluster/Super Answer·평판 점수·Quno Flow)으로 "살아있는 질문" 철학을 구현해왔다. 실제 프론트엔드 작업을 시작하기 전에 아래를 확인한다.

| 이 설계서가 전제하는 기능 | 현재 백엔드 상태 |
|---|---|
| Upvote/Downvote, score 정렬 | 없음 — Question/Answer 모두 투표 필드 없음 |
| Comment(질문/답변 댓글) | 없음 — QPR ReviewRequest로 "정보 요청"만 모델링됨(범용 댓글과 다름) |
| Save(북마크, Watch와 별개) | 없음 — Watch만 있음 |
| Follow User | 없음 — Follow Tag만 있음 |
| Badge(배지) | 없음 — 평판은 합산 점수(`UserReputation`) 하나뿐 |
| 태그별 세분화 Expertise | 없음 — 태그 자체에 설명/문서 링크/상위 기여자 개념 없음 |
| 모더레이션(신고, 리뷰 큐, 역할 기반 권한) | 없음 |
| 답변(Answer) 수정 이력 | 없음 — Question만 리비전을 가짐 |
| 질문/프로필 비로그인 공개 열람 | 없음(모두 인증 필요) — [ADR-0013](../architecture/decisions/0013-defer-public-read-access.md)에서 보류 중 |
| 실시간 질문방(Live Chat) | 없음 — [ADR-0019](../architecture/decisions/0019-quno-flow-and-dashboard-only-no-live-chat.md)로 범위 밖 |

반대로 이 설계서에 없지만 백엔드에는 이미 있는 기능도 있다 — QPR Review(정보 요청/재요청), Cluster/Super Answer, Outdated 표시, Spike Detection, Quno Flow 활동 스트림. 프론트엔드 설계 시 이들을 어느 화면에 어떻게 노출할지도 함께 정해야 한다.
