# ADR-0063: 질문 조회수 — 명시적 POST 집계와 Redis 기반 중복 제거

- 날짜: 2026-10-09
- 상태: 승인됨

## 배경 (Context)

디자인 시안([ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md))의 피드 카드와 질문 상세에는 조회수가 있다. 하지만 백엔드에는 조회수 집계 기능 자체가 없어서 ADR-0061·[ADR-0062](0062-question-activity-signals-and-timeline.md) 모두 이 항목을 보류했다. 사용자 요청으로 구현한다.

결정이 필요한 지점은 세 가지다.

1. 무엇을 "조회"로 셀 것인가
2. 같은 사람의 반복 조회를 어떻게 걸러낼 것인가
3. 카운터를 어디에 저장할 것인가

## 결정 (Decision)

- **`GET /questions/{id}`에서 세지 않고 `POST /questions/{id}/views`로 센다.**
  - 이유: 질문 상세 GET은 Next.js `generateMetadata`가 서버에서 한 번 더 부르고(ADR-0043), 크롤러와 링크 미리보기도 부른다. GET에서 세면 실제 독자 수보다 부풀려진다.
  - 프론트 질문 상세 페이지가 마운트될 때 한 번 호출한다(`useRecordQuestionView`). 실패는 무시한다. 조회수를 놓쳐도 읽기 흐름을 깨면 안 되기 때문이다.
  - 질문 열람이 공개(ADR-0041)이므로 이 POST도 비로그인 허용이다.
- **같은 viewer의 30분 내 재조회는 한 번으로 친다.**
  - Redis `SET question:view:{questionId}:{viewerKey} NX EX 1800`이 성공할 때만 센다.
  - viewer key: 로그인 사용자는 `u:{userId}`, 비로그인 방문자는 `a:` + SHA-256(IP + User-Agent) 앞 16바이트다. 원본 IP는 어디에도 저장하지 않고, 키도 30분 뒤 사라진다.
  - Redis 장애 시에는 중복 제거 없이 센다(ADR-0056의 "Redis는 보조" 정책). Redis 장애로 조회 기능 자체를 실패시키지 않는다.
- **카운터는 `questions` 컬럼이 아니라 별도 테이블 `question_view_counts`에 둔다.**
  - `INSERT ... ON CONFLICT DO UPDATE SET view_count = view_count + 1`로 원자적으로 증가한다.
  - `questions` 컬럼을 기각한 이유는 두 가지다.
    - Question 애그리거트는 전체 행을 저장하므로, 동시에 증가된 카운트를 낡은 값으로 덮어쓸 위험이 있다.
    - 조회가 `questions.updated_at`(피드의 "최근 활동", ADR-0062)을 움직이면 안 된다.
- **노출**: 질문 상세(`QuestionResponse.viewCount`)와 질문 요약(`QuestionSearchResultResponse.viewCount`)에 담는다. 요약 쪽은 `QuestionSummaryHydrator`에서 다른 신호와 같은 방식으로 일괄 조회한다.

## 결과 (Consequences)

- 피드 카드의 숫자 열에 "1.8k 조회" 형식으로, 질문 상세 메타에 "조회 1,834"로 표시된다.
- 봇이 JavaScript를 실행하지 않으면 집계되지 않는다. 반대로 IP+UA를 바꿔가며 반복 호출하면 부풀릴 수 있다. 조회수는 정렬·평판·배지 어디에도 쓰이지 않는 참고 지표라서 이 정도 정확도로 충분하다고 판단했다. 조회수를 순위나 보상에 쓰게 되면 레이트 리밋이나 봇 필터링을 함께 재검토한다.
- 리버스 프록시 뒤에 배포하면 `request.remoteAddr`가 프록시 주소가 되어 비로그인 방문자가 한 명으로 합쳐진다. 그때는 `server.forward-headers-strategy`를 설정해야 한다. 현재 로컬/단일 서버 구성에서는 해당하지 않는다.
- 기존 질문의 조회수는 0에서 시작한다. 과거 데이터는 없다.

## 관련 문서

- [docs/architecture/api-design.md — 질문 조회수](../api-design.md#질문-조회수-adr-0063)
- [ADR-0056](0056-redis-cache-aside-graceful-degradation.md), [ADR-0041](0041-narrow-public-read-access.md)
