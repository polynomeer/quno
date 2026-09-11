# ADR-0053: 국제화(i18n) — 핵심 플로우만 한/영, 경로 기반 라우팅 없이

- 날짜: 2026-09-11
- 상태: 승인됨

## 배경 (Context)

[quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-5는 "다국어 지원이 로드맵에 있는지 사용자 확인 필요"라고 명시하고 있어 착수 전 사용자에게 직접 물었다. 답은 다음과 같았다:

1. 다국어 지원이 필요하다 (Q-5는 보류가 아니라 진행).
2. 대상 언어는 한국어 + 영어 — Quno는 개발자 Q&A 플랫폼이라 영어가 국제 개발자 커뮤니티의 사실상 표준이다.
3. 이번 작업 범위는 "전체 화면 일괄 번역"이 아니라 "핵심 플로우만" — 프론트엔드 거의 모든 화면(수백 개 지점)에 한국어 문자열이 하드코딩돼 있어 전수 번역은 훨씬 큰 별도 작업이다.

Next.js 16 App Router의 공식 국제화 가이드(`node_modules/next/dist/docs/01-app/02-guides/internationalization.md`)는 모든 라우트를 `app/[lang]/...`로 옮기고 proxy(middleware)에서 `Accept-Language`로 리다이렉트하는 경로 기반 라우팅을 권장한다. 이 프로젝트는 이미 `app/(auth)/`, `app/questions/[id]/`, `app/tags/[name]/` 등 여러 라우트 그룹과 동적 세그먼트를 갖고 있어, 이 구조를 `app/[lang]/`로 감싸는 것은 사실상 프론트엔드 라우팅 전체를 다시 만드는 작업이다 — "핵심 플로우만"이라는 사용자의 범위 결정과 정면으로 배치된다.

## 결정 (Decision)

1. **경로 기반 라우팅은 도입하지 않는다.** 대신 클라이언트 전역 `LocaleProvider`(React Context, `localStorage`에 선택 저장, 기본값 `ko`)로 로케일을 관리한다. 서버는 항상 `ko`로 렌더링하고, 마운트 후 `useEffect`로 저장된 로케일을 읽어 전환한다 — 첫 렌더는 서버/클라이언트가 항상 일치해 하이드레이션 불일치가 없지만, 이전에 `en`을 선택했던 사용자는 마운트 직후 한 번 `ko`→`en` 전환이 보인다. SEO(로케일별 URL)는 애초에 이번 범위가 아니므로 감수한다.
2. **외부 i18n 라이브러리(next-intl 등)를 추가하지 않는다.** 대상 화면이 4개뿐이고 복수형 처리 같은 복잡한 포맷팅 요구가 없어, `shared/i18n/dictionary.ts`에 `ko`/`en` 객체 리터럴 하나씩 두고 `useLocale()` 훅으로 `t.영역.키` 형태로 꺼내 쓰는 방식으로 충분하다고 판단했다 — 헤더 햄버거 아이콘 때 인라인 SVG를 택한 것과 같은 이유(의존성 대비 이득이 작음).
3. **번역 대상은 다음 4곳으로 한정한다: 홈(`app/page.tsx`) 비로그인 히어로/섹션 제목, 로그인(`app/(auth)/login`), 회원가입(`app/(auth)/signup`), 질문 작성(`app/ask`, zod 유효성 메시지 포함).** `AppHeader`는 조사해보니 "Tags"/"Ask"/"Sign up"/"Log in" 등 내비게이션 라벨 자체가 이미 전부 영어라 번역할 게 없었다 — 대신 사용자가 실제로 로케일을 바꿀 수 있도록 `LanguageSwitcher`(KO/EN 토글)를 헤더의 데스크톱 메뉴와 모바일 드롭다운 양쪽에 추가했다. 질문 상세(`QuestionDetailContent`)는 `AnswerCard`/`CommentSection`/`ClusterPanel`/`ReviewRequestPanel`/`ReportButton`/`LiveChatPanel` 등 수십 개 하위 컴포넌트로 뻗어 있어 "핵심 플로우"의 범위를 넘어선다고 판단해 이번엔 제외했다.
4. **백엔드 에러 메시지는 번역하지 않는다.** `FormError`가 표시하는 `ApiError.message`는 백엔드가 한국어로만 내려주므로(`GlobalExceptionHandler`), `en` 로케일에서도 API 실패 메시지 자체는 한국어로 보인다 — 프론트엔드 자체 문구(폼 라벨, 기본 fallback 메시지)만 번역 대상이다. 백엔드 다국어화는 훨씬 큰 별도 스코프라 이번 ADR 밖에 둔다.

## 결과 (Consequences)

- 4개 화면 + 헤더 언어 스위처만 다국어를 지원한다 — 나머지 화면(질문 상세, 태그, 조직, 프로필, 모더레이션 등)은 여전히 한국어 하드코딩이다. `en`을 선택해도 이 화면들은 한국어로 보인다.
- 새 화면/문구를 이 4곳에 추가할 때는 하드코딩된 문자열 대신 `shared/i18n/dictionary.ts`에 키를 추가하고 `useLocale().t`로 꺼내 써야 한다 — 그렇지 않으면 이번에 만든 패턴이 다시 깨진다.
- 전체 화면으로 확장하려면 (a) 나머지 화면의 문자열을 사전에 옮기는 작업과 (b) 이 경량 방식이 계속 맞는지(예: SEO가 필요해지면 결국 경로 기반 라우팅으로 재작성) 재검토가 필요하다.
- 백엔드 에러 메시지 다국어화는 여전히 미해결 — `en` 사용자도 서버 검증 실패 메시지는 한국어로 본다.

## 관련 문서

- [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-5
- [0052-ux-responsive-header-shared-form-error.md](0052-ux-responsive-header-shared-form-error.md) (같은 세션에서 AppHeader를 만진 직전 결정)
- [PLAN.md](../../../PLAN.md) Phase 42
