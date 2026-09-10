# ADR-0046: 신뢰성/데이터 — 회원 탈퇴, 개인정보 다운로드, N+1 제거

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

상용 전환 체크리스트([production-readiness.md](../../product/production-readiness.md) B-4)에 따라 배포(32)·보안(33)·관측 가능성(34)에 이어 신뢰성/데이터를 진행한다. 이 중 회원 탈퇴는 지금까지 전혀 없던 기능이라 새로 설계가 필요했다.

## 결정 (Decision)

1. **회원 탈퇴는 Row를 지우지 않고 PII만 익명화한다.** `User.withdraw()`가 `email`을 `withdrawn-user-{id}@quno.invalid`로, `nickname`을 `탈퇴한 사용자{id}`로 바꾸고(둘 다 UNIQUE 제약을 만족하도록 id를 접미사로 붙임), `isActive`를 false로, 비밀번호 해시를 로그인 불가능한 무작위 값으로 교체한다. `questions`/`answers`/`comments`의 `author_id` FK는 손대지 않는다 — Quno는 콘텐츠가 다른 사용자에게도 가치 있는 지식 베이스(Living Question Card)이므로, Stack Overflow류 플랫폼처럼 "작성자 익명화 + 콘텐츠 보존"이 표준적인 절충이다. 이미 존재하던 `User.isActive` 필드(지금까지 아무도 false로 설정하지 않던 휴면 필드)를 그대로 재사용해 로그인/토큰 갱신 차단 로직을 새로 만들 필요가 없었다.
2. **탈퇴는 현재 비밀번호 재확인을 요구한다.** `DELETE /api/v1/me`가 body로 비밀번호를 받아 로그인과 동일하게 검증한 뒤에만 처리한다 — 탈취된 access token 하나만으로 계정을 파괴할 수 없도록 하는 최소한의 방어다.
3. **개인정보 다운로드는 관리자 도구가 아니라 셀프서비스 API로 구현한다.** production-readiness.md는 원래 "최소한 관리자가 수동 처리할 수 있는 내부 도구 수준"을 제안했지만, 검토 결과 셀프서비스 `GET /api/v1/me/data-export`가 오히려 구현이 더 단순하고(관리자 권한 체크·화면이 필요 없음) 개인정보 자기결정권 취지에도 더 맞아 이쪽으로 범위를 조정했다. 응답은 프로필 + 직접 작성한 질문/답변까지만 포함한다(투표·Direct Ask·실시간 채팅 메시지는 범위 밖 — 최소 요건, 필요해지면 확장).
4. **외부 API 재시도는 대상에 따라 다르게 판단한다.** 메일 발송(`SmtpVerificationEmailSender`)은 실패해도 재시도가 안전해(최악의 경우 같은 안내를 두 번 받음) 짧은 지수 백오프로 최대 3회 재시도를 추가했다. 반대로 Toss 결제 확인(`TossPaymentGateway.confirm`)은 재시도를 추가하지 않았다 — 결제 확인 재호출이 실제로 멱등한지 이 프로젝트가 검증할 방법이 없어, 잘못 재시도했다가 중복 승인·이중 청구로 이어질 위험이 재시도 이득보다 크다고 판단했다. 두 외부 API 모두 이미 타임아웃(connect 5s/read 10s)이 있었다는 것도 확인했다(메일 발송에는 없어서 이번에 추가함).
5. **`QuestionSummaryHydrator`의 N+1을 배치 쿼리로 없앤다.** 검색/관련 질문/대시보드/클러스터 멤버 등 여러 곳이 공유하는 이 클래스가 랭킹된 id 목록을 질문 하나씩 조회(`findById`+`findTagsByQuestionId`+`sumScore`)하고 있어 N개 결과에 3*N개 쿼리가 나가고 있었다. `QuestionRepository.findAllByIds`/`QuestionTagRepository.findTagsByQuestionIds`/`VoteRepository.sumScoresByTargets`(각각 배치 버전, IN 절 + GROUP BY) 3개를 추가해 결과 개수와 무관하게 항상 3개 쿼리로 줄였다. 입력 id 순서(랭킹 순서)는 그대로 보존한다.
6. **DB 백업/복구 스크립트를 코드로 준비한다.** `scripts/db-backup.sh`/`scripts/db-restore.sh` — 둘 다 `PGHOST` 등 표준 libpq 환경변수가 있으면 원격 DB를, 없으면 로컬 docker-compose 컨테이너를 대상으로 한다. 주기 실행(cron 등)과 백업 파일의 실제 보관은 인프라를 구성하는 사람의 몫으로 남긴다. 로컬에서 실제로 백업→복구까지 리허설해 데이터가 온전히 복구되는 것을 확인했다.
7. **(발견) `GlobalExceptionHandler`의 catch-all(ADR-0045)이 클라이언트 오류까지 500으로 잘못 응답하고 있었다.** Phase 35 검증 중 잘못된 요청 본문을 보냈더니 `HttpMessageNotReadableException`(요청 파싱 실패, Spring MVC가 핸들러 호출 전에 던지는 프레임워크 예외)이 `Exception::class` catch-all에 걸려 500 + Sentry 리포팅으로 잘못 처리되는 것을 발견했다 — 원래는 Boot 기본 처리로 400이 나가던 경로였다. `HttpMessageNotReadableException`/`MethodArgumentTypeMismatchException` 전용 핸들러를 추가해 두 경우 다시 400으로 고쳤고, catch-all 자체에도 로그를 추가했다(Sentry는 DSN 없으면 조용히 아무것도 안 해서, 로컬에서 원인을 알 방법이 로그뿐이었다).

## 결과 (Consequences)

- 탈퇴한 사용자의 access token은 발급 당시 TTL(최대 30분)까지는 여전히 유효하다 — 스테이트리스 JWT 구조상 즉시 폐기할 방법이 없다(refresh token은 `isActive` 체크로 이미 막힘). 실사용에서 문제가 되면 그때 토큰 블록리스트(Redis) 도입을 재검토한다.
- 개인정보 다운로드가 질문/답변까지만 포함해 투표·Direct Ask 등은 별도 요청 시 수작업으로 대응해야 한다.
- `GlobalExceptionHandler`에 새 프레임워크 예외 타입이 필요해질 때마다(예: `HttpMediaTypeNotSupportedException`) 개별적으로 추가해야 한다 — `ResponseEntityExceptionHandler` 상속은 기존 `MethodArgumentNotValidException` 핸들러와 충돌 위험이 있어 채택하지 않았다.

## 관련 문서

- [production-readiness.md](../../product/production-readiness.md) B-4
- [0045-observability-logging-metrics-error-tracking.md](0045-observability-logging-metrics-error-tracking.md) (catch-all의 원출처)
- [PLAN.md](../../../PLAN.md) Phase 35
