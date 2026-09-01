# ADR-0041: 비로그인 공개 열람은 질문 상세/목록/검색으로 좁혀서 시작한다

- 날짜: 2026-09-01
- 상태: 승인됨 (ADR-0013을 재검토하고 좁은 범위로 확정) — 태그·조직·프로필 확대는 [ADR-0042](0042-expand-public-read-access-tags-orgs-profiles.md), SEO는 [ADR-0043](0043-seo-metadata-question-og-and-sitemap.md)로 이어짐

## 배경 (Context)

[ADR-0013](0013-defer-public-read-access.md)이 "그 시점의 실제 요구 없이 정하면 나중에 뒤집힐 가능성이 높다"며 보류해온 비로그인 공개 열람을, 남은 마지막 프론트엔드 격차로 다시 꺼냈다. 이번에도 SEO 크롤링이나 소셜 공유 미리보기 같은 구체적 요구가 있는 건 아니었지만, 검색엔진 유입·공유 링크로 질문에 바로 들어왔을 때 로그인부터 요구하는 것 자체가 손해라는 데는 이견이 없어 착수를 결정했다. 다만 "어디까지 공개할지"는 여전히 임의로 정할 수 있는 문제라 사용자에게 확인했다(2026-09-01) — 질문 상세/목록/검색만 / 거기에 태그·조직 상세까지 / 사용자 프로필까지 포함한 거의 전체 공개, 세 단계 중 가장 좁은 첫 번째를 선택했고, SEO 메타데이터(Open Graph, sitemap)는 이번 범위에 넣지 않고 접근 제어만 먼저 하기로 했다.

## 결정 (Decision)

1. **공개되는 것은 질문·답변·댓글의 읽기와 검색뿐이다.** `SecurityConfig`에 `GET /questions/{id}`(+versions/diff/related), `GET /questions/{id}/answers`, `GET /questions/{id}/comments`, `GET /answers/{id}/{versions,comments}`, `GET /comments/{id}/versions`, `GET /search`를 **HTTP 메서드 단위로** `permitAll` 추가했다 — 같은 경로의 `POST`/`PUT`/`DELETE`(답변 작성, 리비전, 댓글 작성 등)는 그대로 인증이 필요하다. Spring Security 6.x의 `PathPatternRequestMatcher`가 컨트롤러의 `@GetMapping("/questions/{id}")`와 동일한 `{id}` 플레이스홀더 문법을 그대로 보안 패턴에 쓸 수 있어, 컨트롤러 매핑과 나란히 두고 검토하기 쉽다.
2. **태그/Organization/Direct Ask/실시간 질문방/사용자 프로필/Dashboard/모더레이션/알림은 전부 그대로 인증이 필요하다.** 사용자가 명시적으로 고른 범위이며, 그 중 일부(태그·조직 상세)는 다음 재검토 대상으로 남는다.
3. **투표/Watch/Save/Report/Outdated 표시/댓글 작성/QPR/Cluster/Fork/Live Chat 패널은 익명 방문자에게 아예 보여주지 않는다.** 백엔드를 추가로 열지 않고, 프론트엔드에서 `useSession()`(리다이렉트 없는 버전, `useRequireAuth`와 달리)의 `me`가 없으면 해당 버튼/패널을 숨기거나(`WatchButton`/`SaveButton`/`ReportButton`은 컴포넌트 내부에서 `if (!me) return null`, `FollowUserButton`과 같은 패턴), 아예 렌더링 자체를 건너뛴다(`ClusterPanel`/`ForkPanel`/`LiveChatPanel`/`OutdatedAction`은 페이지에서 `{me && (...)}`). `VoteControl`은 익명 방문자에게 점수만 읽기 전용으로 보여준다(글쓴이 본인에게 보여주던 것과 같은 표시). `AnswerComposer`는 숨기는 대신 "로그인하고 답변을 작성하세요" 링크로 대체해 전환을 유도한다.
4. **SEO 메타데이터(Open Graph, `sitemap.xml`)는 이번 범위에 넣지 않는다.** 접근 제어가 실제로 잘 동작하는지부터 검증하고, 필요해지면 별도로 진행한다.

## 결과 (Consequences)

- Question Detail 페이지의 절반 가까운 하위 컴포넌트(`VoteControl`/`WatchButton`/`SaveButton`/`ReportButton`/`CommentSection`/`CommentItem`)가 "로그인 안 한 방문자"라는 새 상태를 갖게 됐다 — 앞으로 이 페이지에 컴포넌트를 추가할 때는 `useSession()`으로 `me`가 없을 수 있다는 것을 항상 고려해야 한다.
- 태그/조직/사용자 프로필 공개는 여전히 보류 중이다. 실사용에서 "질문은 보이는데 관련 태그를 클릭하면 로그인부터 요구한다"는 불일치가 문제로 확인되면 그때 넓힌다.
- 검색엔진 크롤링·소셜 공유 미리보기 카드는 접근 제어만으로는 완성되지 않는다(메타데이터가 없다) — 이번 결정은 "볼 수는 있다"까지이고, "검색 결과에 잘 나온다"는 다음 단계다.
- Spring Security의 URL 패턴이 컨트롤러 매핑과 어긋나면(오타, `{id}` vs `{questionId}` 등) 조용히 아무것도 막지 않거나(의도보다 넓게 열림) 반대로 의도한 걸 막을 수 있다 — `PublicReadAccessE2ETest`가 실제 필터 체인을 통과시켜 공개/비공개 양쪽을 모두 확인하므로, 새 공개 엔드포인트를 추가할 때는 이 테스트에도 케이스를 추가해야 한다.

## 관련 문서

- [ADR-0013](0013-defer-public-read-access.md) (이번 ADR이 재검토하고 좁게 확정한 원래 보류 결정)
- [api-design.md](../api-design.md#비로그인-공개-열람-phase-29)
- [PLAN.md](../../../PLAN.md) Phase 29
