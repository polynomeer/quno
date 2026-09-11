# Quno 품질 개선 계획

> "배포할 수 있는가"는 [production-readiness.md](production-readiness.md)가 다룬다. 이 문서는 "배포된 제품이 얼마나 잘 만들어졌는가" — 성능, 접근성, UX 완성도, 코드 품질 — 를 다룬다. 상용화 필수 조건은 아니지만, 방치하면 사용자 이탈과 유지보수 비용으로 직결된다.

## 우선순위 기준

기능 추가가 아니라 **이미 만든 것의 완성도**를 높이는 작업이므로, "사용자가 체감하는 정도"와 "지금 고치는 비용 vs 나중에 고치는 비용"을 기준으로 순서를 정한다.

## Q-1. 코드 품질 게이트 (가장 먼저 — 이후 모든 개선의 기반)

- [x] 백엔드 정적 분석 도입 — ktlint 도입, 기존 코드 전체(174개 파일, 순수 포맷팅) 재포맷 후 CI 게이트화(ADR-0049). detekt는 Kotlin 2.2.21과의 하드 호환성 문제(우회 불가 확인)로 새 릴리스가 나올 때까지 보류
- [x] 프론트엔드 ESLint 규칙 강화 — `no-explicit-any`/`no-unused-vars`를 warn에서 error로 상향(기존 `any` 사용 0건이라 즉시 고칠 것 없었음). 검증 중 `coverage/` 리포트 산출물이 lint 대상에 잡히던 것을 발견해 무시 목록에 추가
- [x] 테스트 커버리지 측정 도구 도입 — 백엔드 Jacoco(내장, 도입 시점 라인 커버리지 81.8% 확인), 프론트엔드 `@vitest/coverage-v8`. 둘 다 CI 아티팩트로만 업로드하고 임계값 실패는 두지 않음(ADR-0049)
- [~] 프론트엔드 테스트 실질적 확충 — [production-readiness.md](production-readiness.md) B-5(Phase 36)에서 1→6개 파일로 늘렸지만 전체 커버리지는 여전히 5.5% 수준이었다(도입한 커버리지 도구로 실측). Phase 43에서 순수 유틸/훅(`relativeTime`, `useDebouncedValue`), 이번 세션에 새로 만든 `LocaleProvider`/`FormError`/`LanguageSwitcher`, 자주 재사용되는 프레젠테이션 컴포넌트(`StatusBadge`/`TagChip`/`QuestionCard`/`QuestionList`)에 테스트를 추가해 6→14개 파일, 18→55개 테스트, 라인 커버리지 5.5%→11.75%로 올렸다. 여전히 낮은 수준이라 지속적 확충이 계속 필요하다(끝나지 않는 항목이라 완료 체크 대신 진행 중으로 표시)

## Q-2. 성능

- [x] 프론트엔드 번들 사이즈 점검 — `@next/bundle-analyzer`가 이 프로젝트의 Turbopack 빌드와 비호환임을 발견해 내장 `next experimental-analyze`로 대체(ADR-0050). Live Chat이 항상 `@stomp/stompjs`를 로드하던 것을 발견해 `next/dynamic`으로 실제 지연 로드하도록 수정, 프로덕션 빌드 네트워크 요청으로 검증
- [x] 이미지 최적화 — 코드 전체에 `<img>`/`next/image`/이미지 참조가 전혀 없어 대상 없음(텍스트/Markdown 기반 서비스). `create-next-app` 잔여 미사용 SVG 5개만 정리
- [x] 백엔드 N+1 쿼리 — `AnswerResultAssembler`/`GetActivityFeedUseCase`(`/api/v1/flow`)/`SearchOrganizationsUseCase`(공개)/`GetUserProfileUseCase`(공개) 4곳에서 항목마다 조회하던 것을 배치 쿼리로 교체(ADR-0050)
- [x] 캐싱 확대 — `OrganizationRepositoryAdapter.search()`에 기존 Dashboard와 같은 Redis cache-aside 패턴 적용(TTL 60초), 실제 캐시 히트/라운드트립 확인. 태그는 위키 편집 대상(Phase 28)이라 캐싱 시 "방금 수정한 설명이 안 보이는" 회귀 위험이 커서 의도적으로 제외(ADR-0050)
- [x] 검색 성능 — 함수형 GIN 인덱스(V23 마이그레이션)를 추가했으나, `EXPLAIN`으로 현재 쿼리의 두 테이블에 걸친 OR 조건 구조상 아직 실제로 쓰이지 않는 것까지 확인. 쿼리를 UNION 기반으로 재작성해야 인덱스를 탈 수 있는데, `SearchJpaRepository`를 직접 검증하는 테스트가 없어 회귀 안전망 없이 SQL을 바꾸는 위험을 피해 이번엔 보류(ADR-0050)

## Q-3. 접근성(a11y)

- [x] 시맨틱 HTML/ARIA 속성 전수 점검 — axe-core 자동 스캔(8페이지) + `onClick` 수동 grep 스팟체크(3건, 전부 정상). 별도 이슈 없음(ADR-0051)
- [x] 키보드만으로 전체 플로우 완주 가능한지 점검 — `e2e/keyboard-navigation.spec.ts`(신규, `.click()` 미사용)로 회원가입→질문 작성 완주 확인. Direct Ask 결제는 토스 체크아웃 의존성으로 E2E 범위 밖(ADR-0047/0051)
- [x] 명도 대비 — 핵심 디자인 토큰 9쌍을 WCAG 공식으로 직접 계산해 전수 통과 확인(최저 5.02:1). axe 스캔에서 `bg-brand/10` 배지 2곳(StatusBadge UPDATED, BadgeChip GOLD)이 4.48:1로 미달인 것을 발견해 전용 `--brand-subtle` 토큰으로 교체(ADR-0051)
- [x] 자동 검사 도구 CI 연동 — `@axe-core/playwright` 도입, `e2e/accessibility.spec.ts`가 `playwright.config.ts`의 기존 testDir 설정으로 CI에 자동 편입(별도 설정 불필요)

## Q-4. UX 완성도

- [x] 로딩 상태 일관성 — 화면 전체를 grep으로 전수 점검, 스켈레톤/스피너 사용 패턴이 이미 일관됨을 확인. 수정 불필요
- [x] 에러 상태 일관성 — API 실패 메시지 톤을 전수 점검, 이미 일관됨을 확인. 수정 불필요
- [x] 빈 상태(Empty State) — 질문/태그/조직 빈 상태 화면을 전수 점검, 이미 일관되게 설계돼 있음을 확인. 수정 불필요
- [x] 반응형 디자인 — `src/app`+`src/widgets` 전체에서 반응형 브레이크포인트 클래스를 쓰는 파일이 5개뿐임을 grep으로 확인, 그중 전역 `AppHeader`가 반응형 클래스 없이 로고/검색/전체 메뉴를 한 줄에 배치해 375px 뷰포트에서 우측 메뉴가 화면 밖으로 밀려나는 실제 문제를 Browser 도구로 실측 확인. 데스크톱(`md:` 이상)은 기존 한 줄 레이아웃을 유지하고, 모바일은 로고+검색+햄버거 버튼만 남기고 나머지 전체 메뉴(Tags/Organizations/로그인 상태별 메뉴/Ask/인증)를 토글형 드롭다운으로 이동하는 방식으로 수정(ADR-0052). 375px/768px/데스크톱 3개 뷰포트에서 실제 렌더링으로 검증
- [x] 폼 유효성 검사 피드백 — 조사 중 "제출 실패 메시지"가 27곳에서 거의 동일한 JSX로 복붙돼 있는 것을 발견(폰트 크기가 `text-xs`/`text-sm`로 은근히 갈라짐, `role="alert"` 전무). 신규 `shared/ui/FormError` 컴포넌트로 27곳 전부 통일하고 `role="alert"` 추가(ADR-0052). 폼마다 다른 실시간 검증 방식(react-hook-form+zod 1곳/네이티브 HTML5 2곳/버튼 비활성화만 6곳)을 하나로 통일하는 것은 결제·모더레이션과 맞물린 폼까지 건드려야 해 회귀 위험이 커서 이번엔 범위에서 빼고 후속 과제로 남김(ADR-0052)

## Q-5. 국제화(i18n)

- [x] 현재 한국어 전용으로 하드코딩된 문자열 범위 확인 — 다국어 지원이 로드맵에 있는지 사용자 확인 필요(이 항목은 사용자 의사결정 선행 필요, ADR 대상). 사용자 확인 결과: 한국어+영어 지원 필요, 범위는 핵심 플로우만(전체 화면 일괄 번역은 별도 작업)
- [x] 핵심 플로우(홈/로그인/회원가입/질문 작성) 한/영 지원 구현 — 경로 기반 라우팅 없이 클라이언트 Context+`localStorage`(`shared/i18n/LocaleProvider`)로 로케일 관리, 외부 i18n 라이브러리 없이 자체 dictionary(`shared/i18n/dictionary.ts`)로 구현. 헤더에 KO/EN 스위처 추가(단, 헤더 자체 내비게이션 라벨은 원래도 전부 영어라 번역 대상이 아니었음). 나머지 화면(질문 상세 등)과 백엔드 에러 메시지는 범위 밖으로 명시(ADR-0053)

## 진행 방식

- Q-1(코드 품질 게이트)은 이후 모든 리팩터링 작업의 안전망이므로 가장 먼저 진행한다.
- Q-2~Q-4는 병행 가능하나, 코드 변경 범위가 넓은 순서(성능 > UX > 접근성)로 우선순위를 둔다.
- Q-5는 범위 자체가 스코프 결정이라 착수 전 사용자 확인이 필요하다(ADR 대상).

## 관련 문서

- [production-readiness.md](production-readiness.md) — 상용화 필수 조건
