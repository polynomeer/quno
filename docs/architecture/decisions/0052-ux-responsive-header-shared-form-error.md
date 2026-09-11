# ADR-0052: 반응형 헤더 도입과 폼 제출 에러의 공통 컴포넌트화

- 날짜: 2026-09-11
- 상태: 승인됨

## 배경 (Context)

품질 개선 [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-4(UX 완성도)를 진행한다. 로딩/에러/빈 상태 일관성은 grep 전수 점검 결과 이미 일관돼 있어 수정할 것이 없었다. 나머지 두 항목에서 실제 문제를 발견했다.

1. **반응형 디자인**: `src/app`+`src/widgets` 전체에서 Tailwind 반응형 브레이크포인트(`sm:`/`md:`/`lg:`) 클래스를 쓰는 파일이 5개뿐이었다. 그중 전역 `AppHeader`는 로고·검색창·Tags/Organizations·로그인 상태별 메뉴(Watching/Saved/Direct Asks/Notifications)·Ask 버튼·인증 영역을 반응형 클래스 없이 한 줄(`flex`)에 배치하고 있었다. Browser 도구로 375px 뷰포트에서 실측한 결과 우측 메뉴 전체(로그인/회원가입 버튼 포함)가 화면 밖으로 밀려나 조작 자체가 불가능했다 — 나머지 4개 파일은 그리드/사이드바형 페이지 레이아웃이라 이미 반응형 처리가 돼 있었다.
2. **폼 유효성 검사 피드백**: 클라이언트 검증(react-hook-form+zod를 쓰는 `/ask`가 유일한 예외)의 실시간 검증 방식은 폼마다 제각각이었지만, 이 항목을 조사하다 더 근본적인 문제를 발견했다 — "제출 실패 시 보여주는 에러 메시지" 자체가 공유 컴포넌트 없이 27곳에서 거의 동일한 JSX(`{x.isError && <p className="text-sm text-danger">{x.error instanceof ApiError ? x.error.message : "..."}</p>}`)로 복붙돼 있었다. 그 결과 폰트 크기가 은근히 갈라져 있었고(댓글 관련 UI는 `text-xs`, 나머지는 `text-sm`), `role="alert"` 같은 접근성 속성은 어디에도 없었다.

## 결정 (Decision)

1. **`AppHeader`를 데스크톱(`md:` 이상)은 기존 한 줄 레이아웃 그대로 유지하고, 모바일은 로고+검색+햄버거 버튼만 남기는 방식으로 나눴다.** 나머지 전체 메뉴(퍼블릭 링크, 로그인 상태별 메뉴, Ask, 인증 영역)는 햄버거를 누르면 나타나는 세로 드롭다운으로 옮겼다. 별도 아이콘 라이브러리를 추가하지 않고 인라인 SVG로 햄버거/닫기 아이콘을 그렸다 — 이미 이 프로젝트에 아이콘 라이브러리 의존성이 전혀 없었고, 아이콘 두 개 때문에 새 의존성을 들이는 건 과하다고 판단했다. 375px/768px/데스크톱 세 뷰포트에서 실제 렌더링으로 겹침·잘림이 없는지 확인했다.
2. **`shared/ui/FormError.tsx`를 새로 만들어 27곳의 중복된 제출-에러 표시를 전부 이 컴포넌트로 교체했다.** `error: unknown`을 받아 `ApiError`면 서버 메시지를, 문자열이면 그대로, 그 외엔 `fallback`을 보여준다 — 기존 각 폼이 이미 갖고 있던 "ApiError면 서버 메시지, 아니면 폼별 한국어 기본 메시지" 규칙을 그대로 유지하면서 중복만 제거했다. `role="alert"`를 추가해 스크린 리더가 제출 실패를 즉시 읽게 했다(Q-3 접근성 작업에서 놓쳤던 부분). 폰트 크기는 `className`으로 임의 오버라이드하지 않고 `size: "xs" | "sm"` prop으로 뒀다 — 이 프로젝트의 `cn()`은 `tailwind-merge`가 아니라 순수 `clsx`라서, `text-sm`과 `text-xs`를 문자열로 동시에 넘기면 승자가 Tailwind 빌드의 CSS 소스 순서에 좌우되는 재현하기 어려운 버그가 되기 때문이다. 댓글 관련 3곳(`CommentSection`/`CommentItem` 2곳)과 `AccountDangerZone`의 데이터 다운로드 에러, `ReportButton`은 주변이 전부 `text-xs`라 `size="xs"`를 명시했고 나머지는 기본값 `sm`을 그대로 썼다.
3. **폼별 실시간 검증 방식(디자인 대안 자체)을 통일하는 것은 이번 범위에서 뺐다.** `/ask`만 react-hook-form+zod로 필드별 실시간 메시지를 보여주고, 로그인/회원가입은 HTML5 네이티브 제약(브라우저 로캘 툴팁)에 의존하며, 나머지 6개 폼(조직 생성/답변/댓글/Direct Ask 요청/태그 편집/계정 탈퇴)은 필수값이 비어 있으면 그냥 제출 버튼을 비활성화할 뿐 이유를 알려주지 않는다. 이 세 갈래를 하나로 통일하려면 결제(Direct Ask)·모더레이션과 맞물린 여러 폼의 동작을 건드려야 해서 회귀 위험이 이번에 손댄 "표시 방식 통일"보다 훨씬 크다고 판단해 별도 후속 작업으로 미뤘다.

## 결과 (Consequences)

- 모바일에서 헤더 메뉴 전체가 조작 가능해졌다. 다만 아직 `AppHeader` 밖의 나머지 화면들(질문 상세의 3-컬럼 레이아웃, Direct Ask 패널 등)은 이번 조사에서 그리드가 좁아지는 정도의 문제만 없음을 확인했을 뿐, 픽셀 단위로 전수 검증하지는 않았다 — 새 화면을 추가할 때 375px에서 한 번씩 확인하는 습관이 필요하다.
- `FormError`가 생기면서 앞으로 새 폼의 제출 에러는 이 컴포넌트를 먼저 써야 한다 — `<p className="text-sm text-danger">...instanceof ApiError...</p>`를 다시 손으로 쓰면 이번에 없앤 중복이 재발한다.
- "버튼만 비활성화하고 이유를 안 알려주는" 6개 폼은 여전히 남아 있다 — 사용자가 필수 필드를 놓쳤을 때 왜 안 되는지 알 방법이 없다. 실시간 검증 통일이 필요해지면 이 ADR이 미룬 지점부터 시작하면 된다.

## 관련 문서

- [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-4
- [0051-accessibility-axe-core-e2e.md](0051-accessibility-axe-core-e2e.md) (`role="alert"`가 메우는 접근성 공백의 선례)
- [PLAN.md](../../../PLAN.md) Phase 41
