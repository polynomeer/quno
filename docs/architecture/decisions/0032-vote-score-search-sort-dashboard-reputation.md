# ADR-0032: 검색 Score 정렬, Dashboard 인기순위, 평판 점수에 Vote 반영

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

[ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md) 5~7번은 Vote(Phase 11)를 처음 추가할 때 "실사용 패턴을 관찰한 뒤 가중치를 다시 설계한다"며 검색 Score 정렬, Dashboard 인기순위 공식 개정, 평판 점수 반영 세 가지를 의도적으로 보류했다. Quno는 아직 실제 트래픽이 없는 개발 단계 프로젝트라 "실사용 패턴 관찰"이 현실적으로 불가능하다 — 이번 결정은 관찰 데이터 대신 기존 가중치 체계(Dashboard의 `watch*3 + answer*2`, Reputation의 `accepted*15 + superAnswer*10 + answer*2 + question*1`)와의 상대적 비중을 근거로 한 판단이며, 실사용 후 조정 여지를 열어둔다.

## 결정 (Decision)

1. **`GET /search`에 `sort` 파라미터를 추가한다** — `relevance`(기본값, 기존 동작 그대로 유지)와 `score`(질문이 받은 순 투표 점수 내림차순, 동점이면 `id` 내림차순) 두 가지만 지원한다. **`relevance`는 실제로는 텍스트 관련도 랭킹이 아니라 `id DESC`(최신순 근사)다** — 지금 검색 쿼리 자체가 `ts_rank` 없이 `to_tsvector @@ plainto_tsquery` 매칭 후 `ORDER BY q.id DESC`로 구현되어 있어(코드가 근거, ADR-0023 7번이 우려했던 "전문검색 랭킹과의 상호작용"은 애초에 그런 랭킹이 없어서 발생하지 않는다), 새 `score` 정렬을 기존 동작과 나란히 추가하는 것뿐이다. 후보 질문 집합(텍스트/태그 매칭 결과)은 두 정렬 모드에서 동일하고, 최종 정렬 기준만 다르다.
2. **Dashboard 인기 질문 순위 공식에 투표 점수를 더한다** — `watch_count*3 + answer_count*2 + vote_score*1`. 투표 점수의 가중치를 가장 낮게(1) 둔 이유: Watch/답변 작성은 사용자가 실제 비용(구독/글쓰기)을 들이는 강한 신호인 반면 투표는 클릭 한 번으로 끝나는 약한 신호이기 때문. `vote_score`는 순 합산(음수 가능)을 그대로 더해 — 비호감 질문이 실제로 순위에서 페널티를 받는 것을 의도된 동작으로 본다.
3. **평판 점수 공식에 "자신의 질문/답변이 받은 순 투표 점수"를 더한다** — 기존 `questionCount*1 + answerCount*2 + acceptedAnswerCount*15 + superAnswerCount*10`에 `voteScoreReceived*1`을 추가한다. 이 값은 Badge(Phase 15)의 `WELL_RECEIVED` 배지가 이미 쓰는 `BadgeRepository.sumVoteScoreReceived(userId)` 쿼리를 그대로 재사용한다 — Badge와 Reputation이 이미 같은 Bounded Context(활동 기반 신뢰 신호)로 묶여 있어 새 쿼리를 만들지 않고 포트를 공유한다. 가중치를 1로 낮게 둔 이유는 Dashboard와 동일(약한 신호)하며, 어뷰징 방지 로직(예: 신규 계정 투표 무시, 투표 조작 탐지)은 이번 범위에 포함하지 않는다 — ADR-0023 4번이 우려했던 "점수 조작 유인"은 여전히 남아 있는 트레이드오프로 인지하고 넘어간다.
4. **투표를 알림/이벤트 소스로 바꾸지 않는다** — ADR-0023 6번의 "투표는 알림을 발생시키지 않는다"는 그대로 유지한다. 이번 변경은 모두 조회 시점 집계일 뿐, 새 이벤트를 만들지 않는다.
5. **어뷰징 방지, Score/Date/Answered 필터 확장은 범위 밖이다** — 자기 투표 금지(기존 `SelfVoteException`) 외의 추가 방어(예: 투표 속도 제한, 봇 탐지)나 검색의 Date/Answered 필터([ADR-0022](0022-search-filters-client-side-tag-and-status-only.md)가 별도로 보류한 항목)는 이번 결정에 포함하지 않는다.

## 결과 (Consequences)

- 세 공식 모두 조회 시점 집계라 스키마 변경이 없다 — `votes` 테이블 자체는 Phase 11에서 이미 있고, 이번엔 그 값을 세 곳(검색 정렬, Dashboard 랭킹, 평판 점수)에 추가로 소비할 뿐이다.
- 가중치(모두 1)는 실사용 데이터 없이 정한 값이라 추측에 가깝다 — 실사용 후 신호 대비 체감 순위가 이상하면 가중치만 조정하면 되도록 각 공식을 한 곳에 모아뒀다(`UserReputation.score`, `DashboardJpaRepository.findPopularQuestionIds`).
- Dashboard의 인기 질문 순위 캐시(Redis, TTL 60초, [ADR-0009](0009-redis-cache-global-aggregates-only.md))는 그대로 유지된다 — 공식이 바뀌어도 캐시 전략 자체는 영향받지 않는다.
- 평판 점수가 투표로 조작될 여지가 여전히 남아 있다 — 실제 악용 사례가 관측되면 별도 ADR로 방어 로직을 추가한다.

## 관련 문서

- [ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md)(이 결정이 보류했던 5~7번을 이 ADR이 재검토함)
- [ADR-0018](0018-simple-reputation-score-only.md)(평판 점수 공식의 최초 결정)
- [ADR-0027](0027-badge-as-computed-read-model-no-award-events.md)(재사용하는 `sumVoteScoreReceived` 쿼리의 출처)
- [ADR-0022](0022-search-filters-client-side-tag-and-status-only.md)(검색 Score 정렬을 처음 보류한 프론트엔드 결정)
- [PLAN.md](../../../PLAN.md) Phase 20
