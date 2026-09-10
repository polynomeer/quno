# Quno 상용 서비스 전환 체크리스트

> MVP 백로그(Phase 1~31)는 모두 완료됐다([roadmap.md 7절](../frontend/roadmap.md#7-백엔드-격차-요약과-착수-전-확인-사항)). 이 문서는 "기능이 다 있다"에서 "실제 사용자에게 돈을 받고 운영할 수 있다"로 넘어가기 위해 필요한 작업을 정리한다. 품질(성능/UX/코드 품질) 개선은 범위가 달라 [quality-improvement-plan.md](quality-improvement-plan.md)로 분리했다.

## 이 문서의 원칙

1. **사람만 할 수 있는 일**과 **코드로 구현 가능한 일**을 명확히 나눈다. 전자는 계약·법률·계정 개설처럼 Claude Code가 대신 처리할 수 없는 일이다.
2. Claude Code는 **코드로 구현 가능한 일부터** 우선 진행한다. 각 항목은 CLAUDE.md의 리듬(설계 필요 시 ADR → PLAN.md 체크리스트 → 구현 → 검증 → 커밋)을 그대로 따른다.
3. "사람이 할 일"이 선행되지 않으면 의미가 없는 코드 작업(예: 실제 PG 연동은 가맹점 계약 없이는 테스트 키 이상으로 못 감)은 **그 사실을 코드/설정에 명시적으로 남기고, 실 계약 전까지는 안전한 기본값(현재 상태)을 유지**하는 방식으로 진행한다.

## A. 사람이 해야 할 일

Claude Code가 실행할 수 없는 항목이다. 코드 작업과 별개로 사용자가 병행해야 한다.

| 영역 | 할 일 | 비고 |
|---|---|---|
| 법인/사업자 | 사업자 등록(또는 개인사업자), 통신판매업 신고 | Direct Ask가 유료 결제를 다루므로 전자상거래법상 필요 |
| 결제 | 토스페이먼츠 가맹점 심사·계약, 실 시크릿 키 발급 | 지금은 ADR-0037에 따라 공개 테스트 키로만 동작 |
| 법률 문서 | 이용약관, 개인정보처리방침, 환불 정책 초안 작성 및 법률 검토 | 초안은 Claude Code가 쓸 수 있으나 최종 검토·확정은 사람 책임 |
| 개인정보보호 | 개인정보보호책임자 지정, 개인정보 처리방침 공개 페이지 등록 | 정보통신망법/개인정보보호법 대응 |
| 도메인/인프라 계정 | 실 도메인 구매, 클라우드 계정(AWS/GCP 등) 개설과 결제수단 등록 | 계정 생성 자체는 브라우저 조작이 필요해 사람이 해야 함 |
| 이메일 발신 | 실제 발신 도메인의 SPF/DKIM/DMARC 레코드 등록 | 도메인 DNS 소유자 권한 필요, 현재는 Mailpit 로컬 캐처만 사용 중(ADR-0035) |
| 고객 지원 | 문의 채널(이메일/카카오톡 채널 등) 개설, 응대 인력/프로세스 | |
| 모니터링 알림 수신 | Slack/이메일 등 알림을 받을 실제 채널 개설, 온콜 담당자 지정 | Claude Code는 알림을 "보내는" 코드까지만 구현 가능 |
| 보안 계정 | 클라우드 IAM 최소권한 계정 발급, 시크릿 매니저(예: AWS Secrets Manager) 프로비저닝 | 코드는 이를 "사용하는" 로직까지 구현, 실제 계정 생성은 사람 |

## B. 코드로 구현 가능한 일 — 우선 진행 대상

우선순위 순으로 나열한다. 각 Phase는 PLAN.md에 착수 시점에 등록한다.

### B-1. 배포 파이프라인 (최우선 — 이게 없으면 "상용"이 성립하지 않는다)

- [x] 백엔드 `Dockerfile`(`backend/Dockerfile`) — 멀티스테이지(JDK로 빌드 → JRE로 실행), non-root 유저. 로컬 Docker로 이미지 빌드+기동+헬스체크까지 실측 검증
- [x] 프론트엔드 `Dockerfile`(`frontend/Dockerfile`) — `next.config.ts`에 `output: "standalone"` 추가 후 멀티스테이지 빌드, non-root 유저. 로컬 Docker로 이미지 빌드+기동+HTTP 200 응답 실측 검증
- [x] `application-prod.yml` 프로필 신설(`backend/src/main/resources/application-prod.yml`) — JWT secret/Toss 키/DB·Redis·Mongo·메일 접속정보를 전부 `${VAR}`(기본값 없음) 플레이스홀더로 참조
- [x] 필수 환경변수 검증 — 위 프로필의 `${VAR}` 문법 자체가 검증 수단(Spring의 표준 fail-fast 패턴). 필수값 하나라도 비면 `PlaceholderResolutionException`으로 기동 실패, 전부 채우면 정상 기동하는 것을 Docker 컨테이너로 양쪽 다 실측 확인
- [x] `.env.example` 작성(저장소 루트) — prod 프로필이 요구하는 환경변수 전체와 프론트엔드 빌드 인자를 문서화
- [x] GitHub Actions CI(`.github/workflows/ci.yml`) — PR/main 푸시마다 백엔드(docker compose로 인프라 기동 후 `./gradlew test`)와 프론트엔드(`npm run lint`/`npm test -- --run`/`npm run build`) 자동 실행. 로컬에서 각 명령이 실제로 통과하는 것을 확인(백엔드 테스트는 기존에 이미 검증된 스위트라 재실행은 생략)
- [ ] (선택, 배포 대상 확정 후) CD 파이프라인 — 사람이 클라우드 계정/대상을 정한 뒤 착수

### B-2. 보안 강화

- [x] CORS 허용 오리진을 환경변수화 — `CorsProperties`(`quno.cors.allowed-origins`) 신설, prod는 `QUNO_CORS_ALLOWED_ORIGINS`(콤마 구분, 필수) 참조. 허용 오리진은 200, 그 외는 403인 것을 실측 확인
- [x] 보안 헤더 추가 — `SecurityConfig`의 `headers {}` DSL로 `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, HSTS(HTTPS 응답에만 적용) 추가. 실제 응답 헤더로 확인
- [x] Rate limiting 도입 — `RateLimitFilter`(Bucket4j 인메모리 토큰 버킷)를 로그인/회원가입/토큰 갱신/Direct Ask 결제 확인에 적용. 한도는 `RateLimitProperties`로 분리해 로컬/테스트는 사실상 무제한, prod는 IP당 분당 10회로 좁힘(테스트 스위트가 같은 Spring 컨텍스트를 공유하며 로그인/회원가입을 여러 번 호출하는 것과 충돌하지 않도록). 11번째 요청부터 429 응답을 실측 확인, 전체 테스트 스위트(339개) 회귀 없음 확인
- [x] 의존성 취약점 스캔 자동화 — `.github/dependabot.yml` 신설(Gradle/npm/GitHub Actions 세 생태계, 주간)
- [x] Actuator 프로덕션 노출 최소화 — 기존에 이미 안전하게 구성돼 있었음을 재확인: 노출 엔드포인트는 `health,info`뿐이고 `show-details`는 베이스 기본값이 `never`(로컬만 `always`로 재정의, prod는 손대지 않아 `never` 유지)
- [x] 시크릿 하드코딩 감사 — Phase 32(B-1)의 필수 환경변수 fail-fast 검증으로 이미 커버됨(JWT secret/Toss 키가 비면 prod 기동 자체가 실패)

### B-3. 관측 가능성(Observability)

- [x] 구조화된 JSON 로깅 — Spring Boot 4 내장 기능(`logging.structured.format.console: ecs`)을 prod 프로필에만 적용, 별도 의존성 없음. Docker 컨테이너로 실제 ECS JSON 출력 확인
- [x] 요청 추적 ID(Correlation ID) — `RequestIdFilter` 신규(`X-Request-Id` 헤더 읽기/발급, MDC 저장). 클라이언트가 보낸 ID 그대로 반영, 없으면 새로 발급하는 것을 실측 확인. ECS 포맷이 MDC를 자동으로 로그에 실어줌
- [x] 에러 트래킹 SDK 통합 — Sentry의 Spring Boot Starter가 Spring Boot 4와 호환되지 않는 것을 발견(ApplicationContext 로드 실패)해 코어 SDK로 전환, `SentryConfig`가 직접 초기화. `GlobalExceptionHandler`에 예기치 못한 예외만 잡는 catch-all 추가. DSN 없으면 비활성화(계정 생성은 A 항목). 프론트엔드는 `@sentry/browser`로 같은 원칙 적용(ADR-0045)
- [x] Prometheus 메트릭 노출 — `micrometer-registry-prometheus` 추가, `/actuator/prometheus` 노출. 앱 차원 인증은 걸지 않음(Prometheus 서버가 우리 JWT를 받을 수 없음) — 실제 접근 통제는 인프라 레벨 책임(ADR-0045)
- [x] 프론트엔드 에러 바운더리 강화 — `app/error.tsx`(라우트 세그먼트), `app/global-error.tsx`(루트), `app/not-found.tsx`(전무했음) 신규 추가. 브라우저로 실제 렌더링 오류를 발생시켜 error.tsx가 정상 표시되는 것과 콘솔 리포팅이 호출되는 것을 확인

### B-4. 신뢰성 / 데이터

- [x] DB 백업 전략 스크립트화 — `scripts/db-backup.sh`/`scripts/db-restore.sh` 신규(로컬 docker-compose 또는 `PGHOST` 등 표준 libpq 환경변수로 원격 DB 지정). 실제로 백업→복구까지 리허설해 데이터가 온전히 복구되는 것을 확인
- [x] 외부 API(Toss 결제, 메일 발송) 재시도·타임아웃 정책 명시 — 둘 다 이미 타임아웃이 있었음을 확인(메일 발송에는 없어서 추가). 메일 발송은 재시도 추가(안전), Toss 결제 확인은 멱등성을 검증할 수 없어 재시도 도입을 의도적으로 보류(ADR-0046)
- [x] N+1 쿼리 점검 — `QuestionSummaryHydrator`(검색/관련 질문/대시보드/클러스터 등이 공유)가 N개 결과에 3*N개 쿼리를 내던 것을 발견해 배치 쿼리 3개로 교체(결과 개수와 무관하게 항상 3개). 신규 테스트로 순서 보존·누락 처리 동일함을 확인
- [x] 회원 탈퇴 시 개인정보 삭제/익명화 로직 점검 — 탈퇴 기능 자체가 전무했던 것을 확인해 신규 구현(`DELETE /api/v1/me`, ADR-0046). Row는 유지하고 PII만 익명화, 기존에 아무도 안 쓰던 `User.isActive` 휴면 필드를 재사용
- [x] 개인정보 다운로드/삭제 요청 API — `GET /api/v1/me/data-export`(프로필+직접 작성한 질문/답변)를 셀프서비스로 구현(원래 계획한 관리자 도구보다 단순하고 취지에 부합, ADR-0046). 삭제는 위 탈퇴 API가 커버

### B-5. 테스트

- [x] 프론트엔드 테스트 커버리지 확충 — 1개(`cn.test.ts`)에서 6개 파일·18개 테스트로 확충(http-client의 401 재발급/재시도 로직, token-storage, Button, AccountDangerZone). 극단적 불균형 완화 목적이며 전면적 커버리지는 아님(ADR-0047)
- [x] E2E 테스트 도입(Playwright) — `frontend/e2e/golden-path.spec.ts`(회원가입→질문 작성→답변). 준비 과정에서 회원가입 화면 자체가 전무했던 것을 발견해 함께 신설(`/signup`, `useSignUp`). Direct Ask 결제는 토스 호스팅 체크아웃 의존성 때문에 범위 밖(ADR-0047). CI에 `e2e` 잡 추가, 로컬 실행으로 실제 통과 확인
- [x] 부하 테스트 — `scripts/load-test.js`(k6, 비로그인 공개 읽기 경로) 작성. 로컬 docker-compose 대상 짧은 스모크 실행(VUs=3, 10초, 체크 100% 통과)으로 스크립트 동작만 확인 — 실제 용량 산정은 스테이징 인프라(A 항목 선행)가 갖춰진 뒤로 미룸(ADR-0047)

### B-6. 문서화

- [ ] OpenAPI/Swagger 자동 생성 — 현재 `docs/architecture/api-design.md`가 수기 문서, springdoc-openapi 도입으로 코드-문서 drift 방지
- [ ] 운영 런북(Runbook) 작성 — 장애 대응 절차, 롤백 절차, 온콜 체크리스트

## 진행 순서 제안

B-1(배포 파이프라인)이 없으면 나머지 전부가 "로컬에서만 검증된 코드"에 머무르므로 최우선으로 진행한다. 이후 B-2(보안)와 B-3(관측 가능성)을 병행하고, B-4~B-6은 그 다음이다.

## 관련 문서

- [quality-improvement-plan.md](quality-improvement-plan.md) — 품질(성능/UX/코드 품질) 개선 계획
- [mvp-scope.md](mvp-scope.md) — 완료된 MVP 범위
- [docs/architecture/decisions/](../architecture/decisions/README.md) — 각 Phase 진행 중 발견되는 아키텍처 결정은 여기 ADR로 남긴다
