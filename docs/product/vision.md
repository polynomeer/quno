# Quno 제품 비전

> 원본 근거: [docs/archive/](../archive/README.md) 핵심 계열 4개 문서를 통합·정리했다.

## 한 줄 정의

Quno는 소프트웨어 질문을 일회성 게시물이 아니라 버전 관리되고 연결되고 병합되며 지속적으로 진화하는 **Living Question**으로 관리하는 개발자 지식 플랫폼이다.

> **"질문카드는 죽어있어서는 안 됩니다. 살아있는 것처럼 계속 움직이고, 버전업되고, 뭉쳐져야 합니다."**

가장 중요한 한 문장: **Questions should never die.**

## 이름의 의미

| 해석 | 의미 | 브랜드 메시지 |
|---|---|---|
| Question + Uno | 하나의 질문에서 지식 생태계가 시작된다 | It starts with one question. |
| Q + Know | 질문(Q)이 앎(Know)으로 이어진다 | From Q to Know. |

브랜드 문구: **"Living Questions. Growing Knowledge."**

## 해결하려는 문제

기존 Q&A(Stack Overflow류)의 데이터 모델은 `Question → Answers → Accepted Answer`에서 멈춘다. 여기서 발생하는 문제:

| 기존 Q&A의 문제 | Quno의 방향 |
|---|---|
| 질문이 시간의 영향을 반영하지 못함 (기술 버전이 바뀌어도 과거 답이 그대로 남음) | 기술 버전을 질문의 구조적 메타데이터로 관리하고, 버전 변화에 따라 재검토를 유도 |
| 질문 수정 시 과거 맥락이 사라짐 | 질문 리비전(Qv1→Qv2→...)과 diff로 변화 과정을 보존 |
| 답변이 어느 시점의 질문을 대상으로 했는지 불명확 | 답변이 특정 QuestionVersion을 target으로 명시 |
| 유사 질문이 각각 고립되고 중복은 단순 삭제됨 | 중복은 "문제가 중요하다는 신호"로 보고 Cluster로 발전시킴 |
| 사용자가 해결 이후 다시 방문할 이유가 약함 | Ward(구독), 신문형 대시보드, 개인화 피드 제공 |

## 핵심 개념 — Living Question Card

Quno의 핵심 도메인 객체는 **Question Card**이며 다음 특성을 가진다.

| 특성 | 의미 |
|---|---|
| Versioned | 질문의 변경 이력이 보존된다 (덮어쓰기 없음) |
| Reactive | 답변, 채택, 기술 버전 변화 등 외부 이벤트에 상태와 관심도가 반응한다 |
| Connected | 다른 질문·답변·기술·사람과 연결된다 |
| Mergeable / Composable | 유사 질문과 묶이거나 병합되고, 상위 지식(Super Answer)으로 진화한다 |
| Forkable | 특정 환경·조건에 맞게 파생될 수 있다 |
| Observable / Watchable | 사용자가 질문의 변화를 Ward(구독)로 추적할 수 있다 |
| Context-aware | 기술 스택 및 버전 맥락을 가진다 |
| Evolvable | 시간이 지나도 새로운 지식을 반영할 수 있다 |

> Question ≠ Post. **Question = Living Knowledge Object.**

## Question Lifecycle

```text
Qv1 생성 (OPEN)
   │
   ├── 답변자가 추가 정보 요청 → NEEDS_INFO
   │
   ▼
Qv2, Qv3 ... (로그/환경/재현정보 보강)
   │
   ▼
답변 등록 → 채택 → RESOLVED
   │
   ├── 유사 질문과 연결 → Cluster
   │
   ▼
Super Answer (클러스터의 대표 해결책)
   │
   ├── 기술 버전 변화 감지 → OUTDATED
   │
   ▼
새 Revision / Reopen (다시 살아남)
```

`RESOLVED`는 질문의 죽음이 아니라 "현재 조건에서 해결됨"을 뜻하는 하나의 상태일 뿐이다.

## 질문 상태 모델 (목표 상태 — MVP는 [mvp-scope.md](mvp-scope.md) 참고)

```text
OPEN → NEEDS_INFO → READY_FOR_REVIEW → IN_REVIEW → ANSWERED → RESOLVED
                                                        │
                                    ┌───────────────────┼───────────────┐
                                    ▼                    ▼               ▼
                              DUPLICATED             MERGED         OUTDATED → (새 Revision) → REOPENED
```

`DUPLICATED`, `MERGED`, `OUTDATED`는 삭제가 아니라 지식 그래프 안의 관계/상태로 남는다.

## 다른 서비스에서 차용하는 개념

| 출처 | 개념 | Quno 적용 |
|---|---|---|
| GitHub | Pull Request / Commit / Review / Watch / Fork / Merge | QPR(Question Pull Request): 질문 리비전, Review(정보 요청), Re-request, Ward, Question Fork/Merge |
| Jira | Issue Workflow / Watcher | 질문 상태 모델(OPEN/NEEDS_INFO/RESOLVED/REOPENED 등) |
| LinkedIn | 조직·학교 관계망(Social Graph) | 회사/학교/커뮤니티 기반 전문가 추천, Direct Ask |
| Dependabot | 의존성 변화 감지 | QunoBot: 기술 버전 변화에 따른 Outdated/Regression 알림 |
| 논문 생태계 | 인용·관련 연구 축적 | 질문 간 연결, Cluster, 지식 계보 추적 |

이 개념들은 기능 나열이 아니라 "질문이 살아 움직인다"는 철학을 구현하는 수단으로만 차용한다.

## Quno vs Quora vs Stack Overflow

| 영역 | Quora | Stack Overflow | Quno |
|---|---|---|---|
| 질문 | 콘텐츠 | 기술 질문 | Living Question |
| 질문 Revision | 약함 | 수정 가능 | 핵심 기능 |
| 기술 Version | 거의 없음 | Tag 중심 | 구조화된 메타데이터 |
| Duplicate | 존재 | Close | Cluster/Merge |
| Watch | 제한적 | Follow | Ward |
| Social Graph | 강함 | 약함 | 전문성 + 조직 Graph |
| 지속적 지식 갱신 | 약함 | 수동 | QunoBot |

## 제품 원칙 (기능 우선순위 판단 기준)

새 기능을 검토할 때 아래 5가지 질문으로 판단한다.

1. **이 기능은 질문을 더 살아 움직이게 만드는가?** — 아니라면 우선순위를 낮춘다.
2. **질문 간 연결을 증가시키는가?** — Quno의 장기 자산은 질문 수가 아니라 질문 사이의 관계다.
3. **시간이 지날수록 데이터의 가치가 증가하는가?** — 오래된 질문을 버리지 않고 새 정보와 연결한다.
4. **AI가 지식을 생성하는 것이 아니라 인간의 경험을 구조화하는가?** — Quno의 강점은 실제 개발자의 질문·실패·해결·환경·토론·검증 데이터이며, AI는 이를 정리·연결하는 역할을 한다.
5. **사람의 전문성을 발견할 수 있게 만드는가?**

## 가장 중요한 차별화

Quno의 경쟁력은 기능 목록이 아니라 **데이터 모델 자체**에 있다.

```text
기존 Q&A:  Question → Answers

Quno:      Question ─ Revision ─ Revision ─┬─ Fork
             │                             └─ Answer
             ├── Error / Technology Version / Architecture
             ├── Related Question
             ▼
           Cluster → Super Answer → New Version
```

> **Quno의 Moat는 질문의 양이 아니라 Question Graph의 밀도에 있다.**

## 장기 비전

질문이 충분히 쌓이면 Quno는 단순 검색 결과 목록이 아니라 문제 지식 그래프를 보여준다. 예: `RedisCommandTimeoutException` 검색 시 관련 질문 수, 주요 Problem Cluster, 기술 버전별 분기, 공통 원인, Super Answer, 최근 회귀, 전문가 목록을 함께 제시하고 사용자는 자신의 상황에 맞는 branch로 진입한다.
