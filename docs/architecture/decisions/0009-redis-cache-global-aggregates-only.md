# ADR-0009: Redis 캐시는 "모든 사용자에게 동일한" 전역 집계에만 적용하고 TTL 만료로만 갱신

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

라이트 대시보드(`GET /dashboard`)는 4개 섹션(인기 질문, 내 Ward 업데이트, 팔로우 태그 피드, 태그 트렌드)을 조합한다. 이 중 인기 질문/태그 트렌드는 비용이 큰 native 집계 쿼리이면서 모든 사용자에게 같은 결과를 주지만, Ward 업데이트/팔로우 태그 피드는 사용자마다 다르고 최신성이 정확도에 직결된다(내 알림이 오래된 값으로 보이면 버그처럼 느껴진다). 4개 섹션 전부를 캐시할지, 일부만 캐시할지, 캐시 무효화를 능동적으로 할지 TTL만 쓸지 정해야 했다.

## 결정 (Decision)

`popularQuestions`/`trendingTags` **두 섹션만** cache-aside로 캐시한다. `wardUpdates`/`followingTagsFeed`는 캐시하지 않는다.

- `DashboardRepositoryAdapter`가 `StringRedisTemplate`으로 키를 먼저 조회하고, miss 시 native 쿼리를 실행한 뒤 Jackson으로 직렬화해 저장한다.
- 키: `dashboard:popular-questions:{limit}`, `dashboard:trending-tags:{limit}`. TTL: 60초.
- 질문/답변/와드 생성 시점에 캐시를 능동적으로 무효화하지 않는다 — "트렌드"류 데이터는 약간의 지연이 자연스럽다고 판단했다.

## 결과 (Consequences)

- 캐시가 실제로 읽히는지(단순 재계산이 아닌지)는 redis-cli로 sentinel 값을 주입해 API가 그 값을 그대로 반환하는 것을 확인하는 방식으로 검증했다 — 앞으로 캐시 관련 기능을 검증할 때도 이 3단계(키/TTL 생성 확인 → sentinel 주입 후 그대로 반환되는지 확인 → 키 삭제 후 재조회 시 갱신되는지 확인) 절차를 재사용한다.
- 사용자별 섹션은 항상 최신 상태를 보장하지만, 그만큼 요청마다 DB를 조회한다 — 사용자별 캐시가 필요해지면(예: 개인화 피드 계산 비용이 커지면) 사용자 단위 키 + 짧은 TTL 또는 쓰기 시점 무효화를 재검토한다.
- TTL만으로 갱신하기 때문에 "트렌드가 최대 60초 뒤처질 수 있다"는 것은 의도된 동작이다. 더 강한 신선도가 필요해지면 능동적 무효화를 추가한다.

## 관련 문서

- [api-design.md](../api-design.md#redis-캐시-phase-34)
- [PLAN.md](../../../PLAN.md) Phase 3.4
- 커밋 `d115a0c` (Redis 캐시 적용)
