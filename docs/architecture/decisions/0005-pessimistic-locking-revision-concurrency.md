# ADR-0005: 리비전 생성 동시성 방어로 Pessimistic Locking(SELECT ... FOR UPDATE) 채택

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

ADR-0004에 따라 `version_number`는 질문마다 단조 증가해야 한다. 가장 단순한 구현인 `MAX(version_number) + 1`은 두 리비전 요청이 동시에 들어오면 같은 다음 번호를 계산해 경쟁 조건(중복 버전 번호 또는 unique 제약 위반)을 일으킨다. Optimistic locking(버전 컬럼 + 재시도)과 Pessimistic locking(`SELECT ... FOR UPDATE`) 중 하나를 골라야 했다.

## 결정 (Decision)

`QuestionRepository.findByIdForUpdate`로 리비전 대상 Question row에 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 걸어 리비전 생성을 순차화한다. `(question_id, version_number)` unique 제약은 최후 방어선으로 유지하되, 정상 경로에서는 락으로 경쟁 자체를 막는다.

Optimistic locking + 재시도 대신 Pessimistic locking을 고른 이유: 같은 질문에 대한 동시 리비전은 흔한 경우가 아니고(한 질문을 여러 사람이 동시에 리비전하는 상황은 드묾), 재시도 로직의 복잡도보다 짧은 락 구간(트랜잭션 하나)의 단순함이 낫다고 판단했다.

## 결과 (Consequences)

- 동일 질문에 대한 리비전 요청이 동시에 여러 건 들어와도 버전 번호가 중복되거나 건너뛰지 않는다 — 8개 스레드로 동시 리비전하는 통합 테스트(`ReviseQuestionConcurrencyIntegrationTest`)로 실제 Postgres 트랜잭션 기준 검증했다.
- 이 불변식은 인메모리 fake로는 재현할 수 없다 — 실제 DB 락이 필요하므로 `@SpringBootTest` 통합 테스트로만 검증 가능하다는 점을 테스트 전략(ADR-0011)에도 반영했다.
- 같은 질문에 대한 리비전 트래픽이 실제로 매우 높아지면(예: 여러 리뷰어가 동시에 정보 요청/재요청을 유발하는 QPR Review, ADR-0012) 락 대기가 병목이 될 수 있다 — 그 시점에 optimistic locking + 재시도로 전환을 재검토한다.

## 관련 문서

- [domain-model.md](../domain-model.md#테이블별-책임과-삭제-정책) "Revision 생성의 동시성 주의"
- [system-architecture.md](../system-architecture.md#question-aggregate-설계-원칙)
- [PLAN.md](../../../PLAN.md) Phase 2.3, 2.10
- 커밋 `04306a1` (Question Revision 구현), `ef5c93c` (동시성 통합 테스트)
