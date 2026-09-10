# Quno 운영 런북

> 이 문서는 실제 배포 대상(클라우드/호스팅)이 아직 정해지지 않은 상태에서 작성됐다([production-readiness.md](../product/production-readiness.md) A 항목이 사람이 할 일로 남겨둔 부분). 여기 적힌 도구·엔드포인트·스크립트는 전부 이 저장소에 실제로 존재하는 것들이고, 클라우드 서비스 이름이 필요한 부분(예: "로드밸런서 설정 변경")은 배포 대상이 정해진 뒤 이 문서에 구체적인 절차로 채워 넣어야 한다.

## 1. 평소에 알아둘 것

### 상태 확인 엔드포인트

| 용도 | 엔드포인트 | 비고 |
|---|---|---|
| 헬스체크 | `GET /actuator/health` | 인증 불필요. DB/Redis/Mongo/메일 각 컴포넌트 상태 포함 |
| 메트릭 | `GET /actuator/prometheus` | 인증 불필요(ADR-0045) — 실제 배포 시 인프라 레벨(내부망/리버스 프록시)에서 외부 접근을 막아야 함 |
| API 문서 | `GET /swagger-ui/index.html` | 인증 불필요(ADR-0048) |
| 에러 트래킹 | Sentry 프로젝트(DSN 설정 시) | `SENTRY_DSN` 미설정이면 비활성화 상태 — [ADR-0045](../architecture/decisions/0045-observability-logging-metrics-error-tracking.md) |
| 로그 | 구조화 JSON(ECS 포맷, prod 프로필만) | 요청마다 `X-Request-Id`가 `mdc.requestId`로 실려 있어 로그 관통 추적 가능 |

### 로그에서 특정 요청 추적하기

1. 사용자가 보고한 시각과 증상(브라우저 콘솔의 `X-Request-Id` 응답 헤더가 있으면 그 값)을 확보한다.
2. 로그 수집기(CloudWatch/Loki 등, 실제 연동은 인프라 구성 시점에 결정)에서 `mdc.requestId:"<값>"`으로 검색하면 그 요청과 관련된 모든 로그 라인을 관통 추적할 수 있다.
3. request id를 모르면 `service.name:"qunobackend"` + 시각 범위로 좁힌 뒤 에러 레벨(`log.level:"ERROR"`)로 필터링한다.

## 2. 장애 대응 절차

### 2.1 트리아지 (첫 5분)

1. `GET /actuator/health` 응답을 확인한다 — 어느 컴포넌트가 `DOWN`인지 본다(`db`/`redis`/`mongo`/`mail`/`diskSpace` 등).
2. Sentry(설정돼 있다면)에서 최근 이슈 발생 빈도가 급증했는지 확인한다 — `GlobalExceptionHandler`의 catch-all(ADR-0045/0046)이 예기치 못한 예외를 전부 캡처하므로, 새로운 유형의 에러가 여기 나타난다.
3. `/actuator/prometheus`에서 확인할 만한 신호: 요청 실패율 급증, 응답 지연 증가, DB 커넥션 풀 고갈(HikariCP 메트릭).
4. 배포 직후 발생했다면 먼저 최근 배포가 원인인지 의심한다 → 2.3 롤백 절차로.

### 2.2 컴포넌트별 흔한 원인

| 증상 | 흔한 원인 | 확인 방법 |
|---|---|---|
| `db: DOWN` | Postgres 연결 불가/커넥션 풀 고갈 | DB 자체 상태, HikariCP 메트릭(`hikaricp_connections_active`) |
| `mongo: DOWN` | Mongo 연결 불가/인증 실패 | Live Chat 메시지 저장이 영향받음(다른 기능은 정상) |
| `redis: DOWN` | Redis 연결 불가 | Live Chat 접속자 표시 등에 영향, 인증/일반 API는 영향 없음(Redis는 부가 기능에만 쓰임) |
| `mail: DOWN` | SMTP 서버 응답 없음 | Verified Organization 이메일 인증만 영향받음(재시도 로직 있음, ADR-0046) |
| 로그인/회원가입 다수 실패 | Rate limiting(ADR-0046) 오탐 — 특정 IP가 정상 사용자인데 막힘 | `quno.rate-limit.*` 설정값 확인, 필요시 일시 상향 후 재배포 |
| Direct Ask 결제 확인 실패 급증 | 토스페이먼츠 장애 또는 시크릿 키 문제 | Toss 상태 페이지 확인, `QUNO_TOSS_SECRET_KEY` 환경변수 확인(ADR-0037) |

### 2.3 롤백 절차

CD 파이프라인은 아직 없다(production-readiness.md B-1, 배포 대상 확정 후 착수 예정) — 현재는 수동 롤백을 전제로 한다.

1. **컨테이너 이미지 롤백**: 이전 커밋의 이미지 태그로 재배포한다. `backend/Dockerfile`/`frontend/Dockerfile` 둘 다 태그 없이 매 빌드가 독립적이므로, 배포 시스템이 커밋 SHA 또는 버전 태그로 이미지를 관리하고 있어야 한다(인프라 구성 시 반드시 확립할 것 — 이 문서 갱신 필요).
2. **DB 마이그레이션이 얽힌 경우**: Flyway는 전진만 하고 자동 롤백을 지원하지 않는다. 새 마이그레이션이 문제라면:
   - 마이그레이션이 컬럼 추가/nullable 변경처럼 하위 호환되는 변경이라면, 애플리케이션 코드만 이전 버전으로 롤백해도 대개 안전하다(컬럼이 남아있어도 이전 코드가 무시함).
   - 마이그레이션이 파괴적(컬럼 삭제, NOT NULL 강제 등)이라면 애플리케이션만 롤백해서는 안 되고, [scripts/db-restore.sh](../../scripts/db-restore.sh)로 직전 백업을 복구해야 한다 — 이 경우 롤백 시점 이후 쓰기가 유실되므로 신중히 판단한다.
3. **롤백 후 확인**: `/actuator/health`가 전부 `UP`인지, 최근 배포로 인한 Sentry 이슈가 더 이상 새로 생기지 않는지 확인한다.

### 2.4 심각한 상황: DB 전체 복구가 필요한 경우

1. [scripts/db-backup.sh](../../scripts/db-backup.sh)/[scripts/db-restore.sh](../../scripts/db-restore.sh)를 사용한다 — 둘 다 `PGHOST` 등 표준 libpq 환경변수로 원격 DB를 가리킬 수 있다.
2. 복구 전 반드시 현재 상태도 백업해둔다(`./scripts/db-backup.sh`) — 복구가 잘못된 판단이었을 경우를 대비.
3. 복구는 되돌릴 수 없는 작업이다 — `db-restore.sh`가 확인 프롬프트(`yes` 입력)를 요구하므로 실수로 실행되지 않는다.

## 3. 온콜 체크리스트

### 알림을 받았을 때

- [ ] 알림 내용(어느 컴포넌트, 어떤 지표)을 정확히 확인했는가
- [ ] `/actuator/health` 상태를 직접 확인했는가
- [ ] Sentry에 새로 급증한 이슈가 있는지 확인했는가
- [ ] 최근 1시간 내 배포가 있었는지 확인했는가(있다면 롤백부터 고려)
- [ ] 영향 범위(전체 장애 vs 특정 기능만)를 파악했는가 — 위 2.2 표 참고

### 조치 후

- [ ] 조치 내용과 타임라인을 기록했는가(사후 분석용)
- [ ] 임시 조치(예: rate limit 상향)를 했다면 원상복구 계획을 세웠는가
- [ ] 재발 방지가 필요한 근본 원인이면 ADR 또는 이슈로 남겼는가

### 정기 점검(주간 권장)

- [ ] DB 백업이 실제로 실행되고 있는가([scripts/db-backup.sh](../../scripts/db-backup.sh) 스케줄 확인 — 스케줄링 자체는 인프라 구성의 몫)
- [ ] 백업으로부터 실제 복구가 되는지 리허설했는가(마지막 리허설: 2026-09-10, Phase 35 도입 시)
- [ ] Dependabot이 올린 의존성 업데이트 PR이 쌓여있지 않은가
- [ ] `/actuator/prometheus`가 외부에 그대로 노출돼 있지 않은지(인프라 레벨 접근 통제 확인)

## 관련 문서

- [production-readiness.md](../product/production-readiness.md)
- [0045-observability-logging-metrics-error-tracking.md](../architecture/decisions/0045-observability-logging-metrics-error-tracking.md)
- [0046-reliability-account-withdrawal-data-export-n-plus-1.md](../architecture/decisions/0046-reliability-account-withdrawal-data-export-n-plus-1.md)
