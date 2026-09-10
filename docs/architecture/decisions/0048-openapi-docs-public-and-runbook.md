# ADR-0048: OpenAPI 문서 자동 생성(공개)과 운영 런북

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

상용 전환 체크리스트([production-readiness.md](../../product/production-readiness.md) B-6, 마지막 항목)에 따라 문서화를 진행한다. 지금까지 `docs/architecture/api-design.md`는 사람이 손으로 갱신하는 문서라 실제 컨트롤러와 어긋날 위험이 있었고, 장애 발생 시 참고할 운영 문서도 없었다.

## 결정 (Decision)

1. **springdoc-openapi로 API 문서를 컨트롤러에서 자동 생성한다.** `springdoc-openapi-starter-webmvc-ui:3.1.1`(3.x 라인 — Spring Boot 4/Spring Framework 7 지원)을 추가했다. Sentry Spring Boot Starter(ADR-0045)와 달리 이번엔 별다른 호환성 문제 없이 바로 동작했다 — 실제로 `/v3/api-docs`가 컨트롤러 74개 경로를 정확히 인식하는 것을 확인했다. `OpenApiConfig`가 제목/설명과 JWT Bearer 인증 스킴(Swagger UI의 "Authorize" 버튼으로 토큰을 넣고 인증 필요한 엔드포인트를 바로 호출해볼 수 있게)을 추가한다.
2. **`/v3/api-docs`와 `/swagger-ui/**`는 인증 없이 공개한다.** 공개 질문/태그/조직/프로필(ADR-0041/0042)과 같은 원칙 — API 문서 자체는 민감 정보가 아니라 개발자 참고 자료다. Prometheus(ADR-0045)와 달리 이건 "공개해도 되는 정보"이지 "공개하면 안 되지만 인증을 걸 수 없는 정보"가 아니다.
3. **운영 런북은 실제 배포 대상이 정해지지 않은 상태로 작성한다.** `docs/operations/runbook.md`가 이 저장소에 실제로 존재하는 도구(헬스체크/Prometheus/Sentry/구조화 로그의 요청 추적 ID/DB 백업·복구 스크립트)를 기준으로 트리아지·컴포넌트별 흔한 원인·롤백 절차·온콜 체크리스트를 정리한다. 클라우드 서비스 이름이 필요한 부분(로드밸런서 설정 변경 등)은 배포 대상이 정해진 뒤 채워 넣도록 명시적으로 비워뒀다 — 지금 임의의 클라우드를 가정해서 쓰면 실제 배포 시점에 다시 쓰게 될 가능성이 높다고 판단했다.
4. **롤백 절차는 CD 파이프라인이 아직 없다는 전제로 쓴다.** production-readiness.md B-1이 CD를 "배포 대상 확정 후" 항목으로 이미 보류해뒀으므로, 런북의 롤백 절차는 수동 이미지 재배포와 Flyway 마이그레이션의 전진 전용 특성(파괴적 마이그레이션은 애플리케이션만 롤백해서는 안 되고 DB 복구가 필요할 수 있음)을 명시하는 데 그친다.

## 결과 (Consequences)

- `api-design.md`(수기 문서)와 `/v3/api-docs`(자동 생성) 둘 다 남는다 — 전자는 설계 의도와 배경 설명, 후자는 정확한 현재 스키마. 서로 대체 관계가 아니라 보완 관계로 유지한다.
- 실제 배포 대상이 정해지면 런북의 "인프라 구성 시 확립할 것"으로 표시해둔 부분(이미지 태그 관리, 로그 수집기 연동, 백업 스케줄링)을 채워 넣어야 한다 — 지금은 자리만 잡아뒀다.
- Swagger UI가 공개돼 있으므로, 앞으로 새 엔드포인트를 추가할 때 요청/응답 DTO에 민감한 필드명이나 내부 구현 세부사항이 그대로 노출된다는 것을 염두에 둬야 한다.

## 관련 문서

- [production-readiness.md](../../product/production-readiness.md) B-6
- [docs/operations/runbook.md](../../operations/runbook.md)
- [0045-observability-logging-metrics-error-tracking.md](0045-observability-logging-metrics-error-tracking.md)
- [PLAN.md](../../../PLAN.md) Phase 37
