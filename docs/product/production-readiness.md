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

- [ ] 백엔드 `Dockerfile` (멀티스테이지 빌드, non-root 유저)
- [ ] 프론트엔드 `Dockerfile` (Next.js standalone output)
- [ ] `application-prod.yml` 프로필 신설 — 로컬 전용 기본값(JWT secret 더미값, Toss 테스트 키, `ddl-auto` 등) 중 실서비스에 위험한 것을 프로필 분리로 강제한다
- [ ] 필수 환경변수 검증 — prod 프로필에서 `QUNO_JWT_SECRET` 등 필수값이 비어 있으면 기동 실패하도록(현재는 더미값으로 조용히 기동됨)
- [ ] `.env.example` 작성 — 현재 저장소에 없음, 어떤 환경변수가 필요한지 문서화
- [ ] GitHub Actions CI — PR마다 백엔드(`./gradlew test`)와 프론트엔드(`npm run build`, `npm test`, lint) 자동 실행. 현재 `.github/workflows`가 아예 없어 병합 전 검증이 전적으로 수동임
- [ ] (선택, 배포 대상 확정 후) CD 파이프라인 — 사람이 클라우드 계정/대상을 정한 뒤 착수

### B-2. 보안 강화

- [ ] CORS 허용 오리진을 환경변수화 — `SecurityConfig.kt`에 `http://localhost:3000`이 하드코딩돼 있어 실 도메인을 못 받음
- [ ] 보안 헤더 추가 — HSTS, `X-Content-Type-Options`, `Referrer-Policy` 등 (Spring Security `headers {}` DSL)
- [ ] Rate limiting 도입 — 로그인/회원가입/결제 확인 등 민감 엔드포인트에 무차별 대입·남용 방지 (현재 전무)
- [ ] 의존성 취약점 스캔 자동화 — `./gradlew dependencyCheckAnalyze` 또는 GitHub Dependabot, `npm audit` CI 연동
- [ ] Actuator 프로덕션 노출 최소화 — 현재 `health,info`만 노출되고 있어 기본은 안전하나, prod에서 `/actuator/health` 상세 정보(`show-details`)가 인증 없이 노출되지 않는지 재확인
- [ ] 시크릿 하드코딩 감사 — `application.yml`의 JWT/Toss 기본값이 실수로 prod에 그대로 쓰이지 않도록 B-1의 필수값 검증과 연계

### B-3. 관측 가능성(Observability)

- [ ] 구조화된 JSON 로깅 — 현재 콘솔 로그가 사람이 읽기 좋은 포맷뿐, 로그 수집기(예: CloudWatch/Loki) 연동을 전제로 JSON 인코더 추가
- [ ] 요청 추적 ID(Correlation ID) — 요청마다 ID를 부여해 로그를 관통 추적 가능하게
- [ ] 에러 트래킹 SDK 통합 — Sentry 등 SDK를 코드에 심는 것까지는 가능(계정 생성은 A 항목). DSN이 없으면 비활성화되도록 설계
- [ ] Prometheus 메트릭 노출 — Micrometer는 이미 의존성에 있음(HikariCP/MongoDB 메트릭 로그로 확인됨), `/actuator/prometheus` 활성화만 남음
- [ ] 프론트엔드 에러 바운더리 강화 — 현재 페이지별 에러 처리 상태 재점검, 전역 에러 리포팅 연동

### B-4. 신뢰성 / 데이터

- [ ] DB 백업 전략 스크립트화 — `pg_dump` 주기 백업 + 복구 리허설 스크립트(실행 인프라는 A 항목이지만 스크립트 자체는 코드)
- [ ] 외부 API(Toss 결제, 메일 발송) 재시도·타임아웃 정책 명시 — 현재 `TossPaymentGateway`가 실패 시 그대로 예외 전파하는지 점검
- [ ] N+1 쿼리 점검 — 목록성 API(질문 목록, 태그 목록 등) 대상 QueryDSL/Hibernate 통계로 확인
- [ ] 회원 탈퇴 시 개인정보 삭제/익명화 로직 점검 — 현재 탈퇴 기능 자체가 있는지부터 확인 필요(없으면 개인정보보호법 대응상 신규 구현 대상)
- [ ] 개인정보 다운로드/삭제 요청 API — 최소한 관리자가 수동 처리할 수 있는 내부 도구 수준까지

### B-5. 테스트

- [ ] 프론트엔드 테스트 커버리지 확충 — 현재 `frontend/src`에 테스트 파일이 1개뿐, 백엔드(124개)와 극단적으로 불균형
- [ ] E2E 테스트 도입(Playwright) — 회원가입→질문 작성→답변→결제(Direct Ask) 같은 핵심 플로우
- [ ] 부하 테스트 — k6/Gatling으로 목표 동시접속 규모에서 응답시간·에러율 측정, 실행은 스테이징 인프라가 있어야 의미 있음(A 항목 선행)

### B-6. 문서화

- [ ] OpenAPI/Swagger 자동 생성 — 현재 `docs/architecture/api-design.md`가 수기 문서, springdoc-openapi 도입으로 코드-문서 drift 방지
- [ ] 운영 런북(Runbook) 작성 — 장애 대응 절차, 롤백 절차, 온콜 체크리스트

## 진행 순서 제안

B-1(배포 파이프라인)이 없으면 나머지 전부가 "로컬에서만 검증된 코드"에 머무르므로 최우선으로 진행한다. 이후 B-2(보안)와 B-3(관측 가능성)을 병행하고, B-4~B-6은 그 다음이다.

## 관련 문서

- [quality-improvement-plan.md](quality-improvement-plan.md) — 품질(성능/UX/코드 품질) 개선 계획
- [mvp-scope.md](mvp-scope.md) — 완료된 MVP 범위
- [docs/architecture/decisions/](../architecture/decisions/README.md) — 각 Phase 진행 중 발견되는 아키텍처 결정은 여기 ADR로 남긴다
