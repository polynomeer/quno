# ADR-0021: Tag Detail은 검색 결과 근사로 구현하고, 태그별 통계/탭/Follow 상태는 후속으로 미룬다

- 날짜: 2026-08-26
- 상태: 일부 대체됨([ADR-0040](0040-tag-detail-wiki-editable-and-real-stats.md)로 — 설명/문서 링크/기여자/관련 태그/Follow/실제 질문 조회 API를 채웠다. "질문/프로필 비로그인 공개 열람"과는 무관한 이 ADR의 검색-근사 방식 자체가 대체된 것이며, 이 문서는 그 결정의 배경 기록으로 남긴다)

## 배경 (Context)

[design.md 15장](../../frontend/design.md#15-태그-경험)은 Tag Detail 화면에 태그 설명/공식 문서 링크, Latest/Unanswered/Top 탭, 최근 30일 활동 요약, 태그 상위 기여자, 관련 태그, Follow 버튼을 요구한다. 그러나 백엔드의 `Tag` 도메인은 `id`/`name`/`slug`만 가지고 있고, "특정 태그가 달린 질문 목록"을 반환하는 전용 API(`GET /questions?tag=` 같은)가 없다 — `GET /tags`는 태그 자체 검색만, `GET /search`는 전문검색+태그 이름 부분일치를 OR로 묶은 범용 검색만 지원한다([api-design.md](../api-design.md#검색·관련-질문-구현-phase-29)). 또한 특정 사용자가 이 태그를 이미 팔로우하고 있는지 알려주는 API도 없다(`POST/DELETE /tags/{id}/follow`는 있지만 "내 팔로우 태그 목록/여부" 조회가 없음).

Frontend Phase 1(읽기 경험)에서 Tag Detail을 완전히 건너뛸 수도 있었지만, Phase 0에서 이미 만든 `TagChip` 컴포넌트가 `/tags/{name}`으로 링크를 걸어두었고([TagChip.tsx](../../../frontend/src/shared/ui/TagChip.tsx)) PLAN.md의 Frontend Phase 1 범위에도 "Tag Detail"이 명시돼 있어, 완전히 빈 라우트로 남겨두기보다 백엔드가 실제로 지원하는 만큼만 근사해서 채우는 쪽을 택했다.

## 결정 (Decision)

`/tags/{name}` 페이지는 `GET /search?q={name}`의 결과를 가져온 뒤, 응답의 `tags: string[]`에 해당 태그 이름이 **정확히 포함된** 항목만 클라이언트에서 필터링해 보여준다 — 전문검색이 태그 이름 부분일치와 본문 텍스트 매치를 OR로 섞어 반환하므로, 그대로 노출하면 이 태그와 무관한 질문이 섞여 들어온다. 태그 설명/공식 문서 링크/기여자/관련 태그/활동 요약/Latest·Unanswered·Top 탭/Follow 버튼은 이번 범위에서 만들지 않는다. Follow 토글은 "이미 팔로우 중인지" 상태를 알 방법이 없어 만들면 오히려 잘못된 초기 상태(항상 미팔로우로 보임)를 보여주게 되므로, 커뮤니티 액션을 다루는 Frontend Phase 3로 미룬다(그때 `GET /me`나 별도 API로 팔로우 여부를 함께 내려주는 백엔드 변경이 필요할 수 있다).

`/tags` Tag Directory 페이지는 `GET /tags?q=`로 태그를 검색/나열하는 화면만 만들고, 각 태그는 `/tags/{name}`으로 연결한다.

## 결과 (Consequences)

- Tag Detail이 실제로 동작하지만 설계서보다 훨씬 단순하다 — 태그 이름을 헤더로 보여주고 그 태그가 달린 질문 목록만 보여준다.
- 클라이언트 필터링은 서버가 이미 걸러낸 `limit`개 결과 안에서만 걸러내므로, 특정 태그의 질문이 많은데 상위 `limit`개에 안 걸리면 "결과 없음"으로 보일 수 있다 — 실제 사용성이 문제가 되면 태그 전용 조회 API를 백엔드에 새로 추가하는 것으로 재검토한다.
- Follow 버튼/상태는 Frontend Phase 3에서 백엔드의 "내 팔로우 태그 목록/여부" 조회 지원 여부를 먼저 확인한 뒤 착수한다.
- 태그 상위 기여자/관련 태그/공식 문서 링크는 `Tag` 도메인이 확장되기 전까지 범위 밖으로 유지한다.

## 관련 문서

- [docs/frontend/design.md 15장](../../frontend/design.md#15-태그-경험)
- [docs/frontend/roadmap.md §7](../../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)
- [PLAN.md](../../../PLAN.md) Frontend Phase 1 (F1.6)
- [ADR-0020](0020-frontend-scoped-to-backend-support.md)
