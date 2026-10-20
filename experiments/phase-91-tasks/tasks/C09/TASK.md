# 과제

외부 채널에서 받은 주문 묶음을 저장하는 `OrderImportService`를 만들었습니다. 묶음 안에 잘못된 주문이 있으면 그 묶음은 하나도 저장되지 않아야 하는데, `OrderImportServiceTests`에서 이 경우가 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전은 4.0.8로 고정되어 있습니다. 올리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - 잘못된 주문(수량 0 이하)을 검증하는 시점과 `IllegalArgumentException`을 던지는 동작은 그대로 두세요.
  - `importBatch`의 시그니처(호출 방식)는 바꾸지 마세요.
