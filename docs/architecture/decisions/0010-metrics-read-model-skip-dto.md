# ADR-0010: 성공 지표 스냅샷은 순수 읽기 모델로 취급해 계층별 DTO 복제를 생략

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

Phase 4.1에서 mvp-scope.md의 성공 지표(Revision Rate, Ward Adoption 등) 중 백엔드 데이터만으로 계산 가능한 것들을 계측해야 했다. 이 프로젝트는 지금까지 모든 기능에서 `domain` → `application dto` → `interfaces response`로 이어지는 3단 DTO 체인을 일관되게 지켜왔다. 지표 스냅샷도 같은 원칙을 따를지, 예외를 둘지 정해야 했다.

## 결정 (Decision)

`MetricsSnapshot`(`domain/metrics`)은 도메인 불변조건이 없는 순수 조회 모델이므로, application/interfaces 계층에서 별도 DTO로 복제하지 않고 그대로 재사용한다. `GetMetricsSnapshotUseCase`는 `MetricsRepository.snapshot()` 결과를 그대로 반환하고, `MetricsController`도 이를 그대로 직렬화한다.

이 프로젝트의 원칙("작업 시 필요 이상으로 추상화를 추가하지 않는다")에 따라, 다른 기능들과 다르게 이 경우만 DTO 복제를 생략하기로 명시적으로 결정하고 그 이유를 문서에 남긴다 — 나중에 "왜 여기만 다르지"라는 의문이 생기지 않도록 하기 위함이다.

## 결과 (Consequences)

- 코드가 짧아지고, 필드를 추가/변경할 때 세 곳을 동시에 고칠 필요가 없다.
- 반대로 이 모델에 나중에 도메인 로직이나 불변조건이 생기면(예: 특정 지표를 특정 사용자에게만 보여줘야 한다는 규칙), 그 시점에는 이 예외를 재검토하고 일반적인 3단 DTO 체인으로 되돌려야 한다.
- 이 패턴을 다른 기능에 무분별하게 확장하지 않는다 — 이건 "이 모델이 진짜로 도메인 불변조건이 없는 순수 리포팅 값"일 때만 적용하는 예외다.

## 관련 문서

- [api-design.md](../api-design.md#지표-계측-phase-41)
- [PLAN.md](../../../PLAN.md) Phase 4.1
- 커밋 `a60c220` (MVP 성공 지표 계측 API/스케줄러 추가)
