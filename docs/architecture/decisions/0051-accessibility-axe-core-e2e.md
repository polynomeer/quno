# ADR-0051: 접근성 — axe-core 자동 검사, 명도 대비, 키보드 전용 플로우

- 날짜: 2026-09-11
- 상태: 승인됨

## 배경 (Context)

품질 개선 [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-3(접근성)을 진행한다: 시맨틱 HTML/ARIA, 키보드만으로 전체 플로우 완주, 명도 대비, 자동 검사 도구 CI 연동.

## 결정 (Decision)

1. **자동 검사는 `@axe-core/playwright`로, WCAG A/AA 룰셋만 우선 적용한다.** `e2e/accessibility.spec.ts`(신규)가 공개 페이지 6개(`/`, `/questions`, `/tags`, `/organizations`, `/login`, `/signup`) + 질문 상세(비로그인, "로그인하고 답변을 작성하세요" 인라인 링크 포함) + `/ask`(로그인)까지 8개 페이지를 검사한다. `wcag2a`/`wcag2aa` 태그만 켰다 — best-practice 룰까지 켜면 디자인 토큰 조정이 필요한 지적까지 한꺼번에 쏟아져 우선순위가 흐려진다. `playwright.config.ts`가 이미 `e2e/`를 전부 실행하므로 CI에 별도 설정 없이 자동으로 편입된다.
2. **처음 실행에서 실제 위반 2건을 발견해 고쳤다.**
   - `/login`·`/signup`·질문 상세의 "로그인"/"회원가입" 인라인 링크가 `hover:underline`만 쓰고 있어 평상시(호버 전) 색상에만 의존해 주변 텍스트와 구분되고 있었다(`link-in-text-block`, contrast 1.18:1, 필요 3:1) — 항상 보이는 `underline`으로 바꿨다. 같은 스타일 패턴(`hover:underline`만 쓰는 인라인 링크)이 이 세 곳 말고도 여러 파일에 있었지만, 나머지는 전부 문단 안에 다른 텍스트와 섞이지 않은 독립된 링크라 `link-in-text-block` 규칙 대상이 아니었다(수동으로 각 위치를 확인) — 그대로 둠.
   - `StatusBadge`의 `UPDATED` 톤과 `BadgeChip`의 `GOLD` 톤이 둘 다 `bg-brand/10 text-brand`(불투명도 트릭)를 쓰고 있었는데, 라이트 모드에서 4.48:1로 AA 기준(4.5:1)을 살짝 밑돌았다 — 다른 세 톤(success/warning/danger)처럼 전용 `--brand-subtle` 배경 토큰(`#eff6ff`, 다크 `#182a45`)을 globals.css에 추가해 4.7:1 이상으로 여유를 확보했다.
3. **명도 대비는 자동 스캔 외에 핵심 디자인 토큰 9쌍을 직접 계산해 전수 확인했다.** text-primary/secondary, brand, success/warning/danger 각각을 surface/surface-subtle 배경에 대해 라이트·다크 두 테마 모두 WCAG 공식(상대 휘도)으로 계산 — 전부 4.5:1 이상으로 통과(가장 낮은 게 5.02:1). 위 2건은 토큰 자체가 아니라 "불투명도 트릭으로 만든 파생 배경"에서만 발생한 문제였다.
4. **키보드 전용 플로우는 골든 패스를 별도 스펙으로 다시 구현했다.** `e2e/keyboard-navigation.spec.ts`(신규)가 `.click()`을 전혀 쓰지 않고 회원가입→질문 작성까지 Tab으로 포커스 이동, 키보드 타이핑, 포커스된 버튼에서 Enter로 제출까지 완주하는 것을 확인한다. axe-core는 정적 마크업 위반(예: 버튼에 접근 가능한 이름이 있는지)은 잡지만 실제 Tab 순서·키보드 활성화까지는 검증하지 못해 별도로 필요하다고 판단했다. Direct Ask 결제까지는 포함하지 않았다 — E2E 골든 패스(ADR-0047)와 같은 이유로 토스 호스팅 체크아웃 의존성 때문에 범위 밖.
5. **시맨틱 HTML/ARIA 전수 점검은 자동 스캔 + 수동 grep 스팟체크로 마쳤다.** `onClick`이 있는데 `<Button>`/`type="button"` 등이 아닌 경우를 grep으로 찾아 3곳을 확인했고, 전부 실제 `<button>`/`<Link href>` 위에 얹힌 부가 동작(드롭다운 닫기 등)이라 문제없음을 확인했다 — 별도 컴포넌트 감사 도구 도입 없이 axe-core의 커버리지로 충분하다고 판단했다.

## 결과 (Consequences)

- axe-core 스캔은 8개 페이지로 한정돼 있다 — 로그인 이후 전용 화면(Direct Ask, Organization 관리, 모더레이션 등)은 아직 커버되지 않는다. 새 화면을 추가할 때 이 목록에 넣을지 판단해야 한다.
- `--brand-subtle` 토큰이 새로 생겨, 앞으로 브랜드 색 배경 위에 브랜드 색 텍스트를 올릴 일이 생기면 `bg-brand/N` 대신 이 토큰을 먼저 검토해야 한다.
- best-practice 룰(wcag2a/aa 외)은 검사하지 않아, 예를 들어 색맹 시뮬레이션이나 더 엄격한 권장 사항은 이번 범위에 없다 — 필요해지면 별도로 확장한다.

## 관련 문서

- [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-3
- [0047-testing-signup-page-e2e-load-test-scope.md](0047-testing-signup-page-e2e-load-test-scope.md) (E2E 범위 결정의 선례)
- [PLAN.md](../../../PLAN.md) Phase 40
