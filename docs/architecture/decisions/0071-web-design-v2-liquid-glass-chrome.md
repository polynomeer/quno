# ADR-0071: 웹 디자인 시안 v2 적용 — 크롬만 유리, 새 기능 화면은 해당 Phase에서

- 날짜: 2026-10-22
- 상태: 승인됨

## 배경 (Context)

2026-10-18~19에 Claude Design 캔버스로 "Quno 웹 디자인 시안 v2"를 만들었다. iOS 시안의 Liquid Glass 언어를 웹으로 옮긴 것으로, 두 종류의 화면이 섞여 있다.

- **기존 화면의 리뉴얼**: 홈·질문 상세·질문 작성·알림·프로필, 공용 헤더와 좌측 내비게이션. 지금 백엔드만으로 그릴 수 있다.
- **추가 기획 화면**: 검증 등급, 재현 기록, 영향 매트릭스, 환경 스냅샷, 깨짐 레이더, 해결 기록, 에스컬레이션, 팀 기억, 메인테이너 허브, 연동, IDE 패널 등. PLAN.md Phase 63~99의 백엔드가 아직 없다.

사용자가 "웹 디자인 시안 모두 적용"을 요청했다. 추가 기획 화면을 지금 프론트엔드에 넣으려면 API에 없는 데이터를 지어내야 한다. 이는 [ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md)의 "데이터는 지어내지 않는다" 원칙과 충돌한다. 또 PLAN.md는 Phase 91 실험 결과 전에는 92·95의 대규모 구현을 시작하지 않기로 했다.

## 결정 (Decision)

- **이번 적용 범위는 시각 체계와 기존 화면이다.** 실제 데이터가 있는 화면에만 v2 레이아웃을 입힌다.
- **추가 기획 화면은 해당 Phase를 구현할 때 v2 시안을 기준으로 만든다.** Phase 100에 화면 ↔ Phase 대응표를 남긴다. 목 데이터로 껍데기 화면을 먼저 만들지 않는다.
- **유리(반투명 + 블러)는 크롬에만 쓴다.** 헤더, 탭 스위치(segmented control), 투표 레일 같은 떠 있는 조작 요소가 대상이다. 본문 카드는 불투명한 `--surface`를 유지한다. 반투명 위 본문 글자는 뒤 내용에 따라 대비가 흔들려 WCAG AA를 보장할 수 없기 때문이다. `backdrop-filter`를 지원하지 않는 환경에서는 불투명에 가까운 배경으로 떨어진다.
- **형태 언어**
  - 버튼·칩·배지는 캡슐(완전 둥근)로 바꾼다.
  - 카드는 20~24px 모서리로 바꾼다.
  - 목록은 구분선 표가 아니라 떨어진 카드로 바꾼다.
- **색 토큰은 바꾸지 않는다.** ADR-0061의 semantic token을 그대로 쓰고, 유리 표면용 토큰(`--glass-bg`, `--glass-border`, `--glass-shadow`)만 추가한다.
- **시안과 실제 데이터가 다른 곳은 실제 데이터를 따른다.**
  - 홈 필터 탭: 시안의 추천/최신/미해결/내 태그 대신 대시보드 API가 주는 섹션(인기, 팔로우 태그, 오늘 해결, 재활성화)을 탭으로 바꿔 보여준다.
  - 실시간 질문방 목록: 방 목록 API가 없어 넣지 않는다.

## 결과 (Consequences)

- 공용 프리미티브(Button, TagChip, StatusBadge, Input, SegmentedControl)를 바꾸면 모든 화면이 함께 바뀌므로, 화면별 수정은 레이아웃에만 집중한다.
- 시안 v2의 절반가량(추가 기획 화면)은 당분간 캔버스에만 있다. Phase 63~99 각 항목의 프론트엔드 작업은 이 ADR과 Phase 100의 대응표를 참고해 시안 화면을 기준으로 한다.
- 유리 효과는 성능 비용이 있다(특히 sticky 헤더의 `backdrop-filter`). 저사양 기기에서 문제가 되면 `prefers-reduced-transparency` 대응이나 불투명 대체를 검토한다.

## 관련 문서

- [ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md) 시각 체계, [ADR-0069](0069-agent-era-positioning-verified-fix-memory.md)·[ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md) 추가 기획의 근거
- [PLAN.md](../../../PLAN.md) Phase 100
- [docs/frontend/design.md](../../frontend/design.md)
