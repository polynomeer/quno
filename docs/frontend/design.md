# Quno 프론트엔드 UX/디자인 설계

> 원본: [docs/archive/Quno_프론트엔드_상세_설계서.docx](../archive/Quno_프론트엔드_상세_설계서.docx) (2026-08-26 작성, v1.0). 기술 아키텍처는 [architecture.md](architecture.md), 단계별 로드맵은 [roadmap.md](roadmap.md) 참고. 실제 화면 목업은 [quno-design-sample.png](quno-design-sample.png).
>
> 백엔드는 [../architecture/](../architecture/system-architecture.md) 문서 참고. **2026-08-26 작성 당시 이 문서가 가정한 화면 중 투표/댓글/저장/사용자 팔로우/배지/모더레이션/답변 수정 이력은 이후 Phase 11~20에서, Organization/Direct Ask는 Phase 26에서, 실시간 질문방은 Phase 27에서 모두 백엔드+프론트엔드로 구현되었다** — 각 절의 "백엔드 연동 메모"가 최신 상태를 반영한다. 여전히 남은 격차(태그 상세 정보, 태그별 Expertise, `@mention` 자동완성)는 [roadmap.md 7절](roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항) 참고.

## 1. 설계 목표

Quno의 프론트엔드는 단순한 게시판 UI가 아니라, 개발자가 문제를 검색하고 질문하고 검증된 답을 얻는 전체 과정을 최소한의 마찰로 연결하는 지식 인터페이스여야 한다. Stack Overflow의 정보 밀도와 탐색 효율은 유지하되, 질문의 상태 변화와 히스토리를 더 명확하게 보여주는 방향으로 설계한다.

핵심 목표:

- 질문을 작성하기 전에 기존 답을 찾기 쉽도록 한다.
- 질문을 읽는 동안 현재 상태와 신뢰도를 즉시 파악할 수 있게 한다.
- 답변 작성과 코드 공유의 마찰을 최소화한다.
- 수정·수락·투표·Watch 등 상태 변화를 명확한 피드백으로 보여준다.
- 모바일에서도 읽기와 간단한 답변은 가능하되, 복잡한 코드 작성은 데스크톱 경험을 우선한다.
- 검색 엔진에서 질문 상세 페이지가 높은 품질로 노출되도록 공개 페이지를 SEO 친화적으로 구성한다.

## 2. 제품 경험 원칙

| 원칙 | UX 해석 |
|---|---|
| 정보 우선 | 장식보다 제목, 코드, 오류 메시지, 답변, 태그와 상태 정보가 먼저 보여야 한다 |
| 진행 상태 가시성 | 질문이 새 질문인지, 답변 대기인지, 해결되었는지, 최근 수정되었는지 즉시 보인다 |
| 검색 먼저, 질문은 다음 | Ask 진입 이후에도 유사 질문 검색을 계속 노출한다 |
| 행동은 가깝게 | 투표·수락·댓글·Watch·공유는 해당 콘텐츠 주변에서 바로 수행한다 |
| 신뢰를 설명 | 점수만 보여주기보다 수락, 작성자 기여도, 수정 이력, 출처 등 판단 근거를 함께 제공한다 |
| 점진적 복잡성 | 처음에는 핵심 정보만 보이고, 히스토리·통계·편집 diff 등은 필요할 때 펼친다 |

## 3. 핵심 UI 개념 — 살아있는 질문

Quno의 질문 카드는 등록 시점의 정적인 문서가 아니라, 답변과 댓글이 붙고, 수정되고, 수락되며, 다시 새로운 정보로 갱신되는 지식 단위로 표현한다. 프론트엔드에서는 이 생명주기를 작은 상태 신호와 활동 타임라인으로 시각화한다([docs/product/vision.md](../product/vision.md)의 Living Question Card 철학을 화면으로 옮긴 것).

### 3.1 질문 상태 모델 (화면 표현)

| 상태 | 표현 | 의미 | 현재 백엔드 대응 |
|---|---|---|---|
| NEW | New 배지 + 약한 강조 | 막 등록되어 아직 충분한 반응이 없는 질문 | `QuestionStatus.OPEN` |
| ACTIVE | 활동 점/최근 활동 시각 | 답변·댓글·수정이 최근 발생 중 | 파생 표시(최근 리비전/답변 시각) |
| UNANSWERED | 미답변 라벨 | 답변이 아직 없음 | 답변 수 0건으로 파생 |
| ANSWERED | 답변 수 표시 | 답변은 있으나 수락되지 않음 | 답변 수 > 0, `acceptedAnswerId` 없음 |
| SOLVED | 체크 아이콘 + 해결됨 | 수락 답변이 존재 | `QuestionStatus.RESOLVED` |
| UPDATED | Revision n · updated | 질문이 의미 있게 수정됨 | `QuestionStatus.UPDATED` |
| DUPLICATE | 중복 대상 링크 | 기존 질문으로 지식 흐름을 연결 | `ClusterPanel`("같은 문제로 표시")로 구현됨(Phase 6/18, [ADR-0016](../architecture/decisions/0016-manual-duplicate-marking-cluster.md)/[ADR-0030](../architecture/decisions/0030-cluster-merge-question-fork-graph-data-only.md)) — 다만 "중복 대상 링크" 배지가 아니라 클러스터 멤버 목록 UI로 표현되어 원래 목업과 형태는 다름 |
| CLOSED/HIDDEN | 읽기 전용 상태 | 모더레이션 또는 정책상 추가 참여 제한 | 모더레이션(Phase 16/F8, [ADR-0028](../architecture/decisions/0028-moderation-mvp-report-dismiss-hide-only.md))의 Hide는 "읽기 전용으로 전환"이 아니라 **soft-delete(404)**로 구현됨 — 원래 목업이 가정한 "표시는 되지만 참여만 막힌 상태"와는 다르다 |

백엔드에는 이 외에 `NEEDS_INFO`(QPR 정보 요청 중)와 `OUTDATED`(수동 표시) 상태도 있다 — 둘 다 `StatusBadge` 컴포넌트에 이미 반영되어 구현돼 있다.

### 3.2 Living Question Card 구성

```text
┌────────────────────────────────────────────────────────────────────┐
│ [SOLVED] Kotlin Coroutine에서 transaction context가 끊깁니다        │
│ spring kotlin coroutine · updated 12m ago                           │
│                                                                      │
│ WebFlux 환경에서 coroutineScope를 사용하면...                       │
│                                                                      │
│ ▲ 24  💬 5 answers  👁 1.8k  ◉ 18 watchers                          │
│ accepted answer · revision 4 · active today                         │
└────────────────────────────────────────────────────────────────────┘
```

- 카드 전체를 과도하게 컬러링하지 않고 상태 배지와 메타 정보만으로 변화가 느껴지게 한다.
- updated/active 정보는 "언제 만들어졌는가"보다 "지식이 최근에 움직였는가"를 보여주는 역할을 한다.
- revision, accepted answer, watchers를 한 카드에서 모두 강조하지 않고 피드 종류에 따라 필요한 메타 정보만 노출한다.

### 3.3 활동 타임라인

질문 상세의 우측 또는 접이식 패널에는 생성, 수정, 답변, 수락, 재오픈 등 중요한 이벤트만 요약한 타임라인을 제공한다. 댓글 하나하나를 전부 이벤트로 보여주지 않고 질문의 의미가 변한 이벤트를 중심으로 구성한다.

## 4. 사용자 여정

### 4.1 문제 해결 중심 여정

```text
검색 유입
  ↓
질문 상세 ──→ 관련 질문 ──→ 해결
  │
  ├─ 답이 부족함 → Watch
  └─ 해결 안 됨 → Ask with context
       ↓
     유사 질문 확인
       ↓
     질문 등록
       ↓
     답변 / 댓글 / 알림
       ↓
     답변 수락
```

### 4.2 질문 작성 여정

1. Ask 진입 시 제목을 먼저 입력한다.
2. 제목 입력을 기준으로 유사 질문 검색 결과를 즉시 표시한다.
3. 해결되지 않았다면 본문 에디터에서 문제 상황, 시도, 코드, 오류, 기대 결과를 작성한다.
4. 태그를 선택하면 태그별 질문 작성 가이드가 표시된다.
5. Preview에서 최종 렌더링 결과와 누락된 정보 경고를 확인한다.
6. 등록 후 상세 페이지로 이동하며 질문 상태가 NEW로 표시된다.

## 5. 전체 정보 구조 (IA)

| 1차 메뉴 | 2차 영역 | 주요 목적 |
|---|---|---|
| Home | For you / Latest / Unanswered | 관심 질문과 최신 활동 탐색 |
| Questions | Search / Filters / Sort | 전체 질문 탐색 |
| Tags | Tag directory / Tag detail | 기술 분야별 지식 탐색 |
| Ask | Question composer | 질문 작성 |
| Activity | Notifications / Watching / Saved | 내가 관여한 질문 추적 |
| Profile | Contributions / Reputation / Badges | 개인 기여 이력과 전문성 표시 |
| Moderation | Review queue | 권한 있는 사용자용 품질 관리 |

### 5.1 Route Map

```text
/
/questions
/questions/[questionId]/[slug]
/ask
/search?q=
/tags
/tags/[tag]
/users/[handle]
/me/notifications
/me/watching
/me/saved
/me/settings
/moderation/review
```

## 6. 글로벌 애플리케이션 셸

### 6.1 Desktop Header

```text
┌ Quno ──────────────────────────────────────────────────────────────┐
│ [Search questions, tags, errors...]              [Ask] [🔔] [Profile] │
└────────────────────────────────────────────────────────────────────┘
```

- 검색은 Quno에서 가장 중요한 전역 행동이므로 데스크톱 헤더의 가장 넓은 영역을 차지한다.
- Ask 버튼은 항상 노출하되 Search보다 시각적 우선순위가 높지 않게 한다.
- 알림 아이콘은 unread count를 작은 배지로 표시한다.
- 로그인하지 않은 사용자는 Login / Sign up을 노출하고 검색·읽기는 그대로 허용한다.

### 6.2 Desktop Navigation

```text
┌─────────────┬─────────────────────────────────────┬───────────────┐
│ Home        │                                     │ Trending tags │
│ Questions   │           Main Content               │ Watch list    │
│ Tags        │                                     │ Related       │
│ Watching    │                                     │               │
│ Saved       │                                     │               │
└─────────────┴─────────────────────────────────────┴───────────────┘
```

전체 사이트가 항상 3열이어야 하는 것은 아니다. 질문 작성 화면은 우측 가이드 패널을 사용하고, 질문 상세는 본문 중심 2열, 검색 결과는 필터+본문의 2열 등 업무에 맞게 레이아웃을 바꾼다.

## 7. 디자인 시스템

### 7.1 시각 방향

Quno는 개발자 도구와 기술 문서의 정돈된 분위기를 지향한다. 과도한 카드 박스, 그라디언트, 장식적 아이콘을 줄이고 타이포그래피, 간격, 테두리, 상태 배지와 코드 블록으로 정보 위계를 만든다. 실제 톤앤매너 참고는 [quno-design-sample.png](quno-design-sample.png)의 목업(홈/질문 상세/작성/태그/알림/프로필/모바일).

### 7.2 Color Token

| Token | 역할 | 예시 |
|---|---|---|
| `--surface` | 기본 배경 | white / near-black |
| `--surface-subtle` | 보조 영역 | 검색 필터, 메타 영역 |
| `--text-primary` | 본문/제목 | 고대비 |
| `--text-secondary` | 메타 정보 | 중간 대비 |
| `--brand` | 링크·주요 행동 | blue 계열 |
| `--success` | Solved/Accepted | green 계열 |
| `--warning` | Unanswered/Review | amber 계열 |
| `--danger` | 신고/삭제 | red 계열 |
| `--border` | 구획 | 저대비 gray |

실제 구현에서는 light/dark theme 모두 같은 semantic token을 사용하고 theme별 값만 바꾼다.

### 7.3 Typography

| 용도 | 권장 크기 | 특징 |
|---|---|---|
| Question Title | 24–32px | 한 화면의 가장 강한 정보 |
| Page Title | 24px | 목록/태그/프로필 |
| Body | 15–16px | 장문 기술 글의 가독성 우선 |
| Metadata | 12–13px | 점수, 시간, 작성자, 상태 |
| Code | 13–14px | monospace, 충분한 line-height |
| Badge | 11–12px | 짧고 명확한 상태 |

### 7.4 기본 컴포넌트

- Button: primary / secondary / ghost / danger
- Input, SearchInput, Textarea, Select, Combobox
- TagChip, StatusBadge, ReputationBadge
- QuestionCard, AnswerCard, UserMiniCard
- VoteControl, AcceptControl, WatchButton
- Tabs, FilterBar, SortMenu
- Toast, InlineAlert, EmptyState, Skeleton
- Dialog, Drawer, Popover, Tooltip
- MarkdownRenderer, CodeBlock, DiffViewer

## 8. 반응형 레이아웃 시스템

| 구간 | 레이아웃 |
|---|---|
| ≥ 1280px | 좌측 내비게이션 + 메인 + 선택적 우측 보조 패널 |
| 1024–1279px | 좌측 내비게이션 축소 + 메인, 우측 정보는 inline 또는 drawer |
| 768–1023px | 메인 1열, 좌측 메뉴는 접이식 |
| < 768px | 모바일 1열, 하단/상단 내비게이션, 필터는 bottom sheet |

- 본문의 읽기 폭은 지나치게 넓히지 않고 약 760–860px 범위로 제한한다.
- 코드 블록은 모바일에서 가로 스크롤을 허용하며 본문 전체 폭을 깨지 않도록 한다.
- 질문 상세의 투표 컨트롤은 데스크톱에서는 좌측 세로, 모바일에서는 본문 상단/하단의 가로 형태로 전환한다.

## 9. 홈 / 피드

```text
Home
├─ Feed Tabs: For you | Latest | Unanswered
├─ Question Stream
│  ├─ Living Question Card
│  ├─ Living Question Card
│  └─ ...
└─ Side Panel
   ├─ Trending Tags
   ├─ Questions you watch
   └─ Top contributors
```

Question Card 정보 우선순위: 1순위 제목/해결 상태/태그 → 2순위 요약 1~2줄 → 3순위 score/answer count/view count → 4순위 최근 활동과 작성자 → 선택 정보(revision, watch count, bounty 등).

피드 인터랙션:

- 카드 클릭은 질문 상세로 이동한다.
- 태그 클릭은 태그 상세로 이동하며 카드 클릭 이벤트와 분리한다.
- Watch는 카드에서 바로 수행할 수 있지만 로그인 필요 시 인증 UI를 호출한다.
- 무한 스크롤보다 초기에는 cursor 기반 "Load more"를 권장한다 — 검색 엔진과 브라우저 뒤로가기 경험이 더 예측 가능하다.

**백엔드 연동 메모**: `GET /dashboard`(라이트 대시보드, [api-design.md](../architecture/api-design.md#라이트-대시보드-phase-32))가 인기 질문/Ward 업데이트/팔로우 태그 피드/태그 트렌드를 이미 제공한다. "For you/Latest/Unanswered" 탭 중 "For you"는 대시보드의 `followingTagsFeed`+`popularQuestions`로, `Trending Tags` 패널은 `trendingTags`로 채울 수 있다. `GET /flow`(Quno Flow, [api-design.md](../architecture/api-design.md#quno-flow--고급-dashboard-phase-10))는 이 홈 화면과 별개로 "활동 스트림" 성격의 카드 피드를 제공한다.

## 10. 질문 목록·검색

```text
[ spring transaction coroutine______________________ ] [Search]

Filters: [Tags] [Answered] [Date] [Score]      Sort: Relevance ▼
──────────────────────────────────────────────────────────────────
12,430 results

[✓ Solved] Transaction context lost with Kotlin coroutine...
... highlighted snippet ...
#spring #kotlin #transaction   score 24 · 5 answers
```

MVP는 자유 텍스트 + UI 필터를 우선한다. 이후 숙련 사용자를 위해 `tag:kotlin is:unanswered score:5` 같은 검색 연산자를 지원할 수 있다. UI 필터 조작 결과를 URL query string에 반영하여 공유와 뒤로가기를 보장한다.

검색 결과 상태:

- 검색어 자동완성: 질문 제목, 태그, 자주 검색된 오류 문자열
- No result: 검색어 완화, 태그 제거, 새 질문 작성 CTA
- Loading: 결과 리스트와 동일한 형태의 skeleton
- Error: 기존 결과가 있으면 유지하고 재시도 배너만 표시

**백엔드 연동 메모**: `GET /search?q=&limit=&sort=`([api-design.md](../architecture/api-design.md#검색·관련-질문-구현-phase-29))가 PostgreSQL 전문검색 기반으로 존재하고, `sort=relevance|score`(Phase 20/F11, [ADR-0032](../architecture/decisions/0032-vote-score-search-sort-dashboard-reputation.md))로 Score 정렬도 구현되어 있다 — `/questions` 검색 결과 화면의 Sort 드롭다운이 이를 그대로 쓴다. **Answered/Date 필터는 여전히 없다**([ADR-0022](../architecture/decisions/0022-search-filters-client-side-tag-and-status-only.md)) — 응답에 `answerCount`/`createdAt`이 없어 클라이언트 근사도 불가능하다.

## 11. 질문 작성 플로우

```text
┌──────────────────────────────────────┬────────────────────────────┐
│ Ask a question                        │ Question checklist         │
│                                        │ ✓ Specific title           │
│ Title [__________________________]    │ ○ What did you try?        │
│                                        │ ○ Expected vs actual       │
│ Similar questions                     │ ○ Minimal reproducible     │
│ - ...                                 │                             │
│                                        │ Tag guidance                │
│ Markdown editor                       │                             │
│ [ Write | Preview ]                   │                             │
│                                        │                             │
│ Tags [ kotlin ][ spring ][ + ]        │                             │
│ [Post]                                │                             │
└──────────────────────────────────────┴────────────────────────────┘
```

| 단계 | UX |
|---|---|
| Title | 제목 입력 후 debounce하여 유사 질문을 조회한다 |
| Similar Questions | 3–5개를 즉시 보여주며 "이 질문으로 해결됨" 선택 시 작성 흐름을 종료할 수 있다 |
| Body | Write/Preview 탭, 코드 블록 삽입, 붙여넣기 편의 기능을 제공한다 |
| Tags | 최대 개수 제한, 자동완성, 태그 설명 및 질문 수를 보여준다 |
| Quality Check | 필수 정보 부족은 blocking error가 아니라 우선 suggestion으로 안내한다 |
| Submit | 중복 전송 방지, 진행 상태 표시, 성공 후 질문 상세로 이동한다 |

**Auto-save**: 질문 초안은 브라우저 local storage 또는 서버 draft API에 주기적으로 저장할 수 있다. MVP에서는 local storage로 시작하고, 로그인 사용자의 기기 간 초안 동기화가 필요할 때 서버 저장을 도입한다.

**백엔드 연동 메모**: `POST /questions`(`tags: string[]` 포함)가 이미 이 폼 전체를 지원한다. "유사 질문 검색"은 `GET /search` 또는 `GET /questions/{id}/related`로 구현 가능(단, related는 기존 질문 기준이라 작성 중인 제목으로는 `GET /search` 재사용이 더 적합).

## 12. 질문 상세 페이지

```text
┌────────────────────────────────────────────────────────────────────┐
│ Kotlin coroutine에서 @Transactional이 유지되지 않습니다              │
│ [SOLVED] asked 2d ago · updated 18m ago · viewed 1.8k                │
│ #kotlin #spring #coroutine                                           │
├───────┬───────────────────────────────────────────────┬────────────┤
│  ▲    │ Question body                                  │ Activity   │
│  24   │                                                 │ revision 4 │
│  ▼    │ ```kotlin                                       │ 5 answers  │
│       │ ...                                             │ 18 watch   │
│ [☆]   │ ```                                             │            │
│       │                                                 │ Related    │
│       │ comments ...                                    │ questions  │
├───────┴───────────────────────────────────────────────┴────────────┤
│ 5 Answers   sort: score ▼                                            │
│ ✓ Accepted Answer                                                    │
│ ...                                                                  │
└────────────────────────────────────────────────────────────────────┘
```

### 12.1 헤더

- 제목은 SEO H1이며 해결 상태 배지는 제목 앞 또는 메타 라인에 둔다.
- asked/updated/viewed를 표시하되 updated가 최근이면 더 강하게 노출한다.
- 태그는 제목 바로 아래에서 탐색 진입점 역할을 한다.
- 편집·공유·신고 등 낮은 빈도의 행동은 overflow menu로 묶는다.

### 12.2 본문 왼쪽 Action Rail

- Upvote / score / Downvote
- Watch 또는 Save
- 작성자에게만 Edit/Delete
- 답변 수락은 질문이 아니라 답변 카드에 표시

### 12.3 Revision UX

질문이 수정되었으면 `edited 18m ago · revision 4`를 표시하고 클릭 시 revision history로 이동한다. History에서는 전체 버전 목록과 작성자·시간·변경 설명을 제공하고, 두 버전을 선택해 diff를 볼 수 있게 설계한다.

**백엔드 연동 메모**: `GET /questions/{id}`, `GET /questions/{id}/versions`, `GET /questions/{id}/versions/{version}/diff`가 이 전체 흐름(리비전 히스토리 + diff)을 지원한다. Upvote/score/Downvote는 Vote(Phase 11/F6, [ADR-0023](../architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md))의 `VoteControl`로 구현되어 있다. Watch(`POST/DELETE /questions/{id}/watch`)와 Save(Phase 13/F7, [ADR-0025](../architecture/decisions/0025-save-as-separate-side-aggregate-from-watch.md)) 모두 구현되어 있다.

## 13. 답변 경험

### 13.1 Answer Card

- 답변마다 질문과 동일한 VoteControl을 사용한다.
- 수락 답변은 연한 success 강조와 Accepted 라벨을 사용하며 다른 답변을 압도할 정도로 배경색을 칠하지 않는다.
- 작성자 정보는 답변 하단에 표시해 본문 읽기를 방해하지 않게 한다.
- 수정 이력은 질문과 동일한 revision UI 패턴을 재사용한다.

### 13.2 답변 정렬

| 정렬 | 정책 |
|---|---|
| Best | 수락 답변 우선 + 점수/품질 |
| Score | 점수 내림차순 |
| Newest | 최신 답변 우선 |
| Oldest | 토론의 시간 순서 확인용 |

### 13.3 Answer Composer

질문 본문 하단에 답변 에디터를 배치한다. 질문 작성 에디터와 같은 MarkdownEditor를 재사용하되 제목·태그가 없고 답변 품질 가이드만 다르게 제공한다. 긴 답변 작성 중에는 draft를 자동 저장한다.

**백엔드 연동 메모**: `POST /questions/{id}/answers`, `GET /questions/{id}/answers`, `POST /answers/{id}/accept`가 있다. 답변에는 `targetVersionNumber`/`isStale`([api-design.md](../architecture/api-design.md#답변–질문버전-연결-phase-51))이 있어 "이 답변은 이전 버전을 대상으로 작성됨" 배지를 보여준다. **답변 수정 이력(revision)은 Phase 17/F8([ADR-0029](../architecture/decisions/0029-answer-revision-mirrors-question-version-no-locking.md))로 구현됐다** — `/answers/{answerId}/versions` 페이지가 질문의 revision UI 패턴을 그대로 재사용한다. 투표(`score`)도 Phase 11/F6로 구현되어 각 `AnswerCard`에 표시된다 — 다만 **답변 목록의 Sort UI는 아직 Best/Newest/Oldest 3종뿐이고 "Score" 옵션은 없다**(`AnswerSort` 타입 참고) — 순수 점수 내림차순 정렬이 필요해지면 프론트에서 추가 구현이 필요하다(백엔드는 이미 각 답변의 `score`를 응답에 담고 있어 데이터 자체는 있음).

## 14. 댓글·토론 UX

- 댓글은 답변을 대체하는 공간이 아니라 질문/답변의 clarification을 위한 보조 채널이다.
- 초기에는 상위 일부 댓글만 노출하고 "Show N more comments"로 펼친다.
- `@mention` 자동완성과 작성자/질문자 표시를 제공한다.
- 댓글 투표가 필요하다면 본문 답변 점수와 혼동되지 않도록 작은 reaction 형태로 분리한다.
- 삭제된 댓글은 스레드 맥락을 깨지 않는 수준에서 tombstone 표시를 고려한다.

**백엔드 연동 메모**: Comment는 Phase 12(평면 목록·soft-delete tombstone, [ADR-0024](../architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md))에 이어 Phase 19([ADR-0031](../architecture/decisions/0031-comment-thread-mention-edit-history.md))에서 1단계 답글·수정 이력·`@mention` 알림까지 구현됐다(Frontend Phase 6/10, `CommentSection`/`CommentItem`). QPR `ReviewRequest`는 여전히 별개 워크플로("정보 요청 → 리비전 → 재요청")로 남아 있다. 이 문서가 원했던 것과 실제 구현이 다른 지점: **"Show N more comments" 접기/펼치기는 없다**(전부 표시), **`@mention` 자동완성은 없다**(닉네임 정확 일치 파싱만, 사용자 검색 API가 없어서), **댓글 투표(reaction)는 없다**, **답글은 최대 1단계까지만**(무한 스레드 아님).

## 15. 태그 경험

### 15.1 Tag Directory

```text
Tags
[ Search tags... ]

Kotlin      18.2k questions   +4.2% this month
Spring      31.5k questions   avg first answer 42m
Kafka       8.4k questions    612 followers
Redis       7.9k questions    ...
```

### 15.2 Tag Detail

- 태그 설명 / 공식 문서 링크 / Watch 버튼
- Latest, Unanswered, Top 탭
- 최근 30일 질문·답변 활동 요약
- 태그 상위 기여자
- 관련 태그
- 태그 작성 가이드

**백엔드 연동 메모**: `GET /tags`(검색), `POST/DELETE /tags/{id}/follow`가 있다. "태그 설명/공식 문서 링크", "태그 상위 기여자", "관련 태그" 같은 풍부한 태그 상세 정보는 `Tag` 도메인이 `name`/`slug`만 가지고 있어 대부분 새로 만들어야 한다.

## 16. 사용자 프로필

프로필은 단순 SNS 프로필보다 "어떤 기술 질문에 기여했는가"를 보여주는 지식 포트폴리오에 가깝게 설계한다.

| 영역 | 내용 |
|---|---|
| Header | handle, bio, location/links(선택), reputation |
| Expertise | 태그별 답변 수, 수락률, 받은 vote 등 |
| Contributions | Questions / Answers / Edits / Activity |
| Badges | 배지 및 획득 시점 |
| Impact | 누적 views, accepted answers 등 |

태그 Expertise 시각화는 레이더 차트보다 정렬된 bar/list를 권장한다. 예: `Kotlin · 42 answers · 18 accepted · +320 score`. 정확한 수치와 클릭 가능한 태그가 시각적 장식보다 유용하다.

**백엔드 연동 메모**: `GET /users/{id}/profile`(작성 질문/답변/팔로우 태그)과 `GET /users/{id}/reputation`(활동 기반 평판 점수, [api-design.md](../architecture/api-design.md#전문가-평판-phase-9))이 있다. Badge는 Phase 15/F7([ADR-0027](../architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md))로 구현되어 프로필에 노출된다(획득 시점은 저장하지 않는 계산형 읽기 모델). **태그별 세분화된 Expertise(태그별 답변 수/수락률/vote)는 여전히 없다** — 평판은 사용자 전체 합산 점수 하나뿐이다.

## 17. 알림 센터

분류: Responses(내 질문의 답변/댓글) · Mentions(`@mention`) · Watching(Watch 중인 질문의 중요한 변화) · Reputation(답변 수락, 배지 등) · Moderation(편집/신고 처리).

알림 UX 규칙:

- 읽지 않은 알림은 행 전체 배경보다 작은 unread dot로 구분한다.
- 동일 질문에서 짧은 시간에 발생한 반복 이벤트는 묶을 수 있다.
- 알림 클릭은 단순 질문 페이지가 아니라 해당 답변/댓글 anchor로 이동한다.
- Mark all as read와 유형별 설정을 제공한다.

**백엔드 연동 메모**: `GET /me/notifications`, `POST /me/notifications/mark-read`가 있다. 알림 타입은 이제 10종이다([domain-model.md](../architecture/domain-model.md#domain-events)) — 원래 6종(`QUESTION_REVISION`/`NEW_ANSWER`/`ANSWER_ACCEPTED`/`REVIEW_REQUESTED`/`REVIEW_RE_REQUESTED`/`QUESTION_OUTDATED`)에 `NEW_COMMENT`/`CONTENT_HIDDEN`(모더레이션 통보)/`ANSWER_REVISION`/`MENTIONED_IN_COMMENT`(Phase 19의 실제 `@mention` 알림)가 추가됐다. **Reputation 타입(배지 획득 알림)은 여전히 없다** — Badge는 계산형 읽기 모델이라 획득 이벤트 자체를 발행하지 않는다([ADR-0027](../architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md)). "묶어서 표시"할 그룹핑 로직은 여전히 프론트 책임이다(백엔드는 개별 알림만 반환).

## 18. Watch·북마크·팔로우

| 기능 | 의미 | UI |
|---|---|---|
| Watch Question | 질문의 후속 변화를 알림으로 받음 | bell/eye 계열 아이콘 + Watching 상태 |
| Save | 나중에 다시 읽을 개인 저장 | bookmark 아이콘 |
| Follow Tag | 해당 태그 질문을 피드에 반영 | Follow 버튼 |
| Follow User | 특정 사용자의 공개 활동 관심 | 후속 버전 기능 |

> **Watch와 Save를 분리**: Watch는 "변화를 추적"하는 행동이고 Save는 "다시 읽기 위해 보관"하는 행동이다. 하나의 북마크 기능으로 합치면 알림 기대가 모호해지므로 UI와 데이터 모델을 분리하는 편이 좋다.

**백엔드 연동 메모**: Watch Question(`POST/DELETE /questions/{id}/watch`), Follow Tag(`POST/DELETE /tags/{id}/follow`), Save(Phase 13/F7, [ADR-0025](../architecture/decisions/0025-save-as-separate-side-aggregate-from-watch.md)), Follow User(Phase 14/F7, [ADR-0026](../architecture/decisions/0026-follow-user-relationship-only-no-activity-feed.md)) 모두 구현되어 있다. **Follow User는 관계 기록·조회만 지원한다** — "특정 사용자의 공개 활동 관심"이 뜻하는 활동 피드/알림은 의도적으로 범위 밖이라 팔로우해도 피드에 아무 변화가 없다.

## 19. 명성·배지·기여도

- Reputation은 헤더 프로필과 사용자 페이지에서 숫자로 보여주되 화면 곳곳에 과도하게 반복하지 않는다.
- 답변의 신뢰도는 작성자 reputation 하나로 결정되는 것처럼 표현하지 않는다. 수락·점수·내용·수정 이력 등을 함께 보여준다.
- 배지 획득은 작은 toast/notification으로 알려주며 질문 읽기 흐름을 막는 modal은 사용하지 않는다.
- 태그별 contribution은 프로필에서 전문성을 보여주는 주요 요소로 사용한다.

**백엔드 연동 메모**: `GET /users/{id}/reputation`의 점수 산식은 [ADR-0018](../architecture/decisions/0018-simple-reputation-score-only.md)(질문 수·답변 수·채택 답변 수·Super Answer 지정 횟수 가중합)에 Phase 20에서 순 투표 점수 항이 추가됐다([ADR-0032](../architecture/decisions/0032-vote-score-search-sort-dashboard-reputation.md)). Badge(`GET /users/{id}/badges`)는 Phase 15로 구현됐다 — 6종(Bronze/Silver/Gold) 고정 카탈로그, 영속화 없는 계산형 읽기 모델이라 "획득 시점"은 없다([ADR-0027](../architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md)) — 위 문단의 "toast로 알려준다"는 UX는 획득 이벤트가 없어 구현 불가하다(매 조회 시 조건 재계산만 가능).

## 20. 모더레이션 UI

```text
Moderation / Review
Filters: [Spam] [Duplicate] [Low quality] [All]

┌ Flagged question ───────────────────────────────────────────────┐
│ reason: duplicate · 3 reports                                    │
│ title / excerpt                                                  │
│ Suggested duplicate: ...                                         │
│ [Keep] [Close as duplicate] [Edit] [Hide]                        │
└─────────────────────────────────────────────────────────────────┘
```

안전한 조치 UX:

- 삭제/정지처럼 복구 비용이 높은 행동은 confirmation과 reason을 요구한다.
- 중복 처리 시 canonical question 검색을 같은 화면에서 수행한다.
- 모더레이션 결정의 근거와 처리자를 audit trail로 확인할 수 있게 한다.
- 사용자에게 표시되는 사유와 내부 운영 메모를 구분한다.

**백엔드 연동 메모**: 신고/모더레이션 큐/역할 기반 권한은 Phase 16/F8([ADR-0028](../architecture/decisions/0028-moderation-mvp-report-dismiss-hide-only.md))로 구현됐지만, 위 목업과 실제 구현은 상당히 다르다 — 실제로 존재하는 액션은 **Keep(Dismiss)과 Hide 두 가지뿐**이다. **"Close as duplicate"/"Edit" 액션은 없다**(중복 처리는 기존 Cluster 기능 재사용을 권장하되 모더레이션 액션에 통합되지 않았고, 콘텐츠를 모더레이터가 직접 수정하는 기능 자체가 없다). **Suggested duplicate 검색, confirmation 다이얼로그, 신고 사유별 필터([Spam]/[Duplicate]/[Low quality] 탭)도 없다** — 실제 큐는 사유를 텍스트로만 보여주고 필터 없이 전체 PENDING 목록만 반환한다. Role 부여/회수 API도 없어 모더레이터 지정은 DB에서 직접 해야 한다. `/moderation` 페이지 자체에도 별도 nav 링크가 없다(모더레이터가 URL을 직접 알아야 함, 의도적 설계).

## 21. 빈 상태·오류·로딩 UX

| 상황 | 표현 |
|---|---|
| 질문 없음 | "아직 질문이 없습니다" + 첫 질문 작성 또는 필터 해제 CTA |
| 검색 결과 없음 | 검색어/태그 완화 제안 + Ask CTA |
| 네트워크 오류 | 재시도 버튼, 이전 캐시가 있으면 유지 |
| 권한 없음 | 로그인/권한 필요 사유와 다음 행동 안내 |
| 삭제/숨김 콘텐츠 | 404처럼 사라지게 하기보다 가능한 범위에서 상태 설명 |
| Loading | 콘텐츠 구조와 동일한 skeleton, 전체 화면 spinner 최소화 |

**Optimistic Update 대상**: Watch/Save 토글, 투표(서버 실패 시 원복), 알림 읽음 처리. 질문/답변 작성, 답변 수락, 삭제처럼 의미가 큰 변경은 서버 성공을 확인한 후 상태를 확정한다.

## 22. 모바일 UX

- 헤더는 로고 + Search + Profile 수준으로 축소하고 나머지는 메뉴 drawer로 이동한다.
- 질문 상세에서 투표 컨트롤은 세로 rail 대신 가로 action row로 전환한다.
- 본문 폭을 최우선하며 우측 related/activity 패널은 본문 아래 accordion으로 이동한다.
- 태그/필터 선택은 bottom sheet를 사용한다.
- 답변 에디터는 full-screen compose mode를 제공하면 모바일 작성성이 좋아진다.
- 코드 블록은 가로 스크롤과 Copy 버튼을 유지한다.

## 23. 접근성

- 모든 주요 인터랙션은 키보드만으로 수행 가능해야 한다.
- 투표 버튼은 아이콘만 사용하지 않고 `aria-label`을 제공한다.
- Accepted/Solved 상태를 색상만으로 구분하지 않고 텍스트/아이콘을 함께 사용한다.
- 검색 자동완성, Combobox는 WAI-ARIA 패턴을 따른다.
- 코드 블록의 복사 버튼과 언어 라벨에 접근 가능한 이름을 제공한다.
- Focus ring을 제거하지 않고 디자인 토큰으로 명확하게 표현한다.
- 다크 모드에서도 WCAG 대비 기준을 충족하도록 semantic color token을 검증한다.
