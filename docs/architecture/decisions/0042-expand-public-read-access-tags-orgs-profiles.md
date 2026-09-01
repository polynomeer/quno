# ADR-0042: 비로그인 공개 열람을 태그·조직 상세·사용자 프로필까지 확대한다

- 날짜: 2026-09-02
- 상태: 승인됨 — SEO는 [ADR-0043](0043-seo-metadata-question-og-and-sitemap.md)로 이어짐

## 배경 (Context)

[ADR-0041](0041-narrow-public-read-access.md)은 세 단계(질문만 / +태그·조직 상세 / +사용자 프로필까지) 중 가장 좁은 첫 번째를 선택하면서, "질문은 보이는데 관련 태그를 클릭하면 로그인부터 요구한다"는 불일치가 실제로 문제로 확인되면 넓히겠다고 남겨뒀다. 이번에 남은 마지막 백로그 두 개(이 범위 확대, SEO 메타데이터) 중 사용자가 이것을 먼저 선택했다.

## 결정 (Decision)

1. **다음 GET 엔드포인트를 `SecurityConfig`에 HTTP 메서드 단위로 `permitAll` 추가한다** — ADR-0041과 동일한 원칙(같은 경로의 쓰기 메서드는 그대로 인증 필요):
   - `GET /tags`, `GET /tags/{id}`, `GET /tags/{id}/questions`, `GET /tags/{id}/contributors`, `GET /tags/{id}/related`
   - `GET /organizations`, `GET /organizations/{id}`
   - `GET /users/{id}/profile`, `GET /users/{id}/reputation`, `GET /users/{id}/badges`
2. **그 외(Follow/Unfollow, 태그 위키 편집, Organization 생성/가입/탈퇴/이메일 인증, Direct Ask, Live Chat, Dashboard, 모더레이션, 알림, `/me/*`)는 그대로 인증이 필요하다** — 이번 확대 대상이 아니다.
3. **프론트엔드 5개 페이지**(`/tags`, `/tags/[name]`, `/organizations`, `/organizations/[id]`, `/users/[id]`)를 `useRequireAuth`(리다이렉트)에서 `useSession`(리다이렉트 없음)으로 전환한다 — Question Detail이 ADR-0041에서 이미 쓴 패턴 그대로.
4. **액션 전용 UI를 검토하면서 기존에 없던 방문자 가드 두 종류를 발견해 수정한다**(이번 확대 작업으로 새로 생긴 문제가 아니라, 이 페이지들이 지금까지 항상 `useRequireAuth` 뒤에 있어 `me`가 없는 상태 자체가 도달 불가능했기 때문에 잠재해 있던 결함):
   - `FollowUserButton`은 "자기 자신 팔로우 방지"만 가드하고 "로그인 여부"는 가드하지 않았다 — 익명 방문자에게도 버튼이 보이고 클릭 시 401만 받는다. `if (!me) return null`을 추가한다.
   - `EmailDomainVerificationPanel`은 `viewerId: number | undefined`를 인자로 받으면서 정작 `undefined`일 때를 확인하지 않았다 — 익명 방문자에게도 이메일 인증 폼 전체가 보인다. 컴포넌트 내부에서 `if (!viewerId) return null`을 추가한다.
   - `TagDetailsEditor`(위키 편집)와 `CreateOrganizationForm`도 지금까지 가드가 아예 없었다 — 둘 다 `if (!me) return null` self-guard를 추가한다. 나머지(`JoinOrganizationButton`/`FollowTagButton`/`RequestDirectAskPanel`)는 이미 자체 가드가 있어 손대지 않는다.
5. **`PublicReadAccessE2ETest`가 "범위 밖"으로 단언했던 `GET /tags` 401 케이스를 뒤집고**, 새로 공개된 5개 GET과 여전히 인증이 필요한 쓰기 메서드(태그 편집, Organization 생성/가입, Follow) 양쪽을 커버하는 케이스를 추가한다.
6. **SEO 메타데이터(Open Graph, `sitemap.xml`)는 여전히 범위 밖이다** — ADR-0041과 같은 이유로, 접근 제어부터 검증한다.

## 결과 (Consequences)

- Tag/Organization/User Profile 페이지도 이제 "로그인 안 한 방문자" 상태를 가지므로, 앞으로 이 페이지들에 컴포넌트를 추가할 때 `useSession()`의 `me`가 없을 수 있다는 것을 고려해야 한다(ADR-0041이 Question Detail에 남긴 것과 같은 주의사항).
- `PLAN.md` Phase 29 완료 당시 "격차가 모두 닫혔다"고 기록했던 [roadmap.md 7절](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)의 "질문 비로그인 공개 열람" 행 중 "태그/조직/프로필은 여전히 인증 필요" 부분이 이번으로 사실상 해소된다 — 문서도 함께 갱신한다.
- 남은 유일한 후속 후보는 SEO 메타데이터뿐이다.

## 관련 문서

- [ADR-0041](0041-narrow-public-read-access.md)(이번 ADR이 넓히는 원래 좁은 범위 결정)
- [docs/frontend/roadmap.md 7절](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)
- [PLAN.md](../../../PLAN.md) Phase 30
