# ADR-0043: SEO 메타데이터는 질문 상세의 동적 Open Graph와 태그·조직 sitemap까지만

- 날짜: 2026-09-02
- 상태: 승인됨

## 배경 (Context)

[ADR-0041](0041-narrow-public-read-access.md)/[ADR-0042](0042-expand-public-read-access-tags-orgs-profiles.md)가 접근 제어(볼 수 있다)까지만 다루고 미뤄둔 마지막 후속 후보다. ADR-0041의 원래 동기(검색엔진 유입·공유 링크로 질문에 바로 들어왔을 때의 경험)가 가리키는 가장 가치 있는 타깃은 **질문 상세 링크**다 — 슬랙/카톡에 질문 링크를 공유했을 때 제목·본문 미리보기가 뜨는지가 실사용에서 가장 먼저 체감되는 지점이기 때문이다.

## 결정 (Decision)

1. **질문 상세(`/questions/[id]`)에만 요청 시점에 실제 데이터를 반영하는 동적 Open Graph를 붙인다.** 이 페이지는 지금까지 전체가 `"use client"` 컴포넌트였는데, Next.js App Router의 `generateMetadata`는 Server Component에서만 동작한다 — `page.tsx`를 얇은 async Server Component로 바꿔 `generateMetadata`(백엔드의 이제 공개된 `GET /questions/{id}`를 인증 없이 `fetch`)와 실제 화면(`QuestionDetailContent`, 기존 컴포넌트를 그대로 옮김)을 분리했다. 질문이 없으면(404) 제목만 "질문을 찾을 수 없습니다"로 대체하고 본문 fetch 실패를 조용히 삼킨다 — 메타데이터 생성 실패가 페이지 자체를 깨뜨리면 안 되기 때문이다.
2. **태그/조직/사용자 프로필 상세는 동적 OG를 붙이지 않는다.** 셋 다 지금 서버/클라이언트 컴포넌트 경계를 만들어야 하는 것은 같지만, ADR-0041이 명시한 원래 동기(질문 공유 링크)의 대상이 아니고, 이번에 실사용 데이터로 가치를 검증할 확실한 타깃 하나(질문)에 집중하는 편이 낫다고 판단했다. 필요해지면 질문 상세와 같은 패턴(Server Component 분리 + fetch)을 그대로 반복 적용한다.
3. **사이트 전역 기본 Open Graph/Twitter Card는 루트 레이아웃에 정적으로 추가한다** — 질문 상세를 제외한 모든 페이지(Home, Tags, Organizations, Ask 등)가 이 기본값을 상속한다. `metadataBase`는 `NEXT_PUBLIC_SITE_URL` 환경변수로 오버라이드하고, 없으면 `http://localhost:3000`로 기본값을 둔다(JWT secret·Toss 키와 같은 패턴 — 배포 시점에 실제 도메인으로 덮어써야 함).
4. **`sitemap.xml`은 정적 라우트(`/`, `/tags`, `/organizations`) + 존재하는 모든 태그(`/tags/{name}`) + 존재하는 모든 조직(`/organizations/{id}`)만 포함한다.** 질문과 사용자 프로필은 **포함하지 않는다** — `GET /search`는 `q` 파라미터 없이 전체 목록을 반환하지 않고(문자열 매칭 쿼리라 빈 검색어로 전체를 열거할 방법이 없음), 프로필도 마찬가지로 "전체 사용자 목록" API가 없다. 이를 위해 새 "질문 전체 열거" 백엔드 엔드포인트를 추가하는 것은 sitemap만을 위한 목적치고 범위가 커서 이번엔 포함하지 않는다 — 태그/조직은 `GET /tags`, `GET /organizations`가 `q` 없이 호출하면 이미 전체를 반환하므로(`TagRepositoryAdapter`/`OrganizationRepositoryAdapter`의 `query.isNullOrBlank()` 분기) 그대로 재사용했다. 다만 두 컨트롤러 모두 `limit` 파라미터를 받지 않고 use case의 기본값(20개)에 묶여 있었다 — sitemap이 21번째 태그부터 조용히 빠지는 걸 막기 위해 `TagController`/`OrganizationController`의 `search`에 다른 GET들과 동일한 `@RequestParam(required = false) limit: Int?`를 추가하고, sitemap에서는 넉넉한 값(1000)으로 호출한다.
5. **`robots.txt`도 함께 추가한다** — 전체 허용 + sitemap 위치 안내. sitemap 작업과 짝을 이루는 표준 관례라 별도 후속으로 미루지 않았다.

## 결과 (Consequences)

- 질문 상세 페이지가 이제 Server Component(`page.tsx`) + Client Component(`QuestionDetailContent.tsx`) 두 파일로 나뉜다 — 이 페이지에 새 데이터 훅을 추가할 때 어느 파일에 넣을지 헷갈리지 않으려면, 상호작용(투표/댓글/Watch 등)은 전부 `QuestionDetailContent`에 있고 `page.tsx`는 메타데이터 생성 전용이라는 걸 기억해야 한다.
- sitemap이 질문/프로필을 빠뜨려 완전하지 않다 — 검색엔진이 이 URL들을 sitemap이 아니라 사이트 내부 링크를 따라가며 찾아야 한다(크롤링 자체는 여전히 가능, 다만 디스커버리가 느릴 수 있음). 질문 전체 열거가 실제로 필요해지면 별도 페이지네이션 API를 새로 설계한다.
- `metadataBase`의 로컬호스트 기본값을 실제 배포 도메인으로 바꾸는 걸 잊으면 소셜 공유 미리보기의 이미지/링크가 깨진다 — 배포 체크리스트에 추가해야 한다(이 세션에서는 문서화만 하고 실제 배포 절차는 범위 밖).

## 관련 문서

- [ADR-0041](0041-narrow-public-read-access.md), [ADR-0042](0042-expand-public-read-access-tags-orgs-profiles.md)(이번 ADR이 마무리하는 후속 후보의 출처)
- [PLAN.md](../../../PLAN.md) Phase 31
