# ADR-0011: 유스케이스 직접 호출 통합 테스트에 더해 MockMvc 기반 실제 HTTP E2E 테스트를 추가

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

Phase 2.10까지의 `integration/` 테스트(`TagSlugUniquenessIntegrationTest`, `ReviseQuestionConcurrencyIntegrationTest`)는 모두 유스케이스를 직접 `@Autowired`해 호출하는 방식이었다 — 실제 DB 제약/락은 검증하지만 컨트롤러, Request/Response DTO, `JwtAuthenticationFilter`를 포함한 보안 필터 체인은 전혀 거치지 않는다. Phase 4.2(E2E 시나리오 테스트: 질문 생성 → 리비전 → 답변 → 채택 → Ward 알림)를 기존 방식대로 짤지, HTTP 레이어까지 포함해서 짤지 결정해야 했다.

## 결정 (Decision)

`QuestionLifecycleE2ETest`는 `@SpringBootTest @AutoConfigureMockMvc`로 실제 HTTP 요청을 보내고, 회원가입/로그인으로 발급받은 진짜 JWT를 `Authorization` 헤더에 실어 보안 필터까지 통과시킨다. Outbox 소비는 2초 스케줄러 타이밍에 기대지 않고 `DispatchOutboxEventsUseCase`를 테스트에서 직접 호출해 결정적으로 만든다.

## 결과 (Consequences)

- 이 테스트 하나로 컨트롤러 라우팅, Request 유효성 검증, Response 직렬화, JWT 인증까지 한 번에 검증된다 — 기존 유스케이스-직접-호출 통합 테스트로는 잡을 수 없는 레이어의 회귀를 잡을 수 있다.
- `@Transactional`을 붙이지 않았다 — 각 HTTP 호출이 자기 트랜잭션에서 커밋되어야 스케줄러/후속 조회가 실제로 그 데이터를 보므로, 정리(cleanup)는 `ReviseQuestionConcurrencyIntegrationTest`와 같은 패턴으로 `@AfterEach` + `JdbcTemplate` 수동 삭제를 써야 한다. 이 대가는 의도적으로 받아들였다.
- 앞으로 컨트롤러 레이어까지 포함해 검증하고 싶은 새 시나리오(예: QPR Review E2E, PLAN.md 5.5)는 이 테스트를 템플릿으로 삼는다: 유스케이스 직접 호출 통합 테스트는 "실제 DB 제약/락처럼 fake로 재현 불가능한 규칙"에, MockMvc E2E는 "여러 도메인에 걸친 사용자 시나리오 전체"에 쓴다는 역할 분담을 이 ADR로 명문화한다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 4.2, 2.10
- 커밋 `28ee42c` (E2E 테스트 추가), `ef5c93c` (기존 통합 테스트)
