# Quno 프론트엔드 기술 아키텍처

> 원본: [docs/archive/Quno_프론트엔드_상세_설계서.docx](../archive/Quno_프론트엔드_상세_설계서.docx). UX/화면 설계는 [design.md](design.md), 단계별 로드맵은 [roadmap.md](roadmap.md) 참고. 백엔드 API는 [../architecture/api-design.md](../architecture/api-design.md).

## 24. 권장 프론트엔드 스택

| 영역 | 권장 기술 | 역할 |
|---|---|---|
| Framework | React + Next.js + TypeScript | SSR/SEO가 중요한 공개 Q&A 페이지와 클라이언트 상호작용을 함께 처리 |
| Server State | TanStack Query | 질문·답변·댓글·알림 등 API 상태의 캐시, 재검증, optimistic update |
| Local UI State | Zustand 또는 최소 Context | 에디터 UI, 패널 열림 상태, 임시 필터 등 서버와 무관한 상태만 관리 |
| Form | React Hook Form + Zod | 질문/답변 작성 폼의 입력 상태와 클라이언트 검증 |
| Styling | CSS Variables + Tailwind CSS 또는 CSS Modules | 디자인 토큰과 컴포넌트 스타일을 일관되게 유지 |
| Editor | Markdown Editor + syntax highlighting | 개발자 질문에 최적화된 Markdown, 코드 블록, 미리보기 |
| Testing | Vitest + Testing Library + Playwright | 단위/컴포넌트/E2E 테스트를 계층별로 구성 |

**구조 원칙**: 백엔드와 마찬가지로 프론트엔드도 처음부터 여러 애플리케이션이나 마이크로 프론트엔드로 분리하지 않는다. 하나의 Next.js 애플리케이션 안에서 feature 단위로 경계를 나눈다 — 백엔드의 "모듈형 모놀리스" 결정([ADR-0001](../architecture/decisions/0001-tech-stack.md))과 같은 방향이다.

## 25. Runtime 구조

```text
Browser
  |
  v
Next.js Application
  ├─ Server-rendered public pages
  │    ├─ question detail
  │    ├─ tag detail
  │    └─ public profile
  │
  ├─ Client interaction
  │    ├─ vote / watch / save
  │    ├─ editor
  │    ├─ notifications
  │    └─ filters
  │
  └─ API Client
       |
       v
Kotlin Spring Boot /api/v1
```

### 25.1 Server Component와 Client Component 경계

- 검색 유입이 많은 공개 질문 본문은 서버 렌더링을 우선한다.
- VoteControl, WatchButton, MarkdownEditor처럼 상호작용이 필요한 작은 단위만 client component로 분리한다.
- 페이지 전체를 무조건 client component로 만들지 않는다.
- 로그인 개인화 피드는 client fetching 또는 server prefetch + hydration 전략을 선택할 수 있다.

## 26. 디렉터리·코드 구조

```text
src/
├─ app/
│  ├─ (public)/
│  │  ├─ page.tsx
│  │  ├─ questions/
│  │  ├─ tags/
│  │  └─ users/
│  ├─ (auth)/
│  ├─ ask/
│  └─ me/
├─ features/
│  ├─ auth/
│  ├─ question/
│  ├─ answer/
│  ├─ comment/
│  ├─ vote/
│  ├─ watch/
│  ├─ search/
│  ├─ notification/
│  └─ moderation/
├─ entities/
│  ├─ user/
│  ├─ tag/
│  └─ reputation/
├─ shared/
│  ├─ api/
│  ├─ ui/
│  ├─ hooks/
│  ├─ lib/
│  ├─ types/
│  └─ styles/
└─ widgets/
   ├─ app-header/
   ├─ side-navigation/
   ├─ question-feed/
   └─ activity-panel/
```

폴더명 자체보다 의존 방향이 중요하다. `shared`는 특정 도메인을 모르고, `feature`는 자신의 use case를 소유하며, 페이지는 feature와 widget을 조합한다. 모든 API 호출을 page 컴포넌트에 직접 작성하지 않는다.

`comment`, `moderation` feature 디렉터리는 원본 설계에 포함돼 있지만, 대응하는 백엔드 기능이 아직 없다([design.md](design.md) 14장/20장 참고) — 실제 착수 시점까지 빈 디렉터리로 미리 만들어두지 않는다.

### 26.1 Question Feature 예시

```text
features/question/
├─ api/
│  ├─ question.api.ts
│  └─ question.keys.ts
├─ model/
│  ├─ question.types.ts
│  └─ question.schema.ts
├─ ui/
│  ├─ QuestionCard.tsx
│  ├─ QuestionHeader.tsx
│  ├─ QuestionBody.tsx
│  └─ QuestionEditor.tsx
└─ hooks/
   ├─ useQuestion.ts
   └─ useCreateQuestion.ts
```

## 27. 서버 상태와 클라이언트 상태

| 상태 | 소유자 | 예시 |
|---|---|---|
| Server State | TanStack Query / Server fetch | 질문, 답변, 댓글, 태그, 알림 |
| URL State | Router/Search Params | 검색어, 태그, 정렬, pagination cursor |
| Form State | React Hook Form | 질문 제목/본문/태그, 답변 본문 |
| UI State | Component/Zustand | drawer, editor mode, local panel 상태 |
| Session | Auth layer | 로그인 사용자, 권한, token lifecycle |

### 27.1 Query Key 규칙

```text
questionKeys.all
questionKeys.detail(questionId)
questionKeys.list(filters)
questionKeys.answers(questionId, sort)
notificationKeys.list(filter)
tagKeys.detail(tagName)
```

문자열을 각 컴포넌트에 직접 작성하지 않고 key factory를 사용해 invalidation 범위를 예측 가능하게 만든다.

## 28. API 통신 계층

### 28.1 공통 API Client

```text
shared/api/
├─ http-client.ts
├─ api-error.ts
├─ auth-interceptor.ts
└─ generated/   // OpenAPI 기반 타입/클라이언트 선택 가능
```

### 28.2 Response Handling

| 상태 | 처리 |
|---|---|
| 2xx | typed response 반환 |
| 400 | field validation error를 form에 매핑 |
| 401 | 세션 갱신 또는 로그인 유도 |
| 403 | 권한 부족 메시지 |
| 404 | 콘텐츠 상태에 맞는 not-found 화면 |
| 409 | 투표/수락 등의 충돌을 사용자에게 설명 |
| 429 | 레이트 리밋 남은 시간/재시도 안내 |
| 5xx | 공통 error boundary + 재시도 |

백엔드의 `ErrorResponse`(`{code, message}`, [GlobalExceptionHandler](../architecture/api-design.md))는 이미 `CONFLICT`/`FORBIDDEN`/`NOT_FOUND`/`UNAUTHORIZED`/`VALIDATION_ERROR`/`BAD_REQUEST` 코드를 구분해서 내려주므로, `api-error.ts`는 HTTP 상태코드보다 이 `code` 필드를 기준으로 분기하는 편이 더 정확하다.

### 28.3 DTO와 View Model

백엔드 응답 타입을 화면 곳곳에서 그대로 사용하기보다 필요한 경우 view model로 변환한다. 예를 들어 질문 상세 응답의 timestamp를 표시 문자열로 변환하는 작업은 프레젠테이션 계층에서 수행하되 서버 원본 값을 잃지 않는다.

## 29. 인증·인가 UX

- 읽기와 검색은 비로그인 사용자에게 공개한다.
- 질문·답변·투표·Watch 수행 시 로그인이 필요하다.
- 로그인이 필요한 버튼을 무조건 숨기지 말고 클릭 시 "로그인하면 답변할 수 있습니다" 흐름을 제공한다.
- 로그인 후 사용자가 원래 수행하려던 질문/답변 위치로 돌아오게 `redirectTo`를 보존한다.
- 화면에서 권한을 숨기더라도 실제 권한 검증은 반드시 백엔드가 최종 책임을 진다.

**백엔드 연동 메모**: JWT Access/Refresh Token 방식([ADR-0003](../architecture/decisions/0003-stateless-jwt-auth.md)) — `POST /auth/signup`/`login`/`refresh`. Access Token 만료가 짧으므로(15~30분) `auth-interceptor.ts`에서 401 수신 시 Refresh Token으로 자동 재발급 후 원 요청을 재시도하는 흐름이 필요하다. 질문/프로필 조회 자체도 현재는 인증을 요구한다([ADR-0013](../architecture/decisions/0013-defer-public-read-access.md) — 비로그인 공개 열람 여부는 아직 보류 중) — "읽기는 비로그인 공개"라는 이 설계서의 전제와 어긋나므로, 실제 착수 전에 이 결정을 재검토해야 한다.

## 30. 실시간 업데이트

| 기능 | MVP | 후속 |
|---|---|---|
| 새 답변 | 새로고침/Query invalidate | SSE/WebSocket 알림 |
| 댓글 | 수동 갱신 | 실시간 append |
| 투표 점수 | Optimistic + API 결과 | 필요 시 실시간 동기화 |
| 알림 badge | 주기 polling | SSE/WebSocket |
| 질문 활동 상태 | API timestamp | 실시간 activity pulse |

Quno는 실시간 채팅 서비스가 아니므로 처음부터 모든 이벤트를 WebSocket으로 연결할 필요가 없다. 알림과 새 답변처럼 사용자 가치가 큰 부분부터 도입한다. 백엔드의 실시간 질문방(Live Chat)은 [ADR-0019](../architecture/decisions/0019-quno-flow-and-dashboard-only-no-live-chat.md)에 따라 아직 착수하지 않았다 — MVP는 이 표의 왼쪽 열(polling/새로고침)만으로 구현한다.

## 31. Markdown·코드 렌더링

- Markdown renderer는 서버/클라이언트에서 동일한 sanitization 정책을 적용한다.
- HTML 직접 입력은 제한하거나 안전한 subset만 허용한다.
- 코드 블록에는 언어 라벨, syntax highlighting, Copy 버튼을 제공한다.
- 긴 코드 블록은 접기보다 기본 노출을 우선하되 지나치게 긴 경우 "Show more"를 고려한다.
- 라인 번호는 모든 코드에 강제하지 않고 오류 위치를 논의해야 하는 경우 옵션으로 제공한다.
- 외부 링크는 `rel` 속성과 안전한 `target` 정책을 적용한다.

## 32. 성능 전략

### 32.1 성능 목표

| 대상 | 목표 방향 |
|---|---|
| Question Detail | 검색 유입 핵심 페이지이므로 SSR/캐싱과 최소 JS를 우선 |
| Home Feed | 초기 데이터 빠른 표시 + 이후 pagination |
| Editor | 동적 import로 무거운 편집기 번들을 필요할 때만 로드 |
| Syntax Highlighting | 필요 언어 중심 lazy load 고려 |
| Images | 최적화, lazy load, 크기 예약으로 CLS 방지 |

### 32.2 캐시 전략

- 공개 질문 상세: 서버 캐시/재검증 + 수정 이벤트 발생 시 invalidate 고려
- 태그 설명: 상대적으로 긴 TTL
- 개인 알림/Watch: 사용자별 private no-store 또는 짧은 client cache
- 검색 결과: query string별 짧은 client cache

### 32.3 Bundle

- Markdown editor, diff viewer, chart는 필요 화면에서만 dynamic import
- 공통 icon library 전체 import 금지
- 페이지 단위 route splitting 유지
- 성능 예산을 CI에서 추적할 수 있도록 Lighthouse 또는 Web Vitals 측정

## 33. SEO·공유

- 질문 상세 URL: `/questions/{id}/{slug}` 형태를 권장한다.
- title, description, canonical, Open Graph를 질문별로 생성한다.
- Question/Answer 구조화 데이터를 적용할 수 있도록 백엔드의 수락 답변과 점수 정보를 명확히 제공한다(단, "점수"는 현재 투표 기능이 없어 accepted 여부만 반영 가능).
- 삭제/중복 질문의 canonical/redirect 정책을 설계한다.
- 태그 상세 페이지에도 title/description과 pagination canonical 정책을 둔다.
- 공유 링크는 답변 anchor까지 포함할 수 있다.

## 34. 분석 이벤트와 제품 지표

| Event | 주요 속성 | 의미 |
|---|---|---|
| `search_submitted` | query, tags, source | 검색 수요 파악 |
| `search_result_clicked` | rank, questionId | 검색 품질 |
| `ask_started` | source | 질문 작성 진입 |
| `similar_question_clicked` | rank, questionId | 중복 방지 효과 |
| `question_posted` | tags, qualityHints | 질문 생성 |
| `answer_posted` | questionId | 답변 공급 |
| `vote_cast` | targetType, value | 콘텐츠 평가 |
| `answer_accepted` | questionId, answerId | 문제 해결 |
| `watch_toggled` | questionId, enabled | 지속 관심 |
| `notification_opened` | type | 알림 효율 |

개인정보나 질문 본문 자체를 분석 이벤트로 그대로 전송하지 않는다. 검색어도 민감 정보가 포함될 수 있으므로 보관 정책을 별도로 검토한다.

이 이벤트들은 [../product/mvp-scope.md](../product/mvp-scope.md#성공-지표)의 성공 지표(Revision Rate, Ward Adoption, Answer/Accept Rate 등)와 짝을 이룬다 — 다만 백엔드의 `GET /metrics`([api-design.md](../architecture/api-design.md#지표-계측-phase-41))는 서버에 이미 쌓인 데이터로 집계하는 스냅샷이고, 여기 나열된 이벤트는 클라이언트 측 사용자 행동 트래킹(예: GA4, 자체 이벤트 로그)이라는 점에서 별개다 — 클릭률(CTR)처럼 서버 데이터만으로는 알 수 없는 지표는 이 클라이언트 이벤트가 있어야 계측 가능하다.

## 35. 테스트 전략

| 계층 | 도구/대상 |
|---|---|
| Unit | formatter, schema, query-key factory, pure utility |
| Component | QuestionCard, VoteControl, TagPicker, Editor state |
| Integration | API mock 기반 질문 작성/투표/알림 흐름 |
| E2E | Playwright: 검색→질문 상세, 질문 작성, 답변→수락 |
| Accessibility | axe + keyboard navigation |
| Visual | 핵심 컴포넌트의 visual regression 선택 적용 |

### 35.1 필수 E2E 시나리오

- 비로그인 사용자가 질문 검색→상세 읽기
- 로그인 사용자가 질문 작성→상세 이동
- 답변 작성→질문자 수락
- Upvote→점수 반영→새로고침 후 유지 *(Vote는 Phase 11/F6로 구현됨 — 이 문서 작성 시점의 "백엔드 없음" 보류 사유는 더 이상 유효하지 않음. 단, Playwright 자체는 이 저장소에 아직 설정되지 않은 계획 단계 항목)*
- Watch→알림 이벤트 확인
- 검색 필터 URL 공유→동일 결과 재현
- 모바일 viewport에서 질문 상세 핵심 행동 가능
