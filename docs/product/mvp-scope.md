# Quno MVP 범위

> 제품 철학은 [vision.md](vision.md) 참고. 시스템/도메인 설계는 [../architecture/](../architecture/) 참고.

## MVP 핵심 가설

> **"개발자는 질문을 일회성 게시물이 아니라 지속적으로 관리되는 문제 객체로 사용하는 경험에서 가치를 느끼는가?"**

MVP의 목적은 기능 수를 늘리는 것이 아니라 이 가설을 최소 기능으로 검증하는 것이다. **질문 리비전, Ward(구독), 관련 질문 연결**을 핵심 경험 3축으로 둔다.

## P0 — 반드시 구현

| 영역 | 기능 |
|---|---|
| Account | 회원가입, 로그인, 기본 프로필 |
| Question | 질문 생성(Qv1), 제목/본문/태그/환경정보/에러·로그 첨부 |
| Question Revision | Qv1→Qv2 리비전, Revision History, Diff |
| Answer | 답변 작성, 특정 QuestionVersion 대상 지정, 채택 |
| Status | 최소 상태: `OPEN → NEEDS_INFO → UPDATED → RESOLVED` |
| Ward | 질문 구독/해제, 새 Revision·새 Answer·Resolved 알림 |
| Tag | 태그 등록/검색, 태그 팔로우 |
| Related Questions | 태그·에러 텍스트·기본 유사도 기반 유사 질문 추천 |
| Search | 제목/본문/태그/에러 코드 검색 |

### 기능 상세 규칙

- **질문 생성**: `Question`과 `QuestionVersion`을 분리한다. `Question`은 식별자·작성자·상태·최신 버전 포인터를 가지고, 실제 콘텐츠는 `QuestionVersion`에 저장한다. 생성 시 Question과 Qv1을 같은 트랜잭션에서 만들고 초기 상태는 `OPEN`이다.
- **질문 리비전**: 기존 row를 수정하지 않고 새 `QuestionVersion`을 append한다. `version_number`를 증가시키고 `Question.latest_version_id`를 갱신한다. `QUESTION_REVISION` 도메인 이벤트를 발행해 검색 재색인과 Ward 알림을 트리거한다.
- **답변과 채택**: 답변은 Question과 분리된 Aggregate다. 질문 작성자만 채택할 수 있다. 채택 시 `Question.accepted_answer_id`를 지정하고 상태를 `RESOLVED`로 전환한다.
- **Ward**: 북마크가 아니라 질문의 변화 구독이다. 중복 Ward는 허용하지 않는다.
- **검색/관련 질문**: 초기에는 lexical search + 태그 매칭으로 시작하고, 이후 vector/hybrid search로 확장한다.

## P1 — 핵심 경험 강화

빠르게 뒤따라 추가한다.

| 기능 | 내용 |
|---|---|
| 유사 질문 추천 고도화 | 라이트 클러스터링 |
| 라이트 대시보드 | 오늘의 인기 질문 Top 5, 내 Ward 업데이트, 팔로우 태그 피드, 태그 트렌드 |
| 사용자 프로필 라이트 | 작성 질문/답변, 관심 태그 노출 |

## MVP에서 제외 (Later)

- QPR Review / Needs Info / Re-request 전체 플로우
- Super Answer, Cluster 편집 UI
- QunoBot 자동 Outdated 판정
- Question Fork
- Flow(릴스형 소비 UI), Instant Question
- Direct Ask 및 결제
- Organization / 회사·학교 네트워크
- Architecture Canvas, 실시간 질문방(Live Chat)
- 포스팅/블로그 기능
- 복잡한 Reputation Economy

이 기능들은 모두 가치가 있지만 **"질문이 살아있다"는 핵심 가설을 검증한 이후**에 추가한다.

## 로드맵 (Phase)

| Phase | 목표 | 주요 범위 |
|---|---|---|
| **MVP (Phase 1)** | Living Question 검증 | Revision, Answer, Ward, Tag, Search, Related Question, 라이트 대시보드 |
| Phase 2 | 협업형 QPR | Review/Needs Info/Re-request, 답변-질문버전 연결 고도화 |
| Phase 3 | 질문 네트워크 | Cluster, Merge/Fork, Super Answer, 지식 그래프 시각화 |
| Phase 4 | 자동 유지보수 | QunoBot, 기술 버전 영향 감지, Outdated/Regression, Spike Detection |
| Phase 5 | 신뢰 네트워크 | Organization, 전문가 평판, Direct Ask |
| Phase 6 | 소비 경험 강화 | Quno Flow, Instant Question, 실시간 질문방, 고급 Daily Dashboard |

## 성공 지표

| 지표 | 검증하려는 가설 |
|---|---|
| **Question Revision Rate** | 사용자가 질문을 정적 게시물이 아닌 개선 가능한 객체로 받아들이는가 |
| **Ward Adoption / Ward Revisit Rate** | 질문의 변화를 구독할 가치가 있는가, 알림이 실제 재방문을 만드는가 |
| Related Question CTR | 질문 간 연결이 문제 해결을 가속하는가 |
| Answer / Accept Rate | 기본 Q&A 해결력이 있는가 |
| Tag Feed CTR | 최소 개인화가 유효한가 |
| D1/D7 Retention | 검색 순간에만 쓰이는 도구를 넘어서는가 |

### North Star Metric 후보

> **주간 활성 Living Questions (Living Question Rate)**
> 일정 기간 동안 Revision, Answer, Ward, Status Change 중 하나 이상의 후속 지식 이벤트가 발생한 질문의 비율/수.

절대 목표치보다 코호트별 변화와 기능별 전환을 관찰하는 것을 우선한다. 특히 **Revision 생성**과 **Ward**가 기존 Q&A와 다른 행동 패턴을 실제로 만드는지가 MVP의 가장 중요한 검증 대상이다.
