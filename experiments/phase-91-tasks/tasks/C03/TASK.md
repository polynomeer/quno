# 과제

결제 내역을 JPA로 저장·조회하는 코드를 만들었습니다. `PaymentRepositoryTests`를 돌리면 저장한 내역을 다시 조회하는 단계에서 테스트가 실패합니다. 원인을 찾아 고쳐 주세요.

- 성공 기준: `./CHECK`가 종료 코드 0으로 끝납니다(`./gradlew test` 통과).
- 제약:
  - Spring Boot 버전과 Kotlin 버전(2.2.21)은 고정되어 있습니다. 올리거나 내리지 마세요.
  - 테스트 코드는 바꾸지 마세요.
  - `Payment`의 `orderNo`, `amount`, `method`는 불변(`val`)으로 유지하세요.
