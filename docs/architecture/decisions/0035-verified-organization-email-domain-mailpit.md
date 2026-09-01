# ADR-0035: Verified Organization을 업무/학교 이메일 도메인 인증으로 구현하고, 로컬 개발은 Mailpit으로 검증한다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md)는 외부 신원 인증 인프라가 없다는 이유로 Verified Organization(실제 회사·학교 소속 인증)을 범위 밖에 두고 `PLAN.md`에 남겨뒀다. Phase 22가 구현한 Virtual/Community Organization은 완전히 자기 신고형이라 "소속"이라는 사회적 신뢰를 실제로 담보하지 못한다.

이 프로젝트에는 이메일 발송 인프라가 전혀 없다 — `SignUpUseCase`도 가입 시점에 이메일 소유를 검증하지 않는다(확인 링크 없이 즉시 가입 완료). "이메일 도메인 인증"을 실제로 구현하려면 (1) 실제로 이메일을 보내는 메커니즘과 (2) 그 발송을 로컬 개발 환경에서 실제 왕복으로 검증할 방법이 모두 필요하다.

## 결정 (Decision)

1. **업무/학교 이메일 도메인 매칭으로 Verified Organization을 구현한다.** 사용자가 회사/학교 이메일을 제출하면(`POST /organizations/verify-email`) 6자리 코드를 생성해 그 주소로 발송하고, 사용자가 코드를 입력해 확인하면(`POST /organizations/verify-email/confirm`) 해당 이메일의 도메인으로 Organization을 find-or-create해 자동으로 멤버가 된다. `gmail.com`·`naver.com` 같은 공개 웹메일 도메인은 하드코딩된 차단 목록(`PublicEmailDomains`)으로 거부한다 — 누구나 등록 가능한 도메인은 조직 소속을 증명하지 못한다(Cluster의 "자동 탐색 대신 명시적 큐레이션"과 같은 철학).
2. **Verified 조직의 멤버십은 오직 이메일 인증을 통해서만 부여된다.** 기존 `POST /organizations/{id}/join`은 대상이 Verified 조직이면 거부한다(`VerifiedOrganizationJoinRequiresEmailException`) — 그렇지 않으면 누구나 기존 join 엔드포인트로 검증을 완전히 우회할 수 있어 이 기능 전체가 무의미해진다.
3. **동일 도메인 이름의 Virtual 조직이 이미 존재하면, 새로 만들지 않고 그 조직을 승격(upgrade)한다.** 예: 누군가 "google.com"이라는 이름으로 Virtual 조직을 먼저 만들었는데, 나중에 실제 google.com 이메일 소유자가 인증하면 그 기존 조직이 `emailDomain`을 얻어 Verified로 전환된다(같은 `id` 유지, 기존 멤버 보존). 이름이 충돌한 채 두 개의 혼란스러운 조직이 생기는 것보다 낫다고 판단했다.
4. **로컬 개발/테스트는 `docker-compose`에 추가한 Mailpit(SMTP 캐처)으로 검증한다.** 실제 프로덕션 이메일 제공자(SMTP 릴레이나 SendGrid/SES 같은 API)는 이 코드베이스가 구성하지 않는다 — `spring.mail.*`는 `application-local.yml`에서 로컬 Mailpit(포트 1026)만 가리키고, 프로덕션 자격증명은 배포 시점에 환경변수/시크릿으로 별도 주입해야 한다(JWT secret이 이미 이 패턴을 쓰고 있다). endoflife.date([ADR-0033](0033-technology-version-scan-detection-only-no-auto-outdated.md))와 달리, 이 기능은 실제 운영에서 별도 자격증명 준비가 필요한 진짜 갭이다.

## 결과 (Consequences)

- 실제 SMTP 왕복(코드 생성 → 발송 → Mailpit 수신 확인 → 코드 입력 → 조직 생성/합류)을 curl로 전 구간 검증했다 — 목업이 아니라 진짜 이메일 프로토콜이 동작한다.
- 인증 코드 만료는 5분/15분 같은 TTL을 저장된 `expiresAt`과 비교하는 방식으로 계산한다(Phase 21의 QunoBot 스캔이 배경 작업 대신 조회 시점 계산을 선호한 것과 같은 방향) — 만료된 요청을 정리하는 배치 작업은 없다(불필요, 행이 그냥 남아있어도 무해).
- 재요청은 이전 코드를 명시적으로 무효화하지 않는다 — `findLatestByUserId`가 항상 최신 요청만 보므로 이전 코드는 자연히 도달 불가능해진다.
- 속도 제한(레이트 리밋)은 두지 않았다 — `api-design.md`의 "입력 검증 공통 원칙"이 이미 프로젝트 전역에 레이트 리밋을 "검토 필요"로만 남겨둔 것과 같은 상태를 그대로 물려받는다. 실사용에서 남용(임의 도메인에 스팸성 인증 코드 발송 등)이 확인되면 재검토한다.
- Verified 배지의 신뢰 수준은 "이 도메인 이메일을 받을 수 있는 사람"까지다 — 그 사람이 실제로 그 회사/학교 소속인지(예: 프리랜서가 임시로 받은 이메일)까지는 검증하지 않는다. 실사용에서 문제가 되면 재검토한다.

## 관련 문서

- [domain-model.md](../domain-model.md#trust-network-phase-22)
- [api-design.md](../api-design.md#verified-organization-phase-23)
- [ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md) (이번 ADR이 채워 넣는 범위 밖 결정)
- [ADR-0033](0033-technology-version-scan-detection-only-no-auto-outdated.md) (로컬/프로덕션 갭을 문서화하는 유사 선례, 다만 이번은 진짜 자격증명 갭이라는 점이 다름)
- [PLAN.md](../../../PLAN.md) Phase 23
